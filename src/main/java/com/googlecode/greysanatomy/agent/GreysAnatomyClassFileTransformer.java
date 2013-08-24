package com.googlecode.greysanatomy.agent;

import static com.googlecode.greysanatomy.probe.ProbeJobs.register;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.console.command.Command.Info;
import com.googlecode.greysanatomy.probe.JobListener;
import com.googlecode.greysanatomy.probe.Probes;
import com.googlecode.greysanatomy.util.GaReflectUtils;

public class GreysAnatomyClassFileTransformer implements ClassFileTransformer {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

	private final String perfClzRegex;
	private final String perfMthRegex;
	private final String id;
	private final List<CtBehavior> modifiedBehaviors;

	/*
	 * 对之前做的类进行一个缓存
	 */
	private final static Map<String,byte[]> classBytesCache = new ConcurrentHashMap<String,byte[]>();
	
	private GreysAnatomyClassFileTransformer(
			final String perfClzRegex,
			final String perfMthRegex, 
			final JobListener listener, 
			final List<CtBehavior> modifiedBehaviors,
			final Info info) {
		this.perfClzRegex = perfClzRegex;
		this.perfMthRegex = perfMthRegex;
		this.modifiedBehaviors = modifiedBehaviors;
		this.id = info.getJobId();
		register(this.id, listener);
	}

	@Override
	public byte[] transform(final ClassLoader loader, String classNameForFilepath,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer)
			throws IllegalClassFormatException {
		
		final String className = GaReflectUtils.toClassPath(classNameForFilepath);
		if( !className.matches(perfClzRegex)) {
			return null;
		}
		
		// 这里做一个并发控制，防止两边并发对类进行编译，影响缓存
		synchronized (classBytesCache) {
			final ClassPool cp = new ClassPool(null);
			cp.insertClassPath(new LoaderClassPath(loader));
			if( classBytesCache.containsKey(className) ) {
				cp.insertClassPath(new ByteArrayClassPath(className, classBytesCache.get(className)));
			}
			
			CtClass cc = null;
			byte[] datas;
			try {
				cc = cp.getCtClass(className);
				cc.defrost();
				
				final CtBehavior[] cbs = cc.getDeclaredBehaviors();
				if( null != cbs ) {
					for( CtBehavior cb : cbs ) {
						if( cb.getMethodInfo().getName().matches(perfMthRegex) ) {
							modifiedBehaviors.add(cb);
							Probes.mine(id, loader, cc, cb);
						}
					}
				}
				
				datas = cc.toBytecode();
			} catch (Exception e) {
				logger.warn("transform {} failed!", className, e);
				datas = null;
			} finally {
				if( null != cc ) {
					cc.freeze();
				}
			}
			
			classBytesCache.put(className, datas);
			return datas;
		}
		
	}
	
	
	/**
	 * 渲染结果
	 * @author vlinux
	 *
	 */
	public static class TransformResult {
		
		private final String id;
		private final List<Class<?>> modifiedClasses;
		private final List<CtBehavior> modifiedBehaviors;
		
		private TransformResult(String id, final List<Class<?>> modifiedClasses, final List<CtBehavior> modifiedBehaviors) {
			this.id = id;
			this.modifiedClasses = new ArrayList<Class<?>>(modifiedClasses);
			this.modifiedBehaviors = new ArrayList<CtBehavior>(modifiedBehaviors);
		}

		public List<Class<?>> getModifiedClasses() {
			return modifiedClasses;
		}
		
		public List<CtBehavior> getModifiedBehaviors() {
			return modifiedBehaviors;
		}

		public String getId() {
			return id;
		}
		
	}

	/**
	 * 对类进行形变
	 * @param instrumentation
	 * @param perfClzRegex
	 * @param perfMthRegex
	 * @param listener
	 * @return
	 * @throws UnmodifiableClassException
	 */
	public static TransformResult transform(final Instrumentation instrumentation, 
			final String perfClzRegex, 
			final String perfMthRegex, 
			final JobListener listener,
			final Info info) throws UnmodifiableClassException {
		
		final List<CtBehavior> modifiedBehaviors = new ArrayList<CtBehavior>();
		GreysAnatomyClassFileTransformer jcft = new GreysAnatomyClassFileTransformer(perfClzRegex, perfMthRegex, listener, modifiedBehaviors, info);
		instrumentation.addTransformer(jcft,true);
		final List<Class<?>> modifiedClasses = new ArrayList<Class<?>>();
		for( Class<?> clazz : instrumentation.getAllLoadedClasses() ) {
			if( clazz.getName().matches(perfClzRegex) ) {
				modifiedClasses.add(clazz);
			}
		}
		try {
			synchronized (GreysAnatomyClassFileTransformer.class) {
				
//				int retransformCounter = 0;
//				final List<Class<?>> subModifiedClasses = new ArrayList<Class<?>>();
				for( Class<?> clazz : modifiedClasses ) {
//					subModifiedClasses.add(clazz);
//					if( retransformCounter++ == 10 ) {
//						instrumentation.retransformClasses(subModifiedClasses.toArray(new Class[0]));
//						subModifiedClasses.clear();
//						retransformCounter = 0;
//					}
//				}//for
//				if( !subModifiedClasses.isEmpty() ) {
//					instrumentation.retransformClasses(subModifiedClasses.toArray(new Class[0]));
					try {
						instrumentation.retransformClasses(clazz);
					}catch(Throwable t) {
						logger.warn("retransform class {} failed.", clazz, t);
					}
					
				}
			}
		}finally {
			instrumentation.removeTransformer(jcft);
		}
		
		return new TransformResult(jcft.id, modifiedClasses, modifiedBehaviors);
		
	}
	
	
}

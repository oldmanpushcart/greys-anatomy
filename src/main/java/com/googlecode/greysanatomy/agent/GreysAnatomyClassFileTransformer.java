package com.googlecode.greysanatomy.agent;

import static com.googlecode.greysanatomy.probe.ProbeJobs.createJob;
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

import com.googlecode.greysanatomy.probe.ProbeListener;
import com.googlecode.greysanatomy.probe.Probes;
import com.googlecode.greysanatomy.util.GaReflectUtils;

public class GreysAnatomyClassFileTransformer implements ClassFileTransformer {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

	private final String perfClzRegex;
	private final String perfMthRegex;
	private final int id;
	private final List<CtBehavior> modifiedBehaviors;

	/*
	 * 对之前做的类进行一个缓存
	 */
	private final static Map<String,byte[]> classBytesCache = new ConcurrentHashMap<String,byte[]>();
	
	private GreysAnatomyClassFileTransformer(
			final String perfClzRegex,
			final String perfMthRegex, 
			final ProbeListener listener, 
			final List<CtBehavior> modifiedBehaviors) {
		this.perfClzRegex = perfClzRegex;
		this.perfMthRegex = perfMthRegex;
		this.modifiedBehaviors = modifiedBehaviors;
		register(id = createJob(), listener);
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
		
		private final int id;
		private final List<Class<?>> modifiedClasses;
		private final List<CtBehavior> modifiedBehaviors;
		
		private TransformResult(int id, final List<Class<?>> modifiedClasses, final List<CtBehavior> modifiedBehaviors) {
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

		public int getId() {
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
			final ProbeListener listener) throws UnmodifiableClassException {
		
		final List<CtBehavior> modifiedBehaviors = new ArrayList<CtBehavior>();
		GreysAnatomyClassFileTransformer jcft = new GreysAnatomyClassFileTransformer(perfClzRegex, perfMthRegex, listener, modifiedBehaviors);
		instrumentation.addTransformer(jcft,true);
		final List<Class<?>> modifiedClasses = new ArrayList<Class<?>>();
		for( Class<?> clazz : instrumentation.getAllLoadedClasses() ) {
			if( clazz.getName().matches(perfClzRegex) ) {
				modifiedClasses.add(clazz);
			}
		}
		try {
			synchronized (GreysAnatomyClassFileTransformer.class) {
				instrumentation.retransformClasses(modifiedClasses.toArray(new Class[0]));
			}
		}finally {
			instrumentation.removeTransformer(jcft);
		}
		
		return new TransformResult(jcft.id, modifiedClasses, modifiedBehaviors);
		
	}
	
	
}

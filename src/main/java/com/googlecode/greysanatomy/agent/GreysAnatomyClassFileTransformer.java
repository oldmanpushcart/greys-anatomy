package com.googlecode.greysanatomy.agent;

import com.googlecode.greysanatomy.console.command.Command.Info;
import com.googlecode.greysanatomy.probe.JobListener;
import com.googlecode.greysanatomy.probe.Probes;
import com.googlecode.greysanatomy.util.GaReflectUtils;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.greysanatomy.probe.ProbeJobs.register;

public class GreysAnatomyClassFileTransformer implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private final String prefClzRegex;
    private final String prefMthRegex;
    private final String id;
    private final List<CtBehavior> modifiedBehaviors;

    /*
     * 对之前做的类进行一个缓存
     */
    private final static Map<String, byte[]> classBytesCache = new ConcurrentHashMap<String, byte[]>();

    private GreysAnatomyClassFileTransformer(
            final String prefClzRegex,
            final String prefMthRegex,
            final JobListener listener,
            final List<CtBehavior> modifiedBehaviors,
            final Info info) {
        this.prefClzRegex = prefClzRegex;
        this.prefMthRegex = prefMthRegex;
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
        if (!className.matches(prefClzRegex)) {
            return null;
        }

        // 这里做一个并发控制，防止两边并发对类进行编译，影响缓存
        synchronized (classBytesCache) {
            final ClassPool cp = new ClassPool(null);
            cp.insertClassPath(new LoaderClassPath(loader));
            if (classBytesCache.containsKey(className)) {
                cp.insertClassPath(new ByteArrayClassPath(className, classBytesCache.get(className)));
            }

            CtClass cc = null;
            byte[] data;
            try {
                cc = cp.getCtClass(className);
                cc.defrost();

                final CtBehavior[] cbs = cc.getDeclaredBehaviors();
                if (null != cbs) {
                    for (CtBehavior cb : cbs) {
                        if (cb.getMethodInfo().getName().matches(prefMthRegex)) {
                            modifiedBehaviors.add(cb);
                            Probes.mine(id, cc, cb);
                        }
                    }
                }

                data = cc.toBytecode();
            } catch (Exception e) {
                logger.warn("transform {} failed!", className, e);
                data = null;
            } finally {
                if (null != cc) {
                    cc.freeze();
                }
            }

            classBytesCache.put(className, data);
            return data;
        }

    }


    /**
     * 渲染结果
     *
     * @author vlinux
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
     *
     * @param instrumentation
     * @param prefClzRegex
     * @param prefMthRegex
     * @param listener
     * @return
     * @throws UnmodifiableClassException
     */
    public static TransformResult transform(final Instrumentation instrumentation,
                                            final String prefClzRegex,
                                            final String prefMthRegex,
                                            final JobListener listener,
                                            final Info info) throws UnmodifiableClassException {

        final List<CtBehavior> modifiedBehaviors = new ArrayList<CtBehavior>();
        GreysAnatomyClassFileTransformer jcft = new GreysAnatomyClassFileTransformer(prefClzRegex, prefMthRegex, listener, modifiedBehaviors, info);
        instrumentation.addTransformer(jcft, true);
        final List<Class<?>> modifiedClasses = new ArrayList<Class<?>>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().matches(prefClzRegex)) {
                modifiedClasses.add(clazz);
            }
        }
        synchronized (GreysAnatomyClassFileTransformer.class) {
            try {
                for (final Class<?> clazz : modifiedClasses) {
                    try {
                        instrumentation.retransformClasses(clazz);
                    } catch(Throwable t) {
                        logger.warn("transform failed, class={}.", clazz, t);
                    }
                }//for
            } finally {
                instrumentation.removeTransformer(jcft);
            }//try
        }//sycn

        return new TransformResult(jcft.id, modifiedClasses, modifiedBehaviors);

    }


}

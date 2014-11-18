package com.googlecode.greysanatomy.agent;

import com.googlecode.greysanatomy.console.command.Command.Info;
import com.googlecode.greysanatomy.probe.JobListener;
<<<<<<< HEAD
=======
import com.googlecode.greysanatomy.probe.ProbeJobs;
>>>>>>> pr/8
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

<<<<<<< HEAD
    private final String perfClzRegex;
    private final String perfMthRegex;
    private final String id;
=======
    private final String prefClzRegex;
    private final String prefMthRegex;
    private final int id;
>>>>>>> pr/8
    private final List<CtBehavior> modifiedBehaviors;

    /*
     * 对之前做的类进行一个缓存
     */
    private final static Map<String, byte[]> classBytesCache = new ConcurrentHashMap<String, byte[]>();

    private GreysAnatomyClassFileTransformer(
<<<<<<< HEAD
            final String perfClzRegex,
            final String perfMthRegex,
            final JobListener listener,
            final List<CtBehavior> modifiedBehaviors,
            final Info info) {
        this.perfClzRegex = perfClzRegex;
        this.perfMthRegex = perfMthRegex;
=======
            final String prefClzRegex,
            final String prefMthRegex,
            final JobListener listener,
            final List<CtBehavior> modifiedBehaviors,
            final Info info) {
        this.prefClzRegex = prefClzRegex;
        this.prefMthRegex = prefMthRegex;
>>>>>>> pr/8
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
<<<<<<< HEAD
        if (!className.matches(perfClzRegex)) {
=======
        if (!className.matches(prefClzRegex)) {
>>>>>>> pr/8
            return null;
        }

        // 这里做一个并发控制，防止两边并发对类进行编译，影响缓存
        synchronized (classBytesCache) {
            final ClassPool cp = new ClassPool(null);
<<<<<<< HEAD
            cp.insertClassPath(new LoaderClassPath(loader));
            if (classBytesCache.containsKey(className)) {
                cp.insertClassPath(new ByteArrayClassPath(className, classBytesCache.get(className)));
            }

            CtClass cc = null;
            byte[] datas;
=======

            final String cacheKey = className + "@" + loader;
            if (classBytesCache.containsKey(cacheKey)) {
                cp.appendClassPath(new ByteArrayClassPath(className, classBytesCache.get(cacheKey)));
            }

            cp.appendClassPath(new LoaderClassPath(loader));

            CtClass cc = null;
            byte[] data;
>>>>>>> pr/8
            try {
                cc = cp.getCtClass(className);
                cc.defrost();

                final CtBehavior[] cbs = cc.getDeclaredBehaviors();
                if (null != cbs) {
                    for (CtBehavior cb : cbs) {
<<<<<<< HEAD
                        if (cb.getMethodInfo().getName().matches(perfMthRegex)) {
=======
                        if (cb.getMethodInfo().getName().matches(prefMthRegex)) {
>>>>>>> pr/8
                            modifiedBehaviors.add(cb);
                            Probes.mine(id, cc, cb);
                        }
                    }
                }

<<<<<<< HEAD
                datas = cc.toBytecode();
            } catch (Exception e) {
                logger.warn("transform {} failed!", className, e);
                datas = null;
=======
                data = cc.toBytecode();
            } catch (Exception e) {
                logger.debug("transform class failed. class={}, classloader={}", new Object[]{className, loader, e});
                logger.info("transform class failed. class={}, classloader={}", className, loader);
                data = null;
>>>>>>> pr/8
            } finally {
                if (null != cc) {
                    cc.freeze();
                }
            }

<<<<<<< HEAD
            classBytesCache.put(className, datas);
            return datas;
=======
            classBytesCache.put(cacheKey, data);
            return data;
>>>>>>> pr/8
        }

    }


    /**
     * 渲染结果
     *
     * @author vlinux
     */
    public static class TransformResult {

<<<<<<< HEAD
        private final String id;
        private final List<Class<?>> modifiedClasses;
        private final List<CtBehavior> modifiedBehaviors;

        private TransformResult(String id, final List<Class<?>> modifiedClasses, final List<CtBehavior> modifiedBehaviors) {
=======
        private final int id;
        private final List<Class<?>> modifiedClasses;
        private final List<CtBehavior> modifiedBehaviors;

        private TransformResult(int id, final List<Class<?>> modifiedClasses, final List<CtBehavior> modifiedBehaviors) {
>>>>>>> pr/8
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

<<<<<<< HEAD
        public String getId() {
=======
        public int getId() {
>>>>>>> pr/8
            return id;
        }

    }

<<<<<<< HEAD
//    /*
//     * 多线程对类进行形变
//     */
//    private static ExecutorService transformThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
=======
    /**
     * 进度
     */
    public static interface Progress {

        void progress(int index, int total);

    }

    public static TransformResult transform(final Instrumentation instrumentation,
                                            final String prefClzRegex,
                                            final String prefMthRegex,
                                            final JobListener listener,
                                            final Info info) throws UnmodifiableClassException {
        return transform(instrumentation, prefClzRegex, prefMthRegex, listener, info, null);
    }
>>>>>>> pr/8

    /**
     * 对类进行形变
     *
     * @param instrumentation
<<<<<<< HEAD
     * @param perfClzRegex
     * @param perfMthRegex
=======
     * @param prefClzRegex
     * @param prefMthRegex
>>>>>>> pr/8
     * @param listener
     * @return
     * @throws UnmodifiableClassException
     */
    public static TransformResult transform(final Instrumentation instrumentation,
<<<<<<< HEAD
                                            final String perfClzRegex,
                                            final String perfMthRegex,
                                            final JobListener listener,
                                            final Info info) throws UnmodifiableClassException {

        final List<CtBehavior> modifiedBehaviors = new ArrayList<CtBehavior>();
        GreysAnatomyClassFileTransformer jcft = new GreysAnatomyClassFileTransformer(perfClzRegex, perfMthRegex, listener, modifiedBehaviors, info);
        instrumentation.addTransformer(jcft, true);
        final List<Class<?>> modifiedClasses = new ArrayList<Class<?>>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().matches(perfClzRegex)) {
=======
                                            final String prefClzRegex,
                                            final String prefMthRegex,
                                            final JobListener listener,
                                            final Info info,
                                            final Progress progress) throws UnmodifiableClassException {

        final List<CtBehavior> modifiedBehaviors = new ArrayList<CtBehavior>();
        GreysAnatomyClassFileTransformer jcft = new GreysAnatomyClassFileTransformer(prefClzRegex, prefMthRegex, listener, modifiedBehaviors, info);
        instrumentation.addTransformer(jcft, true);
        final List<Class<?>> modifiedClasses = new ArrayList<Class<?>>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz.getName().matches(prefClzRegex)) {
>>>>>>> pr/8
                modifiedClasses.add(clazz);
            }
        }
        synchronized (GreysAnatomyClassFileTransformer.class) {
            try {
<<<<<<< HEAD
                // 需要渲染的信号量(渲染的类个数)
//                final AtomicInteger counter = new AtomicInteger(modifiedClasses.size());
                for (final Class<?> clazz : modifiedClasses) {
//                    transformThreadPool.execute(new Runnable() {
//
//                        @Override
//                        public void run() {
//                            try {
//                                instrumentation.retransformClasses(clazz);
//                            } catch (Throwable t) {
//                                logger.warn("retransform class {} failed.", clazz, t);
//                            } finally {
//                                counter.decrementAndGet();
//                            }
//                        }
//
//                    });
                    instrumentation.retransformClasses(clazz);
                }//for
                // waiting when the counter is 0
//                while (counter.get() > 0) {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        // do nothing...
//                    }
//                }
=======
                int index = 0;
                int total = modifiedClasses.size();
                for (final Class<?> clazz : modifiedClasses) {
                    try {
                        if(ProbeJobs.isJobKilled(info.getJobId()) ) {
                            logger.info("job[id={}] was killed, stop this retransform.",info.getJobId());
                            break;
                        }
                        instrumentation.retransformClasses(clazz);
                    } catch (Throwable t) {
                        logger.warn("transform failed, class={}.", clazz, t);
                    } finally {
                        if (null != progress) {
                            progress.progress(++index, total);
                        }
                    }
                }//for
>>>>>>> pr/8
            } finally {
                instrumentation.removeTransformer(jcft);
            }//try
        }//sycn

        return new TransformResult(jcft.id, modifiedClasses, modifiedBehaviors);

    }


}

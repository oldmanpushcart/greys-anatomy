package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.util.GaStringUtils;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.SearchUtils;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;

import static com.github.ompc.greys.core.util.GaCheckUtils.isEquals;
import static com.github.ompc.greys.core.util.GaReflectUtils.defineClass;
import static com.github.ompc.greys.core.util.GaReflectUtils.getVisibleMethods;
import static java.lang.System.arraycopy;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeStaticMethod;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;


/**
 * 对类进行通知增强
 * Created by oldmanpushcart@gmail.com on 15/5/17.
 */
public class Enhancer implements ClassFileTransformer {

    private static final Logger logger = LogUtil.getLogger();

    private final int adviceId;
    private final boolean isTracing;
    private final Set<Class<?>> matchingClasses;
    private final Matcher methodNameMatcher;
    private final EnhancerAffect affect;

    // 类-字节码缓存
    private final static Map<Class<?>/*Class*/, byte[]/*bytes of Class*/> classBytesCache
            = new WeakHashMap<Class<?>, byte[]>();

    /**
     * @param adviceId          通知编号
     * @param isTracing         可跟踪方法调用
     * @param matchingClasses   匹配中的类
     * @param methodNameMatcher 方法名匹配
     * @param affect            影响统计
     */
    private Enhancer(int adviceId,
                     boolean isTracing,
                     Set<Class<?>> matchingClasses,
                     Matcher methodNameMatcher,
                     EnhancerAffect affect) {
        this.adviceId = adviceId;
        this.isTracing = isTracing;
        this.matchingClasses = matchingClasses;
        this.methodNameMatcher = methodNameMatcher;
        this.affect = affect;
    }


    /*
     * 从GreysClassLoader中加载Spy
     */
    private Class<?> loadSpyClassFromGreysClassLoader(final ClassLoader greysClassLoader, final String spyClassName) {
        try {
            return greysClassLoader.loadClass(spyClassName);
        } catch (ClassNotFoundException e) {
            logger.warn("Spy load failed from GreysClassLoader, that is impossible!", e);
            return null;
        }
    }

    /*
     * 派遣间谍混入对方的classloader中
     */
    private void spy(final ClassLoader targetClassLoader)
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        // 如果对方是bootstrap就算了
        if (null == targetClassLoader) {
            return;
        }


        // Enhancer类只可能从greysClassLoader中加载
        // 所以找他要ClassLoader是靠谱的
        final ClassLoader greysClassLoader = Enhancer.class.getClassLoader();

        final String spyClassName = GaStringUtils.SPY_CLASSNAME;

        // 从GreysClassLoader中加载Spy
        final Class<?> spyClassFromGreysClassLoader = loadSpyClassFromGreysClassLoader(greysClassLoader, spyClassName);
        if (null == spyClassFromGreysClassLoader) {
            return;
        }

        // 从目标ClassLoader中尝试加载或定义ClassLoader
        Class<?> spyClassFromTargetClassLoader = null;
        try {

            // 去目标类加载器中找下是否已经存在间谍
            // 如果间谍已经存在就算了
            spyClassFromTargetClassLoader = targetClassLoader.loadClass(spyClassName);
            logger.info("Spy already in targetClassLoader : " + targetClassLoader);

        }

        // 看来间谍不存在啊
        catch (ClassNotFoundException cnfe) {

            // 在目标类加载起中混入间谍
            spyClassFromTargetClassLoader = defineClass(
                    targetClassLoader,
                    spyClassName,
                    toByteArray(Enhancer.class.getResourceAsStream("/" + spyClassName.replace('.', '/') + ".class"))
            );

        }


        // 无论从哪里取到spyClass，都需要重新初始化一次
        // 用以兼容重新加载的场景
        // 当然，这样做会给渲染的过程带来一定的性能开销，不过能简化编码复杂度
        finally {

            if (null != spyClassFromTargetClassLoader) {
                // 初始化间谍
                invokeStaticMethod(
                        spyClassFromTargetClassLoader,
                        "init",
                        greysClassLoader,
                        getField(spyClassFromGreysClassLoader, "ON_BEFORE_METHOD").get(null),
                        getField(spyClassFromGreysClassLoader, "ON_RETURN_METHOD").get(null),
                        getField(spyClassFromGreysClassLoader, "ON_THROWS_METHOD").get(null),
                        getField(spyClassFromGreysClassLoader, "BEFORE_INVOKING_METHOD").get(null),
                        getField(spyClassFromGreysClassLoader, "AFTER_INVOKING_METHOD").get(null)
                );
            }

        }

    }

    @Override
    public byte[] transform(
            final ClassLoader inClassLoader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {


        // 这里要再次过滤一次，为啥？因为在transform的过程中，有可能还会再诞生新的类
        // 所以需要将之前需要转换的类集合传递下来，再次进行判断
        if (!matchingClasses.contains(classBeingRedefined)) {
            return null;
        }

        final ClassReader cr;

        // 首先先检查是否在缓存中存在Class字节码
        // 因为要支持多人协作,存在多人同时增强的情况
        final byte[] byteOfClassInCache = classBytesCache.get(classBeingRedefined);
        if (null != byteOfClassInCache) {
            cr = new ClassReader(byteOfClassInCache);
        }

        // 如果没有命中缓存,则从原始字节码开始增强
        else {
            cr = new ClassReader(classfileBuffer);
        }

        // 字节码增强
        final ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS) {

            /*
             * 注意，为了自动计算帧的大小，有时必须计算两个类共同的父类。
             * 缺省情况下，ClassWriter将会在getCommonSuperClass方法中计算这些，通过在加载这两个类进入虚拟机时，使用反射API来计算。
             * 但是，如果你将要生成的几个类相互之间引用，这将会带来问题，因为引用的类可能还不存在。
             * 在这种情况下，你可以重写getCommonSuperClass方法来解决这个问题。
             *
             * 通过重写 getCommonSuperClass() 方法，更正获取ClassLoader的方式，改成使用指定ClassLoader的方式进行。
             * 规避了原有代码采用Object.class.getClassLoader()的方式
             */
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                Class<?> c, d;
                try {
                    c = Class.forName(type1.replace('/', '.'), false, inClassLoader);
                    d = Class.forName(type2.replace('/', '.'), false, inClassLoader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (c.isAssignableFrom(d)) {
                    return type1;
                }
                if (d.isAssignableFrom(c)) {
                    return type2;
                }
                if (c.isInterface() || d.isInterface()) {
                    return "java/lang/Object";
                } else {
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return c.getName().replace('.', '/');
                }
            }

        };

        try {

            // 生成增强字节码
            cr.accept(new AdviceWeaver(adviceId, isTracing, cr.getClassName(), methodNameMatcher, affect, cw), EXPAND_FRAMES);
            final byte[] enhanceClassByteArray = cw.toByteArray();

            // 生成成功,推入缓存
            classBytesCache.put(classBeingRedefined, enhanceClassByteArray);

            // dump the class
            dumpClassIfNecessary(className, enhanceClassByteArray, affect);

            // 成功计数
            affect.cCnt(1);

            // 排遣间谍
            try {
                spy(inClassLoader);
            } catch (Throwable t) {
                logger.warn("print spy failed. classname={};loader={};", className, inClassLoader, t);
                throw t;
            }

            return enhanceClassByteArray;
        } catch (Throwable t) {
            logger.warn("transform loader[{}]:class[{}] failed.", inClassLoader, className, t);
        }

        return null;
    }

    /*
     * dump class to file
     */
    private static void dumpClassIfNecessary(String className, byte[] data, EnhancerAffect affect) {
        if (!GlobalOptions.isDump) {
            return;
        }
        final File dumpClassFile = new File("./greys-class-dump/" + className + ".class");
        final File classPath = new File(dumpClassFile.getParent());

        // 创建类所在的包路径
        if (!classPath.mkdirs()
                && !classPath.exists()) {
            logger.warn("create dump classpath:{} failed.", classPath);
            return;
        }

        // 将类字节码写入文件
        try {
            writeByteArrayToFile(dumpClassFile, data);
            affect.getClassDumpFiles().add(dumpClassFile);
        } catch (IOException e) {
            logger.warn("dump class:{} to file {} failed.", className, dumpClassFile, e);
        }

    }


    /**
     * 是否需要过滤的类
     *
     * @param classes 类集合
     */
    private static void filter(Set<Class<?>> classes) {
        final Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            if (null == clazz
                    || isSelf(clazz)
                    || isUnsafeClass(clazz)
                    || isUnsupportedClass(clazz)
                    || isGreysClass(clazz)) {
                it.remove();
            }
        }
    }

    /*
     * 是否过滤Greys加载的类
     */
    private static boolean isSelf(Class<?> clazz) {
        return null != clazz
                && isEquals(clazz.getClassLoader(), Enhancer.class.getClassLoader());
    }

    /*
     * 是否过滤unsafe类
     */
    private static boolean isUnsafeClass(Class<?> clazz) {
        return !GlobalOptions.isUnsafe
                && clazz.getClassLoader() == null;
    }

    /*
     * 是否过滤目前暂不支持的类
     */
    private static boolean isUnsupportedClass(Class<?> clazz) {

        return clazz.isArray()
                || clazz.isInterface()
                || clazz.isEnum()
                || clazz.isArray()
                ;
    }

    /**
     * Greys唯一不能看到的就是自己<br/>
     * 理论上有isSelf()挡住为啥这里还需要再次判断呢?
     * 原因很简单，因为Spy被派遣到对方的ClassLoader中去了
     */
    private static boolean isGreysClass(Class<?> clazz) {
        return StringUtils.startsWith(clazz.getCanonicalName(), "com.github.ompc.greys.");
    }

    private static Set<Class<?>> searchEnhanceClasses(final Instrumentation inst, final Matcher classNameMatcher) {

        final Set<Class<?>> returnClassSet = new LinkedHashSet<Class<?>>();

        // 1. 添加匹配的一级类
        final Set<Class<?>> matchedClassSet = SearchUtils.searchClassWithSubClass(inst, classNameMatcher);
        returnClassSet.addAll(matchedClassSet);

        // 2. 添加一级类的可见方法所对应的类
        for (Class<?> matchedClass : matchedClassSet) {
            for (Map.Entry<Class<?>, LinkedHashSet<Method>> entry : getVisibleMethods(matchedClass).entrySet()) {
                returnClassSet.add(entry.getKey());
            }
        }

        return returnClassSet;
    }

    /**
     * 对象增强
     *
     * @param inst              inst
     * @param adviceId          通知ID
     * @param isTracing         可跟踪方法调用
     * @param classNameMatcher  类名匹配
     * @param methodNameMatcher 方法名匹配
     * @return 增强影响范围
     * @throws UnmodifiableClassException 增强失败
     */
    public static synchronized EnhancerAffect enhance(
            final Instrumentation inst,
            final int adviceId,
            final boolean isTracing,
            final Matcher classNameMatcher,
            final Matcher methodNameMatcher) throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();

        // 获取需要增强的类集合
        final Set<Class<?>> enhanceClassSet = searchEnhanceClasses(inst, classNameMatcher);

        // 过滤掉无法被增强的类
        filter(enhanceClassSet);

        // 构建增强器
        final Enhancer enhancer = new Enhancer(adviceId, isTracing, enhanceClassSet, methodNameMatcher, affect);
        try {
            inst.addTransformer(enhancer, true);

            // 批量增强
            if (GlobalOptions.isBatchReTransform) {
                final int size = enhanceClassSet.size();
                final Class<?>[] classArray = new Class<?>[size];
                arraycopy(enhanceClassSet.toArray(), 0, classArray, 0, size);
                if (classArray.length > 0) {
                    inst.retransformClasses(classArray);
                }
            }


            // for each 增强
            else {
                for (Class<?> clazz : enhanceClassSet) {
                    try {
                        inst.retransformClasses(clazz);
                    } catch (Throwable t) {
                        logger.warn("reTransform {} failed.", clazz, t);
                        if (t instanceof UnmodifiableClassException) {
                            throw (UnmodifiableClassException) t;
                        } else if (t instanceof RuntimeException) {
                            throw (RuntimeException) t;
                        } else {
                            throw new RuntimeException(t);
                        }
                    }
                }
            }


        } finally {
            inst.removeTransformer(enhancer);
        }

        return affect;
    }


    /**
     * 重置指定的Class
     *
     * @param inst inst
     * @return 增强影响范围
     * @throws UnmodifiableClassException
     */
    public static synchronized EnhancerAffect reset(final Instrumentation inst) throws UnmodifiableClassException {

        final int size = classBytesCache.size();
        final EnhancerAffect affect = new EnhancerAffect();
        final ClassFileTransformer resetClassFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {
                return null;
            }
        };

        try {

            inst.addTransformer(resetClassFileTransformer, true);

            if (!classBytesCache.isEmpty()) {
                // 批量增强
                final Class<?>[] classArray = new Class<?>[size];
                arraycopy(classBytesCache.keySet().toArray(), 0, classArray, 0, size);
                inst.retransformClasses(classArray);
            }

        } finally {
            inst.removeTransformer(resetClassFileTransformer);
            affect.cCnt(classBytesCache.size());
            classBytesCache.clear();
        }

        return affect;
    }

}
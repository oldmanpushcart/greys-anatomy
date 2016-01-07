package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.manager.ReflectManager;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.GaStringUtils;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.PointCut;
import com.github.ompc.greys.core.util.affect.AsmAffect;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import com.github.ompc.greys.core.util.matcher.GroupMatcher;
import com.github.ompc.greys.core.util.matcher.Matcher;
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
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static com.github.ompc.greys.core.util.GaCheckUtils.isEquals;
import static com.github.ompc.greys.core.util.GaReflectUtils.defineClass;
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
    private final Map<Class<?>, Matcher<AsmMethod>> enhanceMap;
    private final EnhancerAffect affect;

    private static final ReflectManager reflectManager = ReflectManager.Factory.getInstance();

    // 类-字节码缓存
    private final static Map<Class<?>/*Class*/, byte[]/*bytes of Class*/> classBytesCache
            = new WeakHashMap<Class<?>, byte[]>();

    /**
     * @param adviceId   通知编号
     * @param isTracing  可跟踪方法调用
     * @param enhanceMap 增强点集合
     * @param affect     影响统计
     */
    private Enhancer(int adviceId,
                     boolean isTracing,
                     Map<Class<?>, Matcher<AsmMethod>> enhanceMap,
                     EnhancerAffect affect) {
        this.adviceId = adviceId;
        this.isTracing = isTracing;
        this.enhanceMap = enhanceMap;
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
     * 派遣间谍混入对方的classLoader中
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
                        getField(spyClassFromGreysClassLoader, "AFTER_INVOKING_METHOD").get(null),
                        getField(spyClassFromGreysClassLoader, "THROW_INVOKING_METHOD").get(null)
                );
            }

        }

    }

    @Override
    public byte[] transform(
            final ClassLoader inClassLoader,
            final String className,
            final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain,
            final byte[] classfileBuffer) throws IllegalClassFormatException {

        // 过滤掉不在增强集合范围内的类
        if (!enhanceMap.containsKey(classBeingRedefined)) {
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

        // 获取这个类所对应的asm方法匹配
        final Matcher<AsmMethod> asmMethodMatcher = enhanceMap.get(classBeingRedefined);

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
            cr.accept(new AdviceWeaver(adviceId, isTracing, cr.getClassName(), asmMethodMatcher, affect, cw), EXPAND_FRAMES);
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
     * 是否需要过滤掉制定类
     *
     * @param clazz 制定类
     * @return true:需要过滤;false:允许强化
     */
    private static boolean isIgnore(Class<?> clazz) {
        return null == clazz
                || isSelf(clazz)
                || isUnsafeClass(clazz)
                || isUnsupportedClass(clazz)
                || isGreysClass(clazz);
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

    private static Map<Class<?>, Matcher<AsmMethod>> toEnhanceMap(final PointCut pointCut) {

        final Map<Class<?>, Matcher<AsmMethod>> enhanceMap = new LinkedHashMap<Class<?>, Matcher<AsmMethod>>();
        final Collection<Class<?>> classes = pointCut.isIncludeSubClass()
                ? reflectManager.searchClassWithSubClass(pointCut.getClassMatcher())
                : reflectManager.searchClass(pointCut.getClassMatcher());


        for (final Class<?> clazz : classes) {

            for (final GaMethod gaMethod : reflectManager.searchClassGaMethods(clazz, pointCut.getGaMethodMatcher())) {

                // 如果当前方法所归属的类不支持增强,则华丽的忽略之
                // 这里不用能上一层循环的clazz,主要的原因在于会找到从父类继承过来的可见方法(照顾到用户习惯)
                final Class<?> targetClass = gaMethod.getDeclaringClass();
                if (isIgnore(targetClass)) {
                    continue;
                }

                final Matcher<AsmMethod> groupMatcher;
                if (enhanceMap.containsKey(targetClass)) {
                    groupMatcher = enhanceMap.get(targetClass);
                } else {
                    groupMatcher = new GroupMatcher.Or<AsmMethod>();
                    enhanceMap.put(targetClass, groupMatcher);
                }

                if (groupMatcher instanceof GroupMatcher) {
                    ((GroupMatcher<AsmMethod>) groupMatcher).add(new AsmMethodMatcher(gaMethod));
                }

            }

        }

        return enhanceMap;
    }

    /**
     * 对象增强
     *
     * @param inst      inst
     * @param adviceId  通知ID
     * @param isTracing 可跟踪方法调用
     * @param pointCut  增强点
     * @return 增强影响范围
     * @throws UnmodifiableClassException 增强失败
     */
    public static synchronized EnhancerAffect enhance(
            final Instrumentation inst,
            final int adviceId,
            final boolean isTracing,
            final PointCut pointCut) throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();


        final Map<Class<?>, Matcher<AsmMethod>> enhanceMap = toEnhanceMap(pointCut);

        // 构建增强器
        final Enhancer enhancer = new Enhancer(adviceId, isTracing, enhanceMap, affect);
        try {
            inst.addTransformer(enhancer, true);

            // 批量增强
            if (GlobalOptions.isBatchReTransform) {
                final int size = enhanceMap.size();
                final Class<?>[] classArray = new Class<?>[size];
                arraycopy(enhanceMap.keySet().toArray(), 0, classArray, 0, size);
                if (classArray.length > 0) {
                    inst.retransformClasses(classArray);
                }
            }


            // for each 增强
            else {
                for (Class<?> clazz : enhanceMap.keySet()) {
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


    /**
     * 获取匹配的类字节码信息
     *
     * @param classes 类集合
     * @param inst    inst
     * @return 增强影响范围
     * @throws UnmodifiableClassException
     */
    public static synchronized AsmAffect getClassByteArray(final Collection<Class<?>> classes, final Instrumentation inst) throws UnmodifiableClassException {

        final AsmAffect affect = new AsmAffect();

        if (null == classes
                || classes.isEmpty()) {
            return affect;
        }

        final ClassFileTransformer getClassByteArrayFileTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) throws IllegalClassFormatException {

                if (classes.contains(classBeingRedefined)) {
                    affect.getClassInfos().add(new AsmAffect.ClassInfo(
                            classBeingRedefined,
                            loader,
                            classfileBuffer,
                            protectionDomain
                    ));
                    affect.rCnt(1);
                }

                if (classBytesCache.containsKey(classBeingRedefined)) {
                    return classBytesCache.get(classBeingRedefined);
                } else {
                    return null;
                }

            }
        };

        try {
            inst.addTransformer(getClassByteArrayFileTransformer, true);
            final int size = classes.size();
            final Class<?>[] classArray = new Class<?>[size];
            arraycopy(classes.toArray(), 0, classArray, 0, size);
            inst.retransformClasses(classArray);
        } finally {
            inst.removeTransformer(getClassByteArrayFileTransformer);
        }

        return affect;

    }

}
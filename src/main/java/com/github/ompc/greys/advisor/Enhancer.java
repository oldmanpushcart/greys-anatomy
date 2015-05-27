package com.github.ompc.greys.advisor;

import com.github.ompc.greys.command.affect.EnhancerAffect;
import com.github.ompc.greys.util.LogUtil;
import com.github.ompc.greys.util.Matcher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import static com.github.ompc.greys.util.CheckUtil.isEquals;
import static com.github.ompc.greys.util.SearchUtil.searchClass;
import static com.github.ompc.greys.util.SearchUtil.searchSubClass;
import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static java.util.logging.Level.WARNING;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;


/**
 * 对类进行通知增强
 * Created by vlinux on 15/5/17.
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

    public byte[] transform(
            ClassLoader loader,
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
        final ClassWriter cw = new ClassWriter(cr, COMPUTE_FRAMES | COMPUTE_MAXS);

        try {

            // 生成增强字节码
            cr.accept(new AdviceWeaver(adviceId, isTracing, cr.getClassName(), methodNameMatcher, affect, cw), EXPAND_FRAMES);
            final byte[] enhanceClassByteArray = cw.toByteArray();

            // 生成成功,推入缓存
            classBytesCache.put(classBeingRedefined, enhanceClassByteArray);

            // 成功计数
            affect.cCnt(1);

            // dump
            final java.io.OutputStream os = new java.io.FileOutputStream(new java.io.File("/tmp/AgentTest.class"));
            os.write(enhanceClassByteArray);
            os.flush();
            os.close();

            return enhanceClassByteArray;
        } catch (Throwable t) {
            if (logger.isLoggable(WARNING)) {
                logger.log(WARNING, format("transform class[%s] failed. ClassLoader=%s;", className, loader), t);
            }
        }

        return null;
    }


    /**
     * 是否需要过滤的类
     *
     * @param classes 类集合
     * @return 过滤后的类
     */
    private static Set<Class<?>> filter(Set<Class<?>> classes) {
        final Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {

            final Class<?> clazz = it.next();

            if (null == clazz
                    || isEquals(clazz.getClassLoader(), Enhancer.class.getClassLoader())) {
                it.remove();
            }

        }
        return classes;
    }


    /**
     * 对象增强
     *
     * @param inst              inst
     * @param adviceId          通知ID
     * @param isTracing         可跟踪方法调用
     * @param classNameMatcher  类名匹配
     * @param methodNameMatcher 方法名匹配
     * @param isIncludeSub      是否包括子类
     * @return 增强影响范围
     * @throws UnmodifiableClassException 增强失败
     */
    public static synchronized EnhancerAffect enhance(
            final Instrumentation inst,
            final int adviceId,
            final boolean isTracing,
            final Matcher classNameMatcher,
            final Matcher methodNameMatcher,
            final boolean isIncludeSub) throws UnmodifiableClassException {

        final EnhancerAffect affect = new EnhancerAffect();

        // 获取需要增强的类集合
        final Set<Class<?>> enhanceClassSet = !isIncludeSub
                ? searchClass(inst, classNameMatcher)
                : searchSubClass(inst, searchClass(inst, classNameMatcher));

        // 过滤掉无法被增强的类
        filter(enhanceClassSet);

        // 构建增强器
        final Enhancer enhancer = new Enhancer(adviceId, isTracing, enhanceClassSet, methodNameMatcher, affect);
        try {
            inst.addTransformer(enhancer, true);

            // 批量增强
            final int size = enhanceClassSet.size();
            final Class<?>[] classArray = new Class<?>[size];
            arraycopy(enhanceClassSet.toArray(), 0, classArray, 0, size);
            if (classArray.length > 0) {
                inst.retransformClasses(classArray);
            }

        } finally {
            inst.removeTransformer(enhancer);
        }

        return affect;
    }

}
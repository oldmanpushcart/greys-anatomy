package com.googlecode.greysanatomy.probe;

import com.googlecode.greysanatomy.probe.Advice.Target;
<<<<<<< HEAD
import com.googlecode.greysanatomy.probe.Advice.TargetBehavior;
import com.googlecode.greysanatomy.probe.Advice.TargetConstructor;
import com.googlecode.greysanatomy.probe.Advice.TargetMethod;
import com.googlecode.greysanatomy.util.GaCheckUtils;
=======
>>>>>>> pr/8
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

<<<<<<< HEAD
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

=======
>>>>>>> pr/8
import static com.googlecode.greysanatomy.probe.ProbeJobs.getJobListeners;
import static com.googlecode.greysanatomy.probe.ProbeJobs.isListener;
import static java.lang.String.format;
import static javassist.Modifier.*;

/**
 * 探测点触发者<br/>
 * 在埋的点中，一共有4种探测点，他们分别对应<br/>
 * fucntion f()
 * {
 * // probe:_before()
 * try {
 * do something...
 * // probe:_success()
 * } catch(Throwable t) {
 * // probe:_throws();
 * throw t;
 * } finally {
 * // probe:_finish();
 * }
 * <p/>
 * }
 *
 * @author vlinux
 */
public class Probes {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private static final String jobsClass = "com.googlecode.greysanatomy.probe.ProbeJobs";
    private static final String probesClass = "com.googlecode.greysanatomy.probe.Probes";

<<<<<<< HEAD
    private static final Map<String, Class<?>> cacheForGetClassByName = new ConcurrentHashMap<String, Class<?>>();
    private static final Map<GetBehaviorKey, Method> cacheForGetMethodByName = new ConcurrentHashMap<GetBehaviorKey, Method>();
    private static final Map<GetBehaviorKey, Constructor<?>> cacheForGetConstructorByParamTypes = new ConcurrentHashMap<GetBehaviorKey, Constructor<?>>();


    /**
     * 根据传入的参数决定最终采用Behaveior
     *
     * @param targetConstructor
     * @param targetMethod
     * @return
     */
    private static TargetBehavior newTargetBehavior(Constructor<?> targetConstructor, Method targetMethod) {
        if (null != targetConstructor) {
            return new TargetConstructor(targetConstructor);
        } else {
            return new TargetMethod(targetMethod);
        }
    }

    /**
     * 构造Target
     *
     * @param targetClass
     * @param targetConstructor
     * @param targetMethod
     * @param targetThis
     * @return
     */
    private static Target newTarget(Class<?> targetClass, Constructor<?> targetConstructor, Method targetMethod, Object targetThis) {
        return new Target(targetClass, newTargetBehavior(targetConstructor, targetMethod), targetThis);
    }

    /**
     * 执行前置
     *
     * @param id
     * @param targetClass
     * @param targetConstructor
     * @param targetMethod
     * @param targetThis
     * @param args
     */
    public static void doBefore(String id, Class<?> targetClass, Constructor<?> targetConstructor, Method targetMethod, Object targetThis, Object[] args) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClass, targetConstructor, targetMethod, targetThis), args, false);
=======
    private static Target newTarget(String targetClassName, String targetBehaviorName, Object targetThis) {
        return new Target(targetClassName, targetBehaviorName, targetThis);
    }

    public static void doBefore(int id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
>>>>>>> pr/8
                ((AdviceListener) getJobListeners(id)).onBefore(p);
            } catch (Throwable t) {
                logger.warn("error at doBefore", t);
            }
        }
    }

<<<<<<< HEAD
    /**
     * 执行成功
     *
     * @param id
     * @param targetClass
     * @param targetConstructor
     * @param targetMethod
     * @param targetThis
     * @param args
     * @param returnObj
     */
    public static void doSuccess(String id, Class<?> targetClass, Constructor<?> targetConstructor, Method targetMethod, Object targetThis, Object[] args, Object returnObj) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClass, targetConstructor, targetMethod, targetThis), args, false);
=======
    public static void doSuccess(int id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Object returnObj) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
>>>>>>> pr/8
                p.setReturnObj(returnObj);
                ((AdviceListener) getJobListeners(id)).onSuccess(p);
            } catch (Throwable t) {
                logger.warn("error at onSuccess", t);
            }
<<<<<<< HEAD
            doFinish(id, targetClass, targetConstructor, targetMethod, targetThis, args, returnObj, null);
=======
            doFinish(id, targetClassName, targetBehaviorName, targetThis, args, returnObj, null);
>>>>>>> pr/8
        }

    }

<<<<<<< HEAD
    /**
     * 执行异常
     *
     * @param id
     * @param targetClass
     * @param targetConstructor
     * @param targetMethod
     * @param targetThis
     * @param args
     * @param throwException
     */
    public static void doException(String id, Class<?> targetClass, Constructor<?> targetConstructor, Method targetMethod, Object targetThis, Object[] args, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClass, targetConstructor, targetMethod, targetThis), args, false);
=======
    public static void doException(int id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, false);
>>>>>>> pr/8
                p.setThrowException(throwException);
                ((AdviceListener) getJobListeners(id)).onException(p);
            } catch (Throwable t) {
                logger.warn("error at onException", t);
            }
<<<<<<< HEAD
            doFinish(id, targetClass, targetConstructor, targetMethod, targetThis, args, null, throwException);
=======
            doFinish(id, targetClassName, targetBehaviorName, targetThis, args, null, throwException);
>>>>>>> pr/8
        }

    }

<<<<<<< HEAD
    /**
     * 执行完成
     *
     * @param id
     * @param targetClass
     * @param targetMethod
     * @param targetThis
     * @param args
     * @param returnObj
     * @param throwException
     * @Param targetConstructor
     */
    public static void doFinish(String id, Class<?> targetClass, Constructor<?> targetConstructor, Method targetMethod, Object targetThis, Object[] args, Object returnObj, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClass, targetConstructor, targetMethod, targetThis), args, true);
=======
    public static void doFinish(int id, String targetClassName, String targetBehaviorName, Object targetThis, Object[] args, Object returnObj, Throwable throwException) {
        if (isListener(id, AdviceListener.class)) {
            try {
                Advice p = new Advice(newTarget(targetClassName, targetBehaviorName, targetThis), args, true);
>>>>>>> pr/8
                p.setThrowException(throwException);
                p.setReturnObj(returnObj);
                ((AdviceListener) getJobListeners(id)).onFinish(p);
            } catch (Throwable t) {
                logger.warn("error at onFinish", t);
            }
        }
    }


    /**
<<<<<<< HEAD
     * 获取类信息
     *
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> getClassByName(String name) throws ClassNotFoundException {
        if (cacheForGetClassByName.containsKey(name)) {
            return cacheForGetClassByName.get(name);
        }
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Class<?> clazz;
        if (null != loader) {
            clazz = loader.loadClass(name);
        } else {
            clazz = java.lang.Class.forName(name);
        }//if
        cacheForGetClassByName.put(name, clazz);
        return clazz;
    }

    /**
     * 获取行为的缓存Key
     *
     * @author vlinux
     */
    private static class GetBehaviorKey {

        private final String className;
        private final String behaviorName;
        private final Class<?>[] paramTypes;

        private GetBehaviorKey(String className, String behaviorName, Class<?>[] paramTypes) {
            this.className = className;
            this.behaviorName = behaviorName;
            this.paramTypes = paramTypes;
        }

        public int hashCode() {
            int hc = className.hashCode() + behaviorName.hashCode();
            if (null != paramTypes) {
                for (Class<?> c : paramTypes) {
                    hc += c.hashCode();
                }
            }
            return hc;
        }

        public boolean equals(Object obj) {
            if (null == obj
                    || !(obj instanceof GetBehaviorKey)) {
                return false;
            }

            GetBehaviorKey o = (GetBehaviorKey) obj;

            if (!className.equals(o.className)
                    || !behaviorName.equals(o.behaviorName)) {
                return false;
            }
            if (null != paramTypes) {
                if (null == o.paramTypes
                        || paramTypes.length != o.paramTypes.length) {
                    return false;
                }
                for (int i = 0; i < paramTypes.length; i++) {
                    if (!paramTypes[i].equals(o.paramTypes[i])) {
                        return false;
                    }
                }
            } else {
                if (null != o.paramTypes) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 通过方法名和参数列表获取方法
     *
     * @param className
     * @param methodName
     * @param paramTypes
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Method getMethodByNameAndParamTypes(String className, String methodName, Class<?>... paramTypes) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        final GetBehaviorKey gmd = new GetBehaviorKey(className, methodName, paramTypes);
        if (cacheForGetMethodByName.containsKey(gmd)) {
            return cacheForGetMethodByName.get(gmd);
        }
        final Class<?> clazz = getClassByName(className);
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        cacheForGetMethodByName.put(gmd, method);
        return method;
    }

    /**
     * 通过参数列表获取构造函数
     *
     * @param className
     * @param paramTypes
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Constructor<?> getConstructorByParamTypes(String className, Class<?>... paramTypes) throws ClassNotFoundException, SecurityException, NoSuchMethodException {
        final GetBehaviorKey key = new GetBehaviorKey(className, "<init>", paramTypes);
        if (cacheForGetConstructorByParamTypes.containsKey(key)) {
            return cacheForGetConstructorByParamTypes.get(key);
        }
        final Class<?> clazz = getClassByName(className);
        final Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
        cacheForGetConstructorByParamTypes.put(key, constructor);
        return constructor;
    }

    /**
     * 获取CtBehavior所封装的参数信息,字符串化,用于javassist
     *
     * @param cb
     * @return
     * @throws NotFoundException
     */
    private static String toJavassistStringParamTypes(CtBehavior cb) throws NotFoundException {
        StringBuilder sb = new StringBuilder();
        CtClass[] ccs = cb.getParameterTypes();
        final String returnStr;
        if (null != ccs && ccs.length > 0) {
            for (CtClass cc : ccs) {

                String name = cc.getName();
                if (cc.isArray()
                        || GaCheckUtils.isIn(name, "long", "int", "double", "float", "char", "byte", "short", "boolean")) {
                    sb.append(name).append(".class");
                } else {
                    sb.append(format("%s.getClassByName(\"%s\")", probesClass, name));
                }//if

                sb.append(",");

            }
            sb.deleteCharAt(sb.length() - 1);
            returnStr = format("new Class[]{%s}", sb.toString());
        } else {
            returnStr = "null";
        }
        return returnStr;
    }

    /**
=======
>>>>>>> pr/8
     * 是否过滤掉当前探测的目标
     *
     * @param cc
     * @param cb
     * @return
     */
    private static boolean isIngore(CtClass cc, CtBehavior cb) {

        final int ccMod = cc.getModifiers();
        final int cbMod = cb.getModifiers();

<<<<<<< HEAD
        // 过滤掉接口
        if (isInterface(ccMod)) {
            return true;
        }

        // 过滤掉抽象方法
        if (isAbstract(cbMod)) {
            return true;
        }

        // 过滤掉自己，避免递归调用
        if (cc.getName().startsWith("com.googlecode.greysanatomy.")) {
=======
        if (isInterface(ccMod)
                || isAbstract(cbMod)
                || cc.getName().startsWith("com.googlecode.greysanatomy.")) {
>>>>>>> pr/8
            return true;
        }

        return false;

    }


    /**
     * 埋点探测器
     *
     * @param id
     * @param cc
     * @param cb
     * @throws CannotCompileException
     * @throws NotFoundException
<<<<<<< HEAD
     * @throws ClassNotFoundException
     */
    public static void mine(String id, CtClass cc, CtBehavior cb) throws CannotCompileException, NotFoundException, ClassNotFoundException {
=======
     */
    public static void mine(int id, CtClass cc, CtBehavior cb) throws CannotCompileException, NotFoundException {
>>>>>>> pr/8

        if (isIngore(cc, cb)) {
            return;
        }

<<<<<<< HEAD
        // 目标类
        final String javassistClass = format("(%s.getClassByName(\"%s\"))",
                probesClass,
                cc.getName());

        // 目标方法
        final String javassistMethod = cb.getMethodInfo().isMethod()
                ? format("%s.getMethodByNameAndParamTypes(\"%s\",\"%s\",%s)",
                probesClass,
                cc.getName(),
                cb.getMethodInfo().getName(),
                toJavassistStringParamTypes(cb))
                : "null";

        // 目标构造函数
        final String javassistConstructor = cb.getMethodInfo().isConstructor()
                ? format("%s.getConstructorByParamTypes(\"%s\",%s)",
                probesClass,
                cc.getName(),
                toJavassistStringParamTypes(cb))
                : "null";

=======
>>>>>>> pr/8
        // 目标实例,如果是静态方法，则为null
        final String javassistThis = isStatic(cb.getModifiers()) ? "null" : "this";

        // 埋点通知
        if (isListener(id, AdviceListener.class)) {
            // 构造函数在这里是不能做insertBefore的,所以构造函数的before是做在doCache中
            if (cb.getMethodInfo().isMethod()) {
<<<<<<< HEAD
                mineProbeForMethod(cb, id, javassistClass, javassistConstructor, javassistMethod, javassistThis);
            } else if (cb.getMethodInfo().isConstructor()) {
                mineProbeForConstructor(cb, id, javassistClass, javassistConstructor, javassistMethod, javassistThis);
=======
                mineProbeForMethod(cb, id, cc.getName(), cb.getName(), javassistThis);
            } else if (cb.getMethodInfo().isConstructor()) {
                mineProbeForConstructor(cb, id, cc.getName(), cb.getName(), javassistThis);
>>>>>>> pr/8
            }
        }

    }

<<<<<<< HEAD
    /**
     * 给构造函数埋点
     *
     * @param cb
     * @param id
     * @param javassistClass
     * @param javassistConstructor
     * @param javassistMethod
     * @param javassistThis
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private static void mineProbeForConstructor(CtBehavior cb, String id, String javassistClass, String javassistConstructor, String javassistMethod, String javassistThis) throws CannotCompileException, NotFoundException {
        cb.addCatch(format("{if(%s.isJobAlive(\"%s\")){%s.doBefore(\"%s\",%s,%s,%s,%s,$args);%s.doException(\"%s\",%s,%s,%s,%s,$args,$e);}throw $e;}",
                        jobsClass, id,
                        probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis,
                        probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));
        cb.insertAfter(format("{if(%s.isJobAlive(\"%s\")){%s.doBefore(\"%s\",%s,%s,%s,%s,$args);%s.doSuccess(\"%s\",%s,%s,%s,%s,$args,($w)$_);}}",
                jobsClass, id,
                probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis,
                probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis));
    }

    /**
     * 给方法体埋点
     *
     * @param cb
     * @param id
     * @param javassistClass
     * @param javassistConstructor
     * @param javassistMethod
     * @param javassistThis
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private static void mineProbeForMethod(CtBehavior cb, String id, String javassistClass, String javassistConstructor, String javassistMethod, String javassistThis) throws CannotCompileException, NotFoundException {
        cb.insertBefore(format("{if(%s.isJobAlive(\"%s\"))%s.doBefore(\"%s\",%s,%s,%s,%s,$args);}",
                jobsClass, id, probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis));
        cb.addCatch(format("{if(%s.isJobAlive(\"%s\"))%s.doException(\"%s\",%s,%s,%s,%s,$args,$e);throw $e;}",
                        jobsClass, id, probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));
        cb.insertAfter(format("{if(%s.isJobAlive(\"%s\"))%s.doSuccess(\"%s\",%s,%s,%s,%s,$args,($w)$_);}",
                jobsClass, id, probesClass, id, javassistClass, javassistConstructor, javassistMethod, javassistThis));
=======
    private static void mineProbeForConstructor(CtBehavior cb, int id, String targetClassName, String targetBehaviorName, String javassistThis) throws CannotCompileException, NotFoundException {
        cb.addCatch(format("{if(%s.isJobAlive(%s)){%s.doBefore(%s,\"%s\",\"%s\",%s,$args);%s.doException(%s,\"%s\",\"%s\",%s,$args,$e);}throw $e;}",
                        jobsClass, id,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));

        // TODO : 奇怪，为啥这里要doBefore两次?
        cb.insertAfter(format("{if(%s.isJobAlive(%s)){%s.doBefore(%s,\"%s\",\"%s\",%s,$args);%s.doSuccess(%s,\"%s\",\"%s\",%s,$args,($w)$_);}}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));

    }

    private static void mineProbeForMethod(CtBehavior cb, int id, String targetClassName, String targetBehaviorName, String javassistThis) throws CannotCompileException, NotFoundException {

        cb.insertBefore(format("{if(%s.isJobAlive(%s))%s.doBefore(%s,\"%s\",\"%s\",%s,$args);}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));

        cb.addCatch(format("{if(%s.isJobAlive(%s))%s.doException(%s,\"%s\",\"%s\",%s,$args,$e);throw $e;}",
                        jobsClass, id,
                        probesClass, id, targetClassName, targetBehaviorName, javassistThis),
                ClassPool.getDefault().get("java.lang.Throwable"));

        cb.insertAfter(format("{if(%s.isJobAlive(%s))%s.doSuccess(%s,\"%s\",\"%s\",%s,$args,($w)$_);}",
                jobsClass, id,
                probesClass, id, targetClassName, targetBehaviorName, javassistThis));
>>>>>>> pr/8
    }

}

package com.github.ompc.greys.advisor;

import com.github.ompc.greys.util.GaMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static com.github.ompc.greys.util.CheckUtil.isEquals;
import static com.github.ompc.greys.util.StringUtil.tranClassName;

/**
 * 反射通知适配器<br/>
 * 通过反射拿到对应的Class/Method类，而不是原始的ClassName/MethodNam
 * 当然性能开销要比普通监听器高许多
 */
public class ReflectAdviceListenerAdapter implements AdviceListener {

    @Override
    public void create() {

    }

    @Override
    public void destroy() {

    }

    private ClassLoader toClassLoader(ClassLoader loader) {
        return null != loader
                ? loader
                : AdviceListener.class.getClassLoader();
    }

    private Class<?> toClass(ClassLoader loader, String className) throws ClassNotFoundException {
        final String tranClassName = tranClassName(className);
        if (isEquals(className, "boolean")) {
            return boolean.class;
        } else if (isEquals(className, "char")) {
            return char.class;
        } else if (isEquals(className, "byte")) {
            return byte.class;
        } else if (isEquals(className, "short")) {
            return short.class;
        } else if (isEquals(className, "int")) {
            return int.class;
        } else if (isEquals(className, "float")) {
            return float.class;
        } else if (isEquals(className, "long")) {
            return long.class;
        } else if (isEquals(className, "double")) {
            return double.class;
        } else if (isEquals(className, "boolean[]")) {
            return boolean[].class;
        } else if (isEquals(className, "char[]")) {
            return char[].class;
        } else if (isEquals(className, "byte[]")) {
            return byte[].class;
        } else if (isEquals(className, "short[]")) {
            return short[].class;
        } else if (isEquals(className, "int[]")) {
            return int[].class;
        } else if (isEquals(className, "float[]")) {
            return float[].class;
        } else if (isEquals(className, "long[]")) {
            return long[].class;
        } else if (isEquals(className, "double[]")) {
            return double[].class;
        } else if (className.endsWith("[]")) {
            return Class.forName("[L" + tranClassName.replaceFirst("\\[]$", ";"), true, toClassLoader(loader));
        } else {
            return Class.forName(tranClassName, true, toClassLoader(loader));
        }
    }

    private GaMethod toMethod(ClassLoader loader, Class<?> clazz, String methodName, String methodDesc)
            throws ClassNotFoundException, NoSuchMethodException {
        final org.objectweb.asm.Type asmType = org.objectweb.asm.Type.getMethodType(methodDesc);
        final Class<?>[] argsClasses = new Class<?>[asmType.getArgumentTypes().length];
        for (int index = 0; index < argsClasses.length; index++) {
            argsClasses[index] = toClass(loader, asmType.getArgumentTypes()[index].getClassName());
        }

        if (isEquals(methodName, "<init>")) {
            return GaMethod.newInit(toConstructor(clazz, argsClasses));
        } else {
            return GaMethod.newMethod(toMethod(clazz, methodName, argsClasses));
        }
    }

    private Method toMethod(Class<?> clazz, String methodName, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, argClasses);
    }

    private Constructor<?> toConstructor(Class<?> clazz, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(argClasses);
    }


    @Override
    final public void before(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        before(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args);
    }

    @Override
    final public void afterReturning(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Object returnObject) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        afterReturning(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, returnObject);
    }

    @Override
    final public void afterThrowing(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Throwable throwable) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        afterThrowing(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, throwable);
    }


    /**
     * 前置通知
     *
     * @param loader 类加载器
     * @param clazz  类
     * @param method 方法
     * @param target 目标类实例
     *               若目标为静态方法,则为null
     * @param args   参数列表
     * @throws Throwable 通知过程出错
     */
    public void before(
            ClassLoader loader, Class<?> clazz, GaMethod method,
            Object target, Object[] args) throws Throwable {

    }

    /**
     * 返回通知
     *
     * @param loader       类加载器
     * @param clazz        类
     * @param method       方法
     * @param target       目标类实例
     *                     若目标为静态方法,则为null
     * @param args         参数列表
     * @param returnObject 返回结果
     *                     若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    public void afterReturning(
            ClassLoader loader, Class<?> clazz, GaMethod method,
            Object target, Object[] args,
            Object returnObject) throws Throwable {

    }

    /**
     * 异常通知
     *
     * @param loader    类加载器
     * @param clazz     类
     * @param method    方法
     * @param target    目标类实例
     *                  若目标为静态方法,则为null
     * @param args      参数列表
     * @param throwable 目标异常
     * @throws Throwable 通知过程出错
     */
    public void afterThrowing(
            ClassLoader loader, Class<?> clazz, GaMethod method,
            Object target, Object[] args,
            Throwable throwable) throws Throwable {

    }

}
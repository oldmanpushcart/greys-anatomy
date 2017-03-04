package com.github.ompc.greys.core.listener;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.GaMethod;
import com.github.ompc.greys.core.util.LazyGet;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Stack;

import static com.github.ompc.greys.core.Advice.*;

/**
 * 反射通知适配器<br/>
 * 通过反射拿到对应的Class/Method类，而不是原始的ClassName/MethodNam
 * 当然性能开销要比普通监听器高许多
 */
public abstract class AdviceListener implements GreysListener {

    private ClassLoader toClassLoader(ClassLoader loader) {
        return null != loader
                ? loader
                : GreysListener.class.getClassLoader();
    }

    private Class<?> toClass(ClassLoader loader, String javaClassName) throws ClassNotFoundException {
        return Class.forName(javaClassName, true, toClassLoader(loader));
    }

    private GaMethod toMethod(ClassLoader loader, Class<?> clazz, String methodName, String methodDesc)
            throws ClassNotFoundException, NoSuchMethodException {
        final Type asmType = Type.getMethodType(methodDesc);

        // to arg types
        final Class<?>[] argsClasses = new Class<?>[asmType.getArgumentTypes().length];
        for (int index = 0; index < argsClasses.length; index++) {

            // asm class descriptor to jvm class
            final Class<?> argumentClass;
            final Type argumentAsmType = asmType.getArgumentTypes()[index];
            switch (argumentAsmType.getSort()) {
                case Type.BOOLEAN: {
                    argumentClass = boolean.class;
                    break;
                }
                case Type.CHAR: {
                    argumentClass = char.class;
                    break;
                }
                case Type.BYTE: {
                    argumentClass = byte.class;
                    break;
                }
                case Type.SHORT: {
                    argumentClass = short.class;
                    break;
                }
                case Type.INT: {
                    argumentClass = int.class;
                    break;
                }
                case Type.FLOAT: {
                    argumentClass = float.class;
                    break;
                }
                case Type.LONG: {
                    argumentClass = long.class;
                    break;
                }
                case Type.DOUBLE: {
                    argumentClass = double.class;
                    break;
                }
                case Type.ARRAY: {
                    argumentClass = toClass(loader, argumentAsmType.getInternalName());
                    break;
                }
                case Type.VOID: {
                    argumentClass = void.class;
                    break;
                }
                case Type.OBJECT:
                case Type.METHOD:
                default: {
                    argumentClass = toClass(loader, argumentAsmType.getClassName());
                    break;
                }
            }

            argsClasses[index] = argumentClass;
        }

        // to method or constructor
        if (StringUtils.equals(methodName, "<init>")) {
            return new GaMethod.ConstructorImpl(toConstructor(clazz, argsClasses));
        } else {
            return new GaMethod.MethodImpl(toMethod(clazz, methodName, argsClasses));
        }
    }


    private Method toMethod(Class<?> clazz, String methodName, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, argClasses);
    }

    private Constructor<?> toConstructor(Class<?> clazz, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(argClasses);
    }


    private LazyGet<Class<?>> toClassRef(final ClassLoader loader, final String className) {
        return new LazyGet<Class<?>>() {
            @Override
            protected Class<?> initialValue() throws Throwable {
                return toClass(loader, className);
            }
        };
    }

    private LazyGet<GaMethod> toMethodRef(final ClassLoader loader, final LazyGet<Class<?>> clazzRef, final String methodName, final String methodDesc) {
        return new LazyGet<GaMethod>() {
            @Override
            protected GaMethod initialValue() throws Throwable {
                return toMethod(loader, clazzRef.get(), methodName, methodDesc);
            }
        };
    }

    private final ThreadLocal<Stack<LazyGet<?>>> infoStackRef = new ThreadLocal<Stack<LazyGet<?>>>() {
        @Override
        protected Stack<LazyGet<?>> initialValue() {
            return new Stack<LazyGet<?>>();
        }
    };

    @Override
    final public void before(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable {

        try {
            final LazyGet<Class<?>> clazzRef = toClassRef(loader, javaClassName);
            final LazyGet<GaMethod> methodRef = toMethodRef(loader, clazzRef, methodName, methodDesc);
            final Stack<LazyGet<?>> infoStack = infoStackRef.get();
            infoStack.push(clazzRef);
            infoStack.push(methodRef);

            before(newForBefore(loader, clazzRef, methodRef, target, args));
        } finally {
            beforeHook();
        }

    }

    /**
     * before()回调钩子<br/>
     * 用来提供给 {@link TracingListener 修正#78问题}
     */
    void beforeHook() {

    }

    /**
     * finish()回调钩子<br/>
     * 用来提供给 {@link TracingListener 修正#78问题}
     */
    void finishHook() {

    }

    @Override
    final public void afterReturning(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args, Object returnObject) throws Throwable {

        try {
            final Stack<LazyGet<?>> infoStack = infoStackRef.get();
            final LazyGet<GaMethod> methodRef = (LazyGet<GaMethod>) infoStack.pop();
            final LazyGet<Class<?>> clazzRef = (LazyGet<Class<?>>) infoStack.pop();

            final Advice advice = newForAfterRetuning(
                    loader,
                    clazzRef,
                    methodRef,
                    target,
                    args,
                    // #98 在return的时候,如果目标函数是<init>,会导致return的内容缺失
                    // 初步的想法是用target(this)去代替returnObj
                    StringUtils.equals("<init>", methodName) ? target : returnObject
            );

            afterReturning(advice);
            afterFinishing(advice);
        } finally {
            finishHook();
        }

    }

    @Override
    final public void afterThrowing(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args, Throwable throwable) throws Throwable {

        try {
            final Stack<LazyGet<?>> infoStack = infoStackRef.get();
            final LazyGet<GaMethod> methodRef = (LazyGet<GaMethod>) infoStack.pop();
            final LazyGet<Class<?>> clazzRef = (LazyGet<Class<?>>) infoStack.pop();
            final Advice advice = newForAfterThrowing(
                    loader,
                    clazzRef,
                    methodRef,
                    target,
                    args,
                    throwable
            );
            afterThrowing(advice);
            afterFinishing(advice);
        } finally {
            finishHook();
        }

    }


    /**
     * 前置通知
     *
     * @param advice 通知点
     * @throws Throwable 通知过程出错
     */
    public void before(Advice advice) throws Throwable {

    }

    /**
     * 返回通知
     *
     * @param advice 通知点
     * @throws Throwable 通知过程出错
     */
    public void afterReturning(Advice advice) throws Throwable {

    }

    /**
     * 异常通知
     *
     * @param advice 通知点
     * @throws Throwable 通知过程出错
     */
    public void afterThrowing(Advice advice) throws Throwable {

    }

    /**
     * 结束通知
     *
     * @param advice 通知点
     * @throws Throwable 通知过程出错
     */
    public void afterFinishing(Advice advice) throws Throwable {

    }

}
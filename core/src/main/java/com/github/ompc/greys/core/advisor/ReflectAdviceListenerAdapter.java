package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.util.GaCheckUtils;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.collection.GaStack;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static com.github.ompc.greys.core.Advice.*;
import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;

/**
 * 反射通知适配器<br/>
 * 通过反射拿到对应的Class/Method类，而不是原始的ClassName/MethodNam
 * 当然性能开销要比普通监听器高许多
 */
public abstract class ReflectAdviceListenerAdapter<PC extends ProcessContext, IC extends InnerContext> implements AdviceListener {

    /**
     * 构造过程上下文
     *
     * @return 返回过程上下文
     */
    abstract protected PC newProcessContext();

    /**
     * 构造方法内部上下文
     *
     * @return 返回方法内部上下文
     */
    abstract protected IC newInnerContext();

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
        return Class.forName(tranClassName(className), true, toClassLoader(loader));
    }

    private GaMethod toMethod(ClassLoader loader, Class<?> clazz, String methodName, String methodDesc)
            throws ClassNotFoundException, NoSuchMethodException {
        final org.objectweb.asm.Type asmType = org.objectweb.asm.Type.getMethodType(methodDesc);

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
        if (GaCheckUtils.isEquals(methodName, "<init>")) {
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


    protected final ThreadLocal<PC> processContextRef = new ThreadLocal<PC>() {
        @Override
        protected PC initialValue() {
            return newProcessContext();
        }
    };

    @Override
    final public void before(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        final PC processContext = processContextRef.get();
        final IC innerContext = newInnerContext();

        final GaStack<IC> innerContextGaStack = processContext.innerContextGaStack;
        innerContextGaStack.push(innerContext);

        before(
                newForBefore(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args),
                processContext,
                innerContext
        );

    }

    @Override
    final public void afterReturning(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Object returnObject) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        final PC processContext = processContextRef.get();
        final GaStack<IC> innerContextGaStack = processContext.innerContextGaStack;
        final IC innerContext = innerContextGaStack.pop();
        try {

            // 关闭上下文
            innerContext.close();

            final Advice advice = newForAfterRetuning(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, returnObject);
            afterReturning(advice, processContext, innerContext);
            afterFinishing(advice, processContext, innerContext);

        } finally {

            // 如果过程上下文已经到了顶层则需要清除掉上下文
            if (processContext.isTop()) {
                processContextRef.remove();
            }

        }

    }

    @Override
    final public void afterThrowing(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Throwable throwable) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        final PC processContext = processContextRef.get();
        final GaStack<IC> innerContextGaStack = processContext.innerContextGaStack;
        final IC innerContext = innerContextGaStack.pop();

        try {

            // 关闭上下文
            innerContext.close();

            final Advice advice = newForAfterThrowing(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, throwable);
            afterThrowing(advice, processContext, innerContext);
            afterFinishing(advice, processContext, innerContext);

        } finally {

            // 如果过程上下文已经到了顶层则需要清除掉上下文
            if (processContext.isTop()) {
                processContextRef.remove();
            }

        }

    }


    /**
     * 前置通知
     *
     * @param advice         通知点
     * @param processContext 处理上下文
     * @param innerContext   当前方法调用上下文
     * @throws Throwable 通知过程出错
     */
    public void before(Advice advice, PC processContext, IC innerContext) throws Throwable {

    }

    /**
     * 返回通知
     *
     * @param advice         通知点
     * @param processContext 处理上下文
     * @param innerContext   当前方法调用上下文
     * @throws Throwable 通知过程出错
     */
    public void afterReturning(Advice advice, PC processContext, IC innerContext) throws Throwable {

    }

    /**
     * 异常通知
     *
     * @param advice         通知点
     * @param processContext 处理上下文
     * @param innerContext   当前方法调用上下文
     * @throws Throwable 通知过程出错
     */
    public void afterThrowing(Advice advice, PC processContext, IC innerContext) throws Throwable {

    }

    /**
     * 结束通知
     *
     * @param advice         通知点
     * @param processContext 处理上下文
     * @param innerContext   当前方法调用上下文
     * @throws Throwable 通知过程出错
     */
    public void afterFinishing(Advice advice, PC processContext, IC innerContext) throws Throwable {

    }


    /**
     * 默认实现
     */
    public static class DefaultReflectAdviceListenerAdapter extends ReflectAdviceListenerAdapter<ProcessContext<InnerContext>, InnerContext> {

        @Override
        protected ProcessContext<InnerContext> newProcessContext() {
            return new ProcessContext<InnerContext>();
        }

        @Override
        protected InnerContext newInnerContext() {
            return new InnerContext();
        }
    }

}
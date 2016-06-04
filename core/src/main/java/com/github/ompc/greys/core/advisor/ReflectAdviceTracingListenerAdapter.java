package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.util.collection.GaStack;
import com.github.ompc.greys.core.util.collection.ThreadUnsafeGaStack;

/**
 * 反射版的方法通知调用通知适配器
 * Created by oldmanpushcart@gmail.com on 15/7/24.
 */
public abstract class ReflectAdviceTracingListenerAdapter
        extends ReflectAdviceListenerAdapter implements AdviceTracingListener {

    // 修复问题 #78
    // 在当前类的<init>调用之前JVM会先调用super.<init>, 这些步骤只能被暂时跳过
    // 所以这里需要记录下被掉过的信息
    private final ThreadLocal<GaStack<Tracing>> skipSuperInitStackRef = new ThreadLocal<GaStack<Tracing>>() {
        @Override
        protected GaStack<Tracing> initialValue() {
            return new ThreadUnsafeGaStack<Tracing>();
        }
    };

    // before()是否已经被调用,用于修正 #78 问题
    private final ThreadLocal<Boolean> isBeforeCalledRef = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    void beforeHook() {
        super.beforeHook();
        isBeforeCalledRef.set(true);
    }

    @Override
    void finishHook() {
        super.finishHook();
        isBeforeCalledRef.remove();
    }

    @Override
    final public void invokeBeforeTracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

        // 如果before()方法尚未被调用
        // 为 #78 问题所触发的情况
        if (!isBeforeCalledRef.get()) {
            skipSuperInitStackRef.get().push(new Tracing(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc));
            return;
        }

        tracingInvokeBefore(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc);
    }


    // 校验之前有多少步骤需要被跳过
    private void popSuperInit() throws Throwable {

        final GaStack<Tracing> stack = skipSuperInitStackRef.get();
        if (!stack.isEmpty()) {
            final Tracing tracing = stack.pop();
            tracingInvokeBefore(
                    tracing.tracingLineNumber,
                    tracing.tracingClassName,
                    tracing.tracingMethodName,
                    tracing.tracingMethodDesc
            );
        }

    }

    @Override
    final public void invokeThrowTracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc, String throwException) throws Throwable {
        popSuperInit();
        tracingInvokeThrowing(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc, throwException);
    }

    @Override
    final public void invokeAfterTracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {
        popSuperInit();
        tracingInvokeAfter(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc);
    }

    public void tracingInvokeBefore(
            Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

    }

    public void tracingInvokeAfter(
            Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

    }

    public void tracingInvokeThrowing(
            Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc, String throwException) throws Throwable {

    }


    static class Tracing {

        private final Integer tracingLineNumber;
        private final String tracingClassName;
        private final String tracingMethodName;
        private final String tracingMethodDesc;

        Tracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) {
            this.tracingLineNumber = tracingLineNumber;
            this.tracingClassName = tracingClassName;
            this.tracingMethodName = tracingMethodName;
            this.tracingMethodDesc = tracingMethodDesc;
        }
    }

}

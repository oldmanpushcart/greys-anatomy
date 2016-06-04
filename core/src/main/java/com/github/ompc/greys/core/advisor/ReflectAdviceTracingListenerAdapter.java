package com.github.ompc.greys.core.advisor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 反射版的方法通知调用通知适配器
 * Created by oldmanpushcart@gmail.com on 15/7/24.
 */
public abstract class ReflectAdviceTracingListenerAdapter
        extends ReflectAdviceListenerAdapter implements AdviceTracingListener {

    // 修复问题 #78
    // 在当前类的<init>调用之前JVM会先调用super.<init>, 这些步骤只能被跳过
    // 所以这里需要记录下被掉过的步数
    private final ThreadLocal<AtomicInteger> skipSuperInitRef = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
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
            skipSuperInitRef.get().incrementAndGet();
            return;
        }

        tracingInvokeBefore(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc);
    }


    // 校验之前有多少步骤需要被跳过
    private boolean skipSuperInit() {
        final AtomicInteger skipSuperInit = skipSuperInitRef.get();
        if (skipSuperInit.get() > 0) {
            skipSuperInit.decrementAndGet();
            return true;
        }

        return false;
    }

    @Override
    final public void invokeThrowTracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc, String throwException) throws Throwable {

        if (skipSuperInit()) {
            return;
        }

        tracingInvokeThrowing(tracingLineNumber, tracingClassName, tracingMethodName, tracingMethodDesc, throwException);
    }

    @Override
    final public void invokeAfterTracing(Integer tracingLineNumber, String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

        if (skipSuperInit()) {
            return;
        }

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

}

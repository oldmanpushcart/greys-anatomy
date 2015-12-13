package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.util.collection.GaStack;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 反射版的方法通知调用通知适配器
 * Created by oldmanpushcart@gmail.com on 15/7/24.
 */
public abstract class ReflectAdviceTracingListenerAdapter<PC extends ProcessContext, IC extends InnerContext>
        extends ReflectAdviceListenerAdapter<PC, IC> implements AdviceTracingListener {

    // 修复问题 #78
    // 在当前类的<init>调用之前JVM会先调用super.<init>, 这些步骤只能被跳过
    // 所以这里需要记录下被掉过的步数
    private final ThreadLocal<AtomicInteger> skipSuperInitRef = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    @Override
    final public void invokeBeforeTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

        final ProcessContextBound bound = processContextBoundRef.get();
        final PC processContext = bound.processContext;
        final GaStack<IC> innerContextGaStack = bound.innerContextGaStack;

        // 如果方法堆栈为空,说明before()方法尚未被调用
        // 为 #78 问题所触发的情况
        if( innerContextGaStack.isEmpty() ) {
            skipSuperInitRef.get().incrementAndGet();
            return;
        }

        final IC innerContext = innerContextGaStack.peek();
        invokeBeforeTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }


    private boolean skipSuperInit() {
        // 校验之前有多少步骤需要被跳过
        final AtomicInteger skipSuperInit = skipSuperInitRef.get();
        if( skipSuperInit.get() > 0 ) {
            skipSuperInit.decrementAndGet();
            return true;
        }

        return false;
    }

    @Override
    final public void invokeThrowTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

        if(skipSuperInit()) {
            return;
        }

        final ProcessContextBound bound = processContextBoundRef.get();
        final PC processContext = bound.processContext;
        final GaStack<IC> innerContextGaStack = bound.innerContextGaStack;
        final IC innerContext = innerContextGaStack.peek();
        invokeThrowTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }

    @Override
    final public void invokeAfterTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

        if(skipSuperInit()) {
            return;
        }

        final ProcessContextBound bound = processContextBoundRef.get();
        final PC processContext = bound.processContext;
        final GaStack<IC> innerContextGaStack = bound.innerContextGaStack;
        final IC innerContext = innerContextGaStack.peek();
        invokeAfterTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }

    public void invokeBeforeTracing(
            String tracingClassName, String tracingMethodName, String tracingMethodDesc,
            PC processContext, IC innerContext) throws Throwable {

    }

    public void invokeAfterTracing(
            String tracingClassName, String tracingMethodName, String tracingMethodDesc,
            PC processContext, IC innerContext) throws Throwable {

    }

    public void invokeThrowTracing(
            String tracingClassName, String tracingMethodName, String tracingMethodDesc,
            PC processContext, IC innerContext) throws Throwable {

    }

}

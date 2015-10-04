package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.util.collection.GaStack;

/**
 * 反射版的方法通知调用通知适配器
 * Created by vlinux on 15/7/24.
 */
public class ReflectAdviceTracingListenerAdapter extends ReflectAdviceListenerAdapter implements AdviceTracingListener {

    @Override
    final public void invokeBeforeTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {
        final ProcessContext processContext = processContextRef.get();
        final GaStack<InnerContext> innerContextGaStack = innerContextStackRef.get();
        final InnerContext innerContext = innerContextGaStack.peek();
        invokeBeforeTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }

    @Override
    final public void invokeAfterTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {
        final ProcessContext processContext = processContextRef.get();
        final GaStack<InnerContext> innerContextGaStack = innerContextStackRef.get();
        final InnerContext innerContext = innerContextGaStack.peek();
        invokeAfterTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }

    public void invokeBeforeTracing(
            String tracingClassName, String tracingMethodName, String tracingMethodDesc,
            ProcessContext processContext, InnerContext innerContext) throws Throwable {

    }

    public void invokeAfterTracing(
            String tracingClassName, String tracingMethodName, String tracingMethodDesc,
            ProcessContext processContext, InnerContext innerContext) throws Throwable {

    }

}

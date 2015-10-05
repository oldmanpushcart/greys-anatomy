package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.util.collection.GaStack;

/**
 * 反射版的方法通知调用通知适配器
 * Created by vlinux on 15/7/24.
 */
public abstract class ReflectAdviceTracingListenerAdapter<PC extends ProcessContext, IC extends InnerContext>
        extends ReflectAdviceListenerAdapter<PC, IC> implements AdviceTracingListener {



    @Override
    final public void invokeBeforeTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {
        final ProcessContextBound bound = processContextBoundRef.get();
        final PC processContext = bound.processContext;
        final GaStack<IC> innerContextGaStack = bound.innerContextGaStack;
        final IC innerContext = innerContextGaStack.peek();
        invokeBeforeTracing(tracingClassName, tracingMethodName, tracingMethodDesc, processContext, innerContext);
    }

    @Override
    final public void invokeAfterTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {
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

}

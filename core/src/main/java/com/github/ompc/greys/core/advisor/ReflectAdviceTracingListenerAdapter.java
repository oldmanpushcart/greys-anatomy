package com.github.ompc.greys.core.advisor;

/**
 * 反射版的方法通知调用通知适配器
 * Created by vlinux on 15/7/24.
 */
public class ReflectAdviceTracingListenerAdapter extends ReflectAdviceListenerAdapter implements AdviceTracingListener {

    @Override
    public void invokeBeforeTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

    }

    @Override
    public void invokeAfterTracing(String tracingClassName, String tracingMethodName, String tracingMethodDesc) throws Throwable {

    }

}

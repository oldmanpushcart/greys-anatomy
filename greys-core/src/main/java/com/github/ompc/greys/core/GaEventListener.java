package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.github.ompc.greys.core.listener.GreysListener;
import com.github.ompc.greys.core.util.LazyGet;

import java.util.Stack;

/**
 * Greys实现的沙箱事件监听器
 * Created by vlinux on 2017/3/2.
 */
public class GaEventListener implements EventListener {

    private final GreysListener listener;
    private final ThreadLocal<Stack<Event>> stackRef = new ThreadLocal<Stack<Event>>() {
        @Override
        protected Stack<Event> initialValue() {
            return new Stack<Event>();
        }
    };
    public GaEventListener(GreysListener listener) {
        this.listener = listener;
    }

    @Override
    public void onEvent(Event event) throws Throwable {

        switch (event.type) {

            case BEFORE: {
                stackRef.get().push(event);
                final BeforeEvent beforeEvent = (BeforeEvent) event;
                listener.before(
                        beforeEvent.javaClassLoader,
                        beforeEvent.javaClassName,
                        beforeEvent.javaMethodName,
                        beforeEvent.javaMethodDesc,
                        beforeEvent.target,
                        beforeEvent.argumentArray
                );
                break;
            }

            case RETURN: {
                final BeforeEvent beforeEvent = (BeforeEvent) stackRef.get().pop();
                final ReturnEvent returnEvent = (ReturnEvent) event;
                listener.afterReturning(
                        beforeEvent.javaClassLoader,
                        beforeEvent.javaClassName,
                        beforeEvent.javaMethodName,
                        beforeEvent.javaMethodDesc,
                        beforeEvent.target,
                        beforeEvent.argumentArray,
                        returnEvent.object
                );
                break;
            }

            case THROWS: {
                final BeforeEvent beforeEvent = (BeforeEvent) stackRef.get().pop();
                final ThrowsEvent throwsEvent = (ThrowsEvent) event;
                listener.afterThrowing(
                        beforeEvent.javaClassLoader,
                        beforeEvent.javaClassName,
                        beforeEvent.javaMethodName,
                        beforeEvent.javaMethodDesc,
                        beforeEvent.target,
                        beforeEvent.argumentArray,
                        throwsEvent.throwable
                );
                break;
            }

        }

    }

}

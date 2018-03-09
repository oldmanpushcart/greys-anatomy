package com.github.ompc.greys.module;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.Behavior;
import lombok.Data;

@Data
public class GaAdvice {

    private final ClassLoader loader;
    private final Class<?> clazz;
    private final Behavior method;
    private final Object target;
    private final Object[] params;
    private final Object returnObj;
    private final Throwable throwExp;

    public final boolean isBefore;
    public final boolean isReturn;
    public final boolean isReturning;
    public final boolean isThrow;
    public final boolean isThrows;
    public final boolean isThrowing;


    public GaAdvice(final Advice advice) {
        this.loader = advice.getBehavior().getDeclaringClass().getClassLoader();
        this.clazz = advice.getBehavior().getDeclaringClass();
        this.method = advice.getBehavior();
        this.target = advice.getTarget();
        this.params = advice.getParameterArray();
        this.returnObj = advice.getReturnObj();
        this.throwExp = advice.getThrowable();
        this.isBefore = !advice.isReturn() && !advice.isThrows();
        this.isReturn = this.isReturning = advice.isReturn();
        this.isThrow = this.isThrows = this.isThrowing = advice.isThrows();
    }

}

package com.github.ompc.greys.util;

/**
 * 通知点
 * Created by vlinux on 15/5/20.
 */
public class Advice {

    private final String className;
    private final String methodName;
    private final Object target;
    private final Object[] params;
    private final Object returnObj;
    private final Throwable throwExp;


    private final static int ACCESS_BEFORE = 1 << 0;
    private final static int ACCESS_AFTER_RETUNING = 1 << 1;
    private final static int ACCESS_AFTER_THROWING = 1 << 2;

    private final int access;

    public boolean isBefore() {
        return (access & ACCESS_BEFORE) == ACCESS_BEFORE;
    }

    public boolean isAfterReturning() {
        return (access & ACCESS_AFTER_RETUNING) == ACCESS_AFTER_RETUNING;
    }

    public boolean isAfterThrowing() {
        return (access & ACCESS_AFTER_THROWING) == ACCESS_AFTER_THROWING;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getTarget() {
        return target;
    }

    public Object[] getParams() {
        return params;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public Throwable getThrowExp() {
        return throwExp;
    }

    /**
     * for finish
     *
     * @param className  类名
     * @param methodName 方法名
     * @param target     目标类
     * @param params     调用参数
     * @param returnObj  返回值
     * @param throwExp   抛出异常
     * @param access     进入场景
     */
    private Advice(
            String className,
            String methodName,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access) {
        this.className = className;
        this.methodName = methodName;
        this.target = target;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        this.access = access;
    }


    public static Advice newForBefore(String className,
                                      String methodName,
                                      Object target,
                                      Object[] params) {
        return new Advice(
                className,
                methodName,
                target,
                params,
                null, //returnObj
                null, //throwExp
                ACCESS_BEFORE
        );
    }

    public static Advice newForAfterRetuning(String className,
                                             String methodName,
                                             Object target,
                                             Object[] params,
                                             Object returnObj) {
        return new Advice(
                className,
                methodName,
                target,
                params,
                returnObj,
                null, //throwExp
                ACCESS_AFTER_RETUNING
        );
    }

    public static Advice newForAfterThrowing(String className,
                                             String methodName,
                                             Object target,
                                             Object[] params,
                                             Throwable throwExp) {
        return new Advice(
                className,
                methodName,
                target,
                params,
                null, //returnObj
                throwExp,
                ACCESS_AFTER_THROWING
        );
    }


}

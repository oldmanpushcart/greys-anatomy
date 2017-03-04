package com.github.ompc.greys.core;

import com.github.ompc.greys.core.util.LazyGet;

/**
 * 通知点
 */
public final class Advice {

    public final ClassLoader loader;
    private final LazyGet<Class<?>> clazzRef;
    private final LazyGet<GaMethod> methodRef;
    public final Object target;
    public final Object[] params;
    public final Object returnObj;
    public final Throwable throwExp;

    private final static int ACCESS_BEFORE = 1;
    private final static int ACCESS_AFTER_RETUNING = 1 << 1;
    private final static int ACCESS_AFTER_THROWING = 1 << 2;

    public final boolean isBefore;
    public final boolean isThrow;
    public final boolean isReturn;
    public final boolean isThrowing;
    public final boolean isReturning;

    // 回放过程processId
    // use for TimeTunnelCommand.doPlay()
    // public final Integer playIndex;

    /**
     * for finish
     *
     * @param loader    类加载器
     * @param clazzRef  类
     * @param methodRef 方法
     * @param target    目标类
     * @param params    调用参数
     * @param returnObj 返回值
     * @param throwExp  抛出异常
     * @param access    进入场景
     */
    private Advice(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access) {
        this.loader = loader;
        this.clazzRef = clazzRef;
        this.methodRef = methodRef;
        this.target = target;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        isBefore = (access & ACCESS_BEFORE) == ACCESS_BEFORE;
        isThrow = (access & ACCESS_AFTER_THROWING) == ACCESS_AFTER_THROWING;
        isReturn = (access & ACCESS_AFTER_RETUNING) == ACCESS_AFTER_RETUNING;

        this.isReturning = isReturn;
        this.isThrowing = isThrow;

        // playIndex = PlayIndexHolder.getInstance().get();
    }

    /**
     * 构建Before通知点
     */
    public static Advice newForBefore(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                null, //returnObj
                null, //throwExp
                ACCESS_BEFORE
        );
    }

    /**
     * 构建正常返回通知点
     */
    public static Advice newForAfterRetuning(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Object returnObj) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                returnObj,
                null, //throwExp
                ACCESS_AFTER_RETUNING
        );
    }

    /**
     * 构建抛异常返回通知点
     */
    public static Advice newForAfterThrowing(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Throwable throwExp) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                null, //returnObj
                throwExp,
                ACCESS_AFTER_THROWING
        );
    }

    /**
     * 获取Java类
     *
     * @return Java Class
     */
    public Class<?> getClazz() {
        return clazzRef.get();
    }

    /**
     * 获取Java方法
     *
     * @return Java Method
     */
    public GaMethod getMethod() {
        return methodRef.get();
    }

}

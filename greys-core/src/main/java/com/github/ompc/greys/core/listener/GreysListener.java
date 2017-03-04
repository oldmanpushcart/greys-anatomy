package com.github.ompc.greys.core.listener;

/**
 * Greys监听器<br/>
 * Created by oldmanpushcart@gmail.com on 15/5/17.
 */
public interface GreysListener {

    /**
     * 前置通知
     *
     * @param loader        类加载器
     * @param javaClassName 类名
     * @param methodName    方法名
     * @param methodDesc    方法描述
     * @param target        目标类实例
     *                      若目标为静态方法,则为null
     * @param args          参数列表
     * @throws Throwable 通知过程出错
     */
    void before(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable;

    /**
     * 返回通知
     *
     * @param loader        类加载器
     * @param javaClassName 类名
     * @param methodName    方法名
     * @param methodDesc    方法描述
     * @param target        目标类实例
     *                      若目标为静态方法,则为null
     * @param args          参数列表
     * @param returnObject  返回结果
     *                      若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    void afterReturning(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args,
            Object returnObject) throws Throwable;

    /**
     * 异常通知
     *
     * @param loader        类加载器
     * @param javaClassName 类名
     * @param methodName    方法名
     * @param methodDesc    方法描述
     * @param target        目标类实例
     *                      若目标为静态方法,则为null
     * @param args          参数列表
     * @param throwable     目标异常
     * @throws Throwable 通知过程出错
     */
    void afterThrowing(
            ClassLoader loader, String javaClassName, String methodName, String methodDesc,
            Object target, Object[] args,
            Throwable throwable) throws Throwable;

}

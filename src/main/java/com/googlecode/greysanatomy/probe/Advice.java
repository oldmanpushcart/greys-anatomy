package com.googlecode.greysanatomy.probe;

/**
 * 通知点
 *
 * @author vlinux
 */
public class Advice {

    /**
     * 探测目标
     *
     * @author vlinux
     */
    public static class Target {

        private final String targetClassName;
        private final String targetBehaviorName;
        private final Object targetThis;
        private final Class targetClass;
        private final Class[] parameterTypes;

        public Target(String targetClassName, String targetBehaviorName, Object targetThis, Class targetClass, Class[] parameterTypes) {
            this.targetClassName = targetClassName;
            this.targetBehaviorName = targetBehaviorName;
            this.targetThis = targetThis;
            this.targetClass = targetClass;
            this.parameterTypes = parameterTypes;
        }

        /**
         * 获取探测目标类类型
         * @return 被探测目标类类型
         */
        public Class getTargetClass() {
            return targetClass;
        }

        /**
         * 获取探测目标类参数类型
         * @return 被探测目标类参数类型
         */
        public Class[] getParameterTypes() {
            return parameterTypes;
        }

        /**
         * 获取探测目标类名称
         *
         * @return 被探测的目标类名称
         */
        public String getTargetClassName() {
            return targetClassName;
        }

        /**
         * 获取探测目标行为(method/constructor)名称
         *
         * @return 被探测的行为名称
         */
        public String getTargetBehaviorName() {
            return targetBehaviorName;
        }

        /**
         * 获取探测目标实例
         *
         * @return 被探测目标实例
         */
        public Object getTargetThis() {
            return targetThis;
        }

    }

    private final Target target;        // 探测目标
    private final Object[] parameters;    // 调用参数
    private final boolean isFinished;    // 是否到doFinish方法

    private Object returnObj;            // 返回值，如果目标方法以抛异常的形式结束，则此值为null
    private Throwable throwException;    // 抛出异常，如果目标方法以正常方式结束，则此值为null

    public Advice(Target target, Object[] parameters, boolean isFinished) {
        this.target = target;
        this.parameters = parameters;
        this.isFinished = isFinished;
    }

    /**
     * 是否以抛出异常结束
     *
     * @return true:以抛异常形式结束/false:以非抛异常形式结束，或尚未结束
     */
    public boolean isThrowException() {
        return isFinished() && null != throwException;
    }

    /**
     * 是否以正常返回结束
     *
     * @return true:以正常返回形式结束/false:以非正常返回形式结束，或尚未结束
     */
    public boolean isReturn() {
        return isFinished() && !isThrowException();
    }

    /**
     * 是否已经结束
     *
     * @return true:已经结束/false:尚未结束
     */
    public boolean isFinished() {
        return isFinished;
    }

    public Target getTarget() {
        return target;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public void setReturnObj(Object returnObj) {
        this.returnObj = returnObj;
    }

    public Throwable getThrowException() {
        return throwException;
    }

    public void setThrowException(Throwable throwException) {
        this.throwException = throwException;
    }

    public Object[] getParameters() {
        return parameters;
    }

    /**
     * getParameters()方法的别名，原来的名字太TM长了
     *
     * @return 参数列表
     */
    public Object[] getParams() {
        return parameters;
    }

    /**
     * getThrowException()方法的别名
     *
     * @return 异常对象
     */
    public Throwable getThrowExp() {
        return throwException;
    }

}

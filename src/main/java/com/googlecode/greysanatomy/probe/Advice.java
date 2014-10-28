package com.googlecode.greysanatomy.probe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

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

        /*
         * 探测目标类
         */
        private final Class<?> targetClass;

        /*
         * 探测目标行为(method/constructor)
         */
        private final TargetBehavior targetBehavior;

        /*
         * 探测目标实例
         */
        private final Object targetThis;

        public Target(Class<?> targetClass, TargetBehavior targetBehavior, Object targetThis) {
            this.targetClass = targetClass;
            this.targetBehavior = targetBehavior;
            this.targetThis = targetThis;
        }

        /**
         * 获取探测目标类
         *
         * @return
         */
        public Class<?> getTargetClass() {
            return targetClass;
        }

        /**
         * 获取探测目标行为(method/constructor)
         *
         * @return
         */
        public TargetBehavior getTargetBehavior() {
            return targetBehavior;
        }

        /**
         * 获取探测目标实例
         *
         * @return
         */
        public Object getTargetThis() {
            return targetThis;
        }

    }

    /**
     * 探测目标行为(method/constructur)
     *
     * @author vlinux
     */
    public static interface TargetBehavior {

        /**
         * 获取行为的名称
         *
         * @return
         */
        String getName();

    }

    /**
     * 探测行为：构造函数探测
     *
     * @author vlinux
     */
    public static class TargetConstructor implements TargetBehavior {

        private final Constructor<?> constructor;

        public TargetConstructor(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public String getName() {
            return "<init>";
        }

        /**
         * 获取构造函数
         *
         * @return
         */
        public Constructor<?> getConstructor() {
            return constructor;
        }

    }

    /**
     * 探测行为：方法探测
     *
     * @author vlinux
     */
    public static class TargetMethod implements TargetBehavior {

        private final Method method;

        public TargetMethod(Method method) {
            this.method = method;
        }

        @Override
        public String getName() {
            return method.getName();
        }

        /**
         * 获取方法体
         *
         * @return
         */
        public Method getMethod() {
            return method;
        }

    }


    private final Target target;        // 探测目标
    private final Object[] parameters;    // 调用参数
    private final boolean isFinished;    // 是否到doFinish方法

    private Object returnObj;            // 返回值，如果目标方法以抛异常的形式结束，则此值为null
    private Throwable throwException;    // 抛出异常，如果目标方法以正常方式结束，则此值为null

    /**
     * 探测器构造函数
     *
     * @param target
     * @param parameters
     * @param isFinished
     */
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
     * @return
     */
    public Object[] getParams() {return parameters;}

    /**
     * getThrowException()方法的别名
     * @return
     */
    public Throwable getThrowExp() {return throwException;}

}

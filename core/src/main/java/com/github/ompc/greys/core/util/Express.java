package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.exception.ExpressException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.lang.reflect.Field;

import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredField;

/**
 * 表达式
 * Created by vlinux on 15/5/20.
 */
public interface Express {

    /**
     * 根据表达式获取值
     *
     * @param express 表达式
     * @return 表达式运算后的值
     * @throws ExpressException 表达式运算出错
     */
    Object get(String express) throws ExpressException;

    /**
     * 根据表达式判断是与否
     *
     * @param express 表达式
     * @return 表达式运算后的布尔值
     * @throws ExpressException 表达式运算出错
     */
    boolean is(String express) throws ExpressException;


    /**
     * 表达式工厂类
     */
    class ExpressFactory {

        /**
         * 构造表达式执行类
         *
         * @param object 执行对象
         * @return 返回表达式实现
         */
        public final static Express newExpress(Object object) {
            return new GroovyExpress(object);
        }

    }


    /**
     * Groovy实现的表达式
     */
    class GroovyExpress implements Express {

        private final Binding bind;

        public GroovyExpress(Object object) {
            bind = new Binding();
            for (Field field : object.getClass().getDeclaredFields()) {
                try {
                    bind.setVariable(field.getName(), readDeclaredField(object, field.getName(), true));
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }

        @Override
        public Object get(String express) throws ExpressException {
            try {
                return new GroovyShell(bind).evaluate(express);
            } catch (Exception e) {
                throw new ExpressException(express, e);
            }
        }

        @Override
        public boolean is(String express) throws ExpressException {
            try {
                final Object ret = get(express);
                return null != ret
                        && ret instanceof Boolean
                        && (Boolean) ret;
            } catch (Throwable t) {
                return false;
            }
        }

    }

}

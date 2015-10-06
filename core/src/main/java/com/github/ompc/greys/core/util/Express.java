package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.exception.ExpressException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.lang.reflect.Field;

import static com.github.ompc.greys.core.util.UnsafeHolder.unsafe;
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
     * 绑定变量
     *
     * @param name  变量名
     * @param value 变量值
     * @return this
     */
    Express bind(String name, Object value);


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
        public static Express newExpress(Object object) {
            return new GroovyExpress(object);
        }

    }


    abstract class UnsafeBindSupport implements Express {

        // -- unsafe offset : Advice --

        private static final long OFFSET_OF_ADVICE_LOADER;
        private static final long OFFSET_OF_ADVICE_CLAZZ;
        private static final long OFFSET_OF_ADVICE_METHOD;
        private static final long OFFSET_OF_ADVICE_TARGET;
        private static final long OFFSET_OF_ADVICE_PARAMS;
        private static final long OFFSET_OF_ADVICE_RETURN_OBJ;
        private static final long OFFSET_OF_ADVICE_THROW_EXP;
        private static final long OFFSET_OF_ADVICE_IS_BEFORE;
        private static final long OFFSET_OF_ADVICE_IS_THROW;
        private static final long OFFSET_OF_ADVICE_IS_RETURN;
        private static final long OFFSET_OF_PLAY_INDEX;

        // init advice offset

        static {
            try {
                OFFSET_OF_ADVICE_LOADER = unsafe.objectFieldOffset(Advice.class.getDeclaredField("loader"));
                OFFSET_OF_ADVICE_CLAZZ = unsafe.objectFieldOffset(Advice.class.getDeclaredField("clazz"));
                OFFSET_OF_ADVICE_METHOD = unsafe.objectFieldOffset(Advice.class.getDeclaredField("method"));
                OFFSET_OF_ADVICE_TARGET = unsafe.objectFieldOffset(Advice.class.getDeclaredField("target"));
                OFFSET_OF_ADVICE_PARAMS = unsafe.objectFieldOffset(Advice.class.getDeclaredField("params"));
                OFFSET_OF_ADVICE_RETURN_OBJ = unsafe.objectFieldOffset(Advice.class.getDeclaredField("returnObj"));
                OFFSET_OF_ADVICE_THROW_EXP = unsafe.objectFieldOffset(Advice.class.getDeclaredField("throwExp"));
                OFFSET_OF_ADVICE_IS_BEFORE = unsafe.objectFieldOffset(Advice.class.getDeclaredField("isBefore"));
                OFFSET_OF_ADVICE_IS_THROW = unsafe.objectFieldOffset(Advice.class.getDeclaredField("isThrow"));
                OFFSET_OF_ADVICE_IS_RETURN = unsafe.objectFieldOffset(Advice.class.getDeclaredField("isReturn"));
                OFFSET_OF_PLAY_INDEX = unsafe.objectFieldOffset(Advice.class.getDeclaredField("playIndex"));
            } catch (Throwable e) {
                throw new Error(e);
            }
        }

        Express bind(Advice a) {
            return bind("loader", unsafe.getObject(a, OFFSET_OF_ADVICE_LOADER))
                    .bind("clazz", unsafe.getObject(a, OFFSET_OF_ADVICE_CLAZZ))
                    .bind("method", unsafe.getObject(a, OFFSET_OF_ADVICE_METHOD))
                    .bind("target", unsafe.getObject(a, OFFSET_OF_ADVICE_TARGET))
                    .bind("params", unsafe.getObject(a, OFFSET_OF_ADVICE_PARAMS))
                    .bind("returnObj", unsafe.getObject(a, OFFSET_OF_ADVICE_RETURN_OBJ))
                    .bind("throwExp", unsafe.getObject(a, OFFSET_OF_ADVICE_THROW_EXP))
                    .bind("isBefore", unsafe.getBoolean(a, OFFSET_OF_ADVICE_IS_BEFORE))
                    .bind("isThrow", unsafe.getBoolean(a, OFFSET_OF_ADVICE_IS_THROW))
                    .bind("isReturn", unsafe.getBoolean(a, OFFSET_OF_ADVICE_IS_RETURN))
                    .bind("playIndex", unsafe.getObject(a, OFFSET_OF_PLAY_INDEX))
                    ;
        }

        Express bind(Object object) {

            if (object instanceof Advice) {
                bind((Advice) object);
            } else {
                for (Field field : object.getClass().getDeclaredFields()) {
                    try {
                        bind(field.getName(), readDeclaredField(object, field.getName(), true));
                    } catch (IllegalAccessException e) {
                        // ignore
                    }
                }
            }

            return this;
        }

    }

    /**
     * Groovy实现的表达式
     */
    class GroovyExpress extends UnsafeBindSupport implements Express {

        private final GroovyShell shell;
        private final Binding bind;

        public GroovyExpress(Object object) {
            bind = new Binding();
            bind(object);
            shell = new GroovyShell(bind);
        }

        @Override
        public Object get(String express) throws ExpressException {
            try {
                return shell.evaluate(express);
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

        @Override
        public Express bind(String name, Object value) {
            bind.setVariable(name, value);
            return this;
        }

    }

}

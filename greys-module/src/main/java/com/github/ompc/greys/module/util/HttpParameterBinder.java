package com.github.ompc.greys.module.util;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

/**
 * HTTP参数绑定器
 *
 * @param <T> 需要绑定的对象类型
 */
public class HttpParameterBinder<T> {

    private final T target;

    public HttpParameterBinder(T target) {
        this.target = target;
    }

    /**
     * HTTP参数绑定
     * 标注的属性将会根据属性类型/标注名称进行自动适配
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface HttpParamBind {

        /**
         * 是否必填参数
         *
         * @return TRUE:必填;FALSE:非必填
         */
        boolean isRequired() default false;

        /**
         * HTTP参数名
         *
         * @return HTTP参数名
         */
        String name();

    }

    /**
     * 一个被标记为必填的绑定参数未能在HTTP-PARAM集合中找到
     * <p>
     * 参数要求必填，但用户没有传递
     */
    public static class BindingRequiredException extends Exception {

        private final String name;

        BindingRequiredException(Field field, String name) {
            super(format(
                    "field %s:%s.%s was required, but HTTP-PARAM[%s] is non-values!",
                    field.getType().getName(),
                    field.getDeclaringClass().getName(),
                    field.getName(),
                    name
            ));
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /*
     * 检查必填项绑定参数
     */
    private void checkRequired(final HttpParamBind httpParamBind,
                               final Field targetField,
                               final Map<String, String[]> httpParameterMap) throws BindingRequiredException {
        if (httpParamBind.isRequired()
                && !httpParameterMap.containsKey(httpParamBind.name())) {
            throw new BindingRequiredException(targetField, httpParamBind.name());
        }
    }

    private void checkNonValues(final Field targetField,
                                final HttpParamBind httpParamBind,
                                final Object objectOfValue) throws BindingRequiredException {
        if (httpParamBind.isRequired()
                && null == objectOfValue) {
            throw new BindingRequiredException(targetField, httpParamBind.name());
        }
    }

    /*
     * 执行转换
     */
    private Object convertTo(final Field targetField,
                             final HttpParamBind httpParamBind,
                             final Map<String, String[]> httpParameterMap) {

        final String[] valueArray = httpParameterMap.get(httpParamBind.name());
        if (ArrayUtils.isEmpty(valueArray)) {
            return null;
        }

        final Class<?> targetFieldType = targetField.getType();
        final Object objectOfValue;

        // 数组转换
        if (targetFieldType.isArray()) {
            objectOfValue = StringConverter.DEFAULT.convertTo(
                    targetFieldType,
                    targetFieldType.getComponentType(),
                    valueArray
            );
        }

        // 集合转换
        else if (Collection.class.isAssignableFrom(targetFieldType)) {
            final Type genericType = targetField.getGenericType();
            final ParameterizedType parameterizedType;
            if (null != genericType
                    && genericType instanceof ParameterizedType
                    && ArrayUtils.isNotEmpty((parameterizedType = (ParameterizedType) genericType).getActualTypeArguments())) {
                final Class<?> componentType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                objectOfValue = StringConverter.DEFAULT.convertTo(
                        targetFieldType,
                        componentType,
                        valueArray
                );
            } else {
                // 这里为NULL真的好么？...
                // 不要在意这么多了，反正这个代码以后复用的概率太低了
                objectOfValue = null;
            }
        }

        // 单值转换
        else {
            objectOfValue = StringConverter.DEFAULT.convertTo(targetFieldType, valueArray[0]);
        }

        return objectOfValue;
    }

    /**
     * 绑定HTTP参数到对象属性
     *
     * @param httpParameterMap HTTP参数集合
     * @return 绑定好的目标对象
     */
    public HttpParameterBinder<T> binding(final Map<String, String[]> httpParameterMap) throws BindingRequiredException, IllegalAccessException {

        final Class<?> targetClass = target.getClass();
        for (final Field targetField : targetClass.getDeclaredFields()) {
            final HttpParamBind httpParamBind = targetField.getAnnotation(HttpParamBind.class);

            // 过滤掉没有标注参数绑定的属性
            if (null == httpParamBind) {
                continue;
            }

            checkRequired(httpParamBind, targetField, httpParameterMap);

            final Object objectOfValue = convertTo(
                    targetField,
                    httpParamBind,
                    httpParameterMap
            );

            checkNonValues(targetField, httpParamBind, objectOfValue);
            if (objectOfValue != null) {
                writeField(targetField, target, objectOfValue, true);
            }

        }

        return this;
    }

    /**
     * 构造绑定好的对象
     *
     * @return 绑定好的对象
     */
    public T build() {
        return target;
    }

}

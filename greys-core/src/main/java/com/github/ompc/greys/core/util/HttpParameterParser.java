package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.http.Param;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * HTTP解析器
 */
public class HttpParameterParser {

    /**
     * 转换器
     */
    public interface Converter {
        /**
         * 转换类型是否匹配
         *
         * @param targetType 目标类型
         * @return TRUE:目标配型匹配该转换器；FALSE：不匹配；
         */
        boolean isMatched(Class<?> targetType);
    }

    /**
     * 字符串转换器接口
     */
    interface StringConverter extends Converter {
        /**
         * 转换字符串到指定类型
         *
         * @param stringValue 字符串值
         * @param targetType  目标类型
         * @return 目标类型对象(如果转换失败, 则返回null)
         */
        Object convert(String stringValue,
                       Class<?> targetType);
    }

    /**
     * 字符串数组转换器接口
     */
    interface StringArrayConverter extends Converter {
        /**
         * 转换字符串数组到指定类型（数组、集合）
         *
         * @param stringValueArray    字符串数组
         * @param targetType          目标类型
         *                            <p>如果是转换为数组，则为int[].class、Integer[].class等</p>
         *                            <p>如果转换为集合，则为Collection&lt;Integer&gt;</p>
         * @param httpParameterParser HTTP参数解析器
         * @return 目标类型对象(如果转换失败, 则返回null)
         */
        Object convert(String[] stringValueArray,
                       Class<?> targetType,
                       HttpParameterParser httpParameterParser);
    }

    /**
     * 基础转换器集合
     */
    private final static Set<Converter> BASE_CONVERTER_SET = new LinkedHashSet<Converter>();

    /*
     * 静态初始化
     */
    static {
        BASE_CONVERTER_SET.add(
                new StringConverter() {

                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return int.class.equals(targetType)
                                || Integer.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return NumberUtils.toInt(stringValue, 0);
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {
                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return long.class.equals(targetType)
                                || Long.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return NumberUtils.toLong(stringValue, 0);
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {
                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return short.class.equals(targetType)
                                || Short.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return NumberUtils.toShort(stringValue, (short) 0);
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {
                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return char.class.equals(targetType)
                                || Character.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return StringUtils.isNotEmpty(stringValue)
                                ? stringValue.charAt(0)
                                : '\u0000';
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {
                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return boolean.class.equals(targetType)
                                || Boolean.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return BooleanUtils.toBoolean(stringValue);
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {

                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return String.class.equals(targetType);
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        return stringValue;
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringConverter() {

                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return targetType.isEnum();
                    }

                    @Override
                    public Object convert(String stringValue, Class<?> targetType) {
                        final Object[] ecArray = targetType.getEnumConstants();
                        if (null != ecArray) {
                            for (Object ec : ecArray) {
                                if (StringUtils.equals(ec.toString(), stringValue)) {
                                    return ec;
                                }
                            }
                        }
                        return null;
                    }

                }
        );
        BASE_CONVERTER_SET.add(
                new StringArrayConverter() {

                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return targetType.isArray();
                    }

                    @Override
                    public Object convert(final String[] stringValueArray,
                                          final Class<?> targetType,
                                          final HttpParameterParser httpParameterParser) {
                        if (null == stringValueArray) {
                            return null;
                        }
                        final Class<?> componentType = targetType.getComponentType();
                        final Object arrayObject = Array.newInstance(componentType, stringValueArray.length);
                        for (int index = 0; index < stringValueArray.length; index++) {
                            Array.set(
                                    arrayObject,
                                    index,
                                    httpParameterParser.convert(
                                            stringValueArray[index],
                                            targetType,
                                            httpParameterParser
                                    )
                            );
                        }
                        return arrayObject;
                    }
                }
        );
        BASE_CONVERTER_SET.add(
                new StringArrayConverter() {

                    private boolean isCollection(Class<?> targetType) {
                        return Collection.class.equals(targetType);
                    }

                    private boolean isArrayList(Class<?> targetType) {
                        return List.class.equals(targetType)
                                || ArrayList.class.isAssignableFrom(targetType);
                    }

                    private boolean isHashSet(Class<?> targetType) {
                        return Set.class.equals(targetType)
                                || HashSet.class.isAssignableFrom(targetType);
                    }

                    @Override
                    public boolean isMatched(Class<?> targetType) {
                        return isCollection(targetType)
                                || isArrayList(targetType)
                                || isHashSet(targetType);
                    }

                    private Collection<Object> newCollection(final Class<?> targetType) {
                        if (isCollection(targetType)) {
                            return new ArrayList<Object>();
                        } else if (isArrayList(targetType)) {
                            return new ArrayList<Object>();
                        } else if (isHashSet(targetType)) {
                            return new HashSet<Object>();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public Object convert(final String[] stringValueArray,
                                          final Class<?> targetType,
                                          final HttpParameterParser httpParameterParser) {

                        final Collection<Object> collection = newCollection(targetType);
                        if (null != collection) {
                            for (final String stringValue : stringValueArray) {
                                collection.add(
                                        httpParameterParser.convert(
                                                stringValue,
                                                targetType,
                                                httpParameterParser
                                        )
                                );
                            }
                        }

                        return null;
                    }


                }
        );
    }


    // 转换器集合
    private final Set<Converter> converterSet = new LinkedHashSet<Converter>();

    private HttpParameterParser() {
        // copy from base_converters
        converterSet.addAll(BASE_CONVERTER_SET);
    }

    /**
     * 获取匹配目标类型的转换器
     *
     * @param targetType 目标类型
     * @return 匹配目标类型的转换器(如果没有遇到匹配的, 则返回null)
     */
    private Converter takeConverter(final Class<?> targetType) {
        for (Converter converter : converterSet) {
            if (converter.isMatched(targetType)) {
                return converter;
            }
        }
        return null;
    }


    /**
     * HTTP参数
     */
    final class HttpParameter {

        final Class<?> parameterType;
        final Param paramAnnotation;
        final Converter converter;

        HttpParameter(final Class<?> parameterType,
                      final Param paramAnnotation) {
            this.parameterType = parameterType;
            this.paramAnnotation = paramAnnotation;
            this.converter = takeConverter(parameterType);
        }

        boolean isEffective() {
            return null != converter
                    && null != paramAnnotation
                    && StringUtils.isNotBlank(paramAnnotation.name());
        }

        private String getParameter(final Map<String, String[]> parameterMap, final String name) {
            if (!parameterMap.containsKey(name)) {
                return null;
            }
            final String[] stringArray = parameterMap.get(name);
            return ArrayUtils.isNotEmpty(stringArray)
                    ? stringArray[0]
                    : null;
        }

        private String[] getParameterValues(final Map<String, String[]> parameterMap, final String name) {
            if (!parameterMap.containsKey(name)) {
                return null;
            }
            return parameterMap.get(name);
        }

        Object parser(final Map<String, String[]> parameterMap) {

            if (!isEffective()
                    || !parameterMap.containsKey(paramAnnotation.name())) {
                return null;
            }

            if (converter instanceof StringConverter) {
                return ((StringConverter) converter).convert(
                        getParameter(parameterMap, paramAnnotation.name()),
                        parameterType
                );
            } else {
                return ((StringArrayConverter) converter).convert(
                        getParameterValues(parameterMap, paramAnnotation.name()),
                        parameterType,
                        HttpParameterParser.this
                );
            }

        }

    }

    /**
     * HTTP方法
     */
    class HttpMethod {

        final HttpParameter[] httpParameterArray;

        HttpMethod(final Method method) {

            // 参数类型
            final Class<?>[] parameterTypeArray = method.getParameterTypes();

            // 参数对应的Annotation数组
            final Annotation[][] parameterAnnotationsArray = method.getParameterAnnotations();

            this.httpParameterArray = new HttpParameter[parameterTypeArray.length];
            for (int index = 0; index < httpParameterArray.length; index++) {
                httpParameterArray[index] = new HttpParameter(
                        parameterTypeArray[index],
                        takeParam(parameterAnnotationsArray[index])
                );
            }

        }

        Param takeParam(final Annotation[] parameterAnnotations) {
            for (Annotation annotation : parameterAnnotations) {
                if (Param.class.equals(annotation.getClass())) {
                    return (Param) annotation;
                }
            }
            return null;
        }

        Object[] parse(final Map<String, String[]> httpParameterMap) {
            final Object[] parameterObjectArray = new Object[httpParameterArray.length];
            for (int index = 0; index < parameterObjectArray.length; index++) {
                parameterObjectArray[index] = httpParameterArray[index].parser(httpParameterMap);
            }
            return parameterObjectArray;
        }

    }

    /**
     * 将字符串转换为目标类型值
     *
     * @param stringValue     字符串值
     * @param targetType      目标类型
     * @param parameterParser HTTP参数解析器
     * @return 目标类型对象(如果转换失败, 则返回null)
     */
    private Object convert(final String stringValue,
                          final Class<?> targetType,
                          final HttpParameterParser parameterParser) {
        for (final Converter converter : parameterParser.converterSet) {
            if (null != converter
                    && converter instanceof StringConverter
                    && converter.isMatched(targetType)) {
                return ((StringConverter) converter).convert(stringValue, targetType);
            }
        }
        return null;
    }

    /**
     * 构造HTTP参数解析器
     *
     * @param converterArray 转换器数组
     * @return HTTP参数解析器
     */
    public static HttpParameterParser build(final Converter... converterArray) {
        final HttpParameterParser parser = new HttpParameterParser();
        if (ArrayUtils.isNotEmpty(converterArray)) {
            for (final Converter converter : converterArray) {
                parser.reg(converter);
            }
        }
        return parser;
    }

    /**
     * 注册转换器
     *
     * @param converter 转换器
     */
    public final HttpParameterParser reg(final Converter converter) {
        converterSet.add(converter);
        return this;
    }

    /**
     * 解析HTTP请求参数Map为对应处理方法{@code httpMethod}的入参数组
     *
     * @param httpMethod       HTTP请求处理方法
     * @param httpParameterMap HTTP请求参数Map
     * @return {@code httpMethod}入参数组
     */
    public Object[] parser(final Method httpMethod,
                           final Map<String, String[]> httpParameterMap) {
        return new HttpMethod(httpMethod).parse(httpParameterMap);
    }


}

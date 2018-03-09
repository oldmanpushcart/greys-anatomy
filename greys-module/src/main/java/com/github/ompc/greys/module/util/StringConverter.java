package com.github.ompc.greys.module.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Array;
import java.util.*;

import static com.github.ompc.greys.module.util.StringConverter.TypeMatching.Any.array;

/**
 * 字符串转换器
 *
 * @author oldmanpushcart@gmail.com
 */
public class StringConverter {

    public static final StringConverter DEFAULT = new StringConverter()
            .any(array(int.class, Integer.class), new Converter<Integer>() {
                @Override
                public Integer convert(Class<Integer> type, String string) {
                    return NumberUtils.toInt(string);
                }
            })
            .any(array(long.class, Long.class), new Converter<Long>() {
                @Override
                public Long convert(Class<Long> type, String string) {
                    return NumberUtils.toLong(string);
                }
            })
            .any(array(short.class, Short.class), new Converter<Short>() {
                @Override
                public Short convert(Class<Short> type, String string) {
                    return NumberUtils.toShort(string);
                }
            })
            .any(array(double.class, Double.class), new Converter<Double>() {
                @Override
                public Double convert(Class<Double> type, String string) {
                    return NumberUtils.toDouble(string);
                }
            })
            .any(array(float.class, Float.class), new Converter<Float>() {
                @Override
                public Float convert(Class<Float> type, String string) {
                    return NumberUtils.toFloat(string);
                }
            })
            .any(array(boolean.class, Boolean.class), new Converter<Boolean>() {
                @Override
                public Boolean convert(Class<Boolean> type, String string) {
                    return BooleanUtils.toBoolean(string);
                }
            })
            .any(array(String.class), new Converter<String>() {
                @Override
                public String convert(Class<String> type, String string) {
                    return string;
                }
            })
            .like(Enum.class, new Converter<Enum<?>>() {
                @Override
                public Enum<?> convert(Class<Enum<?>> type, String string) {
                    final Object[] ecArray = type.getEnumConstants();
                    if (null != ecArray) {
                        for (final Object ec : ecArray) {
                            if (ec.toString().equals(string)) {
                                return (Enum<?>) ec;
                            }
                        }
                    }
                    return null;
                }
            });


    interface Converter<T> {

        /**
         * 字符串转换为目标对象
         * <p>
         * 如果转换失败则返回null
         *
         * @param type   待转换的目标类型
         * @param string 待转换的字符串
         * @return 目标对象
         */
        T convert(Class<T> type, String string);

    }

    interface TypeMatching {

        boolean isMatch(Class<?> targetType);

        class Any implements TypeMatching {

            private final Class<?>[] typeArray;

            public Any(Class<?>... typeArray) {
                this.typeArray = typeArray;
            }

            @Override
            public boolean isMatch(Class<?> targetType) {
                return ArrayUtils.contains(typeArray, targetType);
            }

            public static Class<?>[] array(Class<?>... classes) {
                return classes;
            }

        }

        class Like implements TypeMatching {

            private final Class<?> type;

            public Like(Class<?> type) {
                this.type = type;
            }

            @Override
            public boolean isMatch(Class<?> targetType) {
                return type.isAssignableFrom(targetType);
            }
        }

    }

    private class TypeMatchingConverter {

        final TypeMatching matching;
        final Converter converter;

        TypeMatchingConverter(TypeMatching matching, Converter converter) {
            this.matching = matching;
            this.converter = converter;
        }

    }

    private final ArrayList<TypeMatchingConverter> typeMatchingConverters
            = new ArrayList<TypeMatchingConverter>();


    public StringConverter any(Class<?>[] typeArray, Converter converter) {
        typeMatchingConverters.add(new TypeMatchingConverter(
                new TypeMatching.Any(typeArray),
                converter
        ));
        return this;
    }

    public StringConverter like(Class<?> type, Converter converter) {
        typeMatchingConverters.add(new TypeMatchingConverter(
                new TypeMatching.Like(type),
                converter
        ));
        return this;
    }

    /**
     * 将字符串转换为指定类型的对象实例
     *
     * @param type   指定类型类
     * @param string 字符串
     * @param <T>    指定类型
     * @return 指定类型的对象实例
     */
    public <T> T convertTo(Class<T> type, String string) {
        for (final TypeMatchingConverter typeMatchingConverter : typeMatchingConverters) {
            if (typeMatchingConverter.matching.isMatch(type)) {
                return (T) typeMatchingConverter.converter.convert(type, string);
            }
        }
        return null;
    }

    /**
     * 将字符串数组转换为指定集合/数组对象实例
     *
     * @param type          指定集合/数组类型
     * @param componentType 集合/数组的组件类型
     * @param strings       字符串数组
     * @param <T>           指定集合/数组类型
     * @return 指定集合/数组对象实例
     */
    public <T> T convertTo(Class<T> type, final Class<?> componentType, String... strings) {
        if (type.isArray()) {
            final Object arrayObject = Array.newInstance(componentType, strings.length);
            for (int index = 0; index < strings.length; index++) {
                Array.set(arrayObject, index, convertTo(componentType, strings[index]));
            }
            return (T) arrayObject;
        } else if (type.equals(Collection.class)
                || type.equals(List.class)
                || type.equals(ArrayList.class)) {
            final ArrayList<Object> arrayList = new ArrayList<Object>();
            for (final String string : strings) {
                arrayList.add(convertTo(componentType, string));
            }
            return (T) arrayList;
        } else if (type.equals(Set.class)
                || type.equals(HashSet.class)) {
            final HashSet<Object> hashSet = new HashSet<Object>();
            for (final String string : strings) {
                hashSet.add(convertTo(componentType, string));
            }
            return (T) hashSet;
        }
        return null;
    }

}

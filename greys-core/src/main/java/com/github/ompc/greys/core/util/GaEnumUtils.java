package com.github.ompc.greys.core.util;

import org.apache.commons.lang3.EnumUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 枚举工具类
 * Created by vlinux on 2016/12/10.
 */
public class GaEnumUtils {

    /**
     * 字符串转换枚举
     *
     * @param enumClass    枚举类型
     * @param value        字符串值
     * @param defaultValue 默认枚举值
     * @param <T>          枚举类型
     * @return 转换后的枚举值，如果字符串不存在对应的枚举值，则采用默认枚举值
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumClass, String value, T defaultValue) {
        final T e = EnumUtils.getEnum(enumClass, value);
        return null == e ? defaultValue : e;
    }


    /**
     * 字符串数组转换枚举集合
     *
     * @param enumClass        枚举类型
     * @param valueStringArray 字符串值数组
     * @param defaultArray     默认枚举数组
     * @param <T>              枚举类型
     * @return 转换后的枚举集合，如果字符串不存在对应的枚举值，则采用默认的枚举数组
     */
    public static <T extends Enum<T>> Set<T> valueOf(Class<T> enumClass, String[] valueStringArray, T[] defaultArray) {

        final Set<T> valueSet = new LinkedHashSet<T>();

        // 将字符串数组转换为枚举数组
        for (final String valueString : valueStringArray) {
            final T value = valueOf(enumClass, valueString, null);
            if (null != value) {
                valueSet.add(value);
            }
        }

        // 如果转换出来的集合为空，则需要从默认数组中取
        if (valueSet.isEmpty()
                && null != defaultArray) {
            for (final T defaultValue : defaultArray) {
                valueSet.add(defaultValue);
            }
        }

        return valueSet;
    }

}

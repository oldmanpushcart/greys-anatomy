package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.GlobalOptions;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

/**
 * 阿里巴巴 EagleEye 中间件工具类
 * Created by vlinux on 16/9/24.
 */
public class AliEagleEyeUtils {

    private static final String ILLEGAL_EAGLE_EYE_TRACE_ID = "-1";
    private static final String EAGLE_EYE_CLASS_NAME = "com.taobao.eagleeye.EagleEye";
    private static final String GET_TRACE_ID_NAME = "getTraceId";

    /**
     * 获取EagleEyeId
     *
     * @param loader 目标ClassLoader
     * @return EagleEyeId
     */
    public static String getTraceId(final ClassLoader loader) {
        if (!GlobalOptions.isEnableTraceId) {
            return ILLEGAL_EAGLE_EYE_TRACE_ID;
        }
        final Thread currentThread = Thread.currentThread();
        final ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(loader);
        try {
            final Class<?> classOfEagleEye = loader.loadClass(EAGLE_EYE_CLASS_NAME);
            final Method methodOfGetTraceId = classOfEagleEye.getMethod(GET_TRACE_ID_NAME);
            final Object returnOfGetTraceId = methodOfGetTraceId.invoke(null);
            if (null != returnOfGetTraceId
                    && returnOfGetTraceId instanceof String
                    && StringUtils.isNoneBlank((String) returnOfGetTraceId)) {
                return (String) returnOfGetTraceId;
            } else {
                return ILLEGAL_EAGLE_EYE_TRACE_ID;
            }
        } catch (Throwable t) {
            return ILLEGAL_EAGLE_EYE_TRACE_ID;
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * 判断是否支持EagleEye
     *
     * @param eagleEyeTraceId 目标EagleEyeId
     * @return true:支持EagleEye;false:不支持;
     */
    public static boolean isEagleEyeSupport(final String eagleEyeTraceId) {
        return !StringUtils.equals(ILLEGAL_EAGLE_EYE_TRACE_ID, eagleEyeTraceId);
    }

}

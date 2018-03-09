package com.github.ompc.greys.module.util;

import org.apache.commons.io.IOUtils;

/**
 * GREYS字符串处理工具类
 */
public class GaStringUtils {

    private static final Class<?> CLAZZ = GaStringUtils.class;

    /**
     * 获取GREYS服务端版本号
     *
     * @return GREYS服务端版本号
     */
    public static String getVersion() {
        try {
            return IOUtils.toString(CLAZZ.getResourceAsStream("/com/github/ompc/greys/module/res/version"));
        } catch (Throwable cause) {
            throw new UnCaughtException(cause);
        }
    }

    /**
     * 获取GREYS服务端LOGO
     *
     * @return GREYS服务端LOGO
     */
    public static String getLogo() {
        try {
            return IOUtils.toString(CLAZZ.getResourceAsStream("/com/github/ompc/greys/module/res/logo.txt"));
        } catch (Throwable cause) {
            throw new UnCaughtException(cause);
        }
    }

    /**
     * 获取GREYS服务端感谢名单
     *
     * @return GREYS服务端感谢名单
     */
    public static String getThanks() {
        try {
            return IOUtils.toString(CLAZZ.getResourceAsStream("/com/github/ompc/greys/module/res/thanks.json"));
        } catch (Throwable cause) {
            throw new UnCaughtException(cause);
        }
    }

}

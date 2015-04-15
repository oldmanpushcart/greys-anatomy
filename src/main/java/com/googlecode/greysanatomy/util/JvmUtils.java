package com.googlecode.greysanatomy.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JVM操作相关工具类
 *
 * @author vlinux
 */
public class JvmUtils {

    private static final Logger logger = LogUtils.getLogger();

    /**
     * 关闭钩子
     *
     * @author vlinux
     */
    public static interface ShutdownHook {

        /**
         * 尝试关闭
         *
         * @throws Throwable
         */
        void shutdown() throws Throwable;

    }

    /**
     * 向JVM注册一个关闭的Hook
     *
     * @param name
     * @param shutdownHook
     */
    public static void registShutdownHook(final String name, final ShutdownHook shutdownHook) {

        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, String.format("reg shutdown hook %s.", name));
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    shutdownHook.shutdown();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, String.format("%s shutdown success.", name));
                    }
                } catch (Throwable t) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, String.format("%s shutdown failed, ignore it.", name), t);
                    }
                }
            }

        });

    }

}

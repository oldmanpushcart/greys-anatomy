package com.googlecode.greysanatomy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVM操作相关工具类
 *
 * @author vlinux
 */
public class JvmUtils {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

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

        logger.info("regist shutdown hook {}", name);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    shutdownHook.shutdown();
                    logger.info("{} shutdown successed.", name);
                } catch (Throwable t) {
                    logger.warn("{} shutdown failed, ignore it.", name, t);
                }
            }

        });

    }

}

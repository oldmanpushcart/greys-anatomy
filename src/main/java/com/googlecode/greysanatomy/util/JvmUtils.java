package com.googlecode.greysanatomy.util;

import static com.googlecode.greysanatomy.util.LogUtils.info;
import static com.googlecode.greysanatomy.util.LogUtils.warn;

/**
 * JVM操作相关工具类
 *
 * @author vlinux
 */
public class JvmUtils {


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

        info("reg shutdown hook %s.", name);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    shutdownHook.shutdown();
                    info("%s shutdown success.", name);
                } catch (Throwable t) {
                    warn(t, "%s shutdown failed, ignore it.", name);
                }
            }

        });

    }

}

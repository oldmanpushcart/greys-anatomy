package com.github.ompc.greys.util;

import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IO工具类
 * Created by vlinux on 15/5/2.
 */
public class IOUtil {

    private static final Logger logger = LogUtil.getLogger();

    public static void close(Selector selector) {

        if (null != selector) {
            try {
                selector.close();
            } catch (Throwable t) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("close failed, selector=%s", selector));
                }
            }
        }

    }

    public static void close(SocketChannel socketChannel) {

        if (null != socketChannel) {
            try {
                socketChannel.close();
            } catch (Throwable t) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("close failed, SocketChannel=%s", socketChannel));
                }
            }
        }

    }

    public static void close(ServerSocketChannel serverSocketChannel) {

        if (null != serverSocketChannel) {
            try {
                serverSocketChannel.close();
            } catch (Throwable t) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("close failed, ServerSocketChannel=%s", serverSocketChannel));
                }
            }
        }

    }


}

package com.github.ompc.greys.util;

import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * IO工具类
 * Created by vlinux on 15/5/2.
 */
public class IOUtil {

    public static void close(Selector selector) {

        if (null != selector) {
            try {
                selector.close();
            } catch (Throwable t) {
                //
            }
        }

    }

    public static void close(SocketChannel socketChannel) {

        if (null != socketChannel) {
            try {
                socketChannel.close();
            } catch (Throwable t) {
                //
            }
        }

    }

    public static void close(ServerSocketChannel serverSocketChannel) {

        if (null != serverSocketChannel) {
            try {
                serverSocketChannel.close();
            } catch (Throwable t) {
                //
            }
        }

    }


    public static void close(Writer writer) {

        if (null != writer) {
            try {
                writer.close();
            } catch (Throwable t) {
                //
            }
        }

    }

    public static void close(Reader reader) {

        if (null != reader) {
            try {
                reader.close();
            } catch (Throwable t) {
                //
            }
        }

    }

    public static void close(Socket socket) {

        if (null != socket) {
            try {
                socket.close();
            } catch (Throwable t) {
                //
            }
        }

    }


}

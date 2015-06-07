package com.github.ompc.greys.server;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * 会话管理
 * Created by vlinux on 15/5/2.
 */
public interface SessionManager {

    /**
     * 创建一个会话
     *
     * @param javaPid       Java进程号
     * @param socketChannel 会话所对应的SocketChannel
     * @param charset       会话所用字符集
     * @return 创建的会话
     */
    Session newSession(int javaPid, SocketChannel socketChannel, Charset charset);

    /**
     * 获取一个会话
     *
     * @param sessionId 会话ID
     * @return 返回会话
     */
    Session getSession(int sessionId);

    /**
     * 关闭所有会话
     */
    void clean();

    /**
     * 是否已经被销毁
     *
     * @return true/false
     */
    boolean isDestroy();

    /**
     * 销毁会话管理器所管理的所有会话
     */
    void destroy();

}

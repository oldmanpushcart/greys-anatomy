package com.googlecode.greysanatomy.server;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Greys 会话管理
 * Created by vlinux on 15/5/2.
 */
public interface GaSessionManager {

    /**
     * 创建一个会话
     *
     * @param socketChannel 会话所对应的SocketChannel
     * @param charset       会话所用字符集
     * @return 创建的会话
     */
    GaSession newGaSession(SocketChannel socketChannel, Charset charset);

    /**
     * 获取一个会话
     *
     * @param gaSessionId 会话ID
     * @return 返回会话
     */
    GaSession getGaSession(int gaSessionId);

    /**
     * 关闭所有会话
     */
    void clean();

    /**
     * 销毁会话管理器所管理的所有会话
     */
    void destroy();

}

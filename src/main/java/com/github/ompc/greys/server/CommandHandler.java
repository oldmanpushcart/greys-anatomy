package com.github.ompc.greys.server;

import java.io.IOException;

/**
 * 命令处理器
 * Created by vlinux on 15/5/3.
 */
public interface CommandHandler {

    /**
     * 解析输入行并执行命令
     *
     * @param line      输入的命令行
     * @param session 会话
     * @throws IOException IO错误
     */
    void executeCommand(final String line, final Session session) throws IOException;

}

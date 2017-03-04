package com.github.ompc.greys.core.message;

/**
 * 命令消息
 * Created by vlinux on 2017/3/4.
 */
public class CommandMessage<T> extends GaMessage {

    private final String command;
    private final T data;

    /**
     * 构造命令消息
     *
     * @param command 消息类型
     * @param data    命令结果数据
     */
    public CommandMessage(String command, final T data) {
        super("COMMAND");
        this.command = command;
        this.data = data;
    }

    public String getCommand() {
        return command;
    }

    public T getData() {
        return data;
    }
}

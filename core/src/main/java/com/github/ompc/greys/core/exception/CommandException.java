package com.github.ompc.greys.core.exception;

/**
 * 命令执行错误
 * Created by vlinux on 15/5/2.
 */
public class CommandException extends Exception {

    private final String command;


    public CommandException(String command) {
        this.command = command;
    }

    public CommandException(String command, Throwable cause) {
        super(cause);
        this.command = command;
    }

    /**
     * 获取出错命令
     *
     * @return 命令名称
     */
    public String getCommand() {
        return command;
    }

}

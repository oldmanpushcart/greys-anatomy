package com.github.ompc.greys.client;

/**
 * 命令不存在
 */
public class CommandNotFoundException extends Exception {

    private final String command;

    public CommandNotFoundException(String command) {
        super("command " + command + " not found.");
        this.command = command;
    }

    /**
     * 获取不存在的命令
     *
     * @return 不存在的命令
     */
    public String getCommand() {
        return command;
    }

}

package com.googlecode.greysanatomy.exception;

/**
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

    public String getCommand() {
        return command;
    }

}

package com.github.ompc.greys.client.command;

public class CommandParserException extends Exception {

    private final String command;

    public CommandParserException(final String command,
                                  final Throwable cause) {
        super(cause);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}

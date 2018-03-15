package com.github.ompc.greys.console.command.impl;

import picocli.CommandLine;

@CommandLine.Command(name = "quit")
public class QuitGaCommand extends BaseCommand {

    @Override
    public void execute() {
        consoleOut("Bye!");
        System.exit(0);
    }

    @Override
    public void terminate() {

    }
}

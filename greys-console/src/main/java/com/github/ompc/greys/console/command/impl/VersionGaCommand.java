package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.console.command.GaCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.io.Writer;

@Command(name = "version")
public class VersionGaCommand implements GaCommand {

    @Override
    public void execute(PrintWriter writer) throws Throwable {

    }

}

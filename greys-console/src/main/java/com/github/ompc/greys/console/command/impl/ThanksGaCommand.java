package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.protocol.GreysProtocol;
import picocli.CommandLine;

@CommandLine.Command(name = "thanks")
public class ThanksGaCommand extends CommonGaCommand {

    @Override
    protected void onGp(GreysProtocol<?> gp) throws Throwable {

    }

}

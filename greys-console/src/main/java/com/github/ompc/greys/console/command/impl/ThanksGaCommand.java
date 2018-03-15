package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.console.render.GpRender;
import com.github.ompc.greys.console.render.ThanksGpRender;
import com.github.ompc.greys.protocol.GpType;
import com.github.ompc.greys.protocol.GreysProtocol;
import picocli.CommandLine;

import static com.github.ompc.greys.protocol.GpType.THANKS;

@CommandLine.Command(name = "thanks")
public class ThanksGaCommand extends GpCommand {

    @Override
    protected void onGp(GreysProtocol<?> gp) {
        consoleOut(
                new GpRender.Builder()
                        .append(THANKS, new ThanksGpRender())
                        .build()
                        .rendering(gp)
        );
    }

}

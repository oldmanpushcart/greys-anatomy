package com.github.ompc.greys.console.command.impl;

import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine;

import java.io.IOException;

import static java.lang.String.format;

@CommandLine.Command(name = "shutdown")
public class ShutdownGaCommand extends BaseCommand {

    @Override
    protected void execute() throws IOException {
        final Response response = getHttpClient().newCall(
                new Request.Builder()
                        .url(format("http://%s:%s/sandbox/%s/module/http/control/shutdown",
                                getConfig().getIp(),
                                getConfig().getPort(),
                                getConfig().getNamespace()))
                        .build()
        ).execute();
        if (!response.isSuccessful()) {
            handleError(response);
            return;
        }
        consoleOut("Greys shutting down completed.");
        System.exit(0);
    }

    @Override
    public void terminate() {

    }

}

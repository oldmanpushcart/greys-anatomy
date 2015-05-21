package com.github.ompc.greys.command;

import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.server.Session;

import java.lang.instrument.Instrumentation;

/**
 * 关闭命令
 * Created by vlinux on 14/10/23.
 */
@Cmd(named = "shutdown", sort = 11, desc = "Shutdown the greys server, and exit the console.",
        eg = {
                "shutdown"
        })
public class ShutdownCommand implements Command {

    @Override
    public Action getAction() {
        return new SilentAction() {

            @Override
            public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                sender.send(true, "Greys shutdown completed.\n");
            }

        };
    }
}

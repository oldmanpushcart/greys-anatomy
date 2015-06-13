package com.github.ompc.greys.command;

import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.server.Session;

import java.lang.instrument.Instrumentation;

/**
 * 退出命令
 * Created by vlinux on 15/5/18.
 */
@Cmd(name = "quit", sort = 8, summary = "Quit the Greys console.",
        eg = {
                "quit"
        })
public class QuitCommand implements Command {

    @Override
    public Action getAction() {
        return new SilentAction() {

            @Override
            public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                sender.send(true, "Bye!\n");
            }

        };
    }
}

package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.server.ConsoleServer;

/**
 * πÿ±’√¸¡Ó
 * Created by vlinux on 14/10/23.
 */
@Cmd("shutdown")
public class ShutdownCommand extends Command {

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {
                consoleServer.shutdown();
                sender.send(true, "Greys shutdown complated.");
            }

        };
    }

}

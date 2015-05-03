package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.server.GaSession;

/**
 * πÿ±’√¸¡Ó
 * Created by vlinux on 14/10/23.
 */
@Cmd(named = "shutdown", sort = 11, desc = "Shutdown the greys's RMI service, and exit the console.",
        eg = {
                "shutdown"
        })
public class ShutdownCommand extends Command {

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaSession gaSession, final Info info, final Sender sender) throws Throwable {
                sender.send(true, "Greys shutdown completed.");
            }

        };
    }

}

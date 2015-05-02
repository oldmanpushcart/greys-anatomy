package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.server.GaServer;

/**
 * ÍË³öÃüÁî
 * Created by vlinux on 14/11/1.
 */
@Cmd(named = "quit", sort = 8, desc = "Quit the Greys console.",
        eg = {
                "quit"
        })
public class QuitCommand extends Command {
    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaServer gaServer, final Info info, final Sender sender) throws Throwable {
                sender.send(true, "Bye bye!\n");
            }

        };
    }
}

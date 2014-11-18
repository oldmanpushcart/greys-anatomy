package com.googlecode.greysanatomy.console.command;

<<<<<<< HEAD
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
=======
>>>>>>> pr/8
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.server.ConsoleServer;

/**
 * πÿ±’√¸¡Ó
 * Created by vlinux on 14/10/23.
 */
<<<<<<< HEAD
@Cmd("shutdown")
@RiscCmd(named = "shutdown", sort = 9, desc = "Shutdown the greys's RMI service, and exit the console.")
=======
@RiscCmd(named = "shutdown", sort = 9, desc = "Shutdown the greys's RMI service, and exit the console.",
        eg = {
                "shutdown"
        })
>>>>>>> pr/8
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

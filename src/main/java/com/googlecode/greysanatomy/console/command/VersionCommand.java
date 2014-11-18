package com.googlecode.greysanatomy.console.command;

<<<<<<< HEAD
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
=======
>>>>>>> pr/8
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.util.GaStringUtils;

/**
 * Êä³ö°æ±¾
 *
 * @author vlinux
 */
<<<<<<< HEAD
@Cmd("version")
@RiscCmd(named = "version", sort = 8, desc = "Output the target's greys version")
=======
@RiscCmd(named = "version", sort = 8, desc = "Output the target's greys version",
        eg = {
                "version"
        })
>>>>>>> pr/8
public class VersionCommand extends Command {

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {
                sender.send(true, GaStringUtils.getLogo());
            }

        };
    }

}

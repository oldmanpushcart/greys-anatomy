package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.server.GaServer;
import com.googlecode.greysanatomy.util.GaStringUtils;

/**
 * Êä³ö°æ±¾
 *
 * @author vlinux
 */
@Cmd(named = "version", sort = 8, desc = "Output the target's greys version",
        eg = {
                "version"
        })
public class VersionCommand extends Command {

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaServer gaServer, final Info info, final Sender sender) throws Throwable {
                sender.send(true, GaStringUtils.getLogo());
            }

        };
    }

}

package com.github.ompc.greys.command;


import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.util.GaStringUtils.getLogo;

/**
 * 输出版本
 *
 * @author vlinux
 */
@Cmd(named = "version", sort = 9, desc = "Output the target's greys version",
        eg = {
                "version"
        })
public class VersionCommand implements Command {

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                sender.send(true, getLogo());
                return new RowAffect(1);
            }

        };
    }

}

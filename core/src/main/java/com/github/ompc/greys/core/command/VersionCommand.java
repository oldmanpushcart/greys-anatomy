package com.github.ompc.greys.core.command;


import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.server.Session;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.core.util.GaStringUtils.getLogo;

/**
 * 输出版本
 *
 * @author vlinux
 */
@Cmd(name = "version", sort = 9, summary = "Display Greys version",
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

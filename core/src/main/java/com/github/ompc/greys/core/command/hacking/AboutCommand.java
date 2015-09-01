package com.github.ompc.greys.core.command.hacking;

import com.github.ompc.greys.core.command.Command;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.server.Session;

import java.lang.instrument.Instrumentation;

/**
 * 工具介绍<br/>
 * 感谢
 * Created by vlinux on 15/9/1.
 */
@Cmd(isHacking = true, name = "about", summary = "About",
        eg = {
                "about"
        }
)
public class AboutCommand implements Command {

    @Override
    public Action getAction() {
        return new SilentAction() {

            @Override
            public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {

            }
        };
    }

}

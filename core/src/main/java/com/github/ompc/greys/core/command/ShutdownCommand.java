package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.Enhancer;
import com.github.ompc.greys.core.advisor.Spy;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import com.github.ompc.greys.core.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;

/**
 * 关闭命令
 * Created by vlinux on 14/10/23.
 */
@Cmd(name = "shutdown", sort = 11, summary = "Shut down Greys server and exit the console",
        eg = {
                "shutdown"
        })
public class ShutdownCommand implements Command {

    @Override
    public Action getAction() {
        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                // 退出之前需要重置所有的增强类
                // 重置之前增强的类
                final EnhancerAffect enhancerAffect = Enhancer.reset(
                        inst,
                        new Matcher.WildcardMatcher("*")
                );

                // 重置整个greys
                Spy.AGENT_RESET_METHOD.invoke(null);

                printer.println("Greys Server is shut down.").finish();
                return new RowAffect(enhancerAffect.cCnt());
            }

        };
    }

}

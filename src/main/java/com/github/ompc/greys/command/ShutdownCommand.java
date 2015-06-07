package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.Enhancer;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.affect.EnhancerAffect;
import com.github.ompc.greys.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.util.Properties;

import static com.github.ompc.greys.agent.AgentLauncher.KEY_GREYS_CLASS_LOADER;

/**
 * 关闭命令
 * Created by vlinux on 14/10/23.
 */
@Cmd(named = "shutdown", sort = 11, desc = "Shutdown the greys server, and exit the console.",
        eg = {
                "shutdown"
        })
public class ShutdownCommand implements Command {

    @Override
    public Action getAction() {
        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                // 退出之前需要重置所有的增强类
                final EnhancerAffect enhancerAffect = Enhancer.reset(
                        inst,
                        new Matcher.WildcardMatcher("*")
                );

                // 去掉ClassLoader，逼迫下次重新加载
                // 这样就不需要重启应用才能得到最新版的Greys了
                final Properties props = System.getProperties();
                props.remove(KEY_GREYS_CLASS_LOADER);

                sender.send(true, "Greys shutdown completed.\n");
                return new RowAffect(enhancerAffect.cCnt());
            }

        };
    }
}

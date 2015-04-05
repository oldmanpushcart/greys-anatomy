package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.annotation.IndexArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;

import java.lang.instrument.Instrumentation;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.regJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author vlinux
 */
@Cmd(named = "stack", sort = 7, desc = "The call stack output buried point method in each thread.",
        eg = {
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack *StringUtils isBlank"
        })
public class StackCommand extends Command {

    @IndexArg(index = 0, name = "class-wildcard", description = "wildcard match of classpath.classname")
    private String classWildcard;

    @IndexArg(index = 1, name = "method-wildcard", description = "wildcard match of method name")
    private String methodWildcard;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, Info info, final Sender sender) throws Throwable {

                final Instrumentation inst = info.getInst();
                final TransformResult result = transform(inst, classWildcard, methodWildcard, new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(Advice p) {

                        final String stackStr = GaStringUtils.getStack() + "\n";
                        sender.send(false, stackStr);

                    }

                }, info, false);

                // 注册任务
                regJob(info.getSessionId(), result.getId());

                // 激活任务
                activeJob(result.getId());

                final StringBuilder message = new StringBuilder();
                message.append(GaStringUtils.LINE);
                message.append(String.format("done. probe:c-Cnt=%s,m-Cnt=%s\n",
                        result.getModifiedClasses().size(),
                        result.getModifiedBehaviors().size()));
                message.append(GaStringUtils.ABORT_MSG).append("\n");
                sender.send(false, message.toString());

            }

        };
    }


}

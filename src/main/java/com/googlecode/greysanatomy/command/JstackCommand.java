package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.IndexArg;
import com.googlecode.greysanatomy.command.annotation.NamedArg;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.server.GaSession;
import com.googlecode.greysanatomy.util.GaStringUtils;

import java.lang.instrument.Instrumentation;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author vlinux
 */
@Cmd(named = "jstack", sort = 7, desc = "The call stack output buried point method in each thread.",
        eg = {
                "stack -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack *StringUtils isBlank"
        })
public class JstackCommand extends Command {

    @IndexArg(index = 0, name = "class-pattern", description = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", description = "pattern matching of method name")
    private String methodPattern;

    @NamedArg(named = "S", description = "including sub class")
    private boolean isSuper = false;

    @NamedArg(named = "E", description = "enable the regex pattern matching")
    private boolean isRegEx = false;

    /**
     * 命令是否启用正则表达式匹配
     *
     * @return true启用正则表达式/false不启用
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaSession gaSession, Info info, final Sender sender) throws Throwable {

                final Instrumentation inst = info.getInst();
                final TransformResult result = transform(inst, classPattern, methodPattern, isSuper, isRegEx(), new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(Advice p) {

                        final String stackStr = GaStringUtils.getStack() + "\n";
                        sender.send(false, stackStr);

                    }

                }, info, false);

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

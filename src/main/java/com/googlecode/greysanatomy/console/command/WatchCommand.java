package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.*;
import com.googlecode.greysanatomy.console.command.parameter.WatchPointEnum;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.instrument.Instrumentation;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

@Cmd("watch")
@RiscCmd(named = "watch", sort = 4, desc = "The call context information buried point observation methods.")
public class WatchCommand extends Command {

    @Arg(name = "class", isRequired = true)
    @RiscIndexArg(index = 0, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @Arg(name = "method", isRequired = true)
    @RiscIndexArg(index = 1, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

    @Arg(name = "exp", isRequired = true)
    @RiscIndexArg(index = 2, name = "express",
            description = "expression, write by javascript. use 'p.' before express",
            description2 = ""
                    + " \n"
                    + "For example\n"
                    + "    : p.params[0]\n"
                    + "    : p.params[0]+p.params[1]\n"
                    + "    : p.returnObj\n"
                    + "    : p.throwExp\n"
                    + "    : p.target.targetThis.getClass()\n"
                    + " \n"
                    + "The structure of 'p'\n"
                    + "    p.\n"
                    + "    \\+- params[0..n] : the parameters of methods\n"
                    + "    \\+- returnObj    : the return object of methods\n"
                    + "    \\+- throwExp     : the throw exception of methods\n"
                    + "    \\+- target\n"
                    + "         \\+- targetThis  : the object entity\n"
                    + "         \\+- targetClass : the object's class"
    )

    private String expression;

    @Arg(name = "watch-point", isRequired = false)
    private WatchPointEnum watchPoint = WatchPointEnum.before;

    @RiscNamedArg(named = "b", description = "is watch on before")
    private boolean isBefore = true;

    @RiscNamedArg(named = "f", description = "is watch on finish")
    private boolean isFinish = false;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, Info info, final Sender sender) throws Throwable {
                ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");

                jsEngine.eval("function printWatch(p,o){try{o.send(false, " + expression + "+'\\n');}catch(e){o.send(false, e.message+'\\n');}}");
                final Invocable invoke = (Invocable) jsEngine;

                final Instrumentation inst = info.getInst();
                final TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(Advice p) {
                        if (watchPoint == WatchPointEnum.before
                                && isBefore) {
                            try {
                                invoke.invokeFunction("printWatch", p, sender);
                            } catch (Exception e) {
                            }
                        }
                    }

                    @Override
                    public void onFinish(Advice p) {
                        if (watchPoint == WatchPointEnum.finish
                                || isFinish) {
                            try {
                                invoke.invokeFunction("printWatch", p, sender);
                            } catch (Exception e) {
                            }
                        }
                    }

                }, info);

                // ע������
                registJob(info.getSessionId(), result.getId());

                // ��������
                activeJob(result.getId());

                final StringBuilder message = new StringBuilder();
                message.append(GaStringUtils.LINE);
                message.append(String.format("done. probe:c-Cnt=%s,m-Cnt=%s\n",
                        result.getModifiedClasses().size(),
                        result.getModifiedBehaviors().size()));
                sender.send(false, message.toString());
            }

        };
    }

}

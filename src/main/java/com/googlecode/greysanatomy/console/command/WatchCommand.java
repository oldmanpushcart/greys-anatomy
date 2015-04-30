package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.annotation.IndexArg;
import com.googlecode.greysanatomy.console.command.annotation.NamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaObjectUtils;
import com.googlecode.greysanatomy.util.GaOgnlUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.LogUtils;

import java.lang.instrument.Instrumentation;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.regJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

@Cmd(named = "watch", sort = 4, desc = "The call context information buried point observation methods.",
        eg = {
                "watch -Eb org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]",
                "watch -b org.apache.commons.lang.StringUtils isBlank params[0]",
                "watch -f org.apache.commons.lang.StringUtils isBlank returnObj",
                "watch -bf *StringUtils isBlank params[0]",
                "watch *StringUtils isBlank params[0]"
        })
public class WatchCommand extends Command {

    private final Logger logger = LogUtils.getLogger();

    @IndexArg(index = 0, name = "class-pattern", description = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", description = "pattern matching of method name")
    private String methodPattern;

    @IndexArg(index = 2, name = "express",
            description = "ognl expression, write by ognl.",
            description2 = ""
                    + " \n"
                    + "For example\n"
                    + "    : params[0]\n"
                    + "    : params[0]+params[1]\n"
                    + "    : returnObj\n"
                    + "    : throwExp\n"
                    + "    : target.targetThis.getClass()\n"
                    + " \n"
                    + "The structure of 'advice'\n"
                    + "    params[0..n] : the parameters of methods\n"
                    + "    returnObj    : the return object of methods\n"
                    + "    throwExp     : the throw exception of methods\n"
                    + "    target\n"
                    + "    \\+- targetThis  : the object entity\n"
                    + "    \\+- targetClassName : the object's class\n"
                    + "    \\+- targetBehaviorName : the constructor or method name\n"
    )
    private String expression;

    @NamedArg(named = "b", description = "is watch on before")
    private boolean isBefore = true;

    @NamedArg(named = "f", description = "is watch on finish")
    private boolean isFinish = false;

    @NamedArg(named = "e", description = "is watch on exception")
    private boolean isException = false;

    @NamedArg(named = "s", description = "is watch on success")
    private boolean isSuccess = false;

    @NamedArg(named = "x", hasValue = true, description = "expend level of object. Default level-0")
    private Integer expend;

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
            public void action(final ConsoleServer consoleServer, Info info, final Sender sender) throws Throwable {

                final Instrumentation inst = info.getInst();
                final TransformResult result = transform(inst, classPattern, methodPattern, isSuper, isRegEx(), new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(Advice p) {
                        if (isBefore) {
                            try {
                                final Object value = GaOgnlUtils.getValue(expression, p);
                                if (null != expend
                                        && expend >= 0) {
                                    sender.send(false, "" + GaObjectUtils.toString(value, 0, expend) + "\n");
                                } else {
                                    sender.send(false, "" + value + "\n");
                                }
//                                sender.send(false, "" + value + "\n");
                            } catch (Exception e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, "watch failed.", e);
                                }
                                sender.send(false, e.getMessage() + "\n");
                            }
                        }
                    }

                    @Override
                    public void onFinish(Advice p) {
                        if (isFinish) {
                            try {
                                final Object value = GaOgnlUtils.getValue(expression, p);
                                if (null != expend
                                        && expend >= 0) {
                                    sender.send(false, "" + GaObjectUtils.toString(value, 0, expend) + "\n");
                                } else {
                                    sender.send(false, "" + value + "\n");
                                }
//                                sender.send(false, "" + value + "\n");
                            } catch (Exception e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, "watch failed.", e);
                                }
                                sender.send(false, e.getMessage() + "\n");
                            }
                        }
                    }

                    @Override
                    public void onException(Advice p) {
                        if (isException) {
                            try {
                                final Object value = GaOgnlUtils.getValue(expression, p);
                                if (null != expend
                                        && expend >= 0) {
                                    sender.send(false, "" + GaObjectUtils.toString(value, 0, expend) + "\n");
                                } else {
                                    sender.send(false, "" + value + "\n");
                                }
//                                sender.send(false, "" + value + "\n");
                            } catch (Exception e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, "watch failed.", e);
                                }
                                sender.send(false, e.getMessage() + "\n");
                            }
                        }
                    }

                    @Override
                    public void onSuccess(Advice p) {
                        if (isSuccess) {
                            try {
                                final Object value = GaOgnlUtils.getValue(expression, p);
                                if (null != expend
                                        && expend >= 0) {
                                    sender.send(false, "" + GaObjectUtils.toString(value, 0, expend) + "\n");
                                } else {
                                    sender.send(false, "" + value + "\n");
                                }
//                                sender.send(false, "" + value + "\n");
                            } catch (Exception e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, "watch failed.", e);
                                }
                                sender.send(false, e.getMessage() + "\n");
                            }
                        }
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

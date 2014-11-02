package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer;
import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.clocker.Clocker;
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.ProfilerUtils;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

@RiscCmd(named = "profiler", sort = 6, desc = "The call stack output buried point method for rendering path of.",
        eg = {
                "profiler -c 50 org\\.apache\\.commons\\..* .* org\\.apache\\.commons\\.lang\\.StringUtils isEmpty",
                "profiler -c 50 org\\.apache\\.commons\\..* .* .*StringUtils isEmpty",
        })
public class ProfilerCommand extends Command {

    @RiscIndexArg(index = 0, name = "rendering-class-regex", description = "regex match of rendering classpath.classname")
    private String classRegex;

    @RiscIndexArg(index = 1, name = "rendering-method-regex", description = "regex match of rendering methodname")
    private String methodRegex;

    @RiscIndexArg(index = 2, name = "class-regex", description = "regex match of classpath.classname")
    private String probeClassRegex;

    @RiscIndexArg(index = 3, name = "method-regex", description = "regex match of methodname")
    private String probeMethodRegex;

    @RiscNamedArg(named = "c", hasValue = true, description = "the cost limit for output")
    private long cost;

    @Override
    public Action getAction() {
        return new Action() {

            private final ThreadLocal<Boolean> isEntered = new ThreadLocal<Boolean>() {
                @Override
                protected Boolean initialValue() {
                    return false;
                }
            };

            private final ThreadLocal<Integer> deep = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };

            private final ThreadLocal<Long> beginTimestamp = new ThreadLocal<Long>() {
                @Override
                protected Long initialValue() {
                    return System.currentTimeMillis();
                }
            };

            private final Map<String, Boolean> cmCache = new ConcurrentHashMap<String, Boolean>();

            @Override
            public void action(final ConsoleServer consoleServer, Info info, final Sender sender) throws Throwable {

                final Instrumentation inst = info.getInst();
                final AdviceListenerAdapter advice = new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(Advice p) {
                        init();
                        if (!isEntered(p)) {
                            return;
                        }
                        if (0 == deep.get()) {
                            beginTimestamp.set(Clocker.current().getCurrentTimeMillis());
                            isEntered.set(true);
                            ProfilerUtils.start("");
                        }
                        ProfilerUtils.enter();
                        deep.set(deep.get() + 1);
                    }

                    @Override
                    public void onFinish(Advice p) {
                        if (!isEntered.get()) {
                            return;
                        }
                        deep.set(deep.get() - 1);
                        ProfilerUtils.release();
                        if (0 == deep.get()) {
                            final long cost = Clocker.current().getCurrentTimeMillis() - beginTimestamp.get();
                            final String dump = ProfilerUtils.dump();
                            if (cost >= ProfilerCommand.this.cost) {
                                final StringBuilder dumpSB = new StringBuilder()
                                        .append("Thread Info:").append(Thread.currentThread().getName()).append("\n")
                                        .append(dump).append("\n\n");
                                sender.send(false, dumpSB.toString());
                            }
                            isEntered.set(false);
                        }
                    }

                    private void init() {
                        if (null == deep.get()) {
                            deep.set(0);
                        }
                        if (null == isEntered.get()) {
                            isEntered.set(false);
                        }
                    }

                    private boolean isEntered(Advice p) {
                        if (isEntered.get()) {
                            return true;
                        }
                        final String cmKey = new StringBuilder()
                                .append(p.getTarget().getTargetClassName())
                                .append("#")
                                .append(p.getTarget().getTargetBehaviorName())
                                .toString();

                        if (cmCache.containsKey(cmKey)) {
                            return cmCache.get(cmKey);
                        } else {
                            final boolean isProbe = p.getTarget().getTargetClassName().matches(probeClassRegex)
                                    && p.getTarget().getTargetBehaviorName().matches(probeMethodRegex);
                            cmCache.put(cmKey, isProbe);
                            return isProbe;
                        }
                    }

                };
                final TransformResult result = transform(inst, classRegex, methodRegex, advice, info, new GreysAnatomyClassFileTransformer.Progress() {

                    int nextRate = 0;

                    @Override
                    public void progress(int index, int total) {

                        if (total > 0) {
                            // 进度
                            final int step;

                            if (total < 500) {
                                step = 10;
                            } else if (total < 1000) {
                                step = 5;
                            } else if (total < 2000) {
                                step = 2;
                            } else {
                                step = 1;
                            }

                            final int rate = index * 100 / total;
                            if (rate >= nextRate) {
                                nextRate += step;
                                sender.send(false, GaStringUtils.progress("rendering", index, total) + "\n");
                            }
                        }

                    }

                });
                final TransformResult resultForProbe = transform(inst, probeClassRegex, probeMethodRegex, advice, info);

                // 注册任务
                registJob(info.getSessionId(), result.getId());
                registJob(info.getSessionId(), resultForProbe.getId());

                // 激活任务
                activeJob(result.getId());
                activeJob(resultForProbe.getId());

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

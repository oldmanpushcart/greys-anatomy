package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.InnerContext;
import com.github.ompc.greys.core.advisor.ProcessContext;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.GaStringUtils;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.PointCut;
import com.github.ompc.greys.core.util.matcher.ClassMatcher;
import com.github.ompc.greys.core.util.matcher.GaMethodMatcher;
import com.github.ompc.greys.core.util.matcher.PatternMatcher;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * js脚本增强命令
 * Created by vlinux on 16/2/9.
 */
@Cmd(name = "js", sort = 6, summary = "Enhanced JavaScript",
        eg = {
                "js *StringUtils isBlank /tmp/watch.js"
        })
public class JavaScriptCommand implements ScriptSupportCommand, Command {

    private final Logger logger = LogUtil.getLogger();

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "script-filepath", summary = "Filepath of Groovy script")
    private String scriptFilepath;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final String scriptContent;
        try {
            scriptContent = FileUtils.readFileToString(new File(scriptFilepath));
        } catch (IOException e) {
            logger.warn("javascript file not found. script={};", scriptFilepath, e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("js script file not found").finish();
                }
            };
        }

        final ScriptEngineManager mgr = new ScriptEngineManager(getClass().getClassLoader());
        final ScriptEngine jsEngine = mgr.getEngineByMimeType("application/javascript");
        final Compilable compilable = (Compilable) jsEngine;

        final boolean isDefineBefore;
        final boolean isDefineReturning;
        final boolean isDefineThrowing;
        final boolean isDefineDestroy;
        final boolean isDefineCreate;
        try {
            compilable.compile(scriptContent).eval();
            isDefineBefore = (Boolean) compilable.compile("this.hasOwnProperty('before')").eval();
            isDefineReturning = (Boolean) compilable.compile("this.hasOwnProperty('returning')").eval();
            isDefineThrowing = (Boolean) compilable.compile("this.hasOwnProperty('throwing')").eval();
            isDefineDestroy = (Boolean) compilable.compile("this.hasOwnProperty('destroy')").eval();
            isDefineCreate = (Boolean) compilable.compile("this.hasOwnProperty('create')").eval();
        } catch (ScriptException e) {
            logger.warn("javascript compile failed. script={};", scriptFilepath, e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("javascript compile failed.").finish();
                }
            };
        }

        final Invocable invocable = (Invocable) jsEngine;

        return new GetEnhancerAction() {
            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {

                final Output output = new Output() {

                    @Override
                    public Output print(String string) {
                        printer.print(string);
                        return this;
                    }

                    @Override
                    public Output println(String string) {
                        printer.println(string);
                        return this;
                    }

                    @Override
                    public Output finish() {
                        printer.print(EMPTY).finish();
                        return this;
                    }
                };

                return new GetEnhancer() {

                    @Override
                    public PointCut getPointCut() {
                        return new PointCut(
                                new ClassMatcher(new PatternMatcher(isRegEx, classPattern)),
                                new GaMethodMatcher(new PatternMatcher(isRegEx, methodPattern))
                        );
                    }

                    @Override
                    public AdviceListener getAdviceListener() {

                        return new ReflectAdviceListenerAdapter<ProcessContext, MapInnerContext>() {

                            @Override
                            protected ProcessContext newProcessContext() {
                                return new ProcessContext();
                            }

                            @Override
                            protected MapInnerContext newInnerContext() {
                                return new MapInnerContext();
                            }

                            @Override
                            public void create() {
                                if (isDefineCreate) {
                                    try {
                                        invocable.invokeFunction("create", output);
                                    } catch (ScriptException e) {
                                        output.println("invoke function 'create' failed. because : " + GaStringUtils.getCauseMessage(e));
                                        logger.warn("invoke function 'create' failed.", e);
                                    } catch (NoSuchMethodException e) {
                                        //
                                    }
                                }
                            }

                            @Override
                            public void destroy() {
                                if (isDefineDestroy) {
                                    try {
                                        invocable.invokeFunction("destroy", output);
                                    } catch (ScriptException e) {
                                        output.println("invoke function 'destroy' failed. because : " + GaStringUtils.getCauseMessage(e));
                                        logger.warn("invoke function 'destroy' failed.", e);
                                    } catch (NoSuchMethodException e) {
                                        //
                                    }
                                }
                            }

                            @Override
                            public void before(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                if (isDefineBefore) {
                                    try {
                                        invocable.invokeFunction("before", output, advice, innerContext);
                                    } catch (ScriptException e) {
                                        output.println("invoke function 'before' failed. because : " + GaStringUtils.getCauseMessage(e));
                                        logger.warn("invoke function 'before' failed.", e);
                                    } catch (NoSuchMethodException e) {
                                        //
                                    }
                                }
                            }

                            @Override
                            public void afterReturning(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                if (isDefineReturning) {
                                    try {
                                        invocable.invokeFunction("returning", output, advice, innerContext);
                                    } catch (ScriptException e) {
                                        output.println("invoke function 'returning' failed. because : " + GaStringUtils.getCauseMessage(e));
                                        logger.warn("invoke function 'returning' failed.", e);
                                    } catch (NoSuchMethodException e) {
                                        //
                                    }
                                }
                            }

                            @Override
                            public void afterThrowing(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                if (isDefineThrowing) {
                                    try {
                                        invocable.invokeFunction("throwing", output, advice, innerContext);
                                    } catch (ScriptException e) {
                                        output.println("invoke function 'throwing' failed. because : " + GaStringUtils.getCauseMessage(e));
                                        logger.warn("invoke function 'throwing' failed.", e);
                                    } catch (NoSuchMethodException e) {
                                        //
                                    }
                                }
                            }

                        };
                    }
                };
            }
        };
    }

    /**
     * 用于协同JavaScript作业的Context参数
     */
    public static class MapInnerContext extends InnerContext {

        private ThreadLocal<Map<String, Object>> mapRef = new ThreadLocal<Map<String, Object>>() {
            @Override
            protected Map<String, Object> initialValue() {
                return new HashMap<String, Object>();
            }
        };

        public void put(String key, Object val) {
            mapRef.get().put(key, val);
        }

        public Object get(String key) {
            return mapRef.get().get(key);
        }

        public boolean has(String key) {
            return mapRef.get().containsKey(key);
        }

    }

}

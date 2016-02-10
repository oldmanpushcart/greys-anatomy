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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.script.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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

    @IndexArg(index = 2, name = "script-path", summary = "Path of javascript, support file:// or http://")
    private String scriptPath;

    @NamedArg(name = "c", hasValue = true, summary = "The character of script-path")
    private String charsetString;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    /*
     * 获取指定字符集
     */
    private Charset fetchCharset() throws UnsupportedCharsetException {
        if (StringUtils.isBlank(charsetString)) {
            return Charset.defaultCharset();
        } else {
            return Charset.forName(charsetString);
        }
    }

    /*
     * 构造script-path所对应的URI
     */
    private URI createScriptPathURI() {
        if (StringUtils.isBlank(scriptPath)) {
            throw new IllegalArgumentException("script-path is required.");
        }

        if (StringUtils.startsWithIgnoreCase(scriptPath, "http://")
                || StringUtils.startsWithIgnoreCase(scriptPath, "https://")
                || StringUtils.startsWithIgnoreCase(scriptPath, "file://")) {
            return URI.create(scriptPath);
        } else {
            return URI.create("file://" + scriptPath);
        }
    }

    /*
     * 加载JavaScript脚本支撑
     */
    private void loadJavaScriptSupport(Compilable compilable, Invocable invocable, String scriptContent) throws IOException, ScriptException, NoSuchMethodException {
        // 加载support
        compilable.compile(
                IOUtils.toString(
                        GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/javascript-support.js"),
                        Charset.forName("UTF-8")
                )
        ).eval();
        // 初始化greys
        invocable.invokeFunction("__global_greys_init", scriptContent);
    }

    @Override
    public Action getAction() {


        final String scriptContent;
        try {
            scriptContent = IOUtils.toString(createScriptPathURI(), fetchCharset());
        } catch (UnsupportedCharsetException e) {
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println(String.format("Desupported character[%s].", charsetString)).finish();
                }
            };
        } catch (IOException e) {
            logger.warn("script-path not found. path={};", scriptPath, e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println(String.format("script-path[%s] not found.", scriptPath)).finish();
                }
            };
        }

        final ScriptEngineManager mgr = new ScriptEngineManager(getClass().getClassLoader());
        final ScriptEngine jsEngine = mgr.getEngineByMimeType("application/javascript");
        final Compilable compilable = (Compilable) jsEngine;
        final Invocable invocable = (Invocable) jsEngine;

        final boolean isDefineCreate;
        final boolean isDefineDestroy;
        final boolean isDefineBefore;
        final boolean isDefineReturning;
        final boolean isDefineThrowing;
        try {
            loadJavaScriptSupport(compilable, invocable, scriptContent);
            isDefineCreate = (Boolean) invocable.invokeFunction("__global_greys_is_define_create");
            isDefineDestroy = (Boolean) invocable.invokeFunction("__global_greys_is_define_destroy");
            isDefineBefore = (Boolean) invocable.invokeFunction("__global_greys_is_define_before");
            isDefineReturning = (Boolean) invocable.invokeFunction("__global_greys_is_define_returning");
            isDefineThrowing = (Boolean) invocable.invokeFunction("__global_greys_is_define_throwing");
        } catch (ScriptException e) {
            logger.warn("javascript compile failed. script={};", scriptPath, e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("javascript compile failed.").finish();
                }
            };
        } catch (NoSuchMethodException e) {
            logger.warn("invoke function __global_greys_init failed.", e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("invoke function __global_greys_init failed.").finish();
                }
            };
        } catch (IOException e) {
            logger.warn("load javascript-support.js failed.", e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("load javascript-support.js failed.").finish();
                }
            };
        }

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
                                        invocable.invokeFunction("__global_greys_create", output);
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
                                        invocable.invokeFunction("__global_greys_destroy", output);
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
                                        invocable.invokeFunction("__global_greys_before", output, advice, innerContext);
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
                                        invocable.invokeFunction("__global_greys_returning", output, advice, innerContext);
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
                                        invocable.invokeFunction("__global_greys_throwing", output, advice, innerContext);
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

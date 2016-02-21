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
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

import static com.github.ompc.greys.core.util.GaStringUtils.getCauseMessage;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * js脚本增强命令
 * Created by vlinux on 16/2/9.
 */
@Cmd(name = "js", sort = 6, summary = "Enhanced JavaScript",
        eg = {
                "js *StringUtils isBlank /tmp/watch.js",
                "js -c UTF-8 *StringUtils isBlank /tmp/watch.js"
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
     * 加载支撑脚本(gblocking.js)
     */
    private void loadJavaScriptSupport(Compilable compilable) throws IOException, ScriptException {

        // 加载
        compilable.compile(
                IOUtils.toString(
                        GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/gblocking.js"),
                        Charset.forName("UTF-8")
                )
        ).eval();

    }

    /*
     * 加载greys模块脚本(greys-module.js)
     */
    private void loadGreysModule(Compilable compilable) throws IOException, ScriptException {
        // 加载greys-module.js
        compilable.compile(
                IOUtils.toString(
                        GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/javascript/greys-module.js"),
                        Charset.forName("UTF-8")
                )
        ).eval();
    }

    /*
     * 加载自定义模块地址
     */
    private void loadCustomModule(Invocable invocable, String path, Charset charset) throws ScriptException, NoSuchMethodException {
        invocable.invokeFunction("__greys_load", path, charset.name());
    }

    @Override
    public Action getAction() {


        /**
         * ScriptEngine放在这里是有讲究的,毕竟ScriptEngine将会被多线程并发执行,JavaScript却是单线程的实现
         * 所以一个ScriptEngine的厂商实现是否支持并发,非常关键,还好默认的Rhino和Nashorn都是"MULTITHREADED"级别的实现
         * 在ScriptEngineFactory中有一个getParameter方法，通过传入”THREADING”字符串作为参数，可以获知引擎是否是线程安全的
         *
         * 线程安全级别说明
         * 1. "null"
         *    引擎实现不是线程安全的，并且无法用来在多个线程上并发执行脚本。
         *
         * 2. "MULTITHREADED"
         *    引擎实现是内部线程安全的，并且脚本可以并发执行，尽管在某个线程上执行脚本的效果对于另一个线程上的脚本是可见的。
         *
         * 3. "THREAD-ISOLATED"
         *    该实现满足 "MULTITHREADED" 的要求，并且引擎为不同线程上执行的脚本中的符号维护独立的值。
         *
         * 4. "STATELESS"
         *    该实现满足 "THREAD-ISOLATED" 的要求。此外，脚本执行不改变 Bindings 中的映射关系，该 Bindings 是 ScriptEngine 的引擎范围。
         *    具体来说，Bindings 及其关联值中的键在执行脚本之前和之后是相同的。
         *
         */
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine jsEngine = mgr.getEngineByMimeType("application/javascript");

        final Compilable compilable = (Compilable) jsEngine;
        final Invocable invocable = (Invocable) jsEngine;

        try {
            loadJavaScriptSupport(compilable);
            loadGreysModule(compilable);
            loadCustomModule(invocable, scriptPath, fetchCharset());
        } catch (final ScriptException e) {
            logger.warn("javascript compile failed. script={};", scriptPath, e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("javascript compile failed. because " + getCauseMessage(e)).finish();
                }
            };
        } catch (final NoSuchMethodException e) {
            logger.warn("javascript function not defined.", e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("javascript function not defined. because " + getCauseMessage(e)).finish();
                }
            };
        } catch (final IOException e) {
            logger.warn("load javascript failed.", e);
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("load javascript failed. because " + getCauseMessage(e)).finish();
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
                                try {
                                    invocable.invokeFunction("__greys_module_create", output);
                                } catch (ScriptException e) {
                                    output.println("invoke function 'create' failed. because : " + getCauseMessage(e));
                                    logger.warn("invoke function 'create' failed.", e);
                                } catch (NoSuchMethodException e) {
                                    //
                                }
                            }

                            @Override
                            public void destroy() {
                                try {
                                    invocable.invokeFunction("__greys_module_destroy", output);
                                } catch (ScriptException e) {
                                    output.println("invoke function 'destroy' failed. because : " + getCauseMessage(e));
                                    logger.warn("invoke function 'destroy' failed.", e);
                                } catch (NoSuchMethodException e) {
                                    //
                                }
                            }

                            @Override
                            public void before(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                try {
                                    invocable.invokeFunction("__greys_module_before", output, advice, innerContext);
                                } catch (ScriptException e) {
                                    output.println("invoke function 'before' failed. because : " + getCauseMessage(e));
                                    logger.warn("invoke function 'before' failed.", e);
                                } catch (NoSuchMethodException e) {
                                    //
                                }
                            }

                            @Override
                            public void afterReturning(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                try {
                                    invocable.invokeFunction("__greys_module_returning", output, advice, innerContext);
                                } catch (ScriptException e) {
                                    output.println("invoke function 'returning' failed. because : " + getCauseMessage(e));
                                    logger.warn("invoke function 'returning' failed.", e);
                                } catch (NoSuchMethodException e) {
                                    //
                                }
                            }

                            @Override
                            public void afterThrowing(Advice advice, ProcessContext processContext, MapInnerContext innerContext) throws Throwable {
                                try {
                                    invocable.invokeFunction("__greys_module_throwing", output, advice, innerContext);
                                } catch (ScriptException e) {
                                    output.println("invoke function 'throwing' failed. because : " + getCauseMessage(e));
                                    logger.warn("invoke function 'throwing' failed.", e);
                                } catch (NoSuchMethodException e) {
                                    //
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

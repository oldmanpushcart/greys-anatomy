package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 脚本增强命令
 * Created by oldmanpushcart@gmail.com on 15/5/31.
 */
//@Cmd(name = "groovy", sort = 6, summary = "Enhanced Groovy",
//        eg = {
//                "groovy -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank /tmp/watch.groovy",
//                "groovy org.apache.commons.lang.StringUtils isBlank /tmp/watch.groovy",
//                "groovy *StringUtils isBlank /tmp/watch.groovy"
//        })
@Deprecated
public class GroovyScriptCommand implements ScriptSupportCommand, Command {

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

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

        final File scriptFile = new File(scriptFilepath);
        if (!scriptFile.exists()
                || !scriptFile.canRead()
                || !scriptFile.isFile()) {
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    printer.println("Groovy script not found").finish();
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

                return null;

//                final Class<?> scriptClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(scriptFile);
//                final ScriptListener scriptListener = (ScriptListener) scriptClass.newInstance();
//
//
//                return new GetEnhancer() {
//                    @Override
//                    public Matcher getClassNameMatcher() {
//                        return classNameMatcher;
//                    }
//
//                    @Override
//                    public Matcher getMethodNameMatcher() {
//                        return methodNameMatcher;
//                    }
//
//                    @Override
//                    public AdviceListener getAdviceListener() {
//
//                        return new DefaultReflectAdviceListenerAdapter() {
//
//                            @Override
//                            public void create() {
//                                scriptListener.create(output);
//                            }
//
//                            @Override
//                            public void destroy() {
//                                scriptListener.destroy(output);
//                            }
//
//                            @Override
//                            public void before(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
//                                scriptListener.before(output, advice);
//                            }
//
//                            @Override
//                            public void afterReturning(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
//                                scriptListener.afterReturning(output, advice);
//                            }
//
//                            @Override
//                            public void afterThrowing(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
//                                scriptListener.afterThrowing(output, advice);
//                            }
//                        };
//                    }
//                };
            }
        };
    }

}

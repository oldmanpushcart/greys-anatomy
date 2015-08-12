package com.github.ompc.greys.command;

import com.github.ompc.greys.GlobalOptions;
import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.GaMethod;
import com.github.ompc.greys.util.Matcher;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.util.Advice.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 脚本增强命令
 * Created by vlinux on 15/5/31.
 */
@Cmd(name = "groovy", sort = 6, summary = "The groovy script enhance.",
        eg = {
                "groovy -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank /tmp/watch.groovy",
                "groovy org.apache.commons.lang.StringUtils isBlank /tmp/watch.groovy",
                "groovy *StringUtils isBlank /tmp/watch.groovy"
        })
public class GroovyScriptCommand implements ScriptSupportCommand, Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    @IndexArg(index = 2, name = "script-filepath", summary = "script file path")
    private String scriptFilepath;

    @NamedArg(name = "S", summary = "including sub class")
    private boolean isIncludeSub = GlobalOptions.isIncludeSubClass;

    @NamedArg(name = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final File scriptFile = new File(scriptFilepath);
        if (!scriptFile.exists()
                || !scriptFile.canRead()
                || !scriptFile.isFile()) {
            return new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                    sender.send(true, "script file not found.\n");
                }
            };
        }

        return new GetEnhancerAction() {
            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Sender sender) throws Throwable {

                final Output output = new Output() {

                    @Override
                    public Output print(String string) {
                        sender.send(false, string);
                        return this;
                    }

                    @Override
                    public Output println(String string) {
                        sender.send(false, string + "\n");
                        return this;
                    }

                    @Override
                    public Output finish() {
                        sender.send(true, EMPTY);
                        return this;
                    }
                };

                final Class<?> scriptClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(scriptFile);
                final ScriptListener scriptListener = (ScriptListener) scriptClass.newInstance();


                return new GetEnhancer() {
                    @Override
                    public Matcher getClassNameMatcher() {
                        return isRegEx
                                ? new Matcher.RegexMatcher(classPattern)
                                : new Matcher.WildcardMatcher(classPattern);
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return isRegEx
                                ? new Matcher.RegexMatcher(methodPattern)
                                : new Matcher.WildcardMatcher(methodPattern);
                    }

                    @Override
                    public boolean isIncludeSub() {
                        return isIncludeSub;
                    }

                    @Override
                    public AdviceListener getAdviceListener() {

                        return new ReflectAdviceListenerAdapter() {

                            @Override
                            public void create() {
                                scriptListener.create(output);
                            }

                            @Override
                            public void destroy() {
                                scriptListener.destroy(output);
                            }

                            @Override
                            public void before(
                                    ClassLoader loader, Class<?> clazz, GaMethod method,
                                    Object target, Object[] args) throws Throwable {
                                scriptListener.before(output,
                                        newForBefore(loader, clazz, method, target, args));
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader, Class<?> clazz, GaMethod method,
                                    Object target, Object[] args, Object returnObject) throws Throwable {
                                scriptListener.afterReturning(output,
                                        newForAfterRetuning(loader, clazz, method, target, args, returnObject));
                            }

                            @Override
                            public void afterThrowing(
                                    ClassLoader loader, Class<?> clazz, GaMethod method,
                                    Object target, Object[] args, Throwable throwable) throws Throwable {
                                scriptListener.afterThrowing(output,
                                        newForAfterThrowing(loader, clazz, method, target, args, throwable));
                            }
                        };
                    }
                };
            }
        };
    }

}

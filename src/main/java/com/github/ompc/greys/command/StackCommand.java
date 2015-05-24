package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.AdviceListenerAdapter;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.util.StringUtil.getStack;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author vlinux
 */
@Cmd(named = "stack", sort = 6, desc = "The call stack output buried point method in each thread.",
        eg = {
                "stack -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack *StringUtils isBlank"
        })
public class StackCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    @NamedArg(named = "S", summary = "including sub class")
    private boolean isIncludeSub = false;

    @NamedArg(named = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = isRegEx
                ? new RegexMatcher(classPattern)
                : new WildcardMatcher(classPattern);

        final Matcher methodNameMatcher = isRegEx
                ? new RegexMatcher(methodPattern)
                : new WildcardMatcher(methodPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Sender sender) throws Throwable {
                return new GetEnhancer() {

                    @Override
                    public Matcher getClassNameMatcher() {
                        return classNameMatcher;
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return methodNameMatcher;
                    }

                    @Override
                    public boolean isIncludeSub() {
                        return isIncludeSub;
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new AdviceListenerAdapter() {

                            @Override
                            public void before(
                                    ClassLoader loader,
                                    String className,
                                    String methodName,
                                    String methodDesc,
                                    Object target,
                                    Object[] args) throws Throwable {

                                sender.send(false, getStack() + "\n");

                            }
                        };
                    }
                };
            }

        };
    }


}

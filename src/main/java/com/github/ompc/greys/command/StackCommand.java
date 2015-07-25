package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.AdviceListenerAdapter;
import com.github.ompc.greys.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.exception.ExpressException;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Advice;
import static com.github.ompc.greys.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.util.Advice.newForAfterThrowing;
import static com.github.ompc.greys.util.Express.ExpressFactory.newExpress;
import com.github.ompc.greys.util.GaMethod;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.util.GaStringUtils.getStack;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author vlinux
 */
@Cmd(name = "stack", sort = 6, summary = "The call stack output buried point method callback each thread.",
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

    @IndexArg(index = 2, name = "condition-express", isRequired = false,
            summary = "condition express, write by groovy",
            description = ""
            + "For example\n"
            + "TRUE  : true\n"
            + "FALSE : false\n"
            + "TRUE  : params.length>=0"
            + "\n"
            + "The structure of 'advice' was just like express.\n"
    )
    private String conditionExpress;

    @NamedArg(name = "S", summary = "including sub class")
    private boolean isIncludeSub = false;

    @NamedArg(name = "E", summary = "enable the regex pattern matching")
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
                        return new ReflectAdviceListenerAdapter() {

                            private final ThreadLocal<String> stackThreadLocal = new ThreadLocal<String>();

                            @Override
                            public void before(ClassLoader loader, Class<?> clazz, GaMethod method, Object target, Object[] args) throws Throwable {
                                stackThreadLocal.set(getStack());
                            }

                            @Override
                            public void afterThrowing(ClassLoader loader, Class<?> clazz, GaMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
                                final Advice advice = newForAfterThrowing(loader, clazz, method, target, args, throwable);
                                finishing(advice);
                            }

                            @Override
                            public void afterReturning(ClassLoader loader, Class<?> clazz, GaMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
                                final Advice advice = newForAfterRetuning(loader, clazz, method, target, args, returnObject);
                                finishing(advice);
                            }

                            private boolean isPrintIfNecessary(Advice advice) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
                                }
                            }

                            private void finishing(final Advice advice) {
                                if (isPrintIfNecessary(advice)) {
                                    sender.send(false, stackThreadLocal.get() + "\n");
                                }
                            }

                        };
                    }
                };
            }

        };
    }

}

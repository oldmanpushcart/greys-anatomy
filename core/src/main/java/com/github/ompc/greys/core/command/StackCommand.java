package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.InnerContext;
import com.github.ompc.greys.core.advisor.ProcessContext;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getStack;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author vlinux
 */
@Cmd(name = "stack", sort = 6, summary = "Display the stack trace of specified class and method",
        eg = {
                "stack -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack *StringUtils isBlank",
                "stack *StringUtils isBlank params[0].length==1"
        })
public class StackCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", isRequired = false, summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "condition-express", isRequired = false,
            summary = "Conditional expression by groovy",
            description = "" +
                    "For example\n" +
                    "\n" +
                    "    TRUE  : 1==1\n" +
                    "    TRUE  : true\n" +
                    "    FALSE : false\n" +
                    "    TRUE  : params.length>=0\n" +
                    "    FALSE : 1==2\n" +
                    "\n" +
                    "The structure\n" +
                    "\n" +
                    "          target : the object \n" +
                    "           clazz : the object's class\n" +
                    "          method : the constructor or method\n" +
                    "    params[0..n] : the parameters of method\n" +
                    "       returnObj : the returned object of method\n" +
                    "        throwExp : the throw exception of method\n" +
                    "        isReturn : the method ended by return\n" +
                    "         isThrow : the method ended by throwing exception"
    )
    private String conditionExpress;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer numberOfLimit;

    // 针对stack命令调整
    private static final int STACK_DEEP = 9;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    private final AtomicInteger times = new AtomicInteger();

                    @Override
                    public Matcher getClassNameMatcher() {
                        return classNameMatcher;
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return methodNameMatcher;
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceListenerAdapter() {

                            private final ThreadLocal<String> stackThreadLocal = new ThreadLocal<String>();

                            @Override
                            public void before(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
                                stackThreadLocal.set(getStack(STACK_DEEP));
                            }

                            @Override
                            public void afterThrowing(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
                                finishing(advice);
                            }

                            @Override
                            public void afterReturning(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
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

                            private boolean isLimited(int currentTimes) {
                                return null != numberOfLimit
                                        && currentTimes >= numberOfLimit;
                            }

                            private void finishing(final Advice advice) {
                                if (isPrintIfNecessary(advice)) {
                                    final boolean isF = isLimited(times.incrementAndGet());
                                    printer.println(isF, stackThreadLocal.get());
                                }
                            }

                        };
                    }
                };
            }

        };
    }




}

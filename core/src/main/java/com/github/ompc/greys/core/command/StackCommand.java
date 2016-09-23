package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.InvokeCost;
import com.github.ompc.greys.core.util.PointCut;
import com.github.ompc.greys.core.util.matcher.ClassMatcher;
import com.github.ompc.greys.core.util.matcher.GaMethodMatcher;
import com.github.ompc.greys.core.util.matcher.PatternMatcher;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getStack;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 *
 * @author oldmanpushcart@gmail.com
 */
@Cmd(name = "stack", sort = 6, summary = "Display the stack trace of specified class and method",
        eg = {
                "stack -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
                "stack org.apache.commons.lang.StringUtils isBlank",
                "stack *StringUtils isBlank",
                "stack *StringUtils isBlank 'params[0].length==1'",
                "stack *StringUtils isBlank '#cost>100'"
        })
public class StackCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", isRequired = false, summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "condition-express", isRequired = false,
            summary = "Conditional expression by OGNL",
            description = "" +
                    "FOR EXAMPLE" +
                    "\n" +
                    "     TRUE : 1==1\n" +
                    "     TRUE : true\n" +
                    "    FALSE : false\n" +
                    "     TRUE : params.length>=0\n" +
                    "    FALSE : 1==2\n" +
                    "\n" +
                    "THE STRUCTURE" +
                    "\n" +
                    "          target : the object \n" +
                    "           clazz : the object's class\n" +
                    "          method : the constructor or method\n" +
                    "    params[0..n] : the parameters of method\n" +
                    "       returnObj : the returned object of method\n" +
                    "        throwExp : the throw exception of method\n" +
                    "        isReturn : the method ended by return\n" +
                    "         isThrow : the method ended by throwing exception\n" +
                    "           #cost : the cost of method"
    )
    private String conditionExpress;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;

    @Override
    public Action getAction() {

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    private final AtomicInteger times = new AtomicInteger();

                    @Override
                    public PointCut getPointCut() {
                        return new PointCut(
                                new ClassMatcher(new PatternMatcher(isRegEx, classPattern)),
                                new GaMethodMatcher(new PatternMatcher(isRegEx, methodPattern))
                        );
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceListenerAdapter() {

                            private final ThreadLocal<String> stackInfoRef = new ThreadLocal<String>();
                            private final InvokeCost invokeCost = new InvokeCost();

                            @Override
                            public void before(Advice advice) throws Throwable {
                                stackInfoRef.set(getStack());
                                invokeCost.begin();
                            }

                            private boolean isInCondition(Advice advice, long cost) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).bind("cost", cost).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
                                }
                            }

                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            @Override
                            public void afterFinishing(Advice advice) throws Throwable {
                                if (isInCondition(advice, invokeCost.cost())) {
                                    printer.println(stackInfoRef.get());
                                    if (isOverThreshold(times.incrementAndGet())) {
                                        printer.finish();
                                    }
                                }
                            }

                        };
                    }
                };
            }

        };
    }

}

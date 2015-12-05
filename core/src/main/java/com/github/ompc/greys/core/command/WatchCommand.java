package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.InnerContext;
import com.github.ompc.greys.core.advisor.ProcessContext;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter.DefaultReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.textui.ext.TObject;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getCauseMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Cmd(name = "watch", sort = 4, summary = "Display the details of specified class and method",
        eg = {
                "watch -Eb org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]",
                "watch -b org.apache.commons.lang.StringUtils isBlank params[0]",
                "watch -f org.apache.commons.lang.StringUtils isBlank returnObj",
                "watch -bf *StringUtils isBlank params[0]",
                "watch *StringUtils isBlank params[0]",
                "watch *StringUtils isBlank params[0] params[0].length==1"
        })
public class WatchCommand implements Command {

    private final Logger logger = LogUtil.getLogger();

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "express",
            summary = "express, write by OGNL.",
            description = ""
                    + "FOR EXAMPLE" +
                    "    params[0]\n" +
                    "    params[0]+params[1]\n" +
                    "    returnObj\n" +
                    "    throwExp\n" +
                    "    target\n" +
                    "    clazz\n" +
                    "    method\n" +
                    "\n" +
                    "THE STRUCTURE" +
                    "\n" +
                    "          target : the object\n" +
                    "           clazz : the object's class\n" +
                    "          method : the constructor or method\n" +
                    "    params[0..n] : the parameters of method\n" +
                    "       returnObj : the returned object of method\n" +
                    "        throwExp : the throw exception of method\n" +
                    "        isReturn : the method ended by return\n" +
                    "         isThrow : the method ended by throwing exception"
    )
    private String express;

    @IndexArg(index = 3, name = "condition-express", isRequired = false,
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
                    "         isThrow : the method ended by throwing exception"
    )
    private String conditionExpress;

    @NamedArg(name = "b", summary = "Watch before invocation")
    private boolean isBefore = false;

    @NamedArg(name = "f", summary = "Watch after invocation")
    private boolean isFinish = false;

    @NamedArg(name = "e", summary = "Watch after throw exception")
    private boolean isException = false;

    @NamedArg(name = "s", summary = "Watch after successful invocation")
    private boolean isSuccess = false;

    @NamedArg(name = "x", hasValue = true, summary = "Expand level of object (0 by default)")
    private Integer expend;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

        // set default
        // 如果没有强行指定b/f/e/s中任何一个，则默认为b
        if(!isBefore
                && !isFinish
                && !isException
                && !isSuccess) {
            isBefore = true;
        }

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    private final AtomicInteger timesRef = new AtomicInteger();

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

                        return new DefaultReflectAdviceListenerAdapter() {

                            @Override
                            public void before(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
                                if (isBefore) {
                                    watching(advice);
                                }
                            }

                            @Override
                            public void afterFinishing(Advice advice, ProcessContext processContext, InnerContext innerContext) throws Throwable {
                                if( isSuccess
                                        || isException
                                        || isFinish) {
                                    watching(advice);
                                }
                            }

                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            private boolean isInCondition(Advice advice) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
                                }
                            }

                            private void watching(Advice advice) {
                                try {

                                    if (isInCondition(advice)) {
                                        printer.println(new TObject(newExpress(advice).get(express), expend).rendering());
                                        if (isOverThreshold(timesRef.incrementAndGet())) {
                                            printer.finish();
                                        }
                                    }

                                } catch (Exception e) {
                                    logger.warn("watch failed.", e);
                                    printer.println(getCauseMessage(e));
                                }
                            }

                        };

                    }
                };
            }

        };
    }

}

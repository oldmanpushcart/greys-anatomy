package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.*;
import com.github.ompc.greys.core.view.ObjectView;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Advice.*;
import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getCauseMessage;
import static com.github.ompc.greys.core.util.GaStringUtils.newString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
            summary = "express, write by groovy.",
            description = ""
                    + "For example\n" +
                    "    params[0]\n" +
                    "    params[0]+params[1]\n" +
                    "    returnObj\n" +
                    "    throwExp\n" +
                    "    target\n" +
                    "    clazz\n" +
                    "    method\n" +
                    "\n" +
                    "The structure\n" +
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

    @NamedArg(name = "S", summary = "Include subclass")
    private boolean isIncludeSub = GlobalOptions.isIncludeSubClass;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer numberOfLimit;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(classPattern)
                : new Matcher.WildcardMatcher(classPattern);

        final Matcher methodNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(methodPattern)
                : new Matcher.WildcardMatcher(methodPattern);

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
                    public boolean isIncludeSub() {
                        return isIncludeSub;
                    }

                    @Override
                    public AdviceListener getAdviceListener() {

                        return new ReflectAdviceListenerAdapter() {

                            private boolean isBefore() {
                                if (isBefore) {
                                    return true;
                                }

                                return !isBefore
                                        && !isFinish
                                        && !isException
                                        && !isSuccess;
                            }

                            @Override
                            public void before(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args) throws Throwable {
                                if (isBefore()) {
                                    watching(newForBefore(loader, clazz, method, target, args));
                                }
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args,
                                    Object returnObject) throws Throwable {

                                final Advice advice = newForAfterRetuning(loader, clazz, method, target, args, returnObject);
                                if (isSuccess) {
                                    watching(advice);
                                }

                                finishing(advice);
                            }

                            @Override
                            public void afterThrowing(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args,
                                    Throwable throwable) {

                                final Advice advice = newForAfterThrowing(loader, clazz, method, target, args, throwable);
                                if (isException) {
                                    watching(advice);
                                }

                                finishing(advice);
                            }

                            private void finishing(Advice advice) {
                                if (isFinish) {
                                    watching(advice);
                                }
                            }

                            private boolean isLimited(int currentTimes) {
                                return null != numberOfLimit
                                        && currentTimes >= numberOfLimit;
                            }

                            private boolean isNeedExpend() {
                                return null != expend
                                        && expend >= 0;
                            }

                            private void watching(Advice advice) {
                                try {

                                    if (isNotBlank(conditionExpress)
                                            && !newExpress(advice).is(conditionExpress)) {
                                        return;
                                    }

                                    final boolean isF = isLimited(times.incrementAndGet());
                                    final Object value = newExpress(advice).get(express);
                                    printer.println(
                                            isF,
                                            newString(isNeedExpend() ? new ObjectView(value, expend).draw() : value)
                                    );

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

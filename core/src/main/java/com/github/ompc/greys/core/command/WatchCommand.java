package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.view.ObjectView;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Advice;
import com.github.ompc.greys.core.util.GaMethod;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.core.util.Advice.newForAfterThrowing;
import static com.github.ompc.greys.core.util.Advice.newForBefore;
import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getCauseMessage;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Cmd(name = "watch", sort = 4, summary = "The call context information buried point observation methods.",
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

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    @IndexArg(index = 2, name = "express",
            summary = "express, write by groovy.",
            description = ""
            + "For example\n"
            + "    : params[0]\n"
            + "    : params[0]+params[1]\n"
            + "    : returnObj\n"
            + "    : throwExp\n"
            + "    : target\n"
            + "    : clazz\n"
            + "    : method\n"
            + "The structure of 'advice'\n"
            + "          target : the object entity\n"
            + "           clazz : the object's class\n"
            + "          method : the constructor or method\n"
            + "    params[0..n] : the parameters of methods\n"
            + "       returnObj : the return object of methods\n"
            + "        throwExp : the throw exception of methods\n"
            + "        isReturn : the method finish by return\n"
            + "         isThrow : the method finish by throw an exception\n"
    )
    private String express;

    @IndexArg(index = 3, name = "condition-express", isRequired = false,
            summary = "condition express, write by groovy",
            description = ""
            + "For example\n"
            + "    TRUE  : true\n"
            + "    FALSE : false\n"
            + "    TRUE  : params.length>=0"
            + "The structure of 'advice' just like express\n"
    )
    private String conditionExpress;

    @NamedArg(name = "b", summary = "is watch on before")
    private boolean isBefore = false;

    @NamedArg(name = "f", summary = "is watch on finish")
    private boolean isFinish = false;

    @NamedArg(name = "e", summary = "is watch on exception")
    private boolean isException = false;

    @NamedArg(name = "s", summary = "is watch on success")
    private boolean isSuccess = false;

    @NamedArg(name = "x", hasValue = true, summary = "expend level of object. Default level-0")
    private Integer expend;

    @NamedArg(name = "S", summary = "including sub class")
    private boolean isIncludeSub = GlobalOptions.isIncludeSubClass;

    @NamedArg(name = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "number of limit")
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
            public GetEnhancer action(Session session, Instrumentation inst, final Sender sender) throws Throwable {
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
                                    sender.send(
                                            isF,
                                            (isNeedExpend() ? new ObjectView(value, expend).draw() : value) + "\n"
                                    );

                                } catch (Exception e) {
                                    logger.warn("watch failed.", e);
                                    sender.send(false, getCauseMessage(e) + "\n");
                                }
                            }

                        };

                    }
                };
            }

        };
    }

}

package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.ObjectView;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Advice;
import com.github.ompc.greys.util.Express.OgnlExpress;
import com.github.ompc.greys.util.LogUtil;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static com.github.ompc.greys.util.Advice.*;
import static com.github.ompc.greys.util.StringUtil.getCauseMessage;
import static java.util.logging.Level.WARNING;

@Cmd(named = "watch", sort = 4, desc = "The call context information buried point observation methods.",
        eg = {
                "watch -Eb org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]",
                "watch -b org.apache.commons.lang.StringUtils isBlank params[0]",
                "watch -f org.apache.commons.lang.StringUtils isBlank returnObj",
                "watch -bf *StringUtils isBlank params[0]",
                "watch *StringUtils isBlank params[0]"
        })
public class WatchCommand implements Command {

    private final Logger logger = LogUtil.getLogger();

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    @IndexArg(index = 2, name = "express",
            summary = "ognl express, write by ognl.",
            description = ""
                    + " \n"
                    + "For example\n"
                    + "    : params[0]\n"
                    + "    : params[0]+params[1]\n"
                    + "    : returnObj\n"
                    + "    : throwExp\n"
                    + "    : target\n"
                    + "    : clazz\n"
                    + "    : method\n"
                    + " \n"
                    + "The structure of 'advice'\n"
                    + "          target : the object entity\n"
                    + "           clazz : the object's class\n"
                    + "          method : the constructor or method\n"
                    + "    params[0..n] : the parameters of methods\n"
                    + "       returnObj : the return object of methods\n"
                    + "        throwExp : the throw exception of methods\n"
    )
    private String express;

    @NamedArg(named = "b", summary = "is watch on before")
    private boolean isBefore = true;

    @NamedArg(named = "f", summary = "is watch on finish")
    private boolean isFinish = false;

    @NamedArg(named = "e", summary = "is watch on exception")
    private boolean isException = false;

    @NamedArg(named = "s", summary = "is watch on success")
    private boolean isSuccess = false;

    @NamedArg(named = "x", hasValue = true, summary = "expend level of object. Default level-0")
    private Integer expend;

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

                        return new ReflectAdviceListenerAdapter() {

                            @Override
                            public void before(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    Method method,
                                    Object target,
                                    Object[] args) throws Throwable {
                                if (isBefore) {
                                    watching(newForBefore(loader, clazz, method, target, args));
                                }
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    Method method,
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
                                    Method method,
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

                            private void watching(Advice advice) {
                                try {
                                    final Object value = new OgnlExpress(advice).get(express);
                                    if (null != expend
                                            && expend >= 0) {
                                        sender.send(false, new ObjectView(value, expend).draw() + "\n");
                                    } else {
                                        sender.send(false, value + "\n");
                                    }
                                } catch (Exception e) {
                                    if (logger.isLoggable(WARNING)) {
                                        logger.log(WARNING, "watch failed.", e);
                                    }
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

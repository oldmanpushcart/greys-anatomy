package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.AdviceTracingListener;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.TreeView;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.util.StringUtil.tranClassName;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径
 * Created by vlinux on 15/5/27.
 */
@Cmd(named = "trace", sort = 6, desc = "The call stack output buried point method callback each thread.",
        eg = {
                "trace -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
                "trace org.apache.commons.lang.StringUtils isBlank",
                "trace *StringUtils isBlank"
        })
public class TraceCommand implements Command {

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
                ? new Matcher.RegexMatcher(classPattern)
                : new Matcher.WildcardMatcher(classPattern);

        final Matcher methodNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(methodPattern)
                : new Matcher.WildcardMatcher(methodPattern);

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
                        return new AdviceTracingListener() {

                            private final ThreadLocal<Entity> threadBoundEntity = new ThreadLocal<Entity>() {

                                @Override
                                protected Entity initialValue() {
                                    final Entity e = new Entity();
                                    e.view = createTreeView();
                                    e.deep = 0;
                                    return e;
                                }

                            };

                            @Override
                            public void create() {

                            }

                            @Override
                            public void destroy() {
                                threadBoundEntity.remove();
                            }

                            @Override
                            public void invokeBeforeTracing(
                                    String tracingClassName,
                                    String tracingMethodName,
                                    String tracingMethodDesc) throws Throwable {
                                threadBoundEntity.get().view.begin(tranClassName(tracingClassName) + ":" + tracingMethodName + "()");
                            }

                            @Override
                            public void invokeAfterTracing(
                                    String tracingClassName,
                                    String tracingMethodName,
                                    String tracingMethodDesc) throws Throwable {
                                threadBoundEntity.get().view.end();
                            }

                            @Override
                            public void before(
                                    ClassLoader loader,
                                    String className, String methodName, String methodDesc,
                                    Object target, Object[] args) throws Throwable {
                                threadBoundEntity.get().view.begin(tranClassName(className) + ":" + methodName + "()");
                                threadBoundEntity.get().deep++;
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader,
                                    String className, String methodName, String methodDesc,
                                    Object target, Object[] args, Object returnObject) throws Throwable {
                                threadBoundEntity.get().view.end();
                                finishing();
                            }

                            @Override
                            public void afterThrowing(
                                    ClassLoader loader,
                                    String className, String methodName, String methodDesc,
                                    Object target, Object[] args, Throwable throwable) throws Throwable {
                                threadBoundEntity.get().view.begin("throw:" + throwable.getClass().getName() + "()").end().end();
                                finishing();
                            }

                            private void finishing() {
                                if (--threadBoundEntity.get().deep == 0) {
                                    sender.send(false, threadBoundEntity.get().view.draw() + "\n");
                                    threadBoundEntity.get().view = createTreeView();
                                }
                            }

                            private TreeView createTreeView() {
                                return new TreeView("Tracing...");
                            }

                        };
                    }
                };
            }

        };

    }


    /**
     * 用于在ThreadLocal中传递的实体
     */
    private class Entity {

        TreeView view;
        int deep;

    }

}

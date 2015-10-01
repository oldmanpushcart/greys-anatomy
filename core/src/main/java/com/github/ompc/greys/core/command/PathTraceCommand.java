package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Advice;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.view.TreeView;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.core.util.Advice.newForAfterThrowing;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径 Created by vlinux on 15/10/01.
 */
@Cmd(name = "ptrace", sort = 6, summary = "Display the detailed thread path stack of specified class and method",
        eg = {
                "ptrace -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank org\\.apache\\.commons\\.lang\\..*",
                "ptrace org.apache.commons.lang.StringUtils isBlank org.apache.commons.lang.*",
                "ptrace *StringUtils isBlank org.apache.commons.lang.*",
                "ptrace *StringUtils isBlank org.apache.commons.lang.* params[0].length==1"
        })
public class PathTraceCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @IndexArg(index = 2, name = "tracing-path-pattern", summary = "Tracing path of Pattern Matching")
    private String tracingPathPattern;

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

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;


    @Override
    public Action getAction() {

        final Matcher classNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(classPattern)
                : new Matcher.WildcardMatcher(classPattern);

        final Matcher methodNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(methodPattern)
                : new Matcher.WildcardMatcher(methodPattern);

        final Matcher tracingPathMatcher = isRegEx
                ? new Matcher.RegexMatcher(tracingPathPattern)
                : new Matcher.WildcardMatcher(tracingPathPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    @Override
                    public Matcher getClassNameMatcher() {
                        return new Matcher.GroupOrMatcher(
                                classNameMatcher,
                                tracingPathMatcher
                        );
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return new Matcher.WildcardMatcher("*");
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceListenerAdapter() {

                            // 执行计数器
                            private final AtomicInteger times = new AtomicInteger();

                            // 上下文调用关联
                            private final ThreadLocal<Entity> threadBoundEntity = new ThreadLocal<Entity>() {

                                @Override
                                protected Entity initialValue() {
                                    final Entity e = new Entity();
                                    e.view = new TreeView(true, "Tracing...");
                                    e.deep = 0;
                                    return e;
                                }

                            };

                            @Override
                            public void create() {
                                super.create();
                            }

                            @Override
                            public void destroy() {
                                super.destroy();
                            }


                            // 是否跟踪入口
                            private boolean isTracingEnter(Class<?> clazz, GaMethod method) {
                                return classNameMatcher.matching(clazz.getCanonicalName())
                                        && methodNameMatcher.matching(method.getName());
                            }

                            @Override
                            public void before(
                                    final ClassLoader loader,
                                    final Class<?> clazz,
                                    final GaMethod method,
                                    final Object target,
                                    final Object[] args) throws Throwable {
                                final Entity entity = threadBoundEntity.get();
                                entity.deep++;
                                if (!entity.isTracing
                                        && isTracingEnter(clazz, method)) {
                                    entity.isTracing = true;
                                }

                                if (entity.isTracing) {
                                    entity.view.begin(clazz.getCanonicalName() + ":" + method.getName() + "()");
                                }
                            }

                            @Override
                            public void afterReturning(
                                    final ClassLoader loader,
                                    final Class<?> clazz,
                                    final GaMethod method,
                                    final Object target,
                                    final Object[] args,
                                    final Object returnObject) throws Throwable {
                                finishing(newForAfterRetuning(loader, clazz, method, target, args, returnObject));
                            }

                            @Override
                            public void afterThrowing(
                                    final ClassLoader loader,
                                    final Class<?> clazz,
                                    final GaMethod method,
                                    final Object target,
                                    final Object[] args,
                                    final Throwable throwable) throws Throwable {
                                finishing(newForAfterThrowing(loader, clazz, method, target, args, throwable));
                            }

                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            private void finishing(Advice advice) {
                                final Entity entity = threadBoundEntity.get();
                                entity.deep--;
                                if (entity.isTracing) {

                                    // add throw exception
                                    if (advice.isAfterThrowing()) {
                                        entity.view
                                                .begin("throw:" + advice.getThrowExp().getClass().getCanonicalName())
                                                .end();
                                    }

                                    entity.view.end();
                                    if (entity.deep <= 0) {
                                        printer.println(entity.view.draw());

                                        // 超过调用限制就关闭掉跟踪
                                        if( isOverThreshold(times.incrementAndGet()) ) {
                                            printer.finish();
                                        }
                                    }
                                }

                                // remove thread local
                                if (entity.deep <= 0) {
                                    threadBoundEntity.remove();
                                }
                            }

                        };
                    }

                };//getEnhancer:<init>

            }//action

        };//return
    }

    /**
     * 用于在ThreadLocal中传递的实体
     */
    private static class Entity {

        TreeView view;
        int deep;
        boolean isTracing = false;

    }

}

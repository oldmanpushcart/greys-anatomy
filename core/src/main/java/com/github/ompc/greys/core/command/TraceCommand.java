package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceTracingListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Advice;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.view.TreeView;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.core.util.Advice.newForAfterThrowing;
import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getThreadInfo;
import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径 Created by vlinux on 15/5/27.
 */
@Cmd(name = "trace", sort = 6, summary = "Display the detailed thread stack of specified class and method",
        eg = {
            "trace -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank",
            "trace org.apache.commons.lang.StringUtils isBlank",
            "trace *StringUtils isBlank",
            "trace *StringUtils isBlank params[0].length==1"
        })
public class TraceCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
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

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution timesRef")
    private Integer threshold;
    
    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    // 访问计数器
                    private final AtomicInteger timesRef = new AtomicInteger();
                    
                    @Override
                    public Matcher getClassNameMatcher() {
                        return classNameMatcher;
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return methodNameMatcher;
                    }

                    // 跟踪深度
                    private int tracingDeep = 0;

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceTracingListenerAdapter() {

                            private final ThreadLocal<Entity> entityRef = new ThreadLocal<Entity>() {

                                @Override
                                protected Entity initialValue() {
                                    final Entity e = new Entity();
                                    e.view = createTreeView();
                                    e.deep = 0;
                                    return e;
                                }

                            };

                            @Override
                            public void destroy() {
                                entityRef.remove();
                            }

                            @Override
                            public void invokeBeforeTracing(
                                    String tracingClassName,
                                    String tracingMethodName,
                                    String tracingMethodDesc) throws Throwable {
                                entityRef.get().view.begin(tranClassName(tracingClassName) + ":" + tracingMethodName + "()");
                                tracingDeep++;
                            }

                            @Override
                            public void invokeAfterTracing(
                                    String tracingClassName,
                                    String tracingMethodName,
                                    String tracingMethodDesc) throws Throwable {
                                entityRef.get().view.end();
                                tracingDeep--;
                            }

                            @Override
                            public void before(
                                    ClassLoader loader, 
                                    Class<?> clazz, 
                                    GaMethod method, 
                                    Object target, 
                                    Object[] args) throws Throwable {
                                entityRef.get().view.begin(clazz.getName() + ":" + method.getName() + "()");
                                entityRef.get().deep++;
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader, 
                                    Class<?> clazz, 
                                    GaMethod method, 
                                    Object target, 
                                    Object[] args, 
                                    Object returnObject) throws Throwable {
                                entityRef.get().view.end();
                                finishing(newForAfterRetuning(loader, clazz, method, target, args, returnObject));
                            }

                            @Override
                            public void afterThrowing(
                                    ClassLoader loader, 
                                    Class<?> clazz, 
                                    GaMethod method, 
                                    Object target, 
                                    Object[] args, 
                                    Throwable throwable) throws Throwable {
                                entityRef.get().view.begin("throw:" + throwable.getClass().getName() + "()").end();

                                // 这里将堆栈的end全部补上
                                while( tracingDeep-- >= 0 ) {
                                    entityRef.get().view.end();
                                }

                                finishing(newForAfterThrowing(loader, clazz, method, target, args, throwable));
                            }

                            private boolean isPrintIfNecessary(Advice advice) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
                                }
                            }
                            
                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            private void finishing(Advice advice) {
                                if (--entityRef.get().deep == 0) {
                                    if (isPrintIfNecessary(advice)) {
                                        final boolean isF = isOverThreshold(timesRef.incrementAndGet());
                                        printer.println(isF, entityRef.get().view.draw());
                                    }
                                    entityRef.remove();
                                }
                            }

                            private TreeView createTreeView() {
                                return new TreeView(true, "Tracing for : "+ getThreadInfo());
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
    private static class Entity {

        TreeView view;
        int deep;

    }

}

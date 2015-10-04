package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.advisor.*;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.manager.TimeFragmentManager;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.CacheMatcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.util.Matcher.RelationOrMatcher;
import com.github.ompc.greys.core.util.Matcher.TrueMatcher;
import com.github.ompc.greys.core.util.collection.ThreadUnsafeLRUHashMap;
import com.github.ompc.greys.core.view.TimeFragmentTableView;
import com.github.ompc.greys.core.view.TreeView;

import java.lang.instrument.Instrumentation;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getStack;
import static com.github.ompc.greys.core.util.GaStringUtils.getThreadInfo;
import static org.apache.commons.lang3.StringUtils.isBlank;

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

    // 时间片段管理
    private final TimeFragmentManager timeFragmentManager = TimeFragmentManager.Factory.getInstance();

    // TimeTunnel the method call
    @NamedArg(name = "t", summary = "Record the method invocation within time fragments")
    private boolean isTimeTunnel = false;

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

    // 针对ptrace命令调整
    private static final int STACK_DEEP = 12;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new CacheMatcher(
                new PatternMatcher(isRegEx, classPattern),
                new ThreadUnsafeLRUHashMap<String, Boolean>(GlobalOptions.ptraceClassMatcherLruCapacity)
        );

        final Matcher methodNameMatcher = new CacheMatcher(
                new PatternMatcher(isRegEx, methodPattern),
                new ThreadUnsafeLRUHashMap<String, Boolean>(GlobalOptions.ptraceMethodMatcherLruCapacity)
        );

        final Matcher tracingPathMatcher = new PatternMatcher(isRegEx, tracingPathPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    @Override
                    public Matcher getClassNameMatcher() {
                        return new RelationOrMatcher(
                                classNameMatcher,
                                tracingPathMatcher
                        );
                    }

                    @Override
                    public Matcher getMethodNameMatcher() {
                        return new TrueMatcher();
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceListenerAdapter<PathTraceProcessContext, InnerContext>() {

                            private volatile boolean isInit = false;

                            // 执行计数器
                            private final AtomicInteger timesRef = new AtomicInteger();

                            @Override
                            protected PathTraceProcessContext newProcessContext() {
                                return new PathTraceProcessContext();
                            }

                            @Override
                            protected InnerContext newInnerContext() {
                                return new InnerContext();
                            }

                            @Override
                            public void create() {
                                isInit = true;
                            }

                            @Override
                            public void destroy() {
                                isInit = false;
                            }


                            // 是否跟踪入口
                            private boolean isTracingEnter(Class<?> clazz, GaMethod method) {
                                return classNameMatcher.matching(clazz.getCanonicalName())
                                        && methodNameMatcher.matching(method.getName());
                            }

                            @Override
                            public void before(Advice advice, PathTraceProcessContext processContext, InnerContext innerContext) throws Throwable {

                                if (!isInit) {
                                    return;
                                }

                                if (!processContext.isTracing) {
                                    if (isTracingEnter(advice.clazz, advice.method)) {
                                        processContext.isTracing = true;
                                    } else {
                                        return;
                                    }
                                }

                                final Entity entity = processContext.getEntity(new InitCallback<Entity>() {
                                    @Override
                                    public Entity init() {
                                        return new Entity(timeFragmentManager.generateProcessId());
                                    }
                                });

                                entity.view.begin(advice.clazz.getCanonicalName() + ":" + advice.method.getName() + "()");
                                entity.deep++;
                            }

                            @Override
                            public void afterFinishing(Advice advice, PathTraceProcessContext processContext, InnerContext innerContext) throws Throwable {
                                if (!isInit
                                        || !processContext.isTracing){
                                    return;
                                }

                                final long cost = innerContext.getCost();

                                final Entity entity = processContext.getEntity();
                                entity.deep--;

                                // 是否有匹配到条件
                                // 之所以在这里主要是需要照顾到上下文参数对齐
                                if (!isInCondition(advice)) {
                                    return;
                                }

                                // add throw exception
                                if (advice.isThrow) {
                                    entity.view
                                            .begin("throw:" + advice.throwExp.getClass().getCanonicalName())
                                            .end();
                                }



                                // 记录下调用过程
                                if (isTimeTunnel) {
                                    final TimeFragment timeFragment = timeFragmentManager.append(
                                            entity.processId,
                                            advice,
                                            new Date(),
                                            cost,
                                            getStack(STACK_DEEP)
                                    );
                                    entity.tfView.add(timeFragment);
                                    entity.view.set(entity.view.get()+"; index="+timeFragment.id+";");
                                }

                                entity.view.end();

                                if (entity.deep <= 0) {

                                    // 输出打印内容
                                    if (isTimeTunnel) {
                                        printer.println(entity.view.draw() + entity.tfView.draw());
                                    } else {
                                        printer.println(entity.view.draw());
                                    }

                                    // 超过调用限制就关闭掉跟踪
                                    if (isOverThreshold(timesRef.incrementAndGet())) {
                                        printer.finish();
                                    }

                                    processContext.isTracing = false;
                                    processContext.removeEntity();
                                }

                            }

                            // 是否到达节制阀值
                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            // 匹配过滤规则
                            private boolean isInCondition(Advice advice) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
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
    private class Entity {

        private Entity(int processId) {
            this.processId = processId;
            this.tfView = new TimeFragmentTableView(true);
            this.view = new TreeView(true, "pTracing for : " + getThreadInfo() + "process=" + processId + ";");
            this.deep = 0;
        }

        TimeFragmentTableView tfView;
        TreeView view;
        int deep;
        final int processId;

    }

    private class PathTraceProcessContext extends ProcessContext {
        boolean isTracing;
        Entity entity;

        Entity getEntity() {
            return entity;
        }

        Entity getEntity(InitCallback<Entity> initCallback) {
            if( null == entity ) {
                return entity = initCallback.init();
            } else {
                return entity;
            }
        }

        void removeEntity() {
            entity = null;
        }

    }

}

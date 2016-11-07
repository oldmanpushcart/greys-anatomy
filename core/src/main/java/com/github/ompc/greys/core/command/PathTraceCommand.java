package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.InitCallback;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.manager.TimeFragmentManager;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TTree;
import com.github.ompc.greys.core.textui.ext.TTimeFragmentTable;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.InvokeCost;
import com.github.ompc.greys.core.util.PointCut;
import com.github.ompc.greys.core.util.collection.ThreadUnsafeLRUHashMap;
import com.github.ompc.greys.core.util.matcher.*;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static com.github.ompc.greys.core.util.GaStringUtils.getStack;
import static com.github.ompc.greys.core.util.GaStringUtils.getThreadInfo;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径 Created by oldmanpushcart@gmail.com on 15/10/01.
 */
@Cmd(name = "ptrace", sort = 6, summary = "Display the detailed thread path stack of specified class and method",
        eg = {
                "ptrace -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank org\\.apache\\.commons\\.lang\\..*",
                "ptrace -E .*\\.StringUtils isBlank org\\.apache\\.commons\\.(lang|lang3)\\..*",
                "ptrace org.apache.commons.lang.StringUtils isBlank org.apache.commons.lang.*",
                "ptrace *StringUtils isBlank org.apache.commons.lang.*",
                "ptrace *StringUtils isBlank org.apache.commons.lang.* 'params[0].length==1'",
                "ptrace *StringUtils isBlank org.apache.commons.lang.* '#cost>100'"
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

//    @IndexArg(index = 2, name = "tracing-path-pattern", summary = "Tracing path of Pattern Matching")
//    private String tracingPathPattern;

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
                    "         isThrow : the method ended by throwing exception"
    )
    private String conditionExpress;

    @NamedArg(name = "path", summary = "path-tracing-pattern", hasValue = true)
    private Collection<String> pathTracingPatterns;

    @NamedArg(name = "Epath", summary = "path-tracing-regex-pattern", hasValue = true)
    private Collection<String> pathTracingRegexPatterns;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution times")
    private Integer threshold;

    /*
     * 构造追踪路径匹配
     */
    private Matcher<String> newPathTracingMatcher() {

        final ArrayList<Matcher<String>> matcherList = new ArrayList<Matcher<String>>();

        // fill path
        if (null != pathTracingPatterns) {
            for (String pathTracingPattern : pathTracingPatterns) {
                matcherList.add(new PatternMatcher(isRegEx, pathTracingPattern));
            }
        }

        // fill Epath
        if (null != pathTracingRegexPatterns) {
            for (String pathTracingRegexPattern : pathTracingRegexPatterns) {
                matcherList.add(new PatternMatcher(PatternMatcher.Strategy.REGEX, pathTracingRegexPattern));
            }
        }

        return new GroupMatcher.Or<String>(matcherList);

    }

    @Override
    public Action getAction() {

        final Matcher<String> classNameMatcher = new CachedMatcher<String>(
                new PatternMatcher(isRegEx, classPattern),
                new ThreadUnsafeLRUHashMap<String, Boolean>(GlobalOptions.ptraceClassMatcherLruCapacity)
        );

        final Matcher<String> methodNameMatcher = new CachedMatcher<String>(
                new PatternMatcher(isRegEx, methodPattern),
                new ThreadUnsafeLRUHashMap<String, Boolean>(GlobalOptions.ptraceMethodMatcherLruCapacity)
        );

        final Matcher pathTracingMatcher = newPathTracingMatcher();


        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    @Override
                    public PointCut getPointCut() {
                        return new PointCut(
                                new ClassMatcher(new GroupMatcher.Or<String>(classNameMatcher, pathTracingMatcher)),
                                new GaMethodMatcher(new TrueMatcher<String>())
                        );
                    }

                    @Override
                    public AdviceListener getAdviceListener() {
                        return new ReflectAdviceListenerAdapter() {

                            private final InvokeCost topInvokeCost = new InvokeCost();
                            private final InvokeCost invokeCost = new InvokeCost();

                            private final ThreadLocal<PathTrace> pathTraceRef = new ThreadLocal<PathTrace>() {
                                @Override
                                protected PathTrace initialValue() {
                                    return new PathTrace();
                                }
                            };

                            private volatile boolean isInit = false;

                            // 执行计数器
                            private final AtomicInteger timesRef = new AtomicInteger();

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
                            public void before(final Advice advice) throws Throwable {

                                if (!isInit) {
                                    return;
                                }

                                invokeCost.begin();

                                final PathTrace pathTrace = pathTraceRef.get();
                                if (!pathTrace.isTracing) {
                                    if (isTracingEnter(advice.getClazz(), advice.getMethod())) {
                                        pathTrace.isTracing = true;
                                    } else {
                                        return;
                                    }
                                }

                                final Entity entity = pathTrace.getEntity(new InitCallback<Entity>() {
                                    @Override
                                    public Entity init() {
                                        return new Entity(advice, timeFragmentManager.generateProcessId());
                                    }
                                });

                                // top invoke
                                if(entity.deep <= 0) {
                                    topInvokeCost.begin();
                                }

                                entity.tTree.begin(advice.getClazz().getCanonicalName() + ":" + advice.getMethod().getName() + "()");
                                entity.deep++;
                            }

                            @Override
                            public void afterFinishing(Advice advice) throws Throwable {
                                final PathTrace pathTrace = pathTraceRef.get();
                                if (!isInit
                                        || !pathTrace.isTracing) {
                                    return;
                                }

                                final long cost = invokeCost.cost();

                                final Entity entity = pathTrace.getEntity();
                                entity.deep--;

                                // add throw exception
                                if (advice.isThrow) {
                                    entity.tTree
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
                                            getStack(getThreadInfo())
                                    );
                                    entity.tfTable.add(timeFragment);
                                    entity.tTree.set(entity.tTree.get() + "; index=" + timeFragment.id + ";");
                                }

                                entity.tTree.end();

                                if (entity.deep <= 0) {

                                    // top invoke cost
                                    final long topCost = topInvokeCost.cost();

                                    // 是否有匹配到条件
                                    // 之所以在这里主要是需要照顾到上下文参数对齐
                                    if (isInCondition(advice, topCost)) {
                                        // 输出打印内容
                                        if (isTimeTunnel) {
                                            printer.println(entity.tTree.rendering() + entity.tfTable.rendering());
                                        } else {
                                            printer.println(entity.tTree.rendering());
                                        }

                                        // 超过调用限制就关闭掉跟踪
                                        if (isOverThreshold(timesRef.incrementAndGet())) {
                                            printer.finish();
                                        }
                                    }

                                    pathTrace.isTracing = false;
                                    pathTrace.removeEntity();
                                }

                            }

                            // 是否到达节制阀值
                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            // 匹配过滤规则
                            private boolean isInCondition(Advice advice, long cost) {
                                try {
                                    return isBlank(conditionExpress)
                                            || newExpress(advice).bind("cost", cost).is(conditionExpress);
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

        private Entity(final Advice advice, final int processId) {
            this.processId = processId;
            this.tfTable = new TTimeFragmentTable(true);
            this.tTree = new TTree(true, getTitle(advice, processId));
            this.deep = 0;
        }

        private String getTitle(final Advice advice, final int processId) {
            final StringBuilder titleSB = new StringBuilder()
                    .append("pTracing for : ").append(getThreadInfo())
                    .append("process=").append(processId).append(";");
            if (advice.isTraceSupport()) {
                titleSB.append("traceId=").append(advice.getTraceId()).append(";");
            }
            return titleSB.toString();
        }

        TTimeFragmentTable tfTable;
        TTree tTree;
        int deep;
        final int processId;

    }

    private class PathTrace {
        boolean isTracing;
        Entity entity;

        Entity getEntity() {
            return entity;
        }

        Entity getEntity(InitCallback<Entity> initCallback) {
            if (null == entity) {
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

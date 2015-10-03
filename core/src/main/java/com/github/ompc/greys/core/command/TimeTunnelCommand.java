package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.manager.TimeFragmentManager;
import com.github.ompc.greys.core.manager.TimeFragmentManager.TimeFragment;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.*;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.view.ObjectView;
import com.github.ompc.greys.core.view.TableView;
import com.github.ompc.greys.core.view.TimeFragmentTableView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.core.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.core.util.Advice.newForAfterThrowing;
import static com.github.ompc.greys.core.util.GaStringUtils.getStack;
import static com.github.ompc.greys.core.util.GaStringUtils.hashCodeToHexString;
import static com.github.ompc.greys.core.util.GaStringUtils.newString;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.*;


/**
 * 时光隧道命令<br/>
 * 参数w/d依赖于参数i所传递的记录编号<br/>
 * Created by vlinux on 14/11/15.
 */
@Cmd(name = "tt", sort = 5, summary = "Time Tunnel",
        eg = {
                "tt -t *StringUtils isEmpty",
                "tt -t *StringUtils isEmpty params[0].length==1",
                "tt -l",
                "tt -D",
                "tt -i 1000 -w params[0]",
                "tt -i 1000 -d",
                "tt -i 1000"
        })
public class TimeTunnelCommand implements Command {

    // 时间片段管理
    private final TimeFragmentManager timeFragmentManager = TimeFragmentManager.Factory.getInstance();

    // TimeTunnel the method call
    @NamedArg(name = "t", summary = "Record the method invocation within time fragments")
    private boolean isTimeTunnel = false;

    @IndexArg(index = 0, isRequired = false, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, isRequired = false, name = "method-pattern", summary = "Method of Pattern Matching")
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

    // list the TimeTunnel
    @NamedArg(name = "l", summary = "List all the time fragments")
    private boolean isList = false;

    @NamedArg(name = "D", summary = "Delete all the time fragments")
    private boolean isDeleteAll = false;

    // index of TimeTunnel
    @NamedArg(name = "i", hasValue = true, summary = "Display the detailed information from specified time fragment")
    private Integer index;

    // expend of TimeTunnel
    @NamedArg(name = "x", hasValue = true, summary = "Expand level of object (0 by default)")
    private Integer expend;

    // watch the index TimeTunnel
    @NamedArg(name = "w",
            hasValue = true,
            summary = "watch-express, watch the time fragment by groovy express, like params[0], returnObj, throwExp and so on.",
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
    private String watchExpress = EMPTY;

    @NamedArg(name = "s",
            hasValue = true,
            summary = "Search-expression, to search the time fragments by groovy express",
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
                    "         isThrow : the method ended by throwing exception" +
                    "           index : the index of time-fragment record" +
                    "       processId : the process ID of time-fragment record" +
                    "            cost : the cost time of time-fragment record"
    )
    private String searchExpress = EMPTY;

    // play the index TimeTunnel
    @NamedArg(name = "p", summary = "Replay the time fragment specified by index")
    private boolean isPlay = false;

    // delete the index TimeTunnel
    @NamedArg(name = "d", summary = "Delete time fragment specified by index")
    private boolean isDelete = false;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @NamedArg(name = "n", hasValue = true, summary = "Threshold of execution timesRef")
    private Integer threshold;

    // 针对tt命令调整
    private static final int STACK_DEEP = 11;

    /**
     * 检查参数是否合法
     */
    private void checkArguments() {

        // 检查d/p参数是否有i参数配套
        if ((isDelete || isPlay)
                && null == index) {
            throw new IllegalArgumentException("Time fragment index is expected, please type -i to specify");
        }

        // 在t参数下class-pattern,method-pattern
        if (isTimeTunnel) {
            if (isBlank(classPattern)) {
                throw new IllegalArgumentException("Class-pattern is expected, please type the wildcard expression to match");
            }
            if (isBlank(methodPattern)) {
                throw new IllegalArgumentException("Method-pattern is expected, please type the wildcard expression to match");
            }
        }

        // 一个参数都没有是不行滴
        if (null == index
                && !isTimeTunnel
                && !isDelete
                && !isDeleteAll
                && isBlank(watchExpress)
                && !isList
                && isBlank(searchExpress)
                && !isPlay) {
            throw new IllegalArgumentException("Argument(s) is/are expected, type 'help tt' to read usage");
        }

    }


    /*
     * do the TimeTunnel command
     */
    private GetEnhancerAction doTimeTunnel() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

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

                        return new ReflectAdviceListenerAdapter() {

                            /*
                             * 第一次启动标记
                             */
                            private volatile boolean isFirst = true;

                            /*
                             * 方法执行时间戳
                             */
                            final ThreadLocal<Long> timestampRef = new ThreadLocal<Long>() {
                                @Override
                                protected Long initialValue() {
                                    return currentTimeMillis();
                                }
                            };

                            @Override
                            public void before(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args) throws Throwable {
                                timestampRef.get();
                            }

                            @Override
                            public void afterReturning(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args,
                                    Object returnObject) throws Throwable {
                                afterFinishing(
                                        newForAfterRetuning(
                                                loader,
                                                clazz,
                                                method,
                                                target,
                                                args,
                                                returnObject
                                        ));
                            }

                            @Override
                            public void afterThrowing(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    GaMethod method,
                                    Object target,
                                    Object[] args,
                                    Throwable throwable) {
                                afterFinishing(
                                        newForAfterThrowing(
                                                loader,
                                                clazz,
                                                method,
                                                target,
                                                args,
                                                throwable
                                        ));
                            }

                            private boolean isOverThreshold(int currentTimes) {
                                return null != threshold
                                        && currentTimes >= threshold;
                            }

                            private boolean isInCondition(final Advice advice) {
                                try {
                                    return isNotBlank(conditionExpress)
                                            || Express.ExpressFactory.newExpress(advice).is(conditionExpress);
                                } catch (ExpressException e) {
                                    return false;
                                }
                            }

                            private void afterFinishing(Advice advice) {

                                final long cost = currentTimeMillis() - timestampRef.get();

                                // reset the timestamp
                                timestampRef.remove();

                                if( !isInCondition(advice) ) {
                                    return;
                                }

                                final TimeFragment timeFragment = timeFragmentManager.append(
                                        timeFragmentManager.generateProcessId(),
                                        advice,
                                        new Date(),
                                        cost,
                                        getStack(STACK_DEEP)
                                );

                                final TimeFragmentTableView view = new TimeFragmentTableView(isFirst)
                                        .turnOffBottom()    // 表格控件不输出表格上边框,这样两个表格就能拼凑在一起
                                        .add(timeFragment)  // 填充表格内容
                                        ;
                                if (isFirst) {
                                    isFirst = false;
                                }

                                final boolean isF = isOverThreshold(timesRef.incrementAndGet());
                                if (isF) {
                                    view.turnOnBottom();
                                }
                                printer.print(isF, view.draw());
                            }

                        };
                    }
                };
            }
        };

    }


    /*
     * do list timeFragmentMap
     */
    private RowAction doList() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                final ArrayList<TimeFragment> timeFragments = timeFragmentManager.list();
                printer.print(drawTimeTunnelTable(timeFragments)).finish();
                return new RowAffect(timeFragments.size());
            }
        };

    }

    private boolean hasWatchExpress() {
        return isNotBlank(watchExpress);
    }

    private boolean hasSearchExpress() {
        return isNotBlank(searchExpress);
    }

    private boolean isNeedExpend() {
        return null != expend
                && expend > 0;
    }

    /*
     * do search timeFragmentMap
     */
    private RowAction doSearch() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                // 匹配的时间片段
                final ArrayList<TimeFragment> matchingTimeFragments = timeFragmentManager.search(searchExpress);

                // 执行watchExpress
                if (hasWatchExpress()) {

                    final TableView view = new TableView(new TableView.ColumnDefine[]{
                            new TableView.ColumnDefine(TableView.Align.RIGHT),
                            new TableView.ColumnDefine(TableView.Align.LEFT)
                    })
                            .hasBorder(true)
                            .padding(1)
                            .addRow("INDEX", "SEARCH-RESULT");

                    for (TimeFragment timeFragment : matchingTimeFragments) {
                        final Object value = Express.ExpressFactory.newExpress(timeFragment.advice).get(watchExpress);
                        view.addRow(
                                timeFragment.id,
                                isNeedExpend()
                                        ? new ObjectView(value, expend).draw()
                                        : value
                        );

                    }

                    printer.print(view.draw()).finish();
                } // 单纯的列表格
                else {
                    printer.print(drawTimeTunnelTable(matchingTimeFragments)).finish();
                }

                return new RowAffect(matchingTimeFragments.size());
            }
        };

    }

    /*
     * 清除所有的记录
     */
    private RowAction doDeleteAll() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                final int count = timeFragmentManager.clean();
                printer.println("Time fragments are cleaned.").finish();
                return new RowAffect(count);
            }
        };

    }

    /*
     * 查看记录信息
     */
    private RowAction doWatch() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final TimeFragment timeFragment = timeFragmentManager.get(index);
                if (null == timeFragment) {
                    printer.println(format("Time fragment[%d] does not exist.", index)).finish();
                    return new RowAffect();
                }

                final Advice advice = timeFragment.advice;
                final Object value = Express.ExpressFactory.newExpress(advice).get(watchExpress);
                if (isNeedExpend()) {
                    printer.println(new ObjectView(value, expend).draw()).finish();
                } else {
                    printer.println(newString(value)).finish();
                }

                return new RowAffect(1);
            }
        };

    }

    /*
     * 重放指定记录
     */
    private RowAction doPlay() {
        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final TimeFragment timeFragment = timeFragmentManager.get(index);
                if (null == timeFragment) {
                    printer.println(format("Time fragment[%d] does not exist.", timeFragment.id)).finish();
                    return new RowAffect();
                }

                final Advice advice = timeFragment.advice;
                final String className = advice.getClazz().getName();
                final String methodName = advice.getMethod().getName();
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                final TableView view = new TableView(new TableView.ColumnDefine[]{
                        new TableView.ColumnDefine(TableView.Align.RIGHT),
                        new TableView.ColumnDefine(150, false, TableView.Align.LEFT)
                })
                        .hasBorder(true)
                        .padding(1)
                        .addRow("INDEX", timeFragment.id)
                        .addRow("PROCESS-ID", timeFragment.processId)
                        .addRow("GMT-CREATE", sdf.format(timeFragment.gmtCreate))
                        .addRow("GMT-REPLAY", sdf.format(new Date()))
                        .addRow("OBJECT", hashCodeToHexString(advice.getTarget()))
                        .addRow("CLASS", className)
                        .addRow("METHOD", methodName);

                // fill the parameters
                if (null != advice.getParams()) {

                    int paramIndex = 0;
                    for (Object param : advice.getParams()) {

                        if (isNeedExpend()) {
                            view.addRow("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param, expend).draw());
                        } else {
                            view.addRow("PARAMETERS[" + paramIndex++ + "]", param);
                        }

                    }

                }

                final GaMethod method = advice.getMethod();
                final boolean accessible = advice.getMethod().isAccessible();
                try {
                    method.setAccessible(true);
                    final Object returnObj = method.invoke(advice.getTarget(), advice.getParams());

                    // 执行成功:输出成功状态
                    view.addRow("IS-RETURN", true);
                    view.addRow("IS-EXCEPTION", false);

                    // 执行成功:输出成功结果
                    if (isNeedExpend()) {
                        view.addRow("RETURN-OBJ", new ObjectView(returnObj, expend).draw());
                    } else {
                        view.addRow("RETURN-OBJ", returnObj);
                    }

                } catch (Throwable t) {

                    // 执行失败:输出失败状态
                    view.addRow("IS-RETURN", false);
                    view.addRow("IS-EXCEPTION", true);

                    // 执行失败:输出失败异常信息
                    final Throwable cause;
                    if (t instanceof InvocationTargetException) {
                        cause = t.getCause();
                    } else {
                        cause = t;
                    }

                    if (isNeedExpend()) {
                        view.addRow("THROW-EXCEPTION", new ObjectView(cause, expend).draw());
                    } else {
                        final StringWriter stringWriter = new StringWriter();
                        final PrintWriter printWriter = new PrintWriter(stringWriter);
                        try {
                            cause.printStackTrace(printWriter);
                            view.addRow("THROW-EXCEPTION", stringWriter.toString());
                        } finally {
                            printWriter.close();
                        }
                    }

                } finally {
                    method.setAccessible(accessible);
                }

                printer.print(view.hasBorder(true).padding(1).draw())
                        .println(format("Time fragment[%d] successfully replayed.", index))
                        .finish();
                return new RowAffect(1);
            }
        };
    }

    /*
     * 删除指定记录
     */
    private RowAction doDelete() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                final RowAffect affect = new RowAffect();
                if (timeFragmentManager.delete(index) != null) {
                    affect.rCnt(1);
                }
                printer.println(format("Time fragment[%d] successfully deleted.", index)).finish();
                return affect;
            }
        };

    }

    /*
     * 绘制TimeTunnel表格
     */
    private String drawTimeTunnelTable(final ArrayList<TimeFragment> timeFragments) {
        final TimeFragmentTableView view = new TimeFragmentTableView(true);
        for (TimeFragment timeFragment : timeFragments) {
            view.add(timeFragment);
        }
        return view.draw();
    }


    /*
     * 展示指定记录
     */
    private RowAction doShow() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final TimeFragment timeFragment = timeFragmentManager.get(index);
                if (null == timeFragment) {
                    printer.println(format("Time fragment[%d] does not exist.", index)).finish();
                    return new RowAffect();
                }

                final Advice advice = timeFragment.advice;
                final String className = advice.getClazz().getName();
                final String methodName = advice.getMethod().getName();
                final String objectAddress = advice.getTarget() == null
                        ? "NULL"
                        : "0x" + toHexString(advice.getTarget().hashCode());
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                final TableView view = new TableView(new TableView.ColumnDefine[]{
                        new TableView.ColumnDefine(TableView.Align.RIGHT),
                        new TableView.ColumnDefine(150, false, TableView.Align.LEFT)
                })
                        .hasBorder(true)
                        .padding(1)
                        .addRow("INDEX", timeFragment.id)
                        .addRow("PROCESS", timeFragment.processId)
                        .addRow("GMT-CREATE", sdf.format(timeFragment.gmtCreate))
                        .addRow("COST(ms)", timeFragment.cost)
                        .addRow("OBJECT", objectAddress)
                        .addRow("CLASS", className)
                        .addRow("METHOD", methodName)
                        .addRow("IS-RETURN", advice.isAfterReturning())
                        .addRow("IS-EXCEPTION", advice.isAfterThrowing());

                // fill the parameters
                if (null != advice.getParams()) {

                    int paramIndex = 0;
                    for (Object param : advice.getParams()) {

                        if (isNeedExpend()) {
                            view.addRow("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param, expend).draw());
                        } else {
                            view.addRow("PARAMETERS[" + paramIndex++ + "]", param);
                        }

                    }

                }

                // fill the returnObj
                if (advice.isAfterReturning()) {

                    if (isNeedExpend()) {
                        view.addRow("RETURN-OBJ", new ObjectView(advice.getReturnObj(), expend).draw());
                    } else {
                        view.addRow("RETURN-OBJ", advice.getReturnObj());
                    }

                }

                // fill the throw exception
                if (advice.isAfterThrowing()) {

                    //noinspection ThrowableResultOfMethodCallIgnored
                    final Throwable throwable = advice.getThrowExp();

                    if (isNeedExpend()) {
                        view.addRow("THROW-EXCEPTION", new ObjectView(advice.getThrowExp(), expend).draw());
                    } else {
                        final StringWriter stringWriter = new StringWriter();
                        final PrintWriter printWriter = new PrintWriter(stringWriter);
                        try {
                            throwable.printStackTrace(printWriter);
                            view.addRow("THROW-EXCEPTION", stringWriter.toString());
                        } finally {
                            printWriter.close();
                        }

                    }

                }

                // fill the stack
                view.addRow("STACK", timeFragment.stack);

                printer.print(view.draw()).finish();

                return new RowAffect(1);
            }
        };

    }

    @Override
    public Action getAction() {

        // 检查参数
        checkArguments();

        final Action action;
        if (isTimeTunnel) {
            action = doTimeTunnel();
        } else if (isList) {
            action = doList();
        } else if (isDeleteAll) {
            action = doDeleteAll();
        } else if (isDelete) {
            action = doDelete();
        } else if (isPlay) {
            action = doPlay();
        } else if (null != index) {
            if (hasWatchExpress()) {
                action = doWatch();
            } else {
                action = doShow();
            }
        } else if (hasSearchExpress()) {
            action = doSearch();
        } else {
            action = new SilentAction() {
                @Override
                public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                    throw new UnsupportedOperationException("not support operation.");
                }
            };
        }

        return action;

    }

}

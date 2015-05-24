package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.advisor.ReflectAdviceListenerAdapter;
import com.github.ompc.greys.command.affect.RowAffect;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.ObjectView;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.command.view.TableView.ColumnDefine;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Advice;
import com.github.ompc.greys.util.Express.OgnlExpress;
import com.github.ompc.greys.util.LogUtil;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.ompc.greys.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.command.view.TableView.Align.RIGHT;
import static com.github.ompc.greys.command.view.TableView.BORDER_BOTTOM;
import static com.github.ompc.greys.util.Advice.newForAfterRetuning;
import static com.github.ompc.greys.util.Advice.newForAfterThrowing;
import static com.github.ompc.greys.util.Matcher.WildcardMatcher;
import static com.github.ompc.greys.util.StringUtil.*;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;


/**
 * 时间碎片
 */
class TimeFragment {

    public TimeFragment(Advice advice, Date gmtCreate) {
        this.advice = advice;
        this.gmtCreate = gmtCreate;
    }

    private final Advice advice;
    private final Date gmtCreate;

    public Advice getAdvice() {
        return advice;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }
}


/**
 * 时光隧道命令<br/>
 * 参数w/d依赖于参数i所传递的记录编号<br/>
 * Created by vlinux on 14/11/15.
 */
@Cmd(named = "tt", sort = 5, desc = "TimeTunnel the method call.",
        eg = {
                "tt -t *StringUtils isEmpty",
                "tt -l",
                "tt -D",
                "tt -i 1000 -w params[0]",
                "tt -i 1000 -d",
                "tt -i 1000"
//                "tt -i 1000 -p"
        })
public class TimeTunnelCommand implements Command {

    private final Logger logger = LogUtil.getLogger();

    // 时间隧道(时间碎片的集合)
    private static final Map<Integer, TimeFragment> timeTunnel = new LinkedHashMap<Integer, TimeFragment>();

    // 时间碎片序列生成器
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    // TimeTunnel the method call
    @NamedArg(named = "t", summary = "TimeTunnel the method called.")
    private boolean isTimeTunnel = false;

    @IndexArg(index = 0, isRequired = false, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, isRequired = false, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    // list the TimeTunnel
    @NamedArg(named = "l", summary = "list all the TimeTunnels.")
    private boolean isList = false;

    @NamedArg(named = "D", summary = "delete all TimeTunnels.")
    private boolean isDeleteAll = false;

    // index of TimeTunnel
    @NamedArg(named = "i", hasValue = true, summary = "appoint the index of TimeTunnel. If use only, show the TimeTunnel detail.")
    private Integer index;

    // expend of TimeTunnel
    @NamedArg(named = "x", hasValue = true, summary = "expend level of object. Default level-0")
    private Integer expend;

    // watch the index TimeTunnel
    @NamedArg(named = "w",
            hasValue = true,
            summary = "watch-express, watch the TimeTunnel's data by OGNL-express, like params[0], returnObj, throwExp and so on.",
            description = ""
                    + " \n"
                    + "For example\n"
                    + "    : params[0]\n"
                    + "    : params[0]+params[1]\n"
                    + "    : returnObj\n"
                    + "    : throwExp\n"
                    + "    : target.targetThis.getClass()\n"
                    + " \n"
                    + "The structure of 'advice'\n"
                    + "          target : the object entity\n"
                    + "           clazz : the object's class\n"
                    + "          method : the constructor or method\n"
                    + "    params[0..n] : the parameters of methods\n"
                    + "       returnObj : the return object of methods\n"
                    + "        throwExp : the throw exception of methods\n"
                    + " \n"
    )
    private String watchExpress = EMPTY;


    @NamedArg(named = "s",
            hasValue = true,
            summary = "search-express, searching the TimeTunnels by OGNL-express"
    )
    private String searchExpress = EMPTY;


//    // play the index TimeTunnel
//    @NamedArg(named = "p", summary = "rePlay the TimeTunnel of method called.")
//    private boolean isPlay = false;

    // delete the index TimeTunnel
    @NamedArg(named = "d", summary = "delete the index TimeTunnel")
    private boolean isDelete = false;

    @NamedArg(named = "S", summary = "including sub class")
    private boolean isIncludeSub = false;

    @NamedArg(named = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    /**
     * 检查参数是否合法
     */
    private void checkArguments() {

        // 检查d参数是否有i参数配套
        if (isDelete) {

            if (null == index) {
                throw new IllegalArgumentException("miss TimeTunnel index, please type -i to appoint it.");
            }

        }

        // 在t参数下class-pattern,method-pattern
        if (isTimeTunnel) {
            if (isBlank(classPattern)) {
                throw new IllegalArgumentException("miss class-pattern, please type the wildcard express to matching class.");
            }
            if (isBlank(methodPattern)) {
                throw new IllegalArgumentException("miss method-pattern, please type the wildcard express to matching method.");
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
//                && !isPlay
                ) {

            throw new IllegalArgumentException("miss arguments, type 'help tt' to got usage.");

        }

    }

    /*
     * 记录时间片段
     */
    private int putTimeTunnel(TimeFragment tt) {
        final int index = sequence.getAndIncrement();
        timeTunnel.put(index, tt);
        return index;
    }


    /*
     * 各列宽度
     */
    private static final int[] TABLE_COL_WIDTH = new int[]{

            8,  // index
            20, // timestamp
            8,  // isRet
            8,  // isExp
            15, // object address
            30, // class
            30, // method

    };

    /*
     * 各列名称
     */
    private static final String[] TABLE_COL_TITLE = new String[]{

            "INDEX",
            "TIMESTAMP",
            "IS-RET",
            "IS-EXP",
            "OBJECT",
            "CLASS",
            "METHOD"

    };

    /*
     * do the TimeTunnel command
     */
    private GetEnhancerAction doTimeTunnel() {

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

                            volatile boolean isFirst = true;

                            @Override
                            public void afterReturning(
                                    ClassLoader loader,
                                    Class<?> clazz,
                                    Method method,
                                    Object target,
                                    Object[] args,
                                    Object returnObject) throws Throwable {
                                afterFinishing(newForAfterRetuning(
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
                                    Method method,
                                    Object target,
                                    Object[] args,
                                    Throwable throwable) {
                                afterFinishing(newForAfterThrowing(
                                        loader,
                                        clazz,
                                        method,
                                        target,
                                        args,
                                        throwable
                                ));
                            }

                            private void afterFinishing(Advice advice) {

                                final TimeFragment timeTunnel = new TimeFragment(advice, new Date());
                                final int index = putTimeTunnel(timeTunnel);
                                final TableView view = createTableView();

                                if (isFirst) {
                                    isFirst = false;

                                    // 填充表格头部
                                    fillTableTitle(view);
                                }

                                // 表格控件不输出表格上边框,这样两个表格就能拼凑在一起
                                view.border(view.border() & ~BORDER_BOTTOM);

                                // 填充表格内容
                                fillTableRow(view, index, timeTunnel);

                                sender.send(false, view.draw());
                            }

                        };
                    }
                };
            }
        };

    }


    /*
     * do list timeTunnel
     */
    private RowAction doList() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                sender.send(true, drawTimeTunnelTable(timeTunnel));
                return new RowAffect(timeTunnel.size());
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
     * do search timeTunnel
     */
    private RowAction doSearch() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                // 匹配的时间片段
                final Map<Integer, TimeFragment> matchingTimeSegmentMap = new LinkedHashMap<Integer, TimeFragment>();

                for (Map.Entry<Integer, TimeFragment> entry : timeTunnel.entrySet()) {
                    final int index = entry.getKey();
                    final TimeFragment tf = entry.getValue();
                    final Advice advice = tf.getAdvice();

                    // 搜索出匹配的时间片段
                    if ((new OgnlExpress(advice)).is(searchExpress)) {
                        matchingTimeSegmentMap.put(index, tf);
                    }

                }

                // 执行watchExpress
                if (hasWatchExpress()) {

                    final TableView view = new TableView(new ColumnDefine[]{
                            new ColumnDefine(RIGHT),
                            new ColumnDefine(LEFT)
                    })
                            .border(true)
                            .padding(1)
                            .addRow("INDEX", "SEARCH-RESULT");

                    for (Map.Entry<Integer, TimeFragment> entry : matchingTimeSegmentMap.entrySet()) {
                        final Object value = new OgnlExpress(entry.getValue().getAdvice()).get(watchExpress);
                        view.addRow(
                                entry.getKey(),
                                isNeedExpend()
                                        ? new ObjectView(value, expend).draw()
                                        : value
                        );

                    }

                    sender.send(true, view.draw());
                }

                // 单纯的列表格
                else {
                    sender.send(true, drawTimeTunnelTable(matchingTimeSegmentMap));
                }

                return new RowAffect(matchingTimeSegmentMap.size());
            }
        };

    }

    /*
     * 清除所有的记录
     */
    private RowAction doDeleteAll() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                final int count = timeTunnel.size();
                timeTunnel.clear();
                sender.send(true, "fragments was clean.");
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
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                final TimeFragment tf = timeTunnel.get(index);
                if (null == tf) {
                    sender.send(true, format("%d fragment was not existed.", index));
                    return new RowAffect();
                }

                final Advice advice = tf.getAdvice();
                final Object value = new OgnlExpress(advice).get(watchExpress);
                if (isNeedExpend()) {
                    sender.send(true, new ObjectView(value, expend).draw() + "\n");
                } else {
                    sender.send(true, value + "\n");
                }

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
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                final RowAffect affect = new RowAffect();
                if (timeTunnel.remove(index) != null) {
                    affect.rCnt(1);
                }
                sender.send(true, format("delete %s success.", index));
                return affect;
            }
        };

    }


    private TableView createTableView() {
        return new TableView(new ColumnDefine[]{
                new ColumnDefine(TABLE_COL_WIDTH[0], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[1], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[2], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[3], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[4], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[5], false, RIGHT),
                new ColumnDefine(TABLE_COL_WIDTH[6], false, RIGHT),
        })
                .border(true)
                .padding(1)
                ;
    }

    private TableView fillTableTitle(TableView tableView) {
        return tableView
                .addRow(
                        TABLE_COL_TITLE[0],
                        TABLE_COL_TITLE[1],
                        TABLE_COL_TITLE[2],
                        TABLE_COL_TITLE[3],
                        TABLE_COL_TITLE[4],
                        TABLE_COL_TITLE[5],
                        TABLE_COL_TITLE[6]
                );
    }

    /*
     * 绘制TimeTunnel表格
     */
    private String drawTimeTunnelTable(final Map<Integer, TimeFragment> timeTunnelMap) {
        final TableView view = fillTableTitle(createTableView());
        for (Map.Entry<Integer, TimeFragment> entry : timeTunnelMap.entrySet()) {
            final int index = entry.getKey();
            final TimeFragment tf = entry.getValue();
            fillTableRow(view, index, tf);
        }
        return view.draw();
    }

    /*
     * 填充表格行
     */
    private TableView fillTableRow(TableView tableView, int index, TimeFragment tf) {

        final Advice advice = tf.getAdvice();
        return tableView.addRow(
                index,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tf.getGmtCreate()),
                advice.isAfterReturning(),
                advice.isAfterThrowing(),
                advice.getTarget() == null
                        ? "NULL"
                        : "0x" + toHexString(advice.getTarget().hashCode()),
                substringAfterLast("." + advice.getClazz().getName(), "."),
                advice.getMethod().getName()
        );
    }


    /*
     * 展示指定记录
     */
    private RowAction doShow() {

        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                final TimeFragment tf = timeTunnel.get(index);
                if (null == tf) {
                    sender.send(true, format("%d fragment was not existed.", index));
                    return new RowAffect();
                }

                final Advice advice = tf.getAdvice();
                final String className = advice.getClazz().getName();
                final String methodName = advice.getMethod().getName();
                final String objectAddress = advice.getTarget() == null
                        ? "NULL"
                        : "0x" + toHexString(advice.getTarget().hashCode());
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                final TableView view = new TableView(new ColumnDefine[]{
                        new ColumnDefine(RIGHT),
                        new ColumnDefine(LEFT)
                })
                        .border(true)
                        .padding(1)
                        .addRow("INDEX", index)
                        .addRow("GMT-CREATE", sdf.format(tf.getGmtCreate()))
                        .addRow("IS-RETURN", advice.isAfterReturning())
                        .addRow("IS-EXCEPTION", advice.isAfterThrowing())
                        .addRow("OBJECT", objectAddress)
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

                sender.send(true, view.draw());

                return new RowAffect(1);
            }
        };

    }

    @Override
    public Action getAction() {

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format("classPattern=%s;expend=%s;index=%s;isDelete=%s;isDeleteAll=%s;isList=%s;isRegEx=%s;watchExpress=%s;",
                    this.classPattern,
                    this.expend,
                    this.index,
                    this.isDelete,
                    this.isDeleteAll,
                    this.isList,
                    this.isRegEx,
                    this.watchExpress));
        }

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
                public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                    throw new UnsupportedOperationException("not support operation.");
                }
            };
        }

        return action;

    }

}



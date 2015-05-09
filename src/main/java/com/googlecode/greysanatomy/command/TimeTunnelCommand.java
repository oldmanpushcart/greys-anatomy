package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer;
import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.IndexArg;
import com.googlecode.greysanatomy.command.annotation.NamedArg;
import com.googlecode.greysanatomy.command.view.TableView;
import com.googlecode.greysanatomy.command.view.TableView.ColumnDefine;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.server.GaSession;
import com.googlecode.greysanatomy.util.GaObjectUtils;
import com.googlecode.greysanatomy.util.GaOgnlUtils;
import com.googlecode.greysanatomy.util.LogUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.command.view.TableView.Align.LEFT;
import static com.googlecode.greysanatomy.command.view.TableView.Align.RIGHT;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;
import static com.googlecode.greysanatomy.util.GaStringUtils.*;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.util.logging.Level.WARNING;

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
public class TimeTunnelCommand extends Command {

    private final Logger logger = LogUtils.getLogger();

    // the TimeTunnels collection
    private static final Map<Integer, TimeTunnel> timeTunnelMap = new LinkedHashMap<Integer, TimeTunnel>();

    // the TimeTunnel's index sequence
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    // TimeTunnel the method call
    @NamedArg(named = "t", description = "TimeTunnel the method called.")
    private boolean isTimeTunnel = false;

    @IndexArg(index = 0, isRequired = false, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, isRequired = false, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    // list the TimeTunnel
    @NamedArg(named = "l", description = "list all the TimeTunnels.")
    private boolean isList = false;

    @NamedArg(named = "D", description = "delete all TimeTunnels.")
    private boolean isDeleteAll = false;

    // index of TimeTunnel
    @NamedArg(named = "i", hasValue = true, description = "appoint the index of TimeTunnel. If use only, show the TimeTunnel detail.")
    private Integer index;

    // expend of TimeTunnel
    @NamedArg(named = "x", hasValue = true, description = "expend level of object. Default level-0")
    private Integer expend;

    // watch the index TimeTunnel
    @NamedArg(named = "w",
            hasValue = true,
            description = "watchExpress, watch the TimeTunnel's data by OGNL-express, like params[0], returnObj, throwExp and so on.",
            description2 = ""
                    + " \n"
                    + "For example\n"
                    + "    : params[0]\n"
                    + "    : params[0]+params[1]\n"
                    + "    : returnObj\n"
                    + "    : throwExp\n"
                    + "    : target.targetThis.getClass()\n"
                    + " \n"
                    + "The structure of 'advice'\n"
                    + "    params[0..n] : the parameters of methods\n"
                    + "    returnObj    : the return object of methods\n"
                    + "    throwExp     : the throw exception of methods\n"
                    + "    target\n"
                    + "    \\+- targetThis  : the object entity\n"
                    + "    \\+- targetClassName : the object's class\n"
                    + "    \\+- targetBehaviorName : the object's class\n"
                    + " \n"
    )
    private String watchExpress = EMPTY;


    @NamedArg(named = "s",
            hasValue = true,
            description = "searchExpress, searching the TimeTunnels by OGNL-express"
    )
    private String searchExpress = EMPTY;


//    // play the index TimeTunnel
//    @RiscNamedArg(named = "p", summary = "play the TimeTunnel of method called.")
//    private boolean isPlay = false;

    // delete the index TimeTunnel
    @NamedArg(named = "d", description = "delete the index TimeTunnel")
    private boolean isDelete = false;

    @NamedArg(named = "S", description = "including sub class")
    private boolean isSuper = false;

    @NamedArg(named = "E", description = "enable the regex pattern matching")
    private boolean isRegEx = false;

    /**
     * 命令是否启用正则表达式匹配
     *
     * @return true启用正则表达式/false不启用
     */
    public boolean isRegEx() {
        return isRegEx;
    }

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

//        // 如果只有i参数，没有对应的p/w/d，则是没有意义的
//        if (null != index) {
//
//            if (StringUtils.isBlank(watchExpress)
////                    && !isPlay
//                    && !isDelete) {
//                throw new IllegalArgumentException("miss arguments to work in with -w/-d .");
//            }
//
//        }

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

    /**
     * 创建TimeTunnel
     *
     * @param timeTunnel
     * @return
     */
    private int putTimeTunnel(TimeTunnel timeTunnel) {
        final int index = sequence.getAndIncrement();
        timeTunnelMap.put(index, timeTunnel);
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

    /**
     * do the TimeTunnel command
     *
     * @param info
     * @param sender
     * @throws Throwable
     */
    private void doTimeTunnel(final Info info, final Sender sender) throws Throwable {

        final Instrumentation inst = info.getInst();
        final GreysAnatomyClassFileTransformer.TransformResult result = transform(inst, classPattern, methodPattern, isSuper, isRegEx(), new AdviceListenerAdapter() {

            boolean isFirst = true;

            @Override
            public void onFinish(Advice advice) {

                try {

                    final TimeTunnel timeTunnel = new TimeTunnel(advice, new Date());
                    final int index = putTimeTunnel(timeTunnel);
                    final TableView view = createTableView();

                    if (isFirst) {
                        // output the title
                        isFirst = false;
//                        printTableHead(lineSB);
                        fillTableTitle(view);
                    }
//                    else {
//                        view.border(view.border() & ~TableView.BORDER_TOP);
//                    }

                    view.border(view.border() & ~TableView.BORDER_BOTTOM);

                    fillTableRow(view, index, timeTunnel);
//                    printTimeTunnel(lineSB, index, timeTunnel);

                    sender.send(false, view.draw());

                } catch (Throwable t) {
                    if (logger.isLoggable(WARNING)) {
                        logger.log(WARNING, "TimeTunnel failed.", t);
                    }
                }

            }
        }, info, false);

        // 激活任务
        activeJob(result.getId());

        final StringBuilder message = new StringBuilder();
        message.append(LINE);
        message.append(format("result: matching-class=%s,matching-method=%s.\n",
                result.getModifiedClasses().size(),
                result.getModifiedBehaviors().size()));
        message.append(ABORT_MSG).append("\n");
        sender.send(false, message.toString());

    }


    /**
     * do list timeTunnelMap
     *
     * @param sender
     * @throws Throwable
     */
    private void doList(final Sender sender) throws Throwable {

        final StringBuilder lineSB = new StringBuilder();
        if (timeTunnelMap.isEmpty()) {
            lineSB.append("TimeTunnels was empty.");
        } else {
            lineSB.append(drawTimeTunnelTable(timeTunnelMap));
        }

        sender.send(true, lineSB.toString());

    }

    /**
     * do search timeTunnelMap
     *
     * @param sender
     * @throws Throwable
     */
    private void doSearch(final Sender sender) throws Throwable {

        final Map<Integer, TimeTunnel> matchedTimeTunnelMap = new LinkedHashMap<Integer, TimeTunnel>();

        // search TimeTunnels
        for (Map.Entry<Integer, TimeTunnel> entry : timeTunnelMap.entrySet()) {
            final int index = entry.getKey();
            final TimeTunnel timeTunnel = entry.getValue();

            if (GaOgnlUtils.is(searchExpress, timeTunnel.getAdvice())) {
                matchedTimeTunnelMap.put(index, timeTunnel);
            }

        }

        final StringBuilder lineSB = new StringBuilder();

        // 执行watchExpress
        if (isNotBlank(watchExpress)) {

            final TableView view = new TableView(new ColumnDefine[]{
                    new ColumnDefine(RIGHT),
                    new ColumnDefine(LEFT)
            })
                    .border(true)
                    .padding(1)
                    .addRow("INDEX", "SEARCH-RESULT");

            for (Map.Entry<Integer, TimeTunnel> entry : matchedTimeTunnelMap.entrySet()) {

                final TimeTunnel timeTunnel = entry.getValue();
                final Advice p = timeTunnel.getAdvice();
                final Object value = GaOgnlUtils.getValue(watchExpress, p);

                if (null != expend
                        && expend > 0) {
                    view.addRow(entry.getKey(), GaObjectUtils.toString(value, 0, expend));
                } else {
                    view.addRow(entry.getKey(), value);
                }

            }

            lineSB.append(view.draw());

        }

        // 单纯的列表格
        else {
            if (matchedTimeTunnelMap.isEmpty()) {
                lineSB.append("not match record found.\n");
            } else {
                lineSB.append(drawTimeTunnelTable(matchedTimeTunnelMap));
            }
        }

        lineSB.append(matchedTimeTunnelMap.size() + " record matched.");

        sender.send(true, lineSB.toString());

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
    private String drawTimeTunnelTable(final Map<Integer, TimeTunnel> timeTunnelMap) {
        final TableView view = fillTableTitle(createTableView());
        for (Map.Entry<Integer, TimeTunnel> entry : timeTunnelMap.entrySet()) {
            final int index = entry.getKey();
            final TimeTunnel timeTunnel = entry.getValue();
            fillTableRow(view, index, timeTunnel);
        }
        return view.draw();
    }


    /**
     * 清除所有的记录
     *
     * @param sender
     */
    private void doDeleteAll(final Sender sender) {
        timeTunnelMap.clear();
        sender.send(true, "All TimeTunnels was deleted.");
    }

    /**
     * 查看记录信息
     *
     * @param sender
     * @throws Throwable
     */
    private void doWatch(final Sender sender) throws Throwable {

        // find the TimeTunnel
        final TimeTunnel timeTunnel = timeTunnelMap.get(index);
        if (null == timeTunnel) {
            sender.send(true, format("TimeTunnel %s not found.", index));
            return;
        }

        final Advice p = timeTunnel.getAdvice();
        final Object value = GaOgnlUtils.getValue(watchExpress, p);

        if (null != expend
                && expend > 0) {
            sender.send(true, "" + GaObjectUtils.toString(value, 0, expend) + "\n");
        } else {
            sender.send(true, "" + value + "\n");
        }

    }


    /**
     * 删除指定记录
     *
     * @param sender
     */
    private void doDelete(final Sender sender) {

        timeTunnelMap.remove(index);
        sender.send(true, format("delete %s successed.", index));

    }

//    /**
//     * 执行回放操作
//     *
//     * @param sender
//     * @throws Throwable
//     */
//    private void doPlay(final Sender sender) throws Throwable {
//
//        // find the record
//        final Record record = timeTunnelMap.get(index);
//        if (null == record) {
//            sender.send(true, format("record %s not found.", index));
//            return;
//        }
//
//        try {
//            final Method method = record.getTargetMethod();
//            method.invoke(record.getTargetThis(), record.getParameters());
//        } catch (Throwable t) {
//            // do nothing...
//            logger.info("play record {} got an exception.", index, t);
//        } finally {
//            sender.send(true, format("play record %s done.", index));
//        }
//
//    }


    private TableView fillTableRow(TableView tableView, int index, TimeTunnel timeTunnel) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return tableView.addRow(
                index,
                sdf.format(timeTunnel.getGmtCreate()),
                timeTunnel.getAdvice().isReturn(),
                timeTunnel.getAdvice().isThrowException(),
                timeTunnel.getAdvice().getTarget().getTargetThis() == null
                        ? "NULL"
                        : "0x" + toHexString(timeTunnel.getAdvice().getTarget().getTargetThis().hashCode()),
                substringAfterLast("." + timeTunnel.getAdvice().getTarget().getTargetClassName(), "."),
                timeTunnel.getAdvice().getTarget().getTargetBehaviorName()
        );
    }


    /**
     * 展示指定记录
     *
     * @param sender
     */
    private void doShow(final Sender sender) {

        // find the TimeTunnel
        final TimeTunnel timeTunnel = timeTunnelMap.get(index);
        if (null == timeTunnel) {
            sender.send(true, format("TimeTunnel %s not found.", index));
            return;
        }

        final String className = timeTunnel.getAdvice().getTarget().getTargetClassName();
        final String methodName = timeTunnel.getAdvice().getTarget().getTargetBehaviorName();
        final String objectAddress = timeTunnel.getAdvice().getTarget().getTargetThis() == null
                ? "NULL"
                : "0x" + toHexString(timeTunnel.getAdvice().getTarget().getTargetThis().hashCode());
        final int maxColLen = Math.max(Math.max(className.length(), methodName.length()), 50);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        final TableView view = new TableView(new ColumnDefine[]{
                new ColumnDefine(RIGHT),
                new ColumnDefine(LEFT)
        })
                .border(true)
                .padding(1)
                .addRow("INDEX", index)
                .addRow("GMT-CREATE", sdf.format(timeTunnel.getGmtCreate()))
                .addRow("IS-RETURN", timeTunnel.getAdvice().isReturn())
                .addRow("IS-EXCEPTION", timeTunnel.getAdvice().isThrowException())
                .addRow("OBJECT", objectAddress)
                .addRow("CLASS", className)
                .addRow("METHOD", methodName);


        // fill the parameters
        if (null != timeTunnel.getAdvice().getParameters()) {

            int paramIndex = 0;
            for (Object param : timeTunnel.getAdvice().getParameters()) {

                if (null != expend
                        && expend > 0) {
                    view.addRow("PARAMETERS[" + paramIndex++ + "]", GaObjectUtils.toString(param, 0, expend));
                } else {
                    view.addRow("PARAMETERS[" + paramIndex++ + "]", param);
                }

            }

        }


        // fill the returnObj
        if (timeTunnel.getAdvice().isReturn()) {

            if (null != expend
                    && expend > 0) {
                view.addRow("RETURN-OBJ", GaObjectUtils.toString(timeTunnel.getAdvice().getReturnObj(), 0, expend));
            } else {
                view.addRow("RETURN-OBJ", timeTunnel.getAdvice().getReturnObj());
            }

        }


        // fill the throw exception
        if (timeTunnel.getAdvice().isThrowException()) {
            final Throwable throwable = timeTunnel.getAdvice().getThrowException();

            if (null != expend
                    && expend > 0) {
                view.addRow("THROW-EXCEPTION", GaObjectUtils.toString(throwable, 0, expend));
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

        return new Action() {

            @Override
            public void action(final GaSession gaSession, final Info info, final Sender sender) throws Throwable {

                // 检查参数
                checkArguments();


                // TimeTunnel 命令
                if (isTimeTunnel) {
                    doTimeTunnel(info, sender);
                } else if (isList) {

                    // list 命令
                    doList(sender);

                } else if (isDeleteAll) {

                    // delete all the TimeTunnel
                    doDeleteAll(sender);

                } else if (isDelete) {

                    // delete index TimeTunnel
                    doDelete(sender);

                } else if (null != index) {

                    if (isNotBlank(watchExpress)) {

                        // watch TimeTunnel by js express
                        doWatch(sender);

                    } else {

                        // show the TimeTunnel
                        doShow(sender);

                    }

                } else if (isNotBlank(searchExpress)) {

                    // search
                    doSearch(sender);

                }
//                else if (isPlay) {
//
//                    // play the TimeTunnel
//                    doPlay(sender);
//
//                }


            }

        };
    }

}


/**
 * 探测记录方法调用记录
 */
class TimeTunnel {

    public TimeTunnel(Advice advice, Date gmtCreate) {
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
package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer;
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
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
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;
import static com.googlecode.greysanatomy.util.GaStringUtils.summary;
import static java.lang.String.format;
import static com.googlecode.greysanatomy.util.GaStringUtils.*;

/**
 * 时光隧道命令<br/>
 * 参数w/d依赖于参数i所传递的记录编号<br/>
 * Created by vlinux on 14/11/15.
 */
@RiscCmd(named = "tt", sort = 8, desc = "TimeTunnel the method call.",
        eg = {
                "tt -t .*StringUtils isEmpty",
                "tt -l",
                "tt -D",
                "tt -i 1000 -w p.params[0]",
                "tt -i 1000 -d",
                "tt -i 1000",
//                "tt -i 1000 -p"
        })
public class TimeTunnelCommand extends Command {

    private static final Logger logger = Logger.getLogger("greysanatomy");

    // the TimeTunnels collection
    private static final Map<Integer, TimeTunnel> timeTunnels = new LinkedHashMap<Integer, TimeTunnel>();

    // the TimeTunnel's index sequence
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    // TimeTunnel the method call
    @RiscNamedArg(named = "t", description = "TimeTunnel the method called.")
    private boolean isTimeTunnel = false;

    @RiscIndexArg(index = 0, isRequired = false, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @RiscIndexArg(index = 1, isRequired = false, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

    // list the TimeTunnel
    @RiscNamedArg(named = "l", description = "list all the TimeTunnels.")
    private boolean isList = false;

    @RiscNamedArg(named = "D", description = "delete all TimeTunnels.")
    private boolean isDeleteAll = false;


    // index of TimeTunnel
    @RiscNamedArg(named = "i", hasValue = true, description = "appoint the index of TimeTunnel. If use only, show the TimeTunnel detail.")
    private Integer index;

    // watch the index TimeTunnel
    @RiscNamedArg(named = "w",
            hasValue = true,
            description = "watch the TimeTunnel's data, like p.params[0], p.returnObj, p.throwExp and so on.",
            description2 = ""
                    + " \n"
                    + "For example\n"
                    + "    : p.params[0]\n"
                    + "    : p.params[0]+p.params[1]\n"
                    + "    : p.returnObj\n"
                    + "    : p.throwExp\n"
                    + "    : p.target.targetThis.getClass()\n"
                    + " \n"
                    + "The structure of 'p'\n"
                    + "    p.\n"
                    + "    \\+- params[0..n] : the parameters of methods\n"
                    + "    \\+- returnObj    : the return object of methods\n"
                    + "    \\+- throwExp     : the throw exception of methods\n"
                    + "    \\+- target\n"
                    + "         \\+- targetThis  : the object entity\n"
                    + "         \\+- targetClassName : the object's class\n"
                    + "         \\+- targetBehaviorName : the object's class\n"
                    + " \n")
    private String watchExpress = EMPTY;


//    // play the index TimeTunnel
//    @RiscNamedArg(named = "p", description = "play the TimeTunnel of method called.")
//    private boolean isPlay = false;

    // delete the index TimeTunnel
    @RiscNamedArg(named = "d", description = "delete the index TimeTunnel.")
    private boolean isDelete = false;


    /**
     * 检查参数是否合法
     */
    private void checkArguments() {

        // 检查p/w/d参数是否有i参数配套
        if (isNotBlank(watchExpress)
//                || isPlay
                || isDelete) {

            if (null == index) {
                throw new IllegalArgumentException("miss TimeTunnel index, please type -i to appoint it.");
            }

        }

        // 在r参数下class-regex,method-regex由选填变成必填
        if (isTimeTunnel) {
            if (isBlank(classRegex)) {
                throw new IllegalArgumentException("miss class-regex, please type the regex express to match class.");
            }
            if (isBlank(methodRegex)) {
                throw new IllegalArgumentException("miss method-regex, please type the regex express to match method.");
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
//                && !isPlay
                ) {

            throw new IllegalArgumentException("miss arguments, type help TimeTunnel to got usage.");

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
        timeTunnels.put(index, timeTunnel);
        return index;
    }


    /*
     * 各列宽度
     */
    private static final int[] TABLE_COL_WIDTH = new int[]{

            8, // index
            20, // timestamp
            8, // isRet
            8, // isExp
            15, // object address
//            20, // class loader
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
//            "CLASS-LOADER",
            "CLASS",
            "METHOD"

    };

    /**
     * 打印表头
     *
     * @param lineSB
     */
    private void printTableHead(StringBuilder lineSB) {


        final StringBuilder tableHeadSB = new StringBuilder();
        printLineSplit(tableHeadSB);
        final StringBuilder lineFormatSB = new StringBuilder();
        for (int colWidth : TABLE_COL_WIDTH) {
            lineFormatSB.append("|%").append(colWidth).append("s");
        }
        lineFormatSB.append("|");
        tableHeadSB.append(format(lineFormatSB.toString(), TABLE_COL_TITLE)).append("\n");
        printLineSplit(tableHeadSB);
        lineSB.append(tableHeadSB);

    }

    /**
     * 打印分割行
     *
     * @param lineSB
     */
    private void printLineSplit(StringBuilder lineSB) {
        final StringBuilder lineSplitSB = new StringBuilder();
        for (int colWidth : TABLE_COL_WIDTH) {
            lineSplitSB.append("+").append(repeat("-", colWidth));
        }
        lineSplitSB.append("+").append("\n");
        lineSB.append(lineSplitSB);
    }

    /**
     * 打印记录
     *
     * @param lineSB
     * @param index
     * @param timeTunnel
     */
    private void printTimeTunnel(StringBuilder lineSB, int index, TimeTunnel timeTunnel) {

        final StringBuilder lineFormatSB = new StringBuilder();
        for (int colWidth : TABLE_COL_WIDTH) {
            lineFormatSB.append("|%-").append(colWidth).append("s");
        }
        lineFormatSB.append("|");
        final String lineFormat = lineFormatSB.toString();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lineSB.append(format(lineFormat,
                index,
                sdf.format(timeTunnel.getGmtCreate()),
                timeTunnel.getAdvice().isReturn(),
                timeTunnel.getAdvice().isThrowException(),
                timeTunnel.getAdvice().getTarget().getTargetThis() == null ? "NULL" : "0x" + Integer.toHexString(timeTunnel.getAdvice().getTarget().getTargetThis().hashCode()),
//                substring(TimeTunnel.getTargetClassLoader().getClass().getSimpleName(), 0, TABLE_COL_WIDTH[4]),
                summary(substring(substringAfterLast(timeTunnel.getAdvice().getTarget().getTargetClassName(), "."), 0, TABLE_COL_WIDTH[5]), TABLE_COL_WIDTH[5]),
                summary(substring(timeTunnel.getAdvice().getTarget().getTargetBehaviorName(), 0, TABLE_COL_WIDTH[6]), TABLE_COL_WIDTH[6])
        )).append("\n");

    }

    /**
     * do the TimeTunnel command
     *
     * @param info
     * @param sender
     * @throws Throwable
     */
    private void doTimeTunnel(final Info info, final Sender sender) throws Throwable {

        final Instrumentation inst = info.getInst();
        final GreysAnatomyClassFileTransformer.TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {

            boolean isFirst = true;

            @Override
            public void onFinish(Advice advice) {

                try {

                    final TimeTunnel timeTunnel = new TimeTunnel(advice, new Date());
                    final int index = putTimeTunnel(timeTunnel);

                    final StringBuilder lineSB = new StringBuilder();
                    if (isFirst) {
                        // output the title
                        isFirst = false;
                        printTableHead(lineSB);
                    }

                    printTimeTunnel(lineSB, index, timeTunnel);

                    sender.send(false, lineSB.toString());

                } catch (Throwable t) {
                    if(logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, "TimeTunnel failed.", t);
                    }
                }

            }
        }, info);

        // 注册任务
        registJob(info.getSessionId(), result.getId());

        // 激活任务
        activeJob(result.getId());

        final StringBuilder message = new StringBuilder();
        message.append(LINE);
        message.append(format("done. probe:c-Cnt=%s,m-Cnt=%s\n",
                result.getModifiedClasses().size(),
                result.getModifiedBehaviors().size()));
        message.append(ABORT_MSG).append("\n");
        sender.send(false, message.toString());

    }


    /**
     * do list timeTunnels
     *
     * @param sender
     * @throws Throwable
     */
    private void doList(final Sender sender) throws Throwable {

        final StringBuilder lineSB = new StringBuilder();
        if (timeTunnels.isEmpty()) {
            lineSB.append("timeTunnels is empty.\n");
        } else {
            printTableHead(lineSB);
            for (Map.Entry<Integer, TimeTunnel> entry : timeTunnels.entrySet()) {
                printTimeTunnel(lineSB, entry.getKey(), entry.getValue());
            }
            printLineSplit(lineSB);

        }

        sender.send(true, lineSB.toString());

    }

    /**
     * 清除所有的记录
     *
     * @param sender
     */
    private void doDeleteAll(final Sender sender) {
        timeTunnels.clear();
        sender.send(true, "All timeTunnels was deleted.\n");
    }

    /**
     * 查看记录信息
     *
     * @param sender
     * @throws Throwable
     */
    private void doWatch(final Sender sender) throws Throwable {

        // find the TimeTunnel
        final TimeTunnel timeTunnel = timeTunnels.get(index);
        if (null == timeTunnel) {
            sender.send(true, format("TimeTunnel %s not found.", index));
            return;
        }

        final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");

        jsEngine.eval("function printWatch(p,o){try{o.send(true, " + watchExpress + "+'\\n');}catch(e){o.send(true, e.message+'\\n');}}");
        final Invocable invoke = (Invocable) jsEngine;
        final Advice p = timeTunnel.getAdvice();
        invoke.invokeFunction("printWatch", p, sender);

    }


    /**
     * 删除指定记录
     *
     * @param sender
     */
    private void doDelete(final Sender sender) {

        timeTunnels.remove(index);
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
//        final Record record = timeTunnels.get(index);
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


    /**
     * 展示指定记录
     *
     * @param sender
     */
    private void doShow(final Sender sender) {

        // find the TimeTunnel
        final TimeTunnel timeTunnel = timeTunnels.get(index);
        if (null == timeTunnel) {
            sender.send(true, format("TimeTunnel %s not found.", index));
            return;
        }

        final String className = timeTunnel.getAdvice().getTarget().getTargetClassName();
        final String methodName = timeTunnel.getAdvice().getTarget().getTargetBehaviorName();
        final String objectAddress = timeTunnel.getAdvice().getTarget().getTargetThis() == null ? "NULL" : "0x" + Integer.toHexString(timeTunnel.getAdvice().getTarget().getTargetThis().hashCode());
        final int maxColLen = Math.max(Math.max(className.length(), methodName.length()), 50);

        final StringBuilder detailSB = new StringBuilder();
        final String headFormat = "|%20s|%-" + maxColLen + "s|";
        final String lineSplit = new StringBuilder()
                .append("+").append(repeat("-", 20))
                .append("+").append(repeat("-", maxColLen))
                .append("+\n")
                .toString();

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // fill the head
        detailSB.append(lineSplit)
                .append(format(headFormat, "INDEX: ", index)).append("\n")
                .append(format(headFormat, "GMT-CREATE: ", sdf.format(timeTunnel.getGmtCreate()))).append("\n")
                .append(format(headFormat, "IS-RETURN: ", timeTunnel.getAdvice().isReturn())).append("\n")
                .append(format(headFormat, "IS-EXCEPTION: ", timeTunnel.getAdvice().isThrowException())).append("\n")
                .append(format(headFormat, "OBJECT: ", objectAddress)).append("\n")
                .append(format(headFormat, "CLASS: ", className)).append("\n")
                .append(format(headFormat, "METHOD: ", methodName)).append("\n")
                .append(lineSplit)
        .append("\n");


        // fill the paramenters
        if (null != timeTunnel.getAdvice().getParameters()) {

            int paramIndex = 0;
            for (Object param : timeTunnel.getAdvice().getParameters()) {
                detailSB.append("PARAMETERS[" + paramIndex++ + "]:\n").append(param).append("\n\n");
            }

        }


        // fill the returnObj
        if (timeTunnel.getAdvice().isReturn()) {
            detailSB.append("RETURN-OBJ:\n").append(timeTunnel.getAdvice().getReturnObj()).append("\n\n");
        }


        // fill the throw exception
        if (timeTunnel.getAdvice().isThrowException()) {
            final Throwable throwable = timeTunnel.getAdvice().getThrowException();
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            detailSB.append("THROW-EXCEPTION:\n").append(stringWriter.toString()).append("\n\n");
        }

        sender.send(true, detailSB.toString());

    }

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {

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

                } else if (isNotBlank(watchExpress)) {

                    // watch TimeTunnel by js express
                    doWatch(sender);

                } else if (isDelete) {

                    // delete index TimeTunnel
                    doDelete(sender);

                } else if (null != index) {

                    // show the TimeTunnel
                    doShow(sender);

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
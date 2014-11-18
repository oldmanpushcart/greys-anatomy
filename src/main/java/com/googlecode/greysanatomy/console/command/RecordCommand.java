package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer;
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;
import static com.googlecode.greysanatomy.util.GaStringUtils.summary;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.*;

/**
 * 记录命令<br/>
 * 参数w/d依赖于参数i所传递的记录编号<br/>
 * Created by vlinux on 14/11/15.
 */
@RiscCmd(named = "record", sort = 8, desc = "Recording the method call.",
        eg = {
                "record -r .*StringUtils isEmpty",
                "record -l",
                "record -D",
                "record -i 1000 -w p.params[0]",
                "record -i 1000 -d",
                "record -i 1000",
//                "record -i 1000 -p"
        })
public class RecordCommand extends Command {

    private final Logger logger = LoggerFactory.getLogger("greysanatomy");

    // the records collection
    private static final Map<Integer, Record> records = new LinkedHashMap<Integer, Record>();

    // the record's index sequence
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    // record the method call
    @RiscNamedArg(named = "r", description = "record the method called.")
    private boolean isRecord = false;

    @RiscIndexArg(index = 0, isRequired = false, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @RiscIndexArg(index = 1, isRequired = false, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

    // list the record
    @RiscNamedArg(named = "l", description = "list all the records.")
    private boolean isList = false;

    @RiscNamedArg(named = "D", description = "delete all records.")
    private boolean isDeleteAll = false;


    // index of record
    @RiscNamedArg(named = "i", hasValue = true, description = "appoint the index of record. If use only, show the record detail.")
    private Integer index;

    // watch the index record
    @RiscNamedArg(named = "w",
            hasValue = true,
            description = "watch the record's data, like p.params[0], p.returnObj, p.throwExp and so on.",
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
                    + "         \\+- targetClass : the object's class\n"
                    + " \n")
    private String watchExpress = StringUtils.EMPTY;


//    // play the index record
//    @RiscNamedArg(named = "p", description = "play the record of method called.")
//    private boolean isPlay = false;

    // delete the index record
    @RiscNamedArg(named = "d", description = "delete the index record.")
    private boolean isDelete = false;


    /**
     * 检查参数是否合法
     */
    private void checkArguments() {

        // 检查p/w/d参数是否有i参数配套
        if (StringUtils.isNotBlank(watchExpress)
//                || isPlay
                || isDelete) {

            if (null == index) {
                throw new IllegalArgumentException("miss record index, please type -i to appoint it.");
            }

        }

        // 在r参数下class-regex,method-regex由选填变成必填
        if (isRecord) {
            if (StringUtils.isBlank(classRegex)) {
                throw new IllegalArgumentException("miss class-regex, please type the regex express to match class.");
            }
            if (StringUtils.isBlank(methodRegex)) {
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
                && !isRecord
                && !isDelete
                && !isDeleteAll
                && StringUtils.isBlank(watchExpress)
                && !isList
//                && !isPlay
                ) {

            throw new IllegalArgumentException("miss arguments, type help record to got usage.");

        }

    }

    /**
     * 创建record
     *
     * @param record
     * @return
     */
    private int putRecord(Record record) {
        final int index = sequence.getAndIncrement();
        records.put(index, record);
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
     * @param record
     */
    private void printRecord(StringBuilder lineSB, int index, Record record) {

        final StringBuilder lineFormatSB = new StringBuilder();
        for (int colWidth : TABLE_COL_WIDTH) {
            lineFormatSB.append("|%-").append(colWidth).append("s");
        }
        lineFormatSB.append("|");
        final String lineFormat = lineFormatSB.toString();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        lineSB.append(format(lineFormat,
                index,
                sdf.format(record.getGmtCreate()),
                record.getAdvice().isReturn(),
                record.getAdvice().isThrowException(),
                record.getAdvice().getTarget().getTargetThis() == null ? "NULL" : "0x" + Integer.toHexString(record.getAdvice().getTarget().getTargetThis().hashCode()),
//                substring(record.getTargetClassLoader().getClass().getSimpleName(), 0, TABLE_COL_WIDTH[4]),
                summary(substring(substringAfterLast(record.getAdvice().getTarget().getTargetClassName(), "."), 0, TABLE_COL_WIDTH[5]), TABLE_COL_WIDTH[5]),
                summary(substring(record.getAdvice().getTarget().getTargetBehaviorName(), 0, TABLE_COL_WIDTH[6]), TABLE_COL_WIDTH[6])
        )).append("\n");

    }

    /**
     * do the record command
     *
     * @param info
     * @param sender
     * @throws Throwable
     */
    private void doRecord(final Info info, final Sender sender) throws Throwable {

        final Instrumentation inst = info.getInst();
        final GreysAnatomyClassFileTransformer.TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {

            boolean isFirst = true;

            @Override
            public void onFinish(Advice advice) {

                try {

                    final Record record = new Record(advice, new Date());
                    final int index = putRecord(record);

                    final StringBuilder lineSB = new StringBuilder();
                    if (isFirst) {
                        // output the title
                        isFirst = false;
                        printTableHead(lineSB);
                    }

                    printRecord(lineSB, index, record);

                    sender.send(false, lineSB.toString());

                } catch (Throwable t) {
                    logger.warn("record failed.", t);
                }

            }
        }, info);

        // 注册任务
        registJob(info.getSessionId(), result.getId());

        // 激活任务
        activeJob(result.getId());

        final StringBuilder message = new StringBuilder();
        message.append(GaStringUtils.LINE);
        message.append(format("done. probe:c-Cnt=%s,m-Cnt=%s\n",
                result.getModifiedClasses().size(),
                result.getModifiedBehaviors().size()));
        message.append(GaStringUtils.ABORT_MSG).append("\n");
        sender.send(false, message.toString());

    }


    /**
     * do list records
     *
     * @param sender
     * @throws Throwable
     */
    private void doList(final Sender sender) throws Throwable {

        final StringBuilder lineSB = new StringBuilder();
        if (records.isEmpty()) {
            lineSB.append("records is empty.\n");
        } else {
            printTableHead(lineSB);
            for (Map.Entry<Integer, Record> entry : records.entrySet()) {
                printRecord(lineSB, entry.getKey(), entry.getValue());
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
        records.clear();
        sender.send(true, "All records was deleted.\n");
    }

    /**
     * 查看记录信息
     *
     * @param sender
     * @throws Throwable
     */
    private void doWatch(final Sender sender) throws Throwable {

        // find the record
        final Record record = records.get(index);
        if (null == record) {
            sender.send(true, format("record %s not found.", index));
            return;
        }

        final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");

        jsEngine.eval("function printWatch(p,o){try{o.send(true, " + watchExpress + "+'\\n');}catch(e){o.send(true, e.message+'\\n');}}");
        final Invocable invoke = (Invocable) jsEngine;
        final Advice p = record.getAdvice();
        invoke.invokeFunction("printWatch", p, sender);

    }


    /**
     * 删除指定记录
     *
     * @param sender
     */
    private void doDelete(final Sender sender) {

        records.remove(index);
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
//        final Record record = records.get(index);
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

        // find the record
        final Record record = records.get(index);
        if (null == record) {
            sender.send(true, format("record %s not found.", index));
            return;
        }

        final String className = record.getAdvice().getTarget().getTargetClassName();
        final String methodName = record.getAdvice().getTarget().getTargetBehaviorName();
        final String objectAddress = record.getAdvice().getTarget().getTargetThis() == null ? "NULL" : "0x" + Integer.toHexString(record.getAdvice().getTarget().getTargetThis().hashCode());
        final int maxColLen = Math.max(Math.max(className.length(), methodName.length()), 50);

        final StringBuilder detailSB = new StringBuilder();
        final String headFormat = "|%20s|%-" + maxColLen + "s|";
        final String lineSplit = new StringBuilder()
                .append("+").append(StringUtils.repeat("-", 20))
                .append("+").append(StringUtils.repeat("-", maxColLen))
                .append("+\n")
                .toString();

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // fill the head
        detailSB.append(lineSplit)
                .append(format(headFormat, "INDEX: ", index)).append("\n")
                .append(format(headFormat, "GMT-CREATE: ", sdf.format(record.getGmtCreate()))).append("\n")
                .append(format(headFormat, "IS-RETURN: ", record.getAdvice().isReturn())).append("\n")
                .append(format(headFormat, "IS-EXCEPTION: ", record.getAdvice().isThrowException())).append("\n")
                .append(format(headFormat, "OBJECT: ", objectAddress)).append("\n")
                .append(format(headFormat, "CLASS: ", className)).append("\n")
                .append(format(headFormat, "METHOD: ", methodName)).append("\n")
                .append(lineSplit)
        .append("\n");


        // fill the paramenters
        if (null != record.getAdvice().getParameters()) {

            int paramIndex = 0;
            for (Object param : record.getAdvice().getParameters()) {
                detailSB.append("PARAMETERS[" + paramIndex++ + "]:\n").append(param).append("\n\n");
            }

        }


        // fill the returnObj
        if (record.getAdvice().isReturn()) {
            detailSB.append("RETURN-OBJ:\n").append(record.getAdvice().getReturnObj()).append("\n\n");
        }


        // fill the throw exception
        if (record.getAdvice().isThrowException()) {
            final Throwable throwable = record.getAdvice().getThrowException();
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


                // record 命令
                if (isRecord) {
                    doRecord(info, sender);
                } else if (isList) {

                    // list 命令
                    doList(sender);

                } else if (isDeleteAll) {

                    // delete all the record
                    doDeleteAll(sender);

                } else if (StringUtils.isNotBlank(watchExpress)) {

                    // watch record by js express
                    doWatch(sender);

                } else if (isDelete) {

                    // delete index record
                    doDelete(sender);

                } else if (null != index) {

                    // show the record
                    doShow(sender);

                }
//                else if (isPlay) {
//
//                    // play the record
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
class Record {

    public Record(Advice advice, Date gmtCreate) {
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
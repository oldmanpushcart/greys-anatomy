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
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.apache.commons.lang.StringUtils.substring;

/**
 * 方法调用记录／回放命令<br/>
 * 参数p/w/d依赖于参数i所传递的记录编号<br/>
 * Created by vlinux on 14/11/15.
 */
@RiscCmd(named = "record", sort = 8, desc = "Record the method call.",
        eg = {
                "record -r .*StringUtils isEmpty",
                "record -l",
                "record -D",
                "record -i 1000 -w p.params[0]",
                "record -i 1000 -d",
                "record -i 1000 -p"
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
    @RiscNamedArg(named = "i", hasValue = true, description = "the index of record.")
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


    // play the index record
    @RiscNamedArg(named = "p", description = "play the record of method called.")
    private boolean isPlay = false;

    // delete the index record
    @RiscNamedArg(named = "d", description = "delete the index record.")
    private boolean isDelete = false;


    /**
     * 检查参数是否合法
     */
    private void checkArguments() {

        // 检查p/w/d参数是否有i参数配套
        if (StringUtils.isNotBlank(watchExpress)
                || isPlay
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

        // 如果只有i参数，没有对应的p/w/d，则是没有意义的
        if( null != index ) {

            if( StringUtils.isBlank(watchExpress)
                    && !isPlay
                    && !isDelete) {
                throw new IllegalArgumentException("miss arguments to work in with -w/-d/-p .");
            }

        }

        // 一个参数都没有是不行滴
        if( null == index
                && !isRecord
                && !isDelete
                && !isDeleteAll
                && StringUtils.isBlank(watchExpress)
                && !isList
                && !isPlay) {

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
            20, // class loader
            20, // class
            20, // method

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
            "CLASS-LOADER",
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
                record.isReturn(),
                record.isThrowException(),
                record.getTargetThis() == null ? "NULL" : "0x" + Integer.toHexString(record.getTargetThis().hashCode()),
                substring(record.getTargetClassLoader().getClass().getSimpleName(), 0, TABLE_COL_WIDTH[5]),
                substring(record.getTargetClass().getSimpleName(), 0, TABLE_COL_WIDTH[6]),
                substring(record.getTargetMethod().getName(), 0, TABLE_COL_WIDTH[7])
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

                    final Class<?> targetClass = advice.getTarget().getTargetClass();
                    final ClassLoader targetClassLoader = targetClass.getClassLoader();
                    final Object targetObject = advice.getTarget().getTargetThis();
                    final Method targetMethod = targetClass.getDeclaredMethod(advice.getTarget().getTargetBehaviorName(), advice.getTarget().getParameterTypes());

                    final Record record = new Record(
                            targetClassLoader,
                            targetClass,
                            targetMethod,
                            advice.getTarget().getParameterTypes(),
                            targetObject,
                            advice.getParameters(),
                            advice.getReturnObj(),
                            advice.getThrowException(),
                            advice.isReturn(),
                            advice.isThrowException(),
                            new Date()
                    );
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

        jsEngine.eval("function printWatch(p,o){try{o.send(false, " + watchExpress + "+'\\n');}catch(e){o.send(false, e.message+'\\n');}}");
        final Invocable invoke = (Invocable) jsEngine;
        final Advice.Target target = new Advice.Target(
                record.getTargetClass().getName(),
                record.getTargetMethod().getName(),
                record.getTargetThis(),
                record.getTargetClass(),
                record.getParameterTypes());
        final Advice p = new Advice(target, record.getParameters(), true);
        p.setReturnObj(record.getReturnObj());
        p.setThrowException(record.getThrowException());
        invoke.invokeFunction("printWatch", p, sender);
        sender.send(true, "\n");

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

    /**
     * 执行回放操作
     *
     * @param sender
     * @throws Throwable
     */
    private void doPlay(final Sender sender) throws Throwable {

        // find the record
        final Record record = records.get(index);
        if (null == record) {
            sender.send(true, format("record %s not found.", index));
            return;
        }

        try {
            final Method method = record.getTargetMethod();
            method.invoke(record.getTargetThis(), record.getParameters());
        } catch (Throwable t) {
            // do nothing...
            logger.info("play record {} got an exception.", index, t);
        } finally {
            sender.send(true, format("play record %s done.", index));
        }

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

                } else if (isPlay) {

                    // play the record
                    doPlay(sender);

                }


            }

        };
    }

}


/**
 * 方法调用记录
 */
class Record {

    private final ClassLoader targetClassLoader;
    private final Class<?> targetClass;
    private final Method targetMethod;
    private final Class[] parameterTypes;
    private final Object targetThis;
    private final Object[] parameters;
    private final Object returnObj;
    private final Throwable throwExp;
    private final boolean isReturn;
    private final boolean isThrowException;
    private final Date gmtCreate;


    public Record(
            // JVM style
            ClassLoader targetClassLoader, Class<?> targetClass, Method targetMethod, Class[] parameterTypes,
            // Runtime style
            Object targetThis, Object[] parameters, Object returnObj, Throwable throwExp,
            // record info style
            boolean isReturn, boolean isThrowException, Date gmtCreate) {
        this.targetClassLoader = targetClassLoader;
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
        this.parameterTypes = parameterTypes;
        this.targetThis = targetThis;
        this.parameters = parameters;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        this.isReturn = isReturn;
        this.isThrowException = isThrowException;
        this.gmtCreate = gmtCreate;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public boolean isThrowException() {
        return isThrowException;
    }

    public Object getReturnObj() {
        return returnObj;
    }

    public Throwable getThrowException() {
        return throwExp;
    }

    public ClassLoader getTargetClassLoader() {
        return targetClassLoader;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public Object getTargetThis() {
        return targetThis;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

}
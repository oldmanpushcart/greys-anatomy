package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TKv;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.util.affect.RowAffect;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 查看占用CPU资源的TOP线程
 *
 * @author https://github.com/rodbate
 */
@Cmd(name = "top", sort = 13, summary = "Display The Threads Of Top CPU TIME",
        eg = {
                "top",
                "top -t 5",
                "top -d"
        })
public class ThreadTopCommand implements Command {

    @NamedArg(name = "i", hasValue = true, summary = "The thread id info")
    private String tid;

    @NamedArg(name = "t", hasValue = true, summary = "The top NUM of thread cost CPU times")
    private Integer top;

    @NamedArg(name = "d", summary = "Display the thread stack detail")
    private boolean isDetail = false;

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private String stackToString(StackTraceElement[] stackTraceElementArray) {
        final TKv tKv = new TKv(
                new TTable.ColumnDefine(),
                new TTable.ColumnDefine(80)
        );
        if (ArrayUtils.isNotEmpty(stackTraceElementArray)) {
            for (StackTraceElement ste : stackTraceElementArray) {
                tKv.add("at", ste.toString());
            }
        }
        return tKv.rendering();
    }

    @Override
    public Action getAction() {
        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final ArrayList<ThreadInfoData> threadInfoDatas = new ArrayList<ThreadInfoData>();

                long totalCpuTime = 0L;
                long totalUserTime = 0L;
                for (ThreadInfo tInfo : threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), Integer.MAX_VALUE)) {
                    final long tId = tInfo.getThreadId();
                    final String tName = tInfo.getThreadName();
                    final long cpuTime = threadMXBean.getThreadCpuTime(tId);
                    final long userTime = threadMXBean.getThreadUserTime(tId);
                    final String tStateStr = tInfo.getThreadState().toString();
                    final String tStackStr = isDetail
                            ? stackToString(tInfo.getStackTrace())
                            : StringUtils.EMPTY;
                    totalCpuTime += cpuTime;
                    totalUserTime += userTime;
                    threadInfoDatas.add(new ThreadInfoData(tId, cpuTime, userTime, tName, tStateStr, tStackStr));
                }


                final int topFix = top == null ? threadInfoDatas.size() : Math.min(top, threadInfoDatas.size());
                Collections.sort(threadInfoDatas);

                final TTable tTable = new TTable(
                        isDetail
                                ?
                                new TTable.ColumnDefine[]{
                                        new TTable.ColumnDefine(TTable.Align.LEFT),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(),
                                        new TTable.ColumnDefine(20),
                                        new TTable.ColumnDefine(),
                                }
                                :
                                new TTable.ColumnDefine[]{
                                        new TTable.ColumnDefine(TTable.Align.LEFT),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(TTable.Align.MIDDLE),
                                        new TTable.ColumnDefine(),
                                        new TTable.ColumnDefine(50)
                                }
                )
                        .addRow("ID", "CPU%", "USR%", "STATE", "THREAD_NAME", "THREAD_STACK")
                        .padding(1);


                final DecimalFormat df = new DecimalFormat("00.00");
                for (int index = 0; index < topFix; index++) {
                    final ThreadInfoData data = threadInfoDatas.get(index);
                    if (StringUtils.isNotBlank(tid)) {
                        final String fixTid = StringUtils.replace(tid, "#", "");
                        if (!StringUtils.equals("" + data.tId, fixTid)) {
                            continue;
                        }
                    }
                    final String cpuTimeRateStr = (totalCpuTime > 0 ? df.format(data.cpuTime * 100d / totalCpuTime) : "00.00") + "%";
                    final String userTimeRateStr = (totalUserTime > 0 ? df.format(data.userTime * 100d / totalUserTime) : "00.00") + "%";
                    tTable.addRow("#" + data.tId, cpuTimeRateStr, userTimeRateStr, data.tStateStr, data.tName, data.stackStr);
                }

                printer.println(tTable.rendering()).finish();

                return new RowAffect(topFix);
            }
        };
    }

    private class ThreadInfoData implements Comparable<ThreadInfoData> {

        private final long tId;
        private final long cpuTime;
        private final long userTime;
        private final String tName;
        private final String tStateStr;
        private final String stackStr;

        private ThreadInfoData(long tId, long cpuTime, long userTime, String tName, String tStateStr, String stackStr) {
            this.tId = tId;
            this.cpuTime = cpuTime;
            this.userTime = userTime;
            this.tName = tName;
            this.tStateStr = tStateStr;
            this.stackStr = stackStr;
        }

        @Override
        public int compareTo(ThreadInfoData o) {
            return Long.valueOf(o.cpuTime).compareTo(cpuTime);
        }
    }

}

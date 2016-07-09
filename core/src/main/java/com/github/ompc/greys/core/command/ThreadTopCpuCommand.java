package com.github.ompc.greys.core.command;


import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TKv;
import com.github.ompc.greys.core.textui.TTable;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 查看占用CPU资源的TOP线程
 */

@Cmd(name = "topthread" ,sort = 13, summary = "Display The Threads Of Top CPU TIME" ,
        eg = {
                "topthread",
                "topthread 5",
                "topthread 5 10"
        })
public class ThreadTopCpuCommand implements Command {


    @IndexArg(index = 0, name = "Thread NUM", isRequired = false, description = "THE COUNT OF THREAD U WANNA SHOW")
    private String num;

    @IndexArg(index = 1, name = "Thread Stack Depth", isRequired = false, description = "Thread Stack Depth U WANNA SHOW")
    private String depth;

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Action getAction() {

        return new SilentAction() {
            @Override
            public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                try {
                    int number;

                    if ("".equals(num) || num == null) {
                        number = 5;
                    } else {
                        number = Integer.valueOf(num);
                    }

                    int dep;

                    if ("".equals(depth) || depth == null) {
                        dep = 20;
                    } else {
                        dep = Integer.valueOf(depth);
                    }


                    if (threadMXBean.getAllThreadIds().length < number) {
                        printer.print("The num is more than all the current live threads").finish();
                        return;
                    }

                    List<WrappedMessage> messages = getWrappedMessage(dep);
                    if (messages.size() < number) {
                        printer.print("The num is larger than expected").finish();
                    }

                    messages = messages.subList(0, number);

                    final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                            new TTable.ColumnDefine(TTable.Align.MIDDLE),
                            new TTable.ColumnDefine(TTable.Align.MIDDLE),
                            new TTable.ColumnDefine(TTable.Align.LEFT),
                            new TTable.ColumnDefine(TTable.Align.MIDDLE)
                    }).addRow("\nThread ID\n", "\nThread Name\n", "\nThread Stack Info\n", "\nCPU USE TIME(ms)\n").padding(1);

                    for (WrappedMessage mess : messages) {
                        tTable.addRow("\n" + mess.getId() + "\n", "\n" + mess.getName() + "\n",
                                 mess.getStackInfo(), "\n" + mess.getCpuTime() + "\n");
                    }

                    printer.print(tTable.rendering()).finish();
                } catch (NumberFormatException e) {
                    printer.print(e.getMessage()).finish();
                }
            }
        };
    }

    private String[] drawOneRow(WrappedMessage mess) {
        TKv view1 = new TKv();
        TKv view2 = new TKv();
        TKv view3 = new TKv();
        TKv view4 = new TKv();

        view1.add("PID", mess.getId());
        view2.add("Thread Name", mess.getName());
        view3.add("Thread Stack Info", mess.getStackInfo());
        view4.add("CPU TIME", mess.getCpuTime());

        return new String[]{view1.rendering(), view2.rendering(), view3.rendering(), view4.rendering()};
    }


    public  List<WrappedMessage> getWrappedMessage(int depth){

        long[] ids = threadMXBean.getAllThreadIds();

        List<WrappedMessage> list = new ArrayList<WrappedMessage>();

        ThreadInfo[] infoList = threadMXBean.getThreadInfo(ids, depth);

        for (int i = 0; i < infoList.length; i++) {
            WrappedMessage message = new WrappedMessage();
            ThreadInfo info = infoList[i];
            long id = info.getThreadId();
            String name = info.getThreadName();
            long cpuTime = threadMXBean.getThreadCpuTime(id) / (1000 * 1000);
            String state = switchState(info.getThreadState());
            StackTraceElement[] stackTrace = info.getStackTrace();
            StringBuffer sb = new StringBuffer();

            sb.append("\n");
            sb.append("java.lang.Thread.State: " + state + "\n");
            sb.append("\n");
            for (int j = 0; j < stackTrace.length; j++) {
                //sb.append("\n");
                sb.append("     at " + stackTrace[j].toString() + "\n");
                if (j < stackTrace.length - 1) {
                    sb.append("\n");
                }
            }
            sb.append("\n");
            message.setId(id);
            message.setName(name);
            message.setCpuTime(cpuTime);
            message.setStackInfo(sb.toString());
            list.add(message);
        }

        Collections.sort(list, new Comparator<WrappedMessage>() {
            @Override
            public int compare(WrappedMessage o1, WrappedMessage o2) {
                return (int)(o2.getCpuTime() - o1.getCpuTime());
            }
        });

        return list;
    }

    private String switchState(Thread.State state){

        if (state == Thread.State.BLOCKED) return "BLOCKED";
        if (state == Thread.State.NEW) return "NEW";
        if (state == Thread.State.RUNNABLE) return "RUNNABLE";
        if (state == Thread.State.TERMINATED) return "TERMINATED";
        if (state == Thread.State.TIMED_WAITING) return "TIMED_WAITING";
        if (state == Thread.State.WAITING) return "WAITING";

        return "";
    }

    class WrappedMessage{

        private long id;
        private String name;
        private String stackInfo;
        private long cpuTime;
        private String state;


        public long getCpuTime() {
            return cpuTime;
        }

        public void setCpuTime(long cpuTime) {
            this.cpuTime = cpuTime;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getStackInfo() {
            return stackInfo;
        }

        public void setStackInfo(String stackInfo) {
            this.stackInfo = stackInfo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return (id + "    " + "   " + name + stackInfo + "   " + cpuTime);
        }
    }


    /*public static void main(String[] args) throws InterruptedException {

        List<WrappedMessage> wrappedMessage = new ThreadTopCpuCommand().getWrappedMessage(20);

        for (WrappedMessage mess : wrappedMessage) {
            System.out.println(mess.toString());
        }

        Thread.sleep(1000 * 1000);
    }*/
}

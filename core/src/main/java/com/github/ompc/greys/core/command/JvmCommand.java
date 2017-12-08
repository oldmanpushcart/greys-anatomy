package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.SimpleDateFormatHolder;
import com.github.ompc.greys.core.textui.TKv;
import com.github.ompc.greys.core.textui.TTable;

import java.lang.instrument.Instrumentation;
import java.lang.management.*;
import java.util.Collection;

import static com.github.ompc.greys.core.textui.TTable.Align.LEFT;
import static com.github.ompc.greys.core.textui.TTable.Align.RIGHT;

/**
 * JVM info command
 * Created by oldmanpushcart@gmail.com on 15/6/6.
 */
@Cmd(name = "jvm", sort = 10, summary = "Display the target JVM information",
        eg = {
                "jvm"
        }
)
public class JvmCommand implements Command {

    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
    private final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
    private final Collection<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final Collection<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    //    private final Collection<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Action getAction() {
        return new SilentAction() {
            @Override
            public void action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                        new TTable.ColumnDefine(RIGHT),
                        new TTable.ColumnDefine(LEFT)
                })
                        .addRow("CATEGORY", "INFO")
                        .padding(1);

                tTable.addRow("RUNTIME", drawRuntimeTable());
                tTable.addRow("CLASS-LOADING", drawClassLoadingTable());
                tTable.addRow("COMPILATION", drawCompilationTable());

                if (!garbageCollectorMXBeans.isEmpty()) {
                    tTable.addRow("GARBAGE-COLLECTORS", drawGarbageCollectorsTable());
                }

                if (!memoryManagerMXBeans.isEmpty()) {
                    tTable.addRow("MEMORY-MANAGERS", drawMemoryManagersTable());
                }

                tTable.addRow("MEMORY", drawMemoryTable());
                tTable.addRow("OPERATING-SYSTEM", drawOperatingSystemMXBeanTable());
                tTable.addRow("THREAD", drawThreadTable());

                printer.print(tTable.rendering()).finish();

            }
        };
    }


    private String toCol(Collection<String> strings) {
        final StringBuilder colSB = new StringBuilder();
        if (strings.isEmpty()) {
            colSB.append("[]");
        } else {
            for (String str : strings) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private String toCol(String... stringArray) {
        final StringBuilder colSB = new StringBuilder();
        if (null == stringArray
                || stringArray.length == 0) {
            colSB.append("[]");
        } else {
            for (String str : stringArray) {
                colSB.append(str).append("\n");
            }
        }
        return colSB.toString();
    }

    private TKv createKVView() {
        return new TKv(
                new TTable.ColumnDefine(25, false, RIGHT),
                new TTable.ColumnDefine(70, false, LEFT)
        );
    }

    private String drawRuntimeTable() {
        final TKv view = createKVView()
                .add("MACHINE-NAME", runtimeMXBean.getName())
                .add("JVM-START-TIME", SimpleDateFormatHolder.getInstance().format(runtimeMXBean.getStartTime()))
                .add("MANAGEMENT-SPEC-VERSION", runtimeMXBean.getManagementSpecVersion())
                .add("SPEC-NAME", runtimeMXBean.getSpecName())
                .add("SPEC-VENDOR", runtimeMXBean.getSpecVendor())
                .add("SPEC-VERSION", runtimeMXBean.getSpecVersion())
                .add("VM-NAME", runtimeMXBean.getVmName())
                .add("VM-VENDOR", runtimeMXBean.getVmVendor())
                .add("VM-VERSION", runtimeMXBean.getVmVersion())
                .add("INPUT-ARGUMENTS", toCol(runtimeMXBean.getInputArguments()))
                .add("CLASS-PATH", runtimeMXBean.getClassPath())
                .add("BOOT-CLASS-PATH", runtimeMXBean.isBootClassPathSupported() ?
                        runtimeMXBean.getBootClassPath() :
                        "This JVM does not support boot class path.")
                //TODO: add "MODULE-PATH" for JDK 9
                .add("LIBRARY-PATH", runtimeMXBean.getLibraryPath());

        return view.rendering();
    }

    private String drawClassLoadingTable() {
        final TKv view = createKVView()
                .add("LOADED-CLASS-COUNT", classLoadingMXBean.getLoadedClassCount())
                .add("TOTAL-LOADED-CLASS-COUNT", classLoadingMXBean.getTotalLoadedClassCount())
                .add("UNLOADED-CLASS-COUNT", classLoadingMXBean.getUnloadedClassCount())
                .add("IS-VERBOSE", classLoadingMXBean.isVerbose());
        return view.rendering();
    }

    private String drawCompilationTable() {
        final TKv view = createKVView()
                .add("NAME", compilationMXBean.getName());

        if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
            view.add("TOTAL-COMPILE-TIME", compilationMXBean.getTotalCompilationTime() + "(ms)");
        }
        return view.rendering();
    }

    private String drawGarbageCollectorsTable() {
        final TKv view = createKVView();

        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            view.add(garbageCollectorMXBean.getName() + "\n[count/time]",
                    garbageCollectorMXBean.getCollectionCount() + "/" + garbageCollectorMXBean.getCollectionTime() + "(ms)");
        }

        return view.rendering();
    }

    private String drawMemoryManagersTable() {
        final TKv view = createKVView();

        for (final MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
            if (memoryManagerMXBean.isValid()) {
                final String name = memoryManagerMXBean.isValid()
                        ? memoryManagerMXBean.getName()
                        : memoryManagerMXBean.getName() + "(Invalid)";


                view.add(name, toCol(memoryManagerMXBean.getMemoryPoolNames()));
            }
        }

        return view.rendering();
    }

    private String drawMemoryTable() {
        final TKv view = createKVView();

        view.add("HEAP-MEMORY-USAGE\n[committed/init/max/used]",
                memoryMXBean.getHeapMemoryUsage().getCommitted()
                        + "/" + memoryMXBean.getHeapMemoryUsage().getInit()
                        + "/" + memoryMXBean.getHeapMemoryUsage().getMax()
                        + "/" + memoryMXBean.getHeapMemoryUsage().getUsed()
        );

        view.add("NO-HEAP-MEMORY-USAGE\n[committed/init/max/used]",
                memoryMXBean.getNonHeapMemoryUsage().getCommitted()
                        + "/" + memoryMXBean.getNonHeapMemoryUsage().getInit()
                        + "/" + memoryMXBean.getNonHeapMemoryUsage().getMax()
                        + "/" + memoryMXBean.getNonHeapMemoryUsage().getUsed()
        );

        view.add("PENDING-FINALIZE-COUNT", memoryMXBean.getObjectPendingFinalizationCount());
        return view.rendering();
    }


    private String drawOperatingSystemMXBeanTable() {
        final TKv view = createKVView();
        view
                .add("OS", operatingSystemMXBean.getName())
                .add("ARCH", operatingSystemMXBean.getArch())
                .add("PROCESSORS-COUNT", operatingSystemMXBean.getAvailableProcessors())
                .add("LOAD-AVERAGE", operatingSystemMXBean.getSystemLoadAverage())
                .add("VERSION", operatingSystemMXBean.getVersion());
        return view.rendering();
    }

    private String drawThreadTable() {
        final TKv view = createKVView();

        view
                .add("COUNT", threadMXBean.getThreadCount())
                .add("DAEMON-COUNT", threadMXBean.getDaemonThreadCount())
                .add("LIVE-COUNT", threadMXBean.getPeakThreadCount())
                .add("STARTED-COUNT", threadMXBean.getTotalStartedThreadCount())
        ;
        return view.rendering();
    }

}

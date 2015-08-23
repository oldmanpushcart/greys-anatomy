package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.view.KVView;
import com.github.ompc.greys.core.command.view.TableView;
import com.github.ompc.greys.core.server.Session;

import java.lang.instrument.Instrumentation;
import java.lang.management.*;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import static com.github.ompc.greys.core.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.core.command.view.TableView.Align.RIGHT;

/**
 * JVM info command
 * Created by vlinux on 15/6/6.
 */
@Cmd(name = "jvm", sort = 10, summary = "Show the target JVM info",
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
            public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                final TableView view = new TableView(new TableView.ColumnDefine[]{
                        new TableView.ColumnDefine(RIGHT),
                        new TableView.ColumnDefine(LEFT)
                })
                        .addRow("CATEGORY", "INFO")
                        .padding(1)
                        .hasBorder(true);

                view.addRow("RUNTIME", drawRuntimeTable());
                view.addRow("CLASS-LOADING", drawClassLoadingTable());
                view.addRow("COMPILATION", drawCompilationTable());

                if (!garbageCollectorMXBeans.isEmpty()) {
                    view.addRow("GARBAGE-COLLECTORS", drawGarbageCollectorsTable());
                }

                if (!memoryManagerMXBeans.isEmpty()) {
                    view.addRow("MEMORY-MANAGERS", drawMemoryManagersTable());
                }

                view.addRow("MEMORY", drawMemoryTable());
                view.addRow("OPERATING-SYSTEM", drawOperatingSystemMXBeanTable());
                view.addRow("THREAD", drawThreadTable());

                sender.send(true, view.draw());

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

    private KVView createKVView() {
        return new KVView(
                new TableView.ColumnDefine(25, false, RIGHT),
                new TableView.ColumnDefine(70, false, LEFT)
        );
    }

    private String drawRuntimeTable() {
        final KVView view = createKVView()
                .add("MACHINE-NAME", runtimeMXBean.getName())
                .add("JVM-START-TIME", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(runtimeMXBean.getStartTime())))
                .add("MANAGEMENT-SPEC-VERSION", runtimeMXBean.getManagementSpecVersion())
                .add("SPEC-NAME", runtimeMXBean.getSpecName())
                .add("SPEC-VENDOR", runtimeMXBean.getSpecVendor())
                .add("SPEC-VERSION", runtimeMXBean.getSpecVersion())
                .add("VM-NAME", runtimeMXBean.getVmName())
                .add("VM-VENDOR", runtimeMXBean.getVmVendor())
                .add("VM-VERSION", runtimeMXBean.getVmVersion())
                .add("INPUT-ARGUMENTS", toCol(runtimeMXBean.getInputArguments()))
                .add("CLASS-PATH", runtimeMXBean.getClassPath())
                .add("BOOT-CLASS-PATH", runtimeMXBean.getBootClassPath())
                .add("LIBRARY-PATH", runtimeMXBean.getLibraryPath());

        return view.draw();
    }

    private String drawClassLoadingTable() {
        final KVView view = createKVView()
                .add("LOADED-CLASS-COUNT", classLoadingMXBean.getLoadedClassCount())
                .add("TOTAL-LOADED-CLASS-COUNT", classLoadingMXBean.getTotalLoadedClassCount())
                .add("UNLOADED-CLASS-COUNT", classLoadingMXBean.getUnloadedClassCount())
                .add("IS-VERBOSE", classLoadingMXBean.isVerbose());
        return view.draw();
    }

    private String drawCompilationTable() {
        final KVView view = createKVView()
                .add("NAME", compilationMXBean.getName());

        if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
            view.add("TOTAL-COMPILE-TIME", compilationMXBean.getTotalCompilationTime() + "(ms)");
        }
        return view.draw();
    }

    private String drawGarbageCollectorsTable() {
        final KVView view = createKVView();

        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            view.add(garbageCollectorMXBean.getName() + "\n[count/time]",
                    garbageCollectorMXBean.getCollectionCount() + "/" + garbageCollectorMXBean.getCollectionTime() + "(ms)");
        }

        return view.draw();
    }

    private String drawMemoryManagersTable() {
        final KVView view = createKVView();

        for (final MemoryManagerMXBean memoryManagerMXBean : memoryManagerMXBeans) {
            if (memoryManagerMXBean.isValid()) {
                final String name = memoryManagerMXBean.isValid()
                        ? memoryManagerMXBean.getName()
                        : memoryManagerMXBean.getName() + "(Invalid)";


                view.add(name, toCol(memoryManagerMXBean.getMemoryPoolNames()));
            }
        }

        return view.draw();
    }

    private String drawMemoryTable() {
        final KVView view = createKVView();

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
        return view.draw();
    }


    private String drawOperatingSystemMXBeanTable() {
        final KVView view = createKVView();
        view
                .add("OS", operatingSystemMXBean.getName())
                .add("ARCH", operatingSystemMXBean.getArch())
                .add("PROCESSORS-COUNT", operatingSystemMXBean.getAvailableProcessors())
                .add("LOAD-AVERAGE", operatingSystemMXBean.getSystemLoadAverage())
                .add("VERSION", operatingSystemMXBean.getVersion());
        return view.draw();
    }

    private String drawThreadTable() {
        final KVView view = createKVView();

        view
                .add("COUNT", threadMXBean.getThreadCount())
                .add("DAEMON-COUNT", threadMXBean.getDaemonThreadCount())
                .add("LIVE-COUNT", threadMXBean.getPeakThreadCount())
                .add("STARTED-COUNT", threadMXBean.getTotalStartedThreadCount())
        ;
        return view.draw();
    }

}

package com.github.ompc.greys.core.command;


import com.github.ompc.greys.core.advisor.AdviceListener;
import com.github.ompc.greys.core.advisor.AdviceListenerAdapter;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.util.InvokeCost;
import com.github.ompc.greys.core.util.PointCut;
import com.github.ompc.greys.core.util.SimpleDateFormatHolder;
import com.github.ompc.greys.core.util.matcher.ClassMatcher;
import com.github.ompc.greys.core.util.matcher.GaMethodMatcher;
import com.github.ompc.greys.core.util.matcher.PatternMatcher;

import java.lang.instrument.Instrumentation;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.ompc.greys.core.util.GaCheckUtils.isEquals;
import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;

/**
 * 监控请求命令<br/>
 * 输出的内容格式为:<br/>
 * <style type="text/css">
 * table, th, td {
 * borders:1px solid #cccccc;
 * borders-collapse:collapse;
 * }
 * </style>
 * <table>
 * <tr>
 * <th>时间戳</th>
 * <th>统计周期(s)</th>
 * <th>类全路径</th>
 * <th>方法名</th>
 * <th>调用总次数</th>
 * <th>成功次数</th>
 * <th>失败次数</th>
 * <th>平均耗时(ms)</th>
 * <th>失败率</th>
 * </tr>
 * <tr>
 * <td>2012-11-07 05:00:01</td>
 * <td>120</td>
 * <td>com.taobao.item.ItemQueryServiceImpl</td>
 * <td>queryItemForDetail</td>
 * <td>1500</td>
 * <td>1000</td>
 * <td>500</td>
 * <td>15</td>
 * <td>30%</td>
 * </tr>
 * <tr>
 * <td>2012-11-07 05:00:01</td>
 * <td>120</td>
 * <td>com.taobao.item.ItemQueryServiceImpl</td>
 * <td>queryItemById</td>
 * <td>900</td>
 * <td>900</td>
 * <td>0</td>
 * <td>7</td>
 * <td>0%</td>
 * </tr>
 * </table>
 *
 * @author oldmanpushcart@gmail.com
 */
@Cmd(name = "monitor", sort = 2, summary = "Monitor the execution of specified Class and its method",
        eg = {
                "monitor -c 5 -E org\\.apache\\.commons\\.lang\\.StringUtils *",
                "monitor -c 5 org.apache.commons.lang.StringUtils is*",
                "monitor *StringUtils isBlank"
        })
public class MonitorCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "c", hasValue = true, summary = "The cycle of monitor")
    private int cycle = 120;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    /**
     * 数据监控用的Key
     *
     * @author oldmanpushcart@gmail.com
     */
    private static class Key {
        private final String className;
        private final String methodName;

        private Key(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public int hashCode() {
            return className.hashCode() + methodName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (null == obj
                    || !(obj instanceof Key)) {
                return false;
            }
            Key oKey = (Key) obj;
            return isEquals(oKey.className, className)
                    && isEquals(oKey.methodName, methodName);
        }

    }

    /**
     * 数据监控用的value
     *
     * @author oldmanpushcart@gmail.com
     */
    private static class Data {
        private int total;
        private int success;
        private int failed;
        private long cost;
        private Long maxCost;
        private Long minCost;
    }

    @Override
    public Action getAction() {

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(final Session session, Instrumentation inst, final Printer printer) throws Throwable {
                return new GetEnhancer() {

                    @Override
                    public PointCut getPointCut() {
                        return new PointCut(
                                new ClassMatcher(new PatternMatcher(isRegEx, classPattern)),
                                new GaMethodMatcher(new PatternMatcher(isRegEx, methodPattern))
                        );
                    }

                    @Override
                    public AdviceListener getAdviceListener() {

                        return new AdviceListenerAdapter() {

                            /*
                             * 输出定时任务
                             */
                            private Timer timer;

                            /*
                             * 监控数据
                             */
                            private final ConcurrentHashMap<Key, AtomicReference<Data>> monitorData
                                    = new ConcurrentHashMap<Key, AtomicReference<Data>>();

                            private double div(double a, double b) {
                                if (b == 0) {
                                    return 0;
                                }
                                return a / b;
                            }

                            @Override
                            public void create() {
                                timer = new Timer("Timer-for-greys-monitor-" + session.getSessionId(), true);
                                timer.scheduleAtFixedRate(new TimerTask() {

                                    @Override
                                    public void run() {
//                                        if (monitorData.isTop()) {
//                                            return;
//                                        }

                                        final TTable tTable = new TTable(10)
                                                .addRow(
                                                        "TIMESTAMP",
                                                        "CLASS",
                                                        "METHOD",
                                                        "TOTAL",
                                                        "SUCCESS",
                                                        "FAIL",
                                                        "FAIL-RATE",
                                                        "AVG-RT(ms)",
                                                        "MIN-RT(ms)",
                                                        "MAX-RT(ms)"
                                                );

                                        for (Map.Entry<Key, AtomicReference<Data>> entry : monitorData.entrySet()) {
                                            final AtomicReference<Data> value = entry.getValue();

                                            Data data;
                                            while (true) {
                                                data = value.get();
                                                if (value.compareAndSet(data, new Data())) {
                                                    break;
                                                }
                                            }

                                            if (null != data) {

                                                final DecimalFormat df = new DecimalFormat("0.00");

                                                tTable.addRow(
                                                        SimpleDateFormatHolder.getInstance().format(new Date()),
                                                        entry.getKey().className,
                                                        entry.getKey().methodName,
                                                        data.total,
                                                        data.success,
                                                        data.failed,
                                                        df.format(100.0d * div(data.failed, data.total)) + "%",
                                                        df.format(div(data.cost, data.total)),
                                                        data.minCost,
                                                        data.maxCost
                                                );

                                            }
                                        }

                                        tTable.padding(1);

                                        printer.println(tTable.rendering());
                                    }

                                }, 0, cycle * 1000);
                            }

                            @Override
                            public void destroy() {
                                if (null != timer) {
                                    timer.cancel();
                                }
                            }

                            private final InvokeCost invokeCost = new InvokeCost();
                            private final ThreadLocal<Long> beforeInvokeTimestampRef = new ThreadLocal<Long>();

                            @Override
                            public void before(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
                                invokeCost.begin();
                            }

                            @Override
                            public void afterReturning(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args, Object returnObject) throws Throwable {
                                finishing(tranClassName(className), methodName, true);
                            }

                            @Override
                            public void afterThrowing(ClassLoader loader, String className, String methodName, String methodDesc, Object target, Object[] args, Throwable throwable) throws Throwable {
                                finishing(tranClassName(className), methodName, false);
                            }

                            public void finishing(String className, String methodName, boolean isSuccess) throws Throwable {
                                final Key key = new Key(className, methodName);
                                final long cost = invokeCost.cost();

                                while (true) {
                                    final AtomicReference<Data> value = monitorData.get(key);
                                    if (null == value) {
                                        monitorData.putIfAbsent(key, new AtomicReference<Data>(new Data()));
                                        // 这里不去判断返回值，用continue去强制获取一次
                                        continue;
                                    }

                                    while (true) {
                                        Data oData = value.get();
                                        Data nData = new Data();
                                        nData.cost = oData.cost + cost;
                                        if (!isSuccess) {
                                            nData.failed = oData.failed + 1;
                                            nData.success = oData.success;
                                        } else {
                                            nData.failed = oData.failed;
                                            nData.success = oData.success + 1;
                                        }
                                        nData.total = oData.total + 1;

                                        // setValue max-cost
                                        if (null == oData.maxCost) {
                                            nData.maxCost = cost;
                                        } else {
                                            nData.maxCost = Math.max(oData.maxCost, cost);
                                        }

                                        // setValue min-cost
                                        if (null == oData.minCost) {
                                            nData.minCost = cost;
                                        } else {
                                            nData.minCost = Math.min(oData.minCost, cost);
                                        }


                                        if (value.compareAndSet(oData, nData)) {
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }

                        };
                    }
                };
            }

        };
    }


}

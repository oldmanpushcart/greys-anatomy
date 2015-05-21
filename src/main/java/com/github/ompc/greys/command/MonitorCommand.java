package com.github.ompc.greys.command;


import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.ompc.greys.util.CheckUtil.isEquals;
import static java.lang.System.currentTimeMillis;

/**
 * 监控请求命令<br/>
 * 输出的内容格式为:<br/>
 * <style type="text/css">
 * table, th, td {
 * border:1px solid #cccccc;
 * border-collapse:collapse;
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
 * @author vlinux
 */
@Cmd(named = "monitor", sort = 2, desc = "Buried point method for monitoring the operation.")
public class MonitorCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", summary = "pattern matching of method name")
    private String methodPattern;

    @NamedArg(named = "c", hasValue = true, summary = "the cycle of output")
    private int cycle = 120;

    @NamedArg(named = "S", summary = "including sub class")
    private boolean isIncludeSub = false;

    @NamedArg(named = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    /**
     * 数据监控用的Key
     *
     * @author vlinux
     */
    private static class Key {
        private final String className;
        private final String methodName;

        private Key(String className, String behaviorName) {
            this.className = className;
            this.methodName = behaviorName;
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
            Key okey = (Key) obj;
            return isEquals(okey.className, className)
                    && isEquals(okey.methodName, methodName);
        }

    }

    /**
     * 数据监控用的value
     *
     * @author vlinux
     */
    private static class Data {
        private int total;
        private int success;
        private int failed;
        private long cost;
    }

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = isRegEx
                ? new RegexMatcher(classPattern)
                : new WildcardMatcher(classPattern);

        final Matcher methodNameMatcher = isRegEx
                ? new RegexMatcher(methodPattern)
                : new WildcardMatcher(methodPattern);

        return new GetEnhancerAction() {

            @Override
            public GetEnhancer action(final Session session, Instrumentation inst, final Sender sender) throws Throwable {
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

                        return new AdviceListener() {

                            /*
                             * 输出定时任务
                             */
                            private Timer timer;

                            /*
                             * 监控数据
                             */
                            private ConcurrentHashMap<Key, AtomicReference<Data>> monitorData
                                    = new ConcurrentHashMap<Key, AtomicReference<Data>>();

                            private final ThreadLocal<Long> beginTimestamp = new ThreadLocal<Long>();

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
                                        if (monitorData.isEmpty()) {
                                            return;
                                        }

                                        final TableView tableView = new TableView(8)
                                                .addRow(
                                                        "timestamp",
                                                        "class",
                                                        "method",
                                                        "total",
                                                        "success",
                                                        "fail",
                                                        "rt",
                                                        "fail-rate"
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

                                                tableView.addRow(
                                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                                                        entry.getKey().className,
                                                        entry.getKey().methodName,
                                                        data.total,
                                                        data.success,
                                                        data.failed,
                                                        df.format(div(data.cost, data.total)),
                                                        df.format(100.0d * div(data.failed, data.total))
                                                );

                                            }
                                        }

                                        tableView.padding(1);
                                        tableView.border(true);

                                        sender.send(false, tableView.draw() + "\n");
                                    }

                                }, 0, cycle * 1000);
                            }

                            @Override
                            public void destroy() {
                                if (null != timer) {
                                    timer.cancel();
                                }
                            }

                            @Override
                            public void before(
                                    String className,
                                    String methodName,
                                    String methodDesc,
                                    Object target,
                                    Object[] args) throws Throwable {
                                beginTimestamp.set(currentTimeMillis());
                            }

                            @Override
                            public void afterReturning(
                                    String className,
                                    String methodName,
                                    String methodDesc,
                                    Object target,
                                    Object[] args,
                                    Object returnObject) throws Throwable {
                                finishing(className, methodName, false);
                            }

                            @Override
                            public void afterThrowing(
                                    String className,
                                    String methodName,
                                    String methodDesc,
                                    Object target,
                                    Object[] args,
                                    Throwable throwable) {
                                finishing(className, methodName, true);
                            }

                            private void finishing(String className, String methodName, boolean isThrowing) {
                                final Long startTime = beginTimestamp.get();
                                if (null == startTime) {
                                    return;
                                }
                                final long cost = currentTimeMillis() - startTime;
                                final Key key = new Key(className, methodName);

                                while (true) {
                                    AtomicReference<Data> value = monitorData.get(key);
                                    if (null == value) {
                                        monitorData.putIfAbsent(key, new AtomicReference<Data>(new Data()));
                                        continue;
                                    }

                                    while (true) {
                                        Data oData = value.get();
                                        Data nData = new Data();
                                        nData.cost = oData.cost + cost;
                                        if (isThrowing) {
                                            nData.failed = oData.failed + 1;
                                        } else {
                                            nData.success = oData.success + 1;
                                        }
                                        nData.total = oData.total + 1;
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

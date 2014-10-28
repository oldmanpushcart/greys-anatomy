package com.googlecode.greysanatomy.util;

import com.googlecode.greysanatomy.clocker.Clocker;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 性能统计工具
 *
 * @author vlinux
 */
public class ProfilerUtils {

    private static final ThreadLocal<Entry> entryStack = new ThreadLocal<Entry>();

    /**
     * 开始性能统计
     *
     * @param message
     */
    public static void start(String message) {
        entryStack.set(new Entry(message, null, null));
    }

    /**
     * 进入一个单元
     */
    public static void enter() {
        final StackTraceElement stack = Thread.currentThread().getStackTrace()[4];
        enter(stack.getClassName() + "$" + stack.getMethodName());
    }

    /**
     * 进入一个单元
     *
     * @param message
     */
    public static void enter(String message) {
        final Entry currentEntry = getCurrentEntry();
        if (currentEntry != null) {
            currentEntry.enterSubEntry(message);
        }
    }

    /**
     * 释放一个单元
     */
    public static void release() {
        final Entry currentEntry = getCurrentEntry();
        if (currentEntry != null) {
            currentEntry.release();
        }
    }

    /**
     * 列出所有的entry。
     *
     * @return 列出所有entry，并统计各自所占用的时间
     */
    public static String dump() {
        return dump("", "");
    }

    /**
     * 列出所有的entry。
     *
     * @param prefix1 首行前缀
     * @param prefix2 后续行前缀
     * @return 列出所有entry，并统计各自所占用的时间
     */
    private static String dump(String prefix1, String prefix2) {
        final Entry entry = entryStack.get();
        return null != entry
                ? entry.toString(prefix1, prefix2)
                : "";
    }

    /**
     * 取得第一个entry。
     *
     * @return 第一个entry，如果不存在，则返回<code>null</code>
     */
    public static Entry getEntry() {
        return (Entry) entryStack.get();
    }

    /**
     * 取得最近的一个entry。
     *
     * @return 最近的一个entry，如果不存在，则返回<code>null</code>
     */
    private static Entry getCurrentEntry() {
        Entry subEntry = entryStack.get();
        Entry entry = null;
        if (subEntry != null) {
            do {
                entry = subEntry;
                subEntry = entry.getUnreleasedEntry();
            } while (subEntry != null);
        }

        return entry;
    }

    /**
     * 代表一个计时单元。
     */
    public static final class Entry {

        private final List<Entry> subEntries = new ArrayList<Entry>();
        private final String message;
        private final Entry parentEntry;
        private final Entry firstEntry;
        private final long baseTime;
        private final long startTime;
        private long endTime;

        /**
         * 创建一个新的entry。
         *
         * @param message     entry的信息，可以是<code>null</code>
         * @param parentEntry 父entry，可以是<code>null</code>
         * @param firstEntry  第一个entry，可以是<code>null</code>
         */
        private Entry(String message, Entry parentEntry, Entry firstEntry) {
            this.message = message;
            this.startTime = Clocker.current().getCurrentTimeMillis();
            this.parentEntry = parentEntry;
            this.firstEntry = (Entry) defaultIfNull(firstEntry, this);
            this.baseTime = (firstEntry == null) ? 0 : firstEntry.startTime;
        }

        /**
         * 取得entry的信息。
         */
        public String getMessage() {
            return message;
        }

        /**
         * 取得entry相对于第一个entry的起始时间。
         *
         * @return 相对起始时间
         */
        public long getStartTime() {
            return (baseTime > 0) ? (startTime - baseTime) : 0;
        }

        /**
         * 取得entry相对于第一个entry的结束时间。
         *
         * @return 相对结束时间，如果entry还未结束，则返回<code>-1</code>
         */
        public long getEndTime() {
            return endTime < baseTime ? -1 : endTime - baseTime;
        }

        /**
         * 取得entry持续的时间。
         *
         * @return entry持续的时间，如果entry还未结束，则返回<code>-1</code>
         */
        public long getDuration() {
            return endTime < startTime ? -1 : endTime - startTime;
        }

        /**
         * 取得entry自身所用的时间，即总时间减去所有子entry所用的时间。
         *
         * @return entry自身所用的时间，如果entry还未结束，则返回<code>-1</code>
         */
        public long getDurationOfSelf() {
            long duration = getDuration();

            if (duration < 0) {
                return -1;
            } else if (subEntries.isEmpty()) {
                return duration;
            } else {
                for (int i = 0; i < subEntries.size(); i++) {
                    Entry subEntry = (Entry) subEntries.get(i);
                    duration -= subEntry.getDuration();
                }
                return duration < 0 ? -1 : duration;
            }
        }

        /**
         * 取得当前entry在父entry中所占的时间百分比。
         *
         * @return 百分比
         */
        public double getPecentage() {
            double parentDuration = 0;
            double duration = getDuration();

            if ((parentEntry != null) && parentEntry.isReleased()) {
                parentDuration = parentEntry.getDuration();
            }

            return (duration > 0) && (parentDuration > 0)
                    ? duration / parentDuration
                    : 0;
        }

        /**
         * 取得当前entry在第一个entry中所占的时间百分比。
         *
         * @return 百分比
         */
        public double getPecentageOfAll() {
            double firstDuration = 0;
            double duration = getDuration();

            if ((firstEntry != null) && firstEntry.isReleased()) {
                firstDuration = firstEntry.getDuration();
            }

            return (duration > 0) && (firstDuration > 0)
                    ? duration / firstDuration
                    : 0;
        }

        /**
         * 结束当前entry，并记录结束时间。
         */
        private void release() {
            endTime = Clocker.current().getCurrentTimeMillis();
        }

        /**
         * 判断当前entry是否结束。
         *
         * @return 如果entry已经结束，则返回<code>true</code>
         */
        private boolean isReleased() {
            return endTime > 0;
        }

        /**
         * 创建一个新的子entry。
         *
         * @param message 子entry的信息
         */
        private void enterSubEntry(String message) {
            Entry subEntry = new Entry(message, this, firstEntry);
            subEntries.add(subEntry);
        }

        /**
         * 取得未结束的子entry。
         *
         * @return 未结束的子entry，如果没有子entry，或所有entry均已结束，则返回<code>null</code>
         */
        private Entry getUnreleasedEntry() {
            Entry subEntry = null;

            if (!subEntries.isEmpty()) {
                subEntry = (Entry) subEntries.get(subEntries.size() - 1);
                if (subEntry.isReleased()) {
                    subEntry = null;
                }
            }

            return subEntry;
        }

        /**
         * 将entry转换成字符串的表示。
         *
         * @param prefix1 首行前缀
         * @param prefix2 后续行前缀
         * @return 字符串表示的entry
         */
        private String toString(String prefix1, String prefix2) {
            StringBuffer buffer = new StringBuffer();
            toString(buffer, prefix1, prefix2);
            return buffer.toString();
        }

        /**
         * 将entry转换成字符串的表示。
         *
         * @param buffer  字符串buffer
         * @param prefix1 首行前缀
         * @param prefix2 后续行前缀
         */
        private void toString(StringBuffer buffer, String prefix1, String prefix2) {
            buffer.append(prefix1);

            String message = getMessage();
            long startTime = getStartTime();
            long duration = getDuration();
            long durationOfSelf = getDurationOfSelf();
            double percent = getPecentage();
            double percentOfAll = getPecentageOfAll();

            /*
             * {0} - entry信息
             * {1} - 起始时间
             * {2} - 持续总时间
             * {3} - 自身消耗的时间
             * {4} - 在父entry中所占的时间比例
             * {5} - 在总时间中所旧的时间比例
             */
            Object[] params = new Object[]{
                    message, startTime, duration, durationOfSelf, percent, percentOfAll};

            StringBuffer pattern = new StringBuffer("{1,number} ");

            if (isReleased()) {
                pattern.append("[{2,number}ms");

                if ((durationOfSelf > 0) && (durationOfSelf != duration)) {
                    pattern.append(" ({3,number}ms)");
                }

                if (percent > 0) {
                    pattern.append(", {4,number,##%}");
                }

                if (percentOfAll > 0) {
                    pattern.append(", {5,number,##%}");
                }

                pattern.append("]");
            }

            if (message != null) {
                pattern.append(" - {0}");
            }

            buffer.append(MessageFormat.format(pattern.toString(), params));

            for (int i = 0; i < subEntries.size(); i++) {
                Entry subEntry = (Entry) subEntries.get(i);
                buffer.append('\n');
                if (i == (subEntries.size() - 1)) {
                    subEntry.toString(buffer, prefix2 + "`---", prefix2 + "    "); // 最后一项
                } else if (i == 0) {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   "); // 第一项
                } else {
                    subEntry.toString(buffer, prefix2 + "+---", prefix2 + "|   "); // 中间项
                }
            }
        }
    }

    /**
     * 如果对象为<code>null</code>，则返回指定默认对象，否则返回对象本身。
     * <pre>
     * ObjectUtil.defaultIfNull(null, null)      = null
     * ObjectUtil.defaultIfNull(null, "")        = ""
     * ObjectUtil.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtil.defaultIfNull("abc", *)        = "abc"
     * ObjectUtil.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param object       要测试的对象
     * @param defaultValue 默认值
     * @return 对象本身或默认对象
     */
    public static Object defaultIfNull(Object object, Object defaultValue) {
        return (object != null) ? object : defaultValue;
    }

}

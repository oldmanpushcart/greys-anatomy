package com.github.ompc.greys.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SimpleDateFormat Holder
 * Created by vlinux on 15/10/6.
 */
public class SimpleDateFormatHolder extends ThreadLocal<SimpleDateFormat> {

    @Override
    protected SimpleDateFormat initialValue() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    private static volatile SimpleDateFormatHolder instance = null;

    public static SimpleDateFormatHolder getInstance() {
        if (null == instance) {
            synchronized (SimpleDateFormatHolder.class) {
                if (instance == null) {
                    instance = new SimpleDateFormatHolder();
                }
            }
        }
        return instance;
    }

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 格式化后字符串
     */
    public String format(Date date) {
        return getInstance().get().format(date);
    }


    /**
     * 格式化日期
     *
     * @param gmt gmt
     * @return 格式化后字符串
     */
    public String format(long gmt) {
        return getInstance().get().format(new Date(gmt));
    }

}

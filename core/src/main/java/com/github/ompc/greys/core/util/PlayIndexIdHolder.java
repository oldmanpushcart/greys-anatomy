package com.github.ompc.greys.core.util;

/**
 * 回放时间片段ID线程上下文传递
 * Created by vlinux on 15/10/5.
 */
public class PlayIndexIdHolder extends ThreadLocal<Integer> {

    private static volatile PlayIndexIdHolder instance = null;

    public static PlayIndexIdHolder getInstance() {
        if (null == instance) {
            synchronized (PlayIndexIdHolder.class) {
                if (instance == null) {
                    instance = new PlayIndexIdHolder();
                }
            }
        }

        return instance;
    }

}

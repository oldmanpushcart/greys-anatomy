package com.github.ompc.greys.core.util;

/**
 * 回放时间片段ID线程上下文传递
 * Created by vlinux on 15/10/5.
 */
public class PlayIndexHolder extends ThreadLocal<Integer> {

    private static volatile PlayIndexHolder instance = null;

    public static PlayIndexHolder getInstance() {
        if (null == instance) {
            synchronized (PlayIndexHolder.class) {
                if (instance == null) {
                    instance = new PlayIndexHolder();
                }
            }
        }

        return instance;
    }

}

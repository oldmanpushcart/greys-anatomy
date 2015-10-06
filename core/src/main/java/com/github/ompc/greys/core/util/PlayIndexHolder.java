package com.github.ompc.greys.core.util;

/**
 * 回放时间片段ID线程上下文传递
 * Created by vlinux on 15/10/5.
 */
public class PlayIndexHolder extends ThreadLocal<Integer> {

    private static final PlayIndexHolder instance = new PlayIndexHolder();

    private PlayIndexHolder() {
        //
    }

    public static PlayIndexHolder getInstance() {
        return instance;
    }

}

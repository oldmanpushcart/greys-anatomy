package com.github.ompc.greys.core.util;

/**
 * 回放过程ID线程上下文传递
 * Created by vlinux on 15/10/5.
 */
public class PlayProcessIdHolder extends ThreadLocal<Integer> {

    private static volatile PlayProcessIdHolder instance = null;

    public static PlayProcessIdHolder getInstance() {
        if (null == instance) {
            synchronized (PlayProcessIdHolder.class) {
                if (instance == null) {
                    instance = new PlayProcessIdHolder();
                }
            }
        }

        return instance;
    }

}

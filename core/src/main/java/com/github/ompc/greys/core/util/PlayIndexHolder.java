package com.github.ompc.greys.core.util;

/**
 * 回放时间片段ID线程上下文传递
 * 因为需要排查Perm区泄漏问题,所以暂时先废弃掉相关代码
 * Created by oldmanpushcart@gmail.com on 15/10/5.
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

package com.github.ompc.greys.util;

import static java.lang.System.currentTimeMillis;

/**
 * 影响反馈
 * Created by vlinux on 15/5/21.
 */
public class Affect {

    private final long start = currentTimeMillis();

    /**
     * 影响耗时(ms)
     *
     * @return 获取耗时(ms)
     */
    public long cost() {
        return currentTimeMillis() - start;
    }

    @Override
    public String toString() {
        return String.format("Affect in %s ms.", cost());
    }

}

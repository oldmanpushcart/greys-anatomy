package com.github.ompc.greys.core.advisor;

import com.github.ompc.greys.core.util.collection.GaStack;
import com.github.ompc.greys.core.util.collection.ThreadUnsafeGaStack;

/**
 * 过程上下文
 * Created by vlinux on 15/10/5.
 */
public class ProcessContext<IC extends InnerContext> extends Context {


    GaStack<IC> innerContextGaStack = new ThreadUnsafeGaStack<IC>();

    /**
     * 是否顶层
     *
     * @return 是否顶层上下文
     */
    public boolean isTop() {
        return innerContextGaStack.isEmpty();
    }

}

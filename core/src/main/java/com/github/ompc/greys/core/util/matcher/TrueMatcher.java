package com.github.ompc.greys.core.util.matcher;

/**
 * 永远为真匹配器
 * Created by oldmanpushcart@gmail.com on 15/12/12.
 */
public final class TrueMatcher<T> implements Matcher<T> {

    @Override
    public boolean matching(T target) {
        return true;
    }

}

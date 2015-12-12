package com.github.ompc.greys.core.util.matcher;

import java.util.Map;

/**
 * 带缓存的匹配
 * Created by oldmanpushcart@gmail.com on 15/12/12.
 */
public class CachedMatcher<T> implements Matcher<T> {

    private final Matcher<T> matcher;
    private final Map<T, Boolean> cachedMap;

    public CachedMatcher(Matcher<T> matcher, Map<T, Boolean> cachedMap) {
        this.matcher = matcher;
        this.cachedMap = cachedMap;
    }

    @Override
    public boolean matching(T target) {

        final Boolean valueInCache = cachedMap.get(target);
        if (null == valueInCache) {
            final boolean value = matcher.matching(target);
            cachedMap.put(target, value);
            return value;
        } else {
            return valueInCache;
        }

    }

}

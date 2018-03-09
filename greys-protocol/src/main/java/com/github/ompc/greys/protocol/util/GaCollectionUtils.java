package com.github.ompc.greys.protocol.util;

import java.util.Collection;

public class GaCollectionUtils {

    public static <T> boolean contains(final Collection<T> collection, T target) {
        return null != collection
                && collection.contains(target);
    }

}

package com.github.ompc.greys.module.resource;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchCondition;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class AutoReleaseWatcher implements ModuleEventWatcher {

    private final ModuleEventWatcher proxy;
    private final Set<Integer> watchIds = new ConcurrentSkipListSet<Integer>();

    public AutoReleaseWatcher(ModuleEventWatcher proxy) {
        this.proxy = proxy;
    }

    private int append(int watchId) {
        watchIds.add(watchId);
        return watchId;
    }

    private int remove(int watchId) {
        watchIds.remove(watchId);
        return watchId;
    }

    public void delete() {
        final Iterator<Integer> watchIdIt = watchIds.iterator();
        while (watchIdIt.hasNext()) {
            proxy.delete(watchIdIt.next());
            watchIdIt.remove();
        }
    }

    @Override
    public int watch(Filter filter, EventListener listener, Progress progress, Event.Type... eventType) {
        return append(proxy.watch(filter, listener, progress, eventType));
    }

    @Override
    public int watch(Filter filter, EventListener listener, Event.Type... eventType) {
        return append(proxy.watch(filter, listener, eventType));
    }

    @Override
    public int watch(EventWatchCondition condition, EventListener listener, Progress progress, Event.Type... eventType) {
        return append(proxy.watch(condition, listener, progress, eventType));
    }

    @Override
    public void delete(int watcherId, Progress progress) {
        proxy.delete(remove(watcherId), progress);
    }

    @Override
    public void delete(int watcherId) {
        proxy.delete(remove(watcherId));
    }

    @Override
    public void watching(Filter filter, EventListener listener, Progress wProgress, WatchCallback watchCb, Progress dProgress, Event.Type... eventType) throws Throwable {
        proxy.watching(filter, listener, wProgress, watchCb, dProgress, eventType);
    }

    @Override
    public void watching(Filter filter, EventListener listener, WatchCallback watchCb, Event.Type... eventType) throws Throwable {
        proxy.watching(filter, listener, watchCb, eventType);
    }
}

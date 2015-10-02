package com.github.ompc.greys.core.manager.impl;

import com.github.ompc.greys.core.exception.ExpressException;
import com.github.ompc.greys.core.manager.TimeFragmentManager;
import com.github.ompc.greys.core.util.Advice;
import com.github.ompc.greys.core.util.Express;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认时间碎片实现
 * Created by vlinux on 15/10/3.
 */
public class DefaultTimeFragmentManager implements TimeFragmentManager {

    // 时间碎片序列生成器
    private final AtomicInteger TIME_FRAGMENT_SEQUENCER
            = new AtomicInteger(1000);

    private final AtomicInteger PROCESS_SEQUENCER
            = new AtomicInteger(1000);

    // 时间碎片存储
    private final Map<Integer, TimeFragment> timeFragmentStore
            = new LinkedHashMap<Integer, TimeFragment>();

    /*
     * 生成下一条序列
     */
    private int nextSequence() {
        return TIME_FRAGMENT_SEQUENCER.incrementAndGet();
    }

    @Override
    public int generateProcessId() {
        return PROCESS_SEQUENCER.incrementAndGet();
    }

    @Override
    public TimeFragment append(int processId, Advice advice, Date gmtCreate, long cost, String stack) {
        final int id = nextSequence();
        final TimeFragment timeFragment = new TimeFragment(
                id,
                processId,
                advice,
                gmtCreate,
                cost,
                stack
        );
        timeFragmentStore.put(id, timeFragment);
        return timeFragment;
    }

    @Override
    public ArrayList<TimeFragment> list() {
        return new ArrayList<TimeFragment>(timeFragmentStore.values());
    }

    /*
     * 搜索匹配
     */
    private boolean is(final TimeFragment timeFragment, final String express) {
        try {
            return Express.ExpressFactory
                    .newExpress(timeFragment.advice)
                    .bind("processId", timeFragment.processId)
                    .bind("index", timeFragment.id)
                    .is(express);
        } catch (ExpressException e) {
            return false;
        }
    }


    /**
     * 搜索对象
     */
    class SearchDO {


    }

    @Override
    public ArrayList<TimeFragment> search(final String express) {
        final ArrayList<TimeFragment> timeFragments = new ArrayList<TimeFragment>();
        for (TimeFragment timeFragment : timeFragmentStore.values()) {
            if (is(timeFragment, express)) {
                timeFragments.add(timeFragment);
            }
        }
        return timeFragments;
    }

    @Override
    public TimeFragment get(int id) {
        return timeFragmentStore.get(id);
    }

    @Override
    public TimeFragment delete(int id) {
        return timeFragmentStore.remove(id);
    }

    @Override
    public int clean() {
        final int size = timeFragmentStore.size();
        timeFragmentStore.clear();
        return size;
    }

}

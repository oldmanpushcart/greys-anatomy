package com.github.ompc.greys.core.command;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.GaEventListener;
import com.github.ompc.greys.core.GaFilter;
import com.github.ompc.greys.core.annotation.HttpParam;
import com.github.ompc.greys.core.listener.AdviceListener;
import com.github.ompc.greys.core.manager.GaReflectSearchManager;
import com.github.ompc.greys.core.message.CommandMessage;
import com.github.ompc.greys.core.util.Express;
import com.github.ompc.greys.core.util.GaEnumUtils;
import com.github.ompc.greys.core.util.InvokeCost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.github.ompc.greys.core.util.Express.ExpressFactory.newExpress;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * watch命令
 * Created by vlinux on 2017/3/1.
 */
public class WatchCommand extends Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 触发环节
     */
    enum Trigger {
        BEFORE,
        RETURN,
        THROWS,
        FINISH
    }

    /**
     * watch命令消息
     */
    class WatchMessage extends CommandMessage<Object> {

        /**
         * 构造watch命令
         *
         * @param data watch结果
         */
        public WatchMessage(Object data) {
            super("WATCH", data);
        }
    }

    @Resource
    private GaReflectSearchManager gaReflectSearchManager;

    @HttpParam("class")
    private String patternJavaClassName;

    @HttpParam("method")
    private String patternJavaMethodName;

    @HttpParam("subclass")
    private boolean isIncludeSubClass = false;

    @HttpParam("trigger")
    private String[] triggerStringArray;

    @HttpParam("watch")
    private String watchExpress;

    @HttpParam("condition")
    private String conditionExpress;


    private Event.Type[] getEventTypeArray(final Set<Trigger> triggers) {
        final Set<Event.Type> eventTypeSet = new LinkedHashSet<Event.Type>();
        for (final Trigger trigger : triggers) {
            switch (trigger) {
                case BEFORE:
                    eventTypeSet.add(Event.Type.BEFORE);
                    break;
                case RETURN:
                    eventTypeSet.add(Event.Type.RETURN);
                    break;
                case THROWS:
                    eventTypeSet.add(Event.Type.THROWS);
                    break;
                case FINISH: {
                    eventTypeSet.add(Event.Type.RETURN);
                    eventTypeSet.add(Event.Type.THROWS);
                    break;
                }
            }
        }
        return eventTypeSet.toArray(new Event.Type[]{});
    }

    private Filter getFilter() {
        return new GaFilter(
                gaReflectSearchManager.listVisualGaMethods(
                        patternJavaClassName,
                        patternJavaMethodName,
                        isIncludeSubClass
                )
        );
    }

    private EventListener getEventListener(final Set<Trigger> triggers) {
        return new GaEventListener(
                new AdviceListener() {

                    private final InvokeCost invokeCost = new InvokeCost();

                    @Override
                    public void before(Advice advice) throws Throwable {
                        invokeCost.begin();
                        if (triggers.contains(Trigger.BEFORE)) {
                            watching(advice);
                        }
                    }

                    @Override
                    public void afterReturning(Advice advice) throws Throwable {
                        if (triggers.contains(Trigger.RETURN)
                                && !triggers.contains(Trigger.FINISH)) {
                            watching(advice);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) throws Throwable {
                        if (triggers.contains(Trigger.THROWS)
                                && !triggers.contains(Trigger.FINISH)) {
                            watching(advice);
                        }
                    }

                    @Override
                    public void afterFinishing(Advice advice) throws Throwable {
                        if (triggers.contains(Trigger.FINISH)) {
                            watching(advice);
                        }
                    }

                    private boolean isInCondition(Advice advice) {
                        try {
                            return isBlank(conditionExpress)
                                    || newExpress(advice).bind("cost", invokeCost.cost()).is(conditionExpress);
                        } catch (Express.ExpressException e) {
                            logger.warn("condition-express={}; occur error.", conditionExpress, e);
                            return false;
                        }
                    }

                    private void watching(Advice advice) {
                        try {
                            if (isInCondition(advice)) {
                                write(new WatchMessage(newExpress(advice).get(watchExpress)));
                            }
                        } catch (Exception e) {
                            logger.warn("watch-express={}; occur error.", watchExpress, e);
                        }
                    }

                }
        );
    }

    @Override
    protected void execute() throws Throwable {

        final Set<Trigger> triggers = GaEnumUtils.valueOf(
                Trigger.class,
                triggerStringArray,
                new Trigger[]{Trigger.BEFORE}
        );

        // 开始监听
        watch(
                getFilter(),
                getEventListener(triggers),
                getEventTypeArray(triggers)
        );

    }

}

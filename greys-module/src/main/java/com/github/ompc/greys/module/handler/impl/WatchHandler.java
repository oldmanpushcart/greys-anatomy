package com.github.ompc.greys.module.handler.impl;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.module.GaAdvice;
import com.github.ompc.greys.module.GpProgress;
import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.handler.HttpHandler.Path;
import com.github.ompc.greys.module.resource.GpWriter;
import com.github.ompc.greys.module.util.Express;
import com.github.ompc.greys.module.util.HttpParameterBinder.HttpParamBind;
import com.github.ompc.greys.module.util.ToFormatString;
import com.github.ompc.greys.module.util.ToFormatString.Format;
import com.github.ompc.greys.module.util.TryAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;

import static com.github.ompc.greys.module.handler.impl.WatchHandler.Trigger.*;
import static com.github.ompc.greys.protocol.util.GaCollectionUtils.contains;
import static java.lang.String.format;

@Path("/watch")
public class WatchHandler implements HttpHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private GpWriter gpWriter;

    @Resource
    private ModuleEventWatcher meWatcher;

    @HttpParamBind(name = "class", isRequired = true)
    private String cnPattern;

    @HttpParamBind(name = "method", isRequired = true)
    private String mnPattern;

    @HttpParamBind(name = "at", isRequired = true)
    private Set<Trigger> triggers;

    @HttpParamBind(name = "watch", isRequired = true)
    private String watchExpress;

    @HttpParamBind(name = "when")
    private String whenExpress;

    @HttpParamBind(name = "subclass")
    private boolean isIncludeSubClasses;

    @HttpParamBind(name = "bootstrap")
    private boolean isIncludeBootstrap;

    @HttpParamBind(name = "limit")
    private Integer limit;

    @HttpParamBind(name = "format")
    private Format format;

    @HttpParamBind(name = "expand")
    private int expand = 1;


    /**
     * 观察触点
     */
    enum Trigger {
        BEFORE,
        RETURN,
        THROWS
    }


    @Override
    public void onHandle() {

        new EventWatchBuilder(meWatcher)
                .onClass(cnPattern)
                .isIncludeBootstrap(isIncludeBootstrap)
                .isIncludeSubClasses(isIncludeSubClasses)
                .onBehavior(mnPattern)
                .onWatching()
                .withProgress(new GpProgress("watching", gpWriter))
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(final Advice advice) throws Throwable {
                        advice.attach(System.currentTimeMillis());
                        if (contains(triggers, BEFORE)) {
                            watch(advice);
                        }
                    }

                    @Override
                    public void afterReturning(final Advice advice) throws Throwable {
                        if (contains(triggers, RETURN)) {
                            watch(advice);
                        }
                    }

                    @Override
                    public void afterThrowing(final Advice advice) throws Throwable {
                        if (contains(triggers, THROWS)) {
                            watch(advice);
                        }
                    }


                    private Express newExpressWithCost(final Advice advice) {
                        final long beginTimestamp = advice.attachment();
                        final long costMs = System.currentTimeMillis() - beginTimestamp;
                        return Express
                                .ExpressFactory
                                .newExpress(new GaAdvice(advice))
                                .bind("cost", costMs);
                    }


                    private final TryAccess tryAccess
                            = new TryAccess(whenExpress, limit)
                            .onLimited(new TryAccess.TryAccessCallback() {
                                @Override
                                public void callback() {
                                    gpWriter.close();
                                }
                            });

                    private void watch(final Advice advice) throws IOException {

                        final Express express = newExpressWithCost(advice);
                        try {

                            if (tryAccess.tryAccess(express)) {
                                final Object objectOfWatched = express.get(watchExpress);
                                gpWriter.write(
                                        ToFormatString
                                                .Factory
                                                .create(format, objectOfWatched, expand)
                                                .toFormatString()
                                );
                            }

                        } catch (Express.ExpressException e) {
                            logger.warn("express: {} was wrong!", e.getExpress(), e);
                            gpWriter.write(format("express: %s was wrong! error:%s.", e.getExpress(), e.getMessage()));
                        } catch (TryAccess.TryAccessException e) {
                            logger.warn("watch tryAccess callback occur en error.", e);
                        }

                    }

                });
    }

    @Override
    public void onDestroy() {

    }

}

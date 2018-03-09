package com.github.ompc.greys.module;

import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.module.resource.GpWriter;
import com.github.ompc.greys.protocol.GpConstants;
import com.github.ompc.greys.protocol.GpType;
import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.Progress;
import com.github.ompc.greys.protocol.impl.v1.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.String.format;

public class GpProgress implements ModuleEventWatcher.Progress {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String title;
    private final GpWriter gpWriter;

    private long beginTimestamp;
    private int total;

    public GpProgress(final String title, final GpWriter gpWriter) {
        this.title = title;
        this.gpWriter = gpWriter;
    }

    @Override
    public void begin(int total) {
        this.total = total;
        this.beginTimestamp = System.currentTimeMillis();
    }

    @Override
    public void progressOnSuccess(final Class clazz, final int index) {
        try {
            gpWriter.write(new GreysProtocol<Progress>(
                    GpConstants.GP_VERSION_1_0_0,
                    GpType.PROGRESS,
                    new Progress(
                            title,
                            index == 1,
                            index == total,
                            total,
                            index,
                            clazz.getName(),
                            false,
                            null
                    )
            ));
        } catch (IOException ioCause) {
            logger.warn("report progress occur en error.", ioCause);
        }
    }

    @Override
    public void progressOnFailed(final Class clazz, final int index, final Throwable cause) {
        try {
            gpWriter.write(new GreysProtocol<Progress>(
                    GpConstants.GP_VERSION_1_0_0,
                    GpType.PROGRESS,
                    new Progress(
                            title,
                            index == 1,
                            index == total,
                            total,
                            index,
                            clazz.getName(),
                            true,
                            cause.getMessage()
                    )
            ));
        } catch (IOException ioCause) {
            logger.warn("report progress occur en error.", ioCause);
        }
    }

    @Override
    public void finish(int cCnt, int mCnt) {
        try {
            gpWriter.write(new GreysProtocol<Text>(
                    GpConstants.GP_VERSION_1_0_0,
                    GpType.PROGRESS,
                    new Text(format("cCnt=%s;mCnt=%s;cost=%sms;",
                            cCnt,
                            mCnt,
                            System.currentTimeMillis() - beginTimestamp
                    ))
            ));
        } catch (IOException ioCause) {
            logger.warn("report progress occur en error.", ioCause);
        }
    }

}

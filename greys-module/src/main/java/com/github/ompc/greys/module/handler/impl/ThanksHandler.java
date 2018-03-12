package com.github.ompc.greys.module.handler.impl;

import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.handler.HttpHandler.Path;
import com.github.ompc.greys.module.resource.GpWriter;

import javax.annotation.Resource;

import static com.github.ompc.greys.module.util.GaStringUtils.getThanks;
import static com.github.ompc.greys.protocol.GpSerializer.deserialize;

@Path("/thanks")
public class ThanksHandler implements HttpHandler {

    @Resource
    private GpWriter gpWriter;

    @Override
    public void onHandle() throws Throwable {
        gpWriter.write(deserialize(getThanks()))
                .close();
    }

    @Override
    public void onDestroy() {

    }

}

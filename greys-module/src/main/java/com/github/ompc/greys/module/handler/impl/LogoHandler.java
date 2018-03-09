package com.github.ompc.greys.module.handler.impl;

import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.handler.HttpHandler.Path;
import com.github.ompc.greys.module.resource.GpWriter;
import com.github.ompc.greys.module.util.GaStringUtils;

import javax.annotation.Resource;

@Path("/logo")
public class LogoHandler implements HttpHandler {

    @Resource
    private GpWriter gpWriter;

    @Override
    public void onHandle() throws Throwable {
        gpWriter.write(GaStringUtils.getLogo())
                .close();
    }

    @Override
    public void onDestroy() {

    }
}

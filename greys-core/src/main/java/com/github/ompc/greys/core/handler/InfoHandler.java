package com.github.ompc.greys.core.handler;

import com.github.ompc.greys.common.protocol.InfoLogo;
import com.github.ompc.greys.common.protocol.InfoVersion;
import com.github.ompc.greys.core.JsonPrinter;
import com.github.ompc.greys.core.http.Path;
import com.github.ompc.greys.core.util.GaStringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Path("/info")
public class InfoHandler implements Handler {

    @Path("/version")
    public void version(final JsonPrinter printer) {
        final InfoVersion version = new InfoVersion();
        version.setVersion(GaStringUtils.getVersion());
        printer.println(version);
    }

    @Path("/logo")
    public void logo(final JsonPrinter printer) {
        final InfoLogo logo = new InfoLogo();
        logo.setLogo(GaStringUtils.getLogo());
        printer.println(logo);
    }

    @Path("/thanks")
    public void thanks(final HttpServletResponse resp) throws IOException {
        resp.getWriter().println(GaStringUtils.getThanks());
        resp.getWriter().flush();
    }

}

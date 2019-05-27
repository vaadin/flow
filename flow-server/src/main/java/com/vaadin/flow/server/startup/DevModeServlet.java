package com.vaadin.flow.server.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.VaadinServlet;

/**
 * Serves js bundle files and polyfills during development mode.
 */
public class DevModeServlet extends VaadinServlet {

    private DevModeHandler devmodeHandler;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        devmodeHandler = DevModeHandler.getDevModeHandler();

        super.init(servletConfig);
    }

    @Override
    protected boolean serveStaticOrWebJarRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (devmodeHandler != null && devmodeHandler.isDevModeRequest(request)
                && devmodeHandler.serveDevModeRequest(request, response)) {
            return true;
        }

        return super.serveStaticOrWebJarRequest(request, response);
    }
}

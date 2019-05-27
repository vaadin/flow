package com.vaadin.flow.server.startup;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.vaadin.flow.server.DevModeHandler;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Serves js bundle files and polyfills during development mode.
 */
public class DevModeServlet extends HttpServlet {

    private DevModeHandler devmodeHandler;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        devmodeHandler = DevModeHandler.getDevModeHandler();

        super.init(servletConfig);
    }

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        if (!(devmodeHandler != null && devmodeHandler.isDevModeRequest(request)
                && devmodeHandler.serveDevModeRequest(request, response))) {

            response.sendError(SC_NOT_FOUND);
        }

    }

}

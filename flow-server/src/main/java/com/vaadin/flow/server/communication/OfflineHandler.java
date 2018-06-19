package com.vaadin.flow.server.communication;

import java.io.IOException;

import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ManifestRegistry;

public class OfflineHandler implements RequestHandler {
    @Override public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response)
            throws IOException {

        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        ManifestRegistry registry = ManifestRegistry.getInstance(
                httpRequest.getServletContext());
        PwaConfiguration config = registry.getPwaConfiguration();

        if (config.relOfflinePath().equals(httpRequest.getPathInfo())) {
            response.getWriter().write(registry.getOfflineHtml());
            response.getWriter().close();
            response.setContentType("text/html");
            return true;
        }

        return false;
    }
}

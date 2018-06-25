package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.PWARegistry;

public class IconHandler implements RequestHandler {
    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        VaadinServletRequest httpRequest =  (VaadinServletRequest) request;

        PWARegistry registry = request.getService().getPwaRegistry();

        for (Icon icon : registry.getIcons()) {
            if (icon.relHref().equals(httpRequest.getPathInfo())) {
                response.setContentType(icon.type());
                // Icon is cached with service worker, deny browser caching
                if (icon.cached()) {
                    response.setHeader("Cache-Control",
                            "no-cache, must-revalidate");
                }
                OutputStream out = response.getOutputStream();
                icon.write(out);
                out.close();
                return true;
            }
        }
        return false;
    }
}

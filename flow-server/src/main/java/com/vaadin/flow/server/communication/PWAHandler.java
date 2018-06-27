package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.PWARegistry;

public class PWAHandler implements RequestHandler {

    private PWARegistry pwaRegistry;
    private Map<String, RequestHandler> requestHandlerMap;

    public PWAHandler(PWARegistry pwaRegistry) {
        requestHandlerMap = new HashMap<>();
        this.pwaRegistry = pwaRegistry;

        if (this.pwaRegistry.getPwaConfiguration().isEnabled()) {

            // Icon handling
            for (Icon icon : this.pwaRegistry.getIcons()) {
                requestHandlerMap.put(icon.relHref(),
                        (session, request, response) -> {
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
                        });
            }
            // Offline page handling
            requestHandlerMap.put(
                    this.pwaRegistry.getPwaConfiguration().relOfflinePath(),
                    (session, request, response) -> {
                        response.setContentType("text/html");
                        response.getWriter().write(pwaRegistry.getOfflineHtml());
                        response.getWriter().close();
                        return true;
                    });

            // Manifest.json handling
            requestHandlerMap.put(
                    this.pwaRegistry.getPwaConfiguration().relManifestPath(),
                    (session, request, response) -> {
                        response.setContentType("application/json");
                        PrintWriter writer = response.getWriter();
                        writer.write(this.pwaRegistry.getManifestJson());
                        writer.close();
                        return true;
                    });

            // serviceworker.js handling
            requestHandlerMap.put(
                    this.pwaRegistry.getPwaConfiguration().relServiceWorkerPath(),
                    (session, request, response) -> {
                        response.setContentType("application/javascript");
                        PrintWriter writer = response.getWriter();
                        writer.write(this.pwaRegistry.getServiceWorkerJs());
                        writer.close();
                        return true;
                    });
        }

    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        VaadinServletRequest httpRequest = (VaadinServletRequest) request;
        String requestUri = httpRequest.getPathInfo();

        if (pwaRegistry.getPwaConfiguration().isEnabled() &&
                requestHandlerMap.containsKey(requestUri)) {
            return requestHandlerMap.get(requestUri)
                    .handleRequest(session,request,response);
        }

        return false;
    }



}

/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.server.PWAIcon;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.PWARegistry;

/**
 * Handles serving of PWA resources.
 *
 * - manifest
 * - service worker
 * - offline fallback page
 * - icons
 */
public class PWAHandler implements RequestHandler {

    private PWARegistry pwaRegistry;
    private Map<String, RequestHandler> requestHandlerMap;

    /**
     * Creates PwaHandler from {@link PWARegistry}.
     *
     * Sets up handling for icons, manifest, service worker and offline page.
     *
     * @param pwaRegistry registry for pwa
     */
    public PWAHandler(PWARegistry pwaRegistry) {
        requestHandlerMap = new HashMap<>();
        this.pwaRegistry = pwaRegistry;

        init();

    }

    private void init() {
        // Don't init handlers, if not enabled
        if (!this.pwaRegistry.getPwaConfiguration().isEnabled())
            return;

        // Icon handling
        for (PWAIcon icon : this.pwaRegistry.getIcons()) {
            requestHandlerMap.put(icon.getRelHref(),
                    (session, request, response) -> {
                        response.setContentType(icon.getType());
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

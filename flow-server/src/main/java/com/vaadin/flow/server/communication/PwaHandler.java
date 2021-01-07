/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.server.PwaIcon;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles serving of PWA resources.
 *
 * Resources include:
 * <ul>
 * <li>manifest
 * <li>offline fallback page
 * <li>icons
 * </ul>
 *
 * @since 1.2
 */
public class PwaHandler implements RequestHandler {
    private final Map<String, RequestHandler> requestHandlerMap = new HashMap<>();
    private final PwaRegistry pwaRegistry;

    /**
     * Creates PwaHandler from {@link PwaRegistry}.
     *
     * Sets up handling for icons, manifest and offline page.
     *
     * @param pwaRegistry
     *            registry for PWA
     */
    public PwaHandler(PwaRegistry pwaRegistry) {
        this.pwaRegistry = pwaRegistry;
        init();
    }

    private void init() {
        // Don't init handlers, if not enabled
        if (!pwaRegistry.getPwaConfiguration().isEnabled()) {
            return;
        }

        // Icon handling
        for (PwaIcon icon : pwaRegistry.getIcons()) {
            requestHandlerMap.put(icon.getRelHref(),
                    (session, request, response) -> {
                        response.setContentType(icon.getType());
                        // Icon is cached with service worker, deny browser
                        // caching
                        if (icon.shouldBeCached()) {
                            response.setHeader("Cache-Control",
                                    "no-cache, must-revalidate");
                        }
                        try (OutputStream out = response.getOutputStream()) {
                            icon.write(out);
                        }
                        return true;
                    });
        }

        if (pwaRegistry.getPwaConfiguration().isOfflinePathEnabled()) {
            // Offline page handling
            requestHandlerMap.put(
                    pwaRegistry.getPwaConfiguration().relOfflinePath(),
                    (session, request, response) -> {
                        response.setContentType("text/html");
                        try (PrintWriter writer = response.getWriter()) {
                            writer.write(pwaRegistry.getOfflineHtml());
                        }
                        return true;
                    });
        }

        // manifest.webmanifest handling
        requestHandlerMap.put(
                pwaRegistry.getPwaConfiguration().relManifestPath(),
                (session, request, response) -> {
                    response.setContentType("application/manifest+json");
                    try (PrintWriter writer = response.getWriter()) {
                        writer.write(pwaRegistry.getManifestJson());
                    }
                    return true;
                });

        // sw-runtime.js handling (service worker import for precaching runtime generated assets)
        requestHandlerMap.put(
                // pwaRegistry.getPwaConfiguration().relServiceWorkerPath(),
                "/sw-runtime-resources-precache.js",
                (session, request, response) -> {
                    response.setContentType("application/javascript");
                    try (PrintWriter writer = response.getWriter()) {
                        writer.write(pwaRegistry.getRuntimeServiceWorkerJs());
                    }
                    return true;
                });
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        String requestUri = request.getPathInfo();

        if (pwaRegistry.getPwaConfiguration().isEnabled()
                && requestHandlerMap.containsKey(requestUri)) {
            return requestHandlerMap.get(requestUri).handleRequest(session,
                    request, response);
        }
        return false;
    }

}

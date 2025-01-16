/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableSupplier;
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.2
 */
public class PwaHandler implements RequestHandler {
    public static final String SW_RUNTIME_PRECACHE_PATH = "/sw-runtime-resources-precache.js";
    public static final String DEFAULT_OFFLINE_STUB_PATH = "offline-stub.html";

    private final Map<String, RequestHandler> requestHandlerMap = Collections
            .synchronizedMap(new HashMap<>());
    private final SerializableSupplier<PwaRegistry> pwaRegistryGetter;

    private boolean isInitialized;

    /**
     * Creates PwaHandler from {@link PwaRegistry} getter.
     *
     * @param pwaRegistryGetter
     *            PWA registry getter
     *
     */
    public PwaHandler(SerializableSupplier<PwaRegistry> pwaRegistryGetter) {
        this.pwaRegistryGetter = pwaRegistryGetter;
    }

    private void init(PwaRegistry pwaRegistry) {
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
                        } catch (UncheckedIOException ex) {
                            LoggerFactory.getLogger(PwaHandler.class)
                                    .debug("Error serving PWA icon", ex);
                        }
                        return true;
                    });
        }

        // Assume that offline page and offline stub (for display within app)
        // are the same. This may change in the future.
        List<String> offlinePaths = new ArrayList<>();
        if (pwaRegistry.getPwaConfiguration().isOfflinePathEnabled()) {
            offlinePaths
                    .add(pwaRegistry.getPwaConfiguration().relOfflinePath());
        }
        offlinePaths.add("/" + DEFAULT_OFFLINE_STUB_PATH);
        for (String offlinePath : offlinePaths) {
            requestHandlerMap.put(offlinePath, (session, request, response) -> {
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
                    response.setContentType(
                            "application/manifest+json;charset=utf-8");
                    try (PrintWriter writer = response.getWriter()) {
                        writer.write(pwaRegistry.getManifestJson());
                    }
                    return true;
                });

        // sw-runtime.js handling (service worker import for precaching runtime
        // generated assets)
        requestHandlerMap.put(SW_RUNTIME_PRECACHE_PATH,
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
        PwaRegistry pwaRegistry = pwaRegistryGetter.get();
        boolean hasPwa = pwaRegistry != null
                && pwaRegistry.getPwaConfiguration().isEnabled();
        RequestHandler handler = null;
        synchronized (requestHandlerMap) {
            if (hasPwa) {
                if (!isInitialized) {
                    init(pwaRegistry);
                    isInitialized = true;
                }
                handler = requestHandlerMap.get(request.getPathInfo());
            } else if (isInitialized) {
                requestHandlerMap.clear();
            }
        }

        if (handler == null) {
            return false;
        } else {
            return handler.handleRequest(session, request, response);
        }
    }

}

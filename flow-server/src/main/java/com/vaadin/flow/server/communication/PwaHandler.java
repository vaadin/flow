/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.BootstrapHandler;
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
 * <li>service worker
 * <li>offline fallback page
 * <li>icons
 * </ul>
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.2
 */
public class PwaHandler implements RequestHandler {
    private final Map<String, RequestHandler> requestHandlerMap = new HashMap<>();
    private final PwaRegistry pwaRegistry;

    /**
     * Creates PwaHandler from {@link PwaRegistry}.
     *
     * Sets up handling for icons, manifest, service worker and offline page.
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

        // serviceworker.js handling
        requestHandlerMap.put(
                pwaRegistry.getPwaConfiguration().relServiceWorkerPath(),
                (session, request, response) -> {
                    response.setContentType("application/javascript");
                    try (PrintWriter writer = response.getWriter()) {
                        writer.write(pwaRegistry.getServiceWorkerJs());
                    }
                    return true;
                });
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        String requestUri = request.getPathInfo();

        if (pwaRegistry.getPwaConfiguration().isEnabled()) {
            if (requestHandlerMap.containsKey(requestUri)) {
                return requestHandlerMap.get(requestUri).handleRequest(session,
                        request, response);
            } else if (requestUri != null && requestUri
                    .startsWith("/" + PwaRegistry.WORKBOX_FOLDER)) {

                // allow only files under workbox_folder
                String resourceName = PwaRegistry.WORKBOX_FOLDER + requestUri
                        // remove the extra '/'
                        .substring(PwaRegistry.WORKBOX_FOLDER.length() + 1)
                        .replaceAll("/", "");
                return handleWorkboxResource(resourceName, response);
            }

        }
        return false;
    }

    private boolean handleWorkboxResource(String fileName,
            VaadinResponse response) {
        try (InputStream stream = BootstrapHandler.class
                .getResourceAsStream(fileName);
                InputStreamReader reader = new InputStreamReader(stream,
                        StandardCharsets.UTF_8);) {
            PrintWriter writer = response.getWriter();
            if (fileName.endsWith(".js")) {
                response.setContentType("application/javascript");
            } else {
                response.setContentType("text/plain");
            }

            final char[] buffer = new char[1024];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            return true;
        } catch (NullPointerException e) {
            getLogger().debug("Workbox file '{}' does not exist", fileName, e);
            return false;
        } catch (IOException e) {
            getLogger().warn("Error while reading workbox file '{}'", fileName,
                    e);
            return false;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(PwaHandler.class);
    }

}

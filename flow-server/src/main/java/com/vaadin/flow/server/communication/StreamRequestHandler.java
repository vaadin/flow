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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles {@link StreamResource} and {@link StreamReceiver} instances
 * registered in {@link VaadinSession}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StreamRequestHandler implements RequestHandler {

    private static final char PATH_SEPARATOR = '/';

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/resource/";

    private final StreamResourceHandler resourceHandler =
            new StreamResourceHandler();
    private final StreamReceiverHandler receiverHandler;

    /**
     * Create a new stream request handler with the default
     * StreamReceiverHandler.
     */
    public StreamRequestHandler() {
        this(new StreamReceiverHandler());
    }

    protected StreamRequestHandler(StreamReceiverHandler receiverHandler) {
        this.receiverHandler = receiverHandler;
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return false;
        }
        // remove leading '/'
        assert pathInfo.startsWith(Character.toString(PATH_SEPARATOR));
        pathInfo = pathInfo.substring(1);

        if (!pathInfo.startsWith(DYN_RES_PREFIX)) {
            return false;
        }

        Optional<AbstractStreamResource> abstractStreamResource;
        session.lock();
        try {
            abstractStreamResource = StreamRequestHandler.getPathUri(pathInfo)
                    .flatMap(session.getResourceRegistry()::getResource);
            if (!abstractStreamResource.isPresent()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Resource is not found for path=" + pathInfo);
                return true;
            }
        } finally {
            session.unlock();
        }

        if (abstractStreamResource.isPresent()) {
            AbstractStreamResource resource = abstractStreamResource.get();
            if (resource instanceof StreamResource) {
                resourceHandler.handleRequest(session, request, response,
                        (StreamResource) resource);
            } else if (resource instanceof StreamReceiver) {
                StreamReceiver streamReceiver = (StreamReceiver) resource;
                String[] parts = parsePath(pathInfo);

                receiverHandler.handleRequest(session, request, response,
                        streamReceiver, parts[0], parts[1]);
            } else {
                getLogger().warn("Received unknown stream resource.");
            }
        }
        return true;
    }

    /**
     * Parse the pathInfo for id data.
  s   * <p>
     * URI pattern: VAADIN/dynamic/resource/[UIID]/[SECKEY]/[NAME]
     *
     * @see #generateURI
     */
    private String[] parsePath(String pathInfo) {
        // strip away part until the data we are interested starts
        int startOfData = pathInfo.indexOf(DYN_RES_PREFIX)
                + DYN_RES_PREFIX.length();

        String uppUri = pathInfo.substring(startOfData);
        // [0] UIid, [1] security key, [2] name
        return uppUri.split("/", 3);
    }

    /**
     * Generates URI string for a dynamic resource using its {@code id} and
     * {@code name}. [0] UIid, [1] sec key, [2] name
     *
     * @param name
     *            file or attribute name to use in path
     * @param id
     *            unique resource id
     * @return generated URI string
     */
    public static String generateURI(String name, String id) {
        StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);

        try {
            builder.append(UI.getCurrent().getUIId()).append(PATH_SEPARATOR);
            builder.append(id).append(PATH_SEPARATOR);
            builder.append(
                    URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    private static Optional<URI> getPathUri(String path) {
        int index = path.lastIndexOf('/');
        boolean hasPrefix = index >= 0;
        if (!hasPrefix) {
            getLogger().info("Unsupported path structure, path={}", path);
            return Optional.empty();
        }
        String prefix = path.substring(0, index + 1);
        String name = path.substring(prefix.length());
        // path info returns decoded name but space ' ' remains encoded '+'
        name = name.replace('+', ' ');
        try {
            URI uri = new URI(prefix
                    + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            return Optional.of(uri);
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            getLogger().info("Path '{}' is not correct URI (it violates RFC 2396)", path, e);
            return Optional.empty();
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StreamResourceHandler.class.getName());
    }
}

/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceWriter;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler implements RequestHandler {

    private static final char PATH_SEPARATOR = '/';

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/generated-resources/";

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        StreamResourceWriter writer = null;

        if (request.getPathInfo() == null) {
            return false;
        }
        String pathInfo = request.getPathInfo();
        // remove leading '/'
        pathInfo = pathInfo.substring(1);
        if (!pathInfo.startsWith(DYN_RES_PREFIX)) {
            return false;
        }

        session.lock();
        try {
            Optional<StreamResource> resource = getResource(session, response,
                    pathInfo);
            if (!resource.isPresent()) {
                return true;
            }

            response.setContentType(resource.get().getContentType());
            response.setCacheTime(resource.get().getCacheTime());
            writer = resource.get().getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } finally {
            session.unlock();
        }
        try (OutputStream outputStream = response.getOutputStream()) {
            writer.accept(outputStream, session);
        }
        return true;
    }

    /**
     * Generates URI string for a dynamic resource using its {@code id} and
     * {@code name}.
     * 
     * @param id
     *            unique resource id
     * @param name
     *            resource name
     * @return generated URI string
     */
    public static String generateURI(int id, String name) {
        StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);
        try {
            builder.append(id).append(PATH_SEPARATOR).append(
                    URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    private static Optional<StreamResource> getResource(VaadinSession session,
            VaadinResponse response, String path) throws IOException {
        try {
            int index = path.lastIndexOf('/');
            boolean hasPrefix = index >= 0;
            if (!hasPrefix) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Unsuppored path structure");
                return Optional.empty();
            }
            String prefix = path.substring(0, index + 1);
            String name = path.substring(prefix.length());
            // path info returns decoded name but space ' ' remains encoded '+'
            name = name.replace('+', ' ');

            URI uri = new URI(prefix
                    + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
            Optional<StreamResource> resource = session.getResourceRegistry()
                    .getResource(uri);
            if (!resource.isPresent()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Resource is not found for path=" + path);
            }
            return resource;
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            Logger.getLogger(StreamResourceRequestHandler.class.getName())
                    .log(Level.INFO,
                            "Path '" + path
                                    + "' is not correct URI (it violates RFC 2396)",
                            e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Unsuppored path");
            return Optional.empty();
        }
    }

}

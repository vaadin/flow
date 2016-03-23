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
<<<<<<< HEAD
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
=======
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

/**
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 * 
>>>>>>> ae80070 Some javadocs.
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler implements RequestHandler {

<<<<<<< HEAD
    private static final char PATH_SEPARATOR = '/';

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/generated-resources/";
=======
    private static final int BUFFER_SIZE = 1024;
>>>>>>> 80ab6ba... Stream resource registration on the session level.

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
<<<<<<< HEAD
        StreamResourceWriter writer = null;

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

        session.lock();
        try {
            Optional<StreamResource> resource = getPathUri(pathInfo)
                    .flatMap(session.getResourceRegistry()::getResource);
            if (!resource.isPresent()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "Resource is not found for path=" + pathInfo);
                return true;
            }

            StreamResource streamResource = resource.get();
            response.setContentType(streamResource.getContentType());
            response.setCacheTime(streamResource.getCacheTime());
            writer = streamResource.getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } finally {
            session.unlock();
        }
        try (OutputStream outputStream = response.getOutputStream()) {
            writer.accept(outputStream, session);
=======
        InputStream stream = null;

        if (request.getPathInfo() == null) {
            return false;
        }
        String pathInfo = request.getPathInfo();
        // remove leading '/'
        pathInfo = pathInfo.substring(1);

        boolean requiresLock;
        session.lock();
        try {
            int index = pathInfo.lastIndexOf('/');
            String path = index >= 0 ? pathInfo.substring(0, index + 1) : "";
            String name = pathInfo.substring(path.length());
            // path info returns decoded name but space ' ' remains encoded '+'
            name = name.replace('+', ' ');
            Optional<StreamResource> resource = session.getResourceRegistry()
                    .getResource(path, name);
            if (!resource.isPresent()) {
                return false;
            }

            response.setContentType(resource.get().getContentType());
            response.setCacheTime(resource.get().getCacheTime());
            stream = resource.get().createInputStream();
            if (stream == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }

            requiresLock = resource.get().requiresLock();
        } finally {
            session.unlock();
        }
<<<<<<< Upstream, based on 563d9fae047956f0206e367040e76bb7b77cad51
        if (stream != null) {
            OutputStream out = response.getOutputStream();
            try {
                copy(requiresLock, session, stream, out);
            } finally {
                closeStreams(stream, out);
            }
>>>>>>> 80ab6ba... Stream resource registration on the session level.
=======
        OutputStream out = response.getOutputStream();
        try {
            copy(requiresLock, session, stream, out);
        } finally {
            closeStreams(stream, out);
>>>>>>> 025249e Review based fixes.
        }
        return true;
    }

<<<<<<< HEAD
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

    private static Optional<URI> getPathUri(String path) {
        int index = path.lastIndexOf('/');
        boolean hasPrefix = index >= 0;
        if (!hasPrefix) {
            getLog().info("Unsupported path structure, path=" + path);
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
            getLog().log(Level.INFO, "Path '" + path
                    + "' is not correct URI (it violates RFC 2396)", e);
            return Optional.empty();
        }
    }

    private static Logger getLog() {
        return Logger.getLogger(StreamResourceRequestHandler.class.getName());
=======
    private void closeStreams(InputStream stream, OutputStream out)
            throws IOException {
        try {
            stream.close();
        } finally {
            out.close();
        }
    }

    private long copy(boolean requiresLock, VaadinSession session,
            InputStream source, OutputStream out) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = read(requiresLock, session, source, buf)) > 0) {
            out.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    private int read(boolean useLock, VaadinSession session, InputStream source,
            byte[] buffer) throws IOException {
        if (useLock) {
            session.lock();
            try {
                return source.read(buffer);
            } finally {
                session.unlock();
            }
        } else {
            return source.read(buffer);
        }
>>>>>>> 80ab6ba... Stream resource registration on the session level.
    }

}

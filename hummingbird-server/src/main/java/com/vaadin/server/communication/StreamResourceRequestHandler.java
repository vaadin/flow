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
import java.io.InputStream;
import java.io.OutputStream;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.StreamResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 * 
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler implements RequestHandler {

    private static final int BUFFER_SIZE = 1024;

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        InputStream stream = null;

        if (request.getPathInfo() == null) {
            return false;
        }
        StringBuilder pathInfo = new StringBuilder(request.getPathInfo());
        // remove leading '/'
        pathInfo.delete(0, 1);

        boolean requiresLock;
        session.lock();
        try {
            VaadinServletRequest servletRequest = (VaadinServletRequest) request;
            String queryString = servletRequest.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                pathInfo.append('?').append(servletRequest.getQueryString());
            }
            StreamResource resource = session.getResource(pathInfo.toString());
            if (resource == null) {
                return false;
            }

            response.setContentType(resource.getContentType());
            response.setCacheTime(resource.getCacheTime());
            stream = resource.createInputStream();
            if (stream == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }

            requiresLock = resource.requiresLock();
        } finally {
            session.unlock();
        }
        if (stream != null) {
            OutputStream out = response.getOutputStream();
            try {
                copy(requiresLock, session, stream, out);
            } finally {
                closeStreams(stream, out);
            }
        }
        return true;
    }

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
    }

}

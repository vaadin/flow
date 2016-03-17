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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

/**
 * @author Vaadin Ltd
 *
 */
public class StreamResourceRequestHandler implements RequestHandler {

    private static final int BUFFER_SIZE = 8192;

    private static final class RequestContext
            implements Predicate<StreamResourceWrapper> {

        private final VaadinSession session;

        private final VaadinRequest request;

        private final String requestFileName;

        private RequestContext(VaadinSession session, VaadinRequest request) {
            this.session = session;
            this.request = request;

            // XXX: has to be rewritten
            requestFileName = getRequestFilename(
                    ((VaadinServletRequest) request).getHttpServletRequest());
        }

        @Override
        public boolean test(StreamResourceWrapper resource) {
            // XXX : has to be rewritten
            return resource.getResource().getUri().equals(requestFileName);
        }

        VaadinSession getSession() {
            return session;
        }

        VaadinRequest getRequest() {
            return request;
        }

    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        InputStream stream = null;

        RequestContext context = new RequestContext(session, request);
        session.lock();
        try {
            UI ui = session.getService().findUI(request);
            if (ui == null) {
                return false;
            }
            List<StreamResourceWrapper> resources = ui.getResources().stream()
                    .filter(context).collect(Collectors.toList());
            StreamResourceWrapper wrapper = null;
            if (resources.size() > 1) {
                // TODO : warn
                wrapper = resources.get(0);
            } else if (!resources.isEmpty()) {
                wrapper = resources.get(0);
            } else {
                return false;
            }
            response.setContentType(wrapper.getResource().getContentType());
            stream = wrapper.getResource().get();
        } finally {
            session.unlock();
        }
        if (stream != null) {
            OutputStream out = response.getOutputStream();
            try {
                copy(session, stream, out);
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

    private long copy(VaadinSession session, InputStream source,
            OutputStream out) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = read(session, source, buf)) > 0) {
            out.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    private int read(VaadinSession session, InputStream source, byte[] buffer)
            throws IOException {
        session.lock();
        try {
            return source.read(buffer);
        } finally {
            session.unlock();
        }
    }

    // XXX : this is copy from com.vaadin.server.StaticFileServer. Has to be
    // rewritten
    private static String getRequestFilename(HttpServletRequest request) {
        // http://localhost:8888/context/servlet/folder/file.js
        // ->
        // /servlet/folder/file.js

        String servletPath; // Starts with "/"
        if ("".equals(request.getServletPath())) {
            // /* mapped servlet
            servletPath = "";
        } else {
            // /something or /something/* mapped servlet
            servletPath = request.getServletPath();
        }

        if (request.getPathInfo() == null) {
            return servletPath;
        } else {
            return servletPath + request.getPathInfo();
        }
    }

}

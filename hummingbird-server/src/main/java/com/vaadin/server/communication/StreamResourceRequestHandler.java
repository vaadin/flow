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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.StateNode;
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

    private static final int BUFFER_SIZE = 1024;

    private static final class RequestContext
            implements Predicate<StreamResourceReference> {

        private final String pathInfo;

        private final String queryUri;

        private final UI ui;

        private RequestContext(UI ui, VaadinSession session,
                VaadinRequest request) {
            this.ui = ui;
            VaadinServletRequest servletRequest = (VaadinServletRequest) request;
            StringBuilder pInfo = new StringBuilder(request.getPathInfo())
                    .append('?').append(servletRequest.getQueryString());
            if (pInfo.charAt(0) == '/') {
                pInfo.delete(0, 1);
            }
            pathInfo = pInfo.toString();
            queryUri = new StringBuilder(servletRequest.getRequestURI())
                    .append('?').append(servletRequest.getQueryString())
                    .toString();
        }

        @Override
        public boolean test(StreamResourceReference resource) {
            String uri = resource.getApplicationResourceUri();
            StateNode node = ui.getFrameworkData().getStateTree()
                    .getNodeById(resource.getNodeId());
            if (node == null) {
                return false;
            }
            if (node.getOwner() != ui.getFrameworkData().getStateTree()) {
                return false;
            }
            if (uri.startsWith("/")) {
                return uri.equals(queryUri);
            } else {
                return uri.equals(pathInfo);
            }
        }

    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response) throws IOException {
        InputStream stream = null;

        boolean requiresLock;
        session.lock();
        try {
            UI ui = session.getService().findUI(request);
            if (ui == null) {
                return false;
            }
            RequestContext context = new RequestContext(ui, session, request);
            List<StreamResourceReference> resources = ui.getFrameworkData()
                    .getResources().stream().filter(context)
                    .collect(Collectors.toList());
            StreamResourceReference ref = null;
            if (resources.size() > 1) {
                ref = resources.get(0);
                StringBuilder msg = new StringBuilder();
                msg.append(resources.size());
                msg.append(
                        " stream resource instances are found for the same URI='");
                msg.append(ref.getResource().getUri()).append("'");
                Logger.getLogger(StreamResourceRequestHandler.class.getName())
                        .warning(msg.toString());
            } else if (!resources.isEmpty()) {
                ref = resources.get(0);
            } else {
                return false;
            }
            response.setContentType(ref.getResource().getContentType());
            stream = ref.getResource().createInputStream();

            requiresLock = ref.getResource().requiresLock();
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

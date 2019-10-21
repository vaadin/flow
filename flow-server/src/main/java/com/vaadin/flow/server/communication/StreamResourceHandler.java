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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StreamResourceHandler implements Serializable {

    /**
     * Handle sending for a stream resource request.
     *
     * @param session
     *            session for the request
     * @param request
     *            request to handle
     * @param response
     *            response object to which a response can be written.
     * @param streamResource
     *            stream resource that handles data writer
     *
     * @throws IOException
     *             if an IO error occurred
     */
    public void handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response, StreamResource streamResource)
            throws IOException {

        StreamResourceWriter writer;
        session.lock();
        try {
            ServletContext context = ((VaadinServletRequest) request)
                    .getServletContext();
            response.setContentType(streamResource.getContentTypeResolver()
                    .apply(streamResource, context));
            response.setCacheTime(streamResource.getCacheTime());
            writer = streamResource.getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;

        } finally {
            session.unlock();
        }
        try (OutputStream outputStream = response.getOutputStream()) {
            writer.accept(outputStream, session);
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;
        }
    }

}

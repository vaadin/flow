/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import javax.servlet.ServletContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
            streamResource.getHeaders()
                    .forEach((name, value) -> response.setHeader(name, value));
            writer = streamResource.getWriter();
            if (writer == null) {
                throw new IOException(
                        "Stream resource produces null input stream");
            }
        } catch (Exception exception) {
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            throw exception;

        } finally {
            session.unlock();
        }
        try (OutputStream outputStream = response.getOutputStream()) {
            try {
                writer.accept(outputStream, session);
            } catch (Exception exception) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                throw exception;
            }
        }
    }
}

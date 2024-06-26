/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;

        } finally {
            session.unlock();
        }
        // don't use here "try resource" syntax sugar because in case there is
        // an exception the {@code outputStream} will be closed before "catch"
        // block which sets the status code and this code will not have any
        // effect being called after closing the stream (see #8740).
        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            writer.accept(outputStream, session);
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw exception;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

}

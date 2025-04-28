/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadFileCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.ElementRequestHandler;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.UploadException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Interface for handling download of data from the server to the client.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface UploadHandler extends ElementRequestHandler {

    /**
     * Method that is called when the client wants to upload data to the url
     * stored for this specific handler registration.
     *
     * @param event
     *            upload event containing the necessary data for getting the
     *            request
     */
    void handleUploadRequest(UploadEvent event);

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        boolean isMultipartUpload = request instanceof HttpServletRequest
                && JakartaServletFileUpload
                        .isMultipartContent((HttpServletRequest) request);

        StateNode node = owner.getNode();

        session.lock();
        try {
            if (node == null) {
                session.getErrorHandler()
                        .error(new ErrorEvent(new UploadException(
                                "File upload ignored because the node for the stream variable was not found")));
                return;
            }
            if (!node.isAttached()) {
                session.getErrorHandler().error(new ErrorEvent(
                        new UploadException("Warning: file upload ignored for "
                                + node.getId()
                                + " because the component was disabled")));
                return;
            }
        } finally {
            session.unlock();
        }

        String fileName = getUrlPostfix() == null ? "" : getUrlPostfix();
        if (isMultipartUpload) {
            Collection<Part> parts = Collections.EMPTY_LIST;
            try {
                parts = ((HttpServletRequest) request).getParts();
            } catch (IOException | ServletException ioe) {
                // NOOOP
            }
            if (!parts.isEmpty()) {
                Iterator<Part> iter = parts.iterator();
                while (iter.hasNext()) {
                    Part part = iter.next();

                    UploadEvent event = new UploadEvent(request, response,
                            session, part.getSubmittedFileName(),
                            part.getSize(), part.getContentType(), owner, null,
                            part);
                    handleUploadRequest(event);
                }
            } else {
                long contentLength = request.getContentLengthLong();
                // Parse the request
                FileItemInputIterator iter;
                try {
                    JakartaServletFileUpload upload = new JakartaServletFileUpload();
                    upload.setSizeMax(getRequestSizeMax());
                    upload.setFileSizeMax(getFileSizeMax());
                    upload.setFileCountMax(getFileCountMax());
                    if (request.getCharacterEncoding() == null) {
                        // Request body's file upload headers are expected to be
                        // encoded in
                        // UTF-8 if not explicitly set otherwise in the request.
                        upload.setHeaderCharset(StandardCharsets.UTF_8);
                    }
                    iter = upload.getItemIterator((HttpServletRequest) request);

                    while (iter.hasNext()) {
                        FileItemInput item = iter.next();

                        UploadEvent event = new UploadEvent(request, response,
                                session, item.getName(), contentLength,
                                item.getContentType(), owner, item, null);
                        handleUploadRequest(event);
                    }
                } catch (FileUploadException e) {
                    String limitInfoStr = "{} limit exceeded. To increase the limit "
                            + "extend StreamRequestHandler, override {} method for "
                            + "UploadHandler and provide a higher limit.";
                    if (e instanceof FileUploadByteCountLimitException) {
                        LoggerFactory.getLogger(UploadHandler.class).warn(
                                limitInfoStr, "Request size",
                                "getRequestSizeMax");
                    } else if (e instanceof FileUploadSizeException) {
                        LoggerFactory.getLogger(UploadHandler.class).warn(
                                limitInfoStr, "File size", "getFileSizeMax");
                    } else if (e instanceof FileUploadFileCountLimitException) {
                        LoggerFactory.getLogger(UploadHandler.class).warn(
                                limitInfoStr, "File count", "getFileCountMax");
                    }
                    LoggerFactory.getLogger(UploadHandler.class)
                            .warn("File upload failed.", e);
                } catch (IOException uioe) {
                    throw new UncheckedIOException("e", uioe);
                }
            }
        } else {
            // These are unknown in filexhr ATM
            fileName = "unknown";
            String contentType = "unknown";

            UploadEvent event = new UploadEvent(request, response, session,
                    fileName, request.getContentLengthLong(), contentType,
                    owner, null, null);

            handleUploadRequest(event);
        }
    }

    default long getRequestSizeMax() {
        return -1;
    }

    default long getFileSizeMax() {
        return -1;
    }

    default long getFileCountMax() {
        return 10000;
    }
}

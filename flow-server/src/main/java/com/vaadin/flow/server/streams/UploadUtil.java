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
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Utility class for upload requests. Specifically handles checking if request
 * is multipart of filexhr.
 */
public final class UploadUtil {

    /**
     * Handle upload request and call
     * {@link UploadHandler#handleUploadRequest(UploadEvent)} correctly for xhr
     * and multipart uploads.
     *
     * @param handler
     *            UploadHandler that should be called for this upload
     * @param request
     *            The VaadinRequest for this upload
     * @param response
     *            The VaadinResponse for this upload
     * @param session
     *            Current VaadinSession
     * @param owner
     *            The element that owns the request handler
     */
    public static void handleUpload(UploadHandler handler,
            VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        boolean isMultipartUpload = request instanceof HttpServletRequest
                && JakartaServletFileUpload
                        .isMultipartContent((HttpServletRequest) request);
        try {
            String fileName;
            if (isMultipartUpload) {
                Collection<Part> parts = Collections.EMPTY_LIST;
                try {
                    parts = ((HttpServletRequest) request).getParts();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                } catch (ServletException ioe) {
                    LoggerFactory.getLogger(UploadHandler.class).trace(
                            "Pretending the request did not contain any parts because of exception",
                            ioe);
                }
                if (!parts.isEmpty()) {
                    for (Part part : parts) {
                        UploadEvent event = new UploadEvent(request, response,
                                session, part.getSubmittedFileName(),
                                part.getSize(), part.getContentType(), owner,
                                null, part);
                        handler.handleUploadRequest(event);
                    }
                    handler.responseHandled(true, response);
                } else {
                    long contentLength = request.getContentLengthLong();
                    // Parse the request
                    FileItemInputIterator iter;
                    try {
                        JakartaServletFileUpload upload = new JakartaServletFileUpload();
                        upload.setSizeMax(handler.getRequestSizeMax());
                        upload.setFileSizeMax(handler.getFileSizeMax());
                        upload.setFileCountMax(handler.getFileCountMax());
                        if (request.getCharacterEncoding() == null) {
                            // Request body's file upload headers are expected
                            // to be
                            // encoded in
                            // UTF-8 if not explicitly set otherwise in the
                            // request.
                            upload.setHeaderCharset(StandardCharsets.UTF_8);
                        }
                        iter = upload
                                .getItemIterator((HttpServletRequest) request);
                        while (iter.hasNext()) {
                            FileItemInput item = iter.next();

                            UploadEvent event = new UploadEvent(request,
                                    response, session, item.getName(),
                                    contentLength, item.getContentType(), owner,
                                    item, null);
                            handler.handleUploadRequest(event);
                        }
                        handler.responseHandled(true, response);
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
                                    limitInfoStr, "File size",
                                    "getFileSizeMax");
                        } else if (e instanceof FileUploadFileCountLimitException) {
                            LoggerFactory.getLogger(UploadHandler.class).warn(
                                    limitInfoStr, "File count",
                                    "getFileCountMax");
                        }
                        LoggerFactory.getLogger(UploadHandler.class)
                                .warn("File upload failed.", e);
                        handler.responseHandled(false, response);
                    } catch (IOException ioe) {
                        LoggerFactory.getLogger(UploadHandler.class)
                                .warn("IO Exception during file upload", ioe);
                        handler.responseHandled(false, response);
                    }
                }
            } else {
                // These are unknown in filexhr ATM
                fileName = "unknown";
                String contentType = "unknown";

                UploadEvent event = new UploadEvent(request, response, session,
                        fileName, request.getContentLengthLong(), contentType,
                        owner, null, null);

                handler.handleUploadRequest(event);
                handler.responseHandled(true, response);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(UploadHandler.class)
                    .error("Exception during upload", e);
            handler.responseHandled(false, response);
        }
    }
}

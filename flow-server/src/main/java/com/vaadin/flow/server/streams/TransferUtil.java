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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadByteCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.FileUploadFileCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.streams.UploadCompleteEvent;
import com.vaadin.flow.internal.streams.UploadStartEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Utility class with methods for handling transfer of upload and download
 * requests.
 */
public final class TransferUtil {

    /**
     * Default buffer size for reading data from the input stream.
     * <p>
     * Follows the default buffer size of the Java
     * {@link InputStream#transferTo(OutputStream)}.
     */
    public static int DEFAULT_BUFFER_SIZE = 16384;

    /**
     * Transfers data from the given input stream to the output stream while
     * notifying the progress to the given listeners.
     *
     * @param inputStream
     *            the input stream to read from
     * @param outputStream
     *            the output stream to write to
     * @param transferContext
     *            the transfer request containing metadata about the transfer
     * @param listeners
     *            collection of listeners to notify about progress
     * @return the number of bytes transferred
     * @throws IOException
     *             if an I/O error occurs during the transfer
     */
    public static long transfer(InputStream inputStream,
            OutputStream outputStream, TransferContext transferContext,
            Collection<TransferProgressListener> listeners) throws IOException {
        Objects.requireNonNull(inputStream, "InputStream cannot be null");
        Objects.requireNonNull(outputStream, "OutputStream cannot be null");
        Objects.requireNonNull(transferContext,
                "TransferRequest cannot be null");
        Objects.requireNonNull(listeners,
                "TransferProgressListener cannot be null");
        listeners.forEach(listener -> listener.onStart(transferContext));
        long transferred = 0;
        Map<TransferProgressListener, Long> lastNotified = new HashMap<>(
                listeners.size());
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            outputStream.write(buffer, 0, read);
            if (transferred < Long.MAX_VALUE) {
                try {
                    transferred = Math.addExact(transferred, read);
                } catch (ArithmeticException ignore) {
                    transferred = Long.MAX_VALUE;
                }
                for (TransferProgressListener listener : listeners) {
                    Long lastNotifiedLong = lastNotified.getOrDefault(listener,
                            0L);
                    long progressReportInterval = listener
                            .progressReportInterval();
                    if (progressReportInterval > -1 && transferred
                            - lastNotifiedLong >= progressReportInterval) {
                        long finalTransferred = transferred;
                        listener.onProgress(transferContext, finalTransferred,
                                transferContext.contentLength());
                        lastNotified.put(listener, transferred);
                    }
                }
            }
        }
        outputStream.flush();
        long finalTransferred = transferred;
        listeners.forEach(listener -> listener.onComplete(transferContext,
                finalTransferred));
        return transferred;
    }

    /**
     * Handle upload request and call
     * {@link UploadHandler#handleUploadRequest(UploadEvent)} correctly for xhr
     * and multipart uploads.
     * <p>
     * Fires internal events for the owner upload component to indicate that the
     * upload has started and completed.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
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
    static void handleUpload(UploadHandler handler, VaadinRequest request,
            VaadinResponse response, VaadinSession session, Element owner) {
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
                } catch (ServletException | IllegalStateException ioe) {
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
                        handleUploadRequest(handler, event);
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
                            handleUploadRequest(handler, event);
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

                handleUploadRequest(handler, event);
                handler.responseHandled(true, response);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(UploadHandler.class)
                    .error("Exception during upload", e);
            handler.responseHandled(false, response);
        }
    }

    private static void handleUploadRequest(UploadHandler handler,
            UploadEvent event) throws IOException {
        Component owner = event.getOwningComponent();
        try {
            ComponentUtil.fireEvent(owner, new UploadStartEvent(owner));
            handler.handleUploadRequest(event);
        } finally {
            ComponentUtil.fireEvent(owner, new UploadCompleteEvent(owner));
        }
    }
}

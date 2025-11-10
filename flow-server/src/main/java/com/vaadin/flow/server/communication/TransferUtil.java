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
package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.internal.streams.UploadCompleteEvent;
import com.vaadin.flow.internal.streams.UploadStartEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;

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
    public static void handleUpload(UploadHandler handler,
            VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        boolean isMultipartUpload = isMultipartContent(request);
        try {
            if (isMultipartUpload) {
                Collection<Part> parts = Collections.EMPTY_LIST;
                try {
                    parts = ((HttpServletRequest) request).getParts();
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                } catch (ServletException | IllegalStateException e) {
                    throw new MultipartConfigurationException(e);
                }
                if (!parts.isEmpty()) {
                    validateUploadLimits(handler, request, parts);

                    for (Part part : parts) {
                        UploadEvent event = new UploadEvent(request, response,
                                session, part.getSubmittedFileName(),
                                part.getSize(), part.getContentType(), owner,
                                part);
                        handleUploadRequest(handler, event);
                    }
                    handler.responseHandled(true, response);
                } else {
                    LoggerFactory.getLogger(UploadHandler.class)
                            .warn("Multipart request has no parts");
                    handler.responseHandled(false, response);
                }
            } else {
                String fileName = extractFilenameFromXhrRequest(request);
                String contentType = extractContentTypeFromXhrRequest(request);

                UploadEvent event = new UploadEvent(request, response, session,
                        fileName, request.getContentLengthLong(), contentType,
                        owner, null);

                handleUploadRequest(handler, event);
                handler.responseHandled(true, response);
            }
        } catch (UploadSizeLimitExceededException
                | UploadFileSizeLimitExceededException
                | UploadFileCountLimitExceededException e) {
            String limitInfoStr = "{} limit exceeded. To increase the limit "
                    + "extend StreamRequestHandler, override {} method for "
                    + "UploadHandler and provide a higher limit.";
            if (e instanceof UploadSizeLimitExceededException) {
                LoggerFactory.getLogger(UploadHandler.class).warn(limitInfoStr,
                        "Request size", "getRequestSizeMax");
            } else if (e instanceof UploadFileSizeLimitExceededException fileSizeException) {
                LoggerFactory.getLogger(UploadHandler.class).warn(
                        limitInfoStr + " File: {}", "File size",
                        "getFileSizeMax", fileSizeException.getFileName());
            } else if (e instanceof UploadFileCountLimitExceededException) {
                LoggerFactory.getLogger(UploadHandler.class).warn(limitInfoStr,
                        "File count", "getFileCountMax");
            }
            LoggerFactory.getLogger(UploadHandler.class)
                    .warn("File upload failed.", e);
            handler.responseHandled(false, response);
        } catch (Exception e) {
            LoggerFactory.getLogger(UploadHandler.class)
                    .error("Exception during upload", e);
            handler.responseHandled(false, response);
        }
    }

    /**
     * Checks if the request is a multipart request by examining the HTTP method
     * and Content-Type header.
     *
     * @param request
     *            the request to check
     * @return true if the request is multipart, false otherwise
     */
    private static boolean isMultipartContent(VaadinRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * Extracts the filename from an XHR upload request.
     * <p>
     * The filename is extracted from the X-Filename header, which is set by
     * vaadin-upload. The filename is encoded using JavaScript's
     * encodeURIComponent and decoded on the server using
     * {@link UrlUtil#decodeURIComponent(String)} (RFC 3986).
     *
     * @param request
     *            the request to extract the filename from
     * @return the decoded filename, or "unknown" if not present
     */
    public static String extractFilenameFromXhrRequest(VaadinRequest request) {
        String fileName = request.getHeader("X-Filename");

        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }

        // Decode the percent-encoded filename
        return UrlUtil.decodeURIComponent(fileName);
    }

    /**
     * Extracts the content type from an XHR upload request.
     * <p>
     * The content type is extracted from the Content-Type header.
     *
     * @param request
     *            the request to extract the content type from
     * @return the content type, or "unknown" if not present
     */
    public static String extractContentTypeFromXhrRequest(
            VaadinRequest request) {
        String contentType = request.getHeader("Content-Type");

        if (contentType == null || contentType.isEmpty()) {
            return "unknown";
        }

        return contentType;
    }

    /**
     * Validates upload limits for the given parts.
     *
     * @param handler
     *            the upload handler containing the limits
     * @param request
     *            the request
     * @param parts
     *            the parts to validate
     * @throws UploadSizeLimitExceededException
     *             if the total size exceeds the limit
     * @throws UploadFileSizeLimitExceededException
     *             if a file size exceeds the limit
     * @throws UploadFileCountLimitExceededException
     *             if the file count exceeds the limit
     */
    private static void validateUploadLimits(UploadHandler handler,
            VaadinRequest request, Collection<Part> parts)
            throws UploadSizeLimitExceededException,
            UploadFileSizeLimitExceededException,
            UploadFileCountLimitExceededException {
        long requestSizeMax = handler.getRequestSizeMax();
        long fileSizeMax = handler.getFileSizeMax();
        long fileCountMax = handler.getFileCountMax();

        // Check file count
        if (fileCountMax > -1 && parts.size() > fileCountMax) {
            throw new UploadFileCountLimitExceededException(parts.size(),
                    fileCountMax);
        }

        // Check total request size
        long contentLength = request.getContentLengthLong();
        if (requestSizeMax > -1 && contentLength > requestSizeMax) {
            throw new UploadSizeLimitExceededException(contentLength,
                    requestSizeMax);
        }

        // Check individual file sizes
        if (fileSizeMax > -1) {
            for (Part part : parts) {
                long size = part.getSize();
                if (size > fileSizeMax) {
                    throw new UploadFileSizeLimitExceededException(
                            part.getSubmittedFileName(), size, fileSizeMax);
                }
            }
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

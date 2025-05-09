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

import java.io.File;
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
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import static com.vaadin.flow.server.Constants.DEFAULT_FILE_COUNT_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_FILE_SIZE_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_REQUEST_SIZE_MAX;

/**
 * Interface for handling upload of data from the client to the server.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface UploadHandler extends ElementRequestHandler {

    /**
     * Method that is called when the client wants to upload data to the url
     * stored for this specific handler registration.
     * <p>
     * After upload of all files is done the method
     * {@link UploadEvent#sendUploadResponse(boolean)} can be called to write an
     * upload response. The method
     * {@link #responseHandled(boolean, VaadinResponse)} will be called when all
     * upload items have been handled.
     *
     * @param event
     *            upload event containing the necessary data for getting the
     *            request
     */
    void handleUploadRequest(UploadEvent event);

    /**
     * Method called by framework when
     * {@link UploadHandler#handleUploadRequest(UploadEvent)} methods have been
     * called for all files.
     * <p>
     * This method sets the http response return codes according to internal
     * exception handling in the framework.
     * <p>
     * If you want custom exception handling and to set the return code,
     * implement this method and overwrite the default functionality.
     *
     * @param success
     *            is there was no exception thrown for upload
     * @param response
     *            the response object for the upload request
     */
    default void responseHandled(boolean success, VaadinResponse response) {
        if (success) {
            response.setStatus(HttpStatusCode.OK.getCode());
        } else {
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    default void handleRequest(VaadinRequest request, VaadinResponse response,
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
                        handleUploadRequest(event);
                    }
                    responseHandled(true, response);
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
                            handleUploadRequest(event);
                        }
                        responseHandled(true, response);
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
                        responseHandled(false, response);
                    } catch (IOException ioe) {
                        LoggerFactory.getLogger(UploadHandler.class)
                                .warn("IO Exception during file upload", ioe);
                        responseHandled(false, response);
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
                responseHandled(true, response);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(UploadHandler.class)
                    .error("Exception during upload", e);
            responseHandled(false, response);
        }
    }

    /**
     * The maximum allowed size of a complete request, as opposed to
     * {@link #getFileSizeMax()}. Only targets stream multipart uploads.
     * <p>
     * The default value of -1 indicates, that there is no limit.
     *
     * @return The maximum allowed size, in bytes
     */
    default long getRequestSizeMax() {
        return DEFAULT_REQUEST_SIZE_MAX;
    }

    /**
     * The maximum allowed size of a single uploaded file, as opposed to
     * {@link #getRequestSizeMax()}. Only targets stream multipart uploads.
     * <p>
     * The default value of -1 indicates, that there is no limit.
     *
     * @return Maximum size of a single uploaded file, in bytes
     */
    default long getFileSizeMax() {
        return DEFAULT_FILE_SIZE_MAX;
    }

    /**
     * The maximum number of files allowed per request. Only targets stream
     * multipart uploads.
     * <p>
     * Default is 10000.
     *
     * @return the maximum number of files allowed, -1 means no limit
     */
    default long getFileCountMax() {
        return DEFAULT_FILE_COUNT_MAX;
    }

    /**
     * Generate a upload handler for storing upload stream into a file.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @param fileFactory
     *            factory for generating file to write to
     * @return file upload handler
     */
    static FileUploadHandler toFile(
            SerializableBiConsumer<UploadMetadata, File> successHandler,
            FileFactory fileFactory) {
        return new FileUploadHandler(successHandler, fileFactory);
    }

    /**
     * Generate a upload handler for storing upload stream into a file.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @param fileFactory
     *            factory for generating file to write to
     * @param listener
     *            listener for transfer progress events
     * @return file upload handler instance with progress listener
     */
    static FileUploadHandler toFile(
            SerializableBiConsumer<UploadMetadata, File> successHandler,
            FileFactory fileFactory, TransferProgressListener listener) {
        FileUploadHandler fileUploadHandler = new FileUploadHandler(
                successHandler, fileFactory);
        fileUploadHandler.addTransferProgressListener(listener);
        return fileUploadHandler;
    }

    /**
     * Generate upload handler for storing upload stream into a temporary file.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @return temporary file upload handler instance
     */
    static TemporaryFileUploadHandler toTempFile(
            SerializableBiConsumer<UploadMetadata, File> successHandler) {
        return new TemporaryFileUploadHandler(successHandler);
    }

    /**
     * Generate upload handler for storing upload stream into a temporary file.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @param listener
     *            listener for transfer progress events
     * @return temporary file upload handler instance with progress listener
     */
    static TemporaryFileUploadHandler toTempFile(
            SerializableBiConsumer<UploadMetadata, File> successHandler,
            TransferProgressListener listener) {
        TemporaryFileUploadHandler temporaryFileUploadHandler = new TemporaryFileUploadHandler(
                successHandler);
        temporaryFileUploadHandler.addTransferProgressListener(listener);
        return temporaryFileUploadHandler;
    }

    /**
     * Generate upload handler for storing download into in-memory
     * {@code byte[]}.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @return in-memory upoload handler
     */
    static InMemoryUploadHandler inMemory(
            SerializableBiConsumer<UploadMetadata, byte[]> successHandler) {
        return new InMemoryUploadHandler(successHandler);
    }

    /**
     * Generate upload handler for storing download into in-memory
     * {@code byte[]}.
     *
     * @param successHandler
     *            consumer to be called when upload successfully completes
     * @param listener
     *            listener for transfer progress events
     * @return in-memory upoload handler with progress listener
     */
    static InMemoryUploadHandler inMemory(
            SerializableBiConsumer<UploadMetadata, byte[]> successHandler,
            TransferProgressListener listener) {
        InMemoryUploadHandler inMemoryUploadHandler = new InMemoryUploadHandler(
                successHandler);
        inMemoryUploadHandler.addTransferProgressListener(listener);
        return inMemoryUploadHandler;
    }
}

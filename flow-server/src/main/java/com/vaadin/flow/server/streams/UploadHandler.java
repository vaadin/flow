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
import java.io.PrintWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;

import static com.vaadin.flow.server.Constants.DEFAULT_FILE_COUNT_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_FILE_SIZE_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_REQUEST_SIZE_MAX;

/**
 * Provides a flexible high-level abstraction for implementing file and
 * arbitrary content uploads from client to server in Vaadin applications.
 * <p>
 * This interface can be implemented in two ways:
 * <ul>
 * <li>By creating a lambda expression that implements the
 * {@link #handleUploadRequest(UploadEvent)} method</li>
 * <li>By creating a child or anonymous class that implements this
 * interface</li>
 * </ul>
 * <p>
 * The interface provides several factory methods for common upload scenarios:
 * <ul>
 * <li>{@link #toFile(FileUploadCallback, FileFactory)} - for uploading files to
 * the server file system</li>
 * <li>{@link #toTempFile(FileUploadCallback)} - for uploading to temporary
 * files</li>
 * <li>{@link #inMemory(InMemoryUploadCallback)} - for uploading files to
 * memory</li>
 * </ul>
 * Example:
 *
 * <pre>
 * UploadHandler.inMemory((metadata, bytes) -> {
 *     // validate and save data
 * });
 * </pre>
 *
 * All factory methods have overloads that allow adding a transfer progress
 * listener:
 *
 * <pre>
 * UploadHandler.toFile((metadata, file) -> {
 *     // validate and save file
 * }, filename -> new File("/path/to/file", filename),
 *         new TransferProgressListener() {
 *             &#064;Override
 *             public void onComplete(TransferContext context,
 *                     long transferredBytes) {
 *                 // show notification about file upload completion
 *             }
 *         });
 * </pre>
 * <p>
 * You can use a lambda expression to handle uploads directly:
 *
 * <pre>
 * UploadHandler handler = event -> {
 *     var name = event.getContentType();
 *     var size = event.getFileSize();
 *     // validate file
 *     try (InputStream inputStream = event.getInputStream()) {
 *         // process input stream
 *     }
 * };
 * </pre>
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
     * {@link #responseHandled(UploadResult)} will be called.
     *
     * @param event
     *            upload event containing the necessary data for getting the
     *            request
     * @throws IOException
     *             if an error occurs during upload
     */
    void handleUploadRequest(UploadEvent event) throws IOException;

    /**
     * Method called by framework when
     * {@link UploadHandler#handleUploadRequest(UploadEvent)} methods have been
     * called for all files.
     * <p>
     * This method sets the HTTP response return codes and writes JSON responses
     * for rejected files:
     * <ul>
     * <li>200 OK - all files accepted</li>
     * <li>422 Unprocessable Entity - all files rejected (with JSON body)</li>
     * <li>207 Multi-Status - some files accepted, some rejected (with JSON
     * body)</li>
     * <li>500 Internal Server Error - exception occurred</li>
     * </ul>
     * <p>
     * If you want custom exception handling and to set the return code,
     * implement this method and overwrite the default functionality.
     *
     * @param result
     *            the result of the upload operation containing success status,
     *            response object, any exception that occurred, and lists of
     *            accepted/rejected files
     */
    default void responseHandled(UploadResult result) {
        VaadinResponse response = result.response();
        try {
            if (result.exception() != null) {
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            } else if (result.allRejected()) {
                response.setStatus(422); // Unprocessable Entity
                response.setContentType("application/json");
                writeJsonResponse(response,
                        new RejectedFilesResponse(result.rejectedFiles()));
            } else if (result.hasMixed()) {
                response.setStatus(207); // Multi-Status
                response.setContentType("application/json");
                writeJsonResponse(response, new MixedUploadResponse(
                        result.acceptedFiles(), result.rejectedFiles()));
            } else {
                response.setStatus(HttpStatusCode.OK.getCode());
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(UploadHandler.class)
                    .error("Error writing upload response", e);
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    private static void writeJsonResponse(VaadinResponse response,
            Object responseObject) throws IOException {
        ObjectMapper mapper = JacksonUtils.getMapper();
        try {
            String json = mapper.writeValueAsString(responseObject);
            PrintWriter writer = response.getWriter();
            writer.write(json);
        } catch (JacksonException e) {
            throw new IOException("Failed to serialize response to JSON", e);
        }
    }

    /**
     * JSON response structure for rejected files.
     */
    record RejectedFilesResponse(
            List<UploadResult.RejectedFile> rejected) implements
            java.io.Serializable {
    }

    /**
     * JSON response structure for mixed upload results.
     */
    record MixedUploadResponse(List<String> accepted,
            List<UploadResult.RejectedFile> rejected) implements
            java.io.Serializable {
    }

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) throws IOException {
        TransferUtil.handleUpload(this, request, response, session, owner);
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
     * Generate an upload handler for storing upload stream into a file.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @param fileFactory
     *            factory for generating file to write to
     * @return file upload handler
     */
    static FileUploadHandler toFile(FileUploadCallback successCallback,
            FileFactory fileFactory) {
        return new FileUploadHandler(successCallback, fileFactory);
    }

    /**
     * Generate an upload handler for storing upload stream into a file with
     * progress handling.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @param fileFactory
     *            factory for generating file to write to
     * @param listener
     *            listener for transfer progress events
     * @return file upload handler instance with progress listener
     */
    static FileUploadHandler toFile(FileUploadCallback successCallback,
            FileFactory fileFactory, TransferProgressListener listener) {
        FileUploadHandler fileUploadHandler = new FileUploadHandler(
                successCallback, fileFactory);
        fileUploadHandler.addTransferProgressListener(listener);
        return fileUploadHandler;
    }

    /**
     * Generate an upload handler for storing upload stream into a temporary
     * file.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @return temporary file upload handler instance
     */
    static TemporaryFileUploadHandler toTempFile(
            FileUploadCallback successCallback) {
        return new TemporaryFileUploadHandler(successCallback);
    }

    /**
     * Generate an upload handler for storing upload stream into a temporary
     * file with progress handling.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @param listener
     *            listener for transfer progress events
     * @return temporary file upload handler instance with progress listener
     */
    static TemporaryFileUploadHandler toTempFile(
            FileUploadCallback successCallback,
            TransferProgressListener listener) {
        TemporaryFileUploadHandler temporaryFileUploadHandler = new TemporaryFileUploadHandler(
                successCallback);
        temporaryFileUploadHandler.addTransferProgressListener(listener);
        return temporaryFileUploadHandler;
    }

    /**
     * Generate upload handler for storing download into in-memory
     * {@code byte[]}.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @return in-memory upload handler
     */
    static InMemoryUploadHandler inMemory(
            InMemoryUploadCallback successCallback) {
        return new InMemoryUploadHandler(successCallback);
    }

    /**
     * Generate upload handler for storing download into in-memory
     * {@code byte[]} with progress handling.
     *
     * @param successCallback
     *            consumer to be called when upload successfully completes
     * @param listener
     *            listener for transfer progress events
     * @return in-memory upload handler with progress listener
     */
    static InMemoryUploadHandler inMemory(
            InMemoryUploadCallback successCallback,
            TransferProgressListener listener) {
        InMemoryUploadHandler inMemoryUploadHandler = new InMemoryUploadHandler(
                successCallback);
        inMemoryUploadHandler.addTransferProgressListener(listener);
        return inMemoryUploadHandler;
    }
}

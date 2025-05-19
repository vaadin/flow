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

import java.io.InputStream;
import java.io.Serializable;

import com.vaadin.flow.server.HttpStatusCode;

/**
 * Data class containing required information for sending the given input stream
 * to the client.
 * <p>
 * The given input stream will be read at a later time and will be automatically
 * closed by the caller.
 *
 * @since 24.8
 */
public class DownloadResponse implements Serializable {

    private final InputStream inputStream;

    private final String fileName;
    private final String contentType;
    private final int size;

    private Integer error;
    private String errorMessage;

    /**
     * Create a download response with content stream and content data.
     *
     * @param inputStream
     *            data stream for data to send to client, stream will be closed
     *            automatically after use by the caller.
     * @param fileName
     *            file name of download
     * @param contentType
     *            content type
     * @param size
     *            byte size of stream
     */
    public DownloadResponse(InputStream inputStream, String fileName,
            String contentType, int size) {
        this.inputStream = inputStream;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
    }

    /**
     * Get the InputStream to read the content data from.
     * <p>
     * InputStream needs to be closed by the called after reading is over.
     *
     * @return content InputStream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Get the defined file name.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the content type.
     *
     * @return content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the defined size for the content
     *
     * @return content size
     */
    public int getSize() {
        return size;
    }

    /**
     * Generate an error response for download.
     *
     * @param statusCode
     *            error status code
     * @return DownloadResponse for request
     */
    public static DownloadResponse error(int statusCode) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode);
        return downloadResponse;
    }

    /**
     * Generate an error response for download with message.
     *
     * @param statusCode
     *            error status code
     * @param message
     *            error message for details on what went wrong
     * @return DownloadResponse for request
     */
    public static DownloadResponse error(int statusCode, String message) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, message);
        return downloadResponse;
    }

    /**
     * Generate an error response for download.
     *
     * @param statusCode
     *            error status code
     * @return DownloadResponse for request
     */
    public static DownloadResponse error(HttpStatusCode statusCode) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode);
        return downloadResponse;
    }

    /**
     * Generate an error response for download with message.
     *
     * @param statusCode
     *            error status code
     * @param message
     *            error message for details on what went wrong
     * @return DownloadResponse for request
     */
    public static DownloadResponse error(HttpStatusCode statusCode,
            String message) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, message);
        return downloadResponse;
    }

    /**
     * Check if response has an error code.
     *
     * @return {@code true} is error code has been set
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Set http error code.
     *
     * @param error
     *            error code
     */
    public void setError(int error) {
        this.error = error;
    }

    /**
     * Set http error code and error message.
     *
     * @param error
     *            error code
     * @param errorMessage
     *            error message
     */
    public void setError(int error, String errorMessage) {
        this.error = error;
        this.errorMessage = errorMessage;
    }

    /**
     * Set http error code.
     *
     * @param error
     *            error code
     */
    public void setError(HttpStatusCode error) {
        this.error = error.getCode();
    }

    /**
     * Set http error code and error message.
     *
     * @param error
     *            error code
     * @param errorMessage
     *            error message
     */
    public void setError(HttpStatusCode error, String errorMessage) {
        this.error = error.getCode();
        this.errorMessage = errorMessage;
    }

    /**
     * Get the set error code.
     *
     * @return error code or -1 if not set
     */
    public int getError() {
        if (error == null) {
            return -1;
        }
        return error;
    }

    /**
     * Get error message if set for error response.
     *
     * @return error message or null if not set
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}

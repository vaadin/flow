/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.regex.Pattern;

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

    // A strong entity tag as defined by RFC 9110: an opaque quoted string
    private static final Pattern STRONG_ETAG_PATTERN = Pattern
            .compile("\"[\\x21\\x23-\\x7E\\x80-\\xFF]*\"");

    private final InputStream inputStream;

    private final String fileName;
    private final String contentType;
    private final long contentLength;

    private Integer error;
    private String errorMessage;
    private Exception exception;
    private String eTag;

    /**
     * Create a download response with content stream and content data.
     *
     * @param inputStream
     *            data stream for data to send to client, stream will be closed
     *            automatically after use by the caller.
     * @param fileName
     *            name of the file to be downloaded, configured by setting the
     *            Content-Disposition header to 'attachment' if the value is not
     *            <code>null</code>, otherwise the header is not set
     * @param contentType
     *            content type or a value determined from fileName if
     *            {@code null}
     * @param contentLength
     *            byte size of a stream or <code>-1</code> if unknown
     */
    public DownloadResponse(InputStream inputStream, String fileName,
            String contentType, long contentLength) {
        this.inputStream = inputStream;
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.contentType = contentType;
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
     * <p>
     * For a {@code null} value the type should be gotten from
     * {@code VaadinService.getMimeType(fileName)} or be set to default value
     * {@code application/octet-stream}
     *
     * @return content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the defined size for the content or <code>-1</code> if unknown.
     *
     * @return content size
     */
    public long getContentLength() {
        return contentLength;
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
     * Generate an error response for download.
     *
     * @param statusCode
     *            error status code
     * @param exception
     *            exception that caused the error
     * @return DownloadResponse for request
     * @since 24.9
     */
    public static DownloadResponse error(int statusCode, Exception exception) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, exception);
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
     * Generate an error response for download with message.
     *
     * @param statusCode
     *            error status code
     * @param message
     *            error message for details on what went wrong
     * @param exception
     *            exception that caused the error
     * @return DownloadResponse for request
     * @since 24.9
     */
    public static DownloadResponse error(int statusCode, String message,
            Exception exception) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, message, exception);
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
     * Generate an error response for download.
     *
     * @param statusCode
     *            error status code
     * @param exception
     *            exception that caused the error
     * @return DownloadResponse for request
     * @since 24.9
     */
    public static DownloadResponse error(HttpStatusCode statusCode,
            Exception exception) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, exception);
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
     * Generate an error response for download with message.
     *
     * @param statusCode
     *            error status code
     * @param message
     *            error message for details on what went wrong
     * @param exception
     *            exception that caused the error
     * @return DownloadResponse for request
     * @since 24.9
     */
    public static DownloadResponse error(HttpStatusCode statusCode,
            String message, Exception exception) {
        DownloadResponse downloadResponse = new DownloadResponse(null, null,
                null, -1);
        downloadResponse.setError(statusCode, message, exception);
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
     * Set http error code.
     *
     * @param error
     *            error code
     * @param exception
     *            exception that caused the error
     * @since 24.9
     */
    public void setError(int error, Exception exception) {
        this.error = error;
        this.exception = exception;
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
     * Set http error code and error message.
     *
     * @param error
     *            error code
     * @param errorMessage
     *            error message
     * @param exception
     *            exception that caused the error
     * @since 24.9
     */
    public void setError(int error, String errorMessage, Exception exception) {
        setError(error, errorMessage);
        this.exception = exception;
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
     * Set http error code.
     *
     * @param error
     *            error code
     * @param exception
     *            exception that caused the error
     * @since 24.9
     */
    public void setError(HttpStatusCode error, Exception exception) {
        this.error = error.getCode();
        this.exception = exception;
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
     * Set http error code and error message.
     *
     * @param error
     *            error code
     * @param errorMessage
     *            error message
     * @param exception
     *            exception that caused the error
     * @since 24.9
     */
    public void setError(HttpStatusCode error, String errorMessage,
            Exception exception) {
        setError(error, errorMessage);
        this.exception = exception;
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

    /**
     * Get the exception that occurred during the download process, if any.
     *
     * @return the exception or null if no exception occurred
     * @see TransferContext#exception()
     * @since 24.9
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Sets the strong entity tag of the content, which declares that the same
     * tag always stands for the same bytes.
     * <p>
     * With an entity tag and a known content length, a download from
     * {@link DownloadHandler#fromInputStream(InputStreamDownloadCallback)}
     * answers byte range requests, which media players use to seek and browsers
     * use to resume a download. Safari does not play audio or video without
     * them. The callback is then called for every range request, and the bytes
     * before the range are skipped from the returned stream, which is fast for
     * a stream that supports seeking, such as a
     * {@link java.io.FileInputStream}. A client that asks for a range with an
     * outdated entity tag gets the whole content.
     * <p>
     * Change the entity tag whenever the content changes, for example by
     * deriving it from a version number or a hash of the content. Content that
     * differs between requests with the same entity tag ends up as corrupt data
     * in the browser.
     * <p>
     * For example:
     *
     * <pre>
     * DownloadHandler.fromInputStream(event -&gt; {
     *     DownloadResponse response = new DownloadResponse(video.openStream(),
     *             video.getName(), "video/mp4", video.getSize());
     *     response.setETag("\"" + video.getVersion() + "\"");
     *     return response;
     * });
     * </pre>
     *
     * @param eTag
     *            the strong entity tag, an opaque value in double quotes such
     *            as {@code "v42"} with the quotes, or {@code null} to not serve
     *            byte ranges
     * @throws IllegalArgumentException
     *             if the value is not a strong entity tag
     */
    public void setETag(String eTag) {
        if (eTag != null && !STRONG_ETAG_PATTERN.matcher(eTag).matches()) {
            throw new IllegalArgumentException("'" + eTag
                    + "' is not a strong entity tag, which is an opaque value"
                    + " in double quotes without a W/ prefix, such as \"v42\"");
        }
        this.eTag = eTag;
    }

    /**
     * Gets the strong entity tag of the content.
     *
     * @return the entity tag, or {@code null} if not set
     * @see #setETag(String)
     */
    public String getETag() {
        return eTag;
    }
}

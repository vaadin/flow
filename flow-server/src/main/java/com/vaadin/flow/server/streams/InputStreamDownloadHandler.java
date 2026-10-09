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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for serving an input stream for client download.
 * <p>
 * Byte range requests, which media players use to seek and browsers use to
 * resume a download, are not answered by default. Every range request calls the
 * callback again for a new stream, and the stream has to be read from its start
 * up to the range: unless the stream can seek, such as a
 * {@link java.io.FileInputStream}, each seek or resume reads all the bytes
 * before it again, which adds up for large media that a player fetches in many
 * small ranges. Ranges are also only correct if the callback returns the same
 * content every time, which only the application knows. Call
 * {@link #enableRangeRequests()} when that holds, or use
 * {@link DownloadHandler#forFile(java.io.File)}, which answers ranges by
 * seeking in the file.
 *
 * @since 24.8
 */
public class InputStreamDownloadHandler
        extends AbstractDownloadHandler<InputStreamDownloadHandler> {

    private final InputStreamDownloadCallback callback;
    private String fileNameOverride;
    private boolean rangeRequestsEnabled;

    /**
     * Create an input stream download handler for given event -> response
     * function.
     *
     * @param callback
     *            serializable function for handling download
     */
    public InputStreamDownloadHandler(InputStreamDownloadCallback callback) {
        this.callback = callback;
    }

    /**
     * Create an input stream download handler for given event -> response
     * function.
     * <p>
     * The downloaded file fileNameOverride and download URL postfix will be set
     * to <code>fileNameOverride</code>.
     *
     * @param callback
     *            serializable function for handling download
     * @param fileNameOverride
     *            used as a downloaded file name as a download request URL
     *            postfix, e.g.
     *            <code>/VAADIN/dynamic/resource/0/5298ee8b-9686-4a5a-ae1d-b38c62767d6a/my-file.txt</code>
     * @since 25.2
     */
    public InputStreamDownloadHandler(InputStreamDownloadCallback callback,
            String fileNameOverride) {
        this.callback = callback;
        this.fileNameOverride = fileNameOverride;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        VaadinResponse response = downloadEvent.getResponse();
        setTransferUI(downloadEvent.getUI());
        DownloadResponse download;
        try {
            String resourceName = getUrlPostfix();
            if (isInline()) {
                downloadEvent.inline(resourceName);
            } else {
                downloadEvent.setFileName(resourceName);
            }
            download = callback.complete(downloadEvent);
        } catch (IOException | RuntimeException e) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            IOException cause;
            if (e instanceof IOException ioe) {
                cause = ioe;
            } else if (e instanceof UncheckedIOException uioe) {
                cause = uioe.getCause();
            } else {
                cause = new IOException(e.getMessage(), e);
            }
            downloadEvent.setException(e);
            notifyError(downloadEvent, cause);
            throw e;
        }
        if (download.hasError()) {
            response.setStatus(download.getError());
            String message = download.getErrorMessage();
            if (message == null) {
                message = "Download failed with code " + download.getError();
            }
            IOException ioException = new IOException(message);
            if (download.getException() != null) {
                downloadEvent.setException(download.getException());
            } else {
                downloadEvent.setException(ioException);
            }
            notifyError(downloadEvent, ioException);
            return;
        }

        String downloadName = download.getFileName();
        String contentType = download.getContentType() == null
                ? getContentType(downloadName, response)
                : download.getContentType();
        downloadEvent.setContentType(contentType);

        if (isInline()) {
            downloadEvent.inline(downloadName);
        } else {
            downloadEvent.setFileName(downloadName);
        }

        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = download.getInputStream()) {
            transferContent(downloadEvent, inputStream, outputStream,
                    download.getContentLength(),
                    rangeRequestsEnabled
                            ? new SeekableContent(downloadName, null, null)
                            : null);
        } catch (RangeRequestException e) {
            // Not reported again: a cancel is not an error, and a smaller
            // range was never reported as started
            if (!e.isCancelled()) {
                throw e;
            }
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            downloadEvent.setException(ioe);
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
    }

    /**
     * Enables answering byte range requests, which media players use to seek
     * and browsers use to resume a download. Safari does not play audio or
     * video without them.
     * <p>
     * Only enable them if the callback returns the same content on every call,
     * with a known content length: the callback is called for every range
     * request, and a range that a client joins with a range of other content
     * ends up as corrupt data. The bytes before a range are skipped from the
     * returned stream, which is a seek for a stream that supports it, such as a
     * {@link java.io.FileInputStream}, but reads them for other streams.
     *
     * @return this instance for method chaining
     */
    public InputStreamDownloadHandler enableRangeRequests() {
        rangeRequestsEnabled = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Range requests are answered after {@link #enableRangeRequests()}.
     */
    @Override
    public boolean isRangeRequestsEnabled() {
        return rangeRequestsEnabled;
    }

    @Override
    public String getUrlPostfix() {
        return fileNameOverride;
    }
}

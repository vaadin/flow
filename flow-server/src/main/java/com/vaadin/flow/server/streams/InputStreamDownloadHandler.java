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
import com.vaadin.flow.server.communication.TransferUtil;

/**
 * Download handler for serving an input stream for client download.
 *
 * @since 24.8
 */
public class InputStreamDownloadHandler
        extends AbstractDownloadHandler<InputStreamDownloadHandler> {

    private final InputStreamDownloadCallback callback;
    private String fileNameOverride;

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
        downloadEvent.setContentLength(download.getContentLength());

        if (isInline()) {
            downloadEvent.inline(downloadName);
        } else {
            downloadEvent.setFileName(downloadName);
        }

        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = download.getInputStream()) {
            TransferUtil.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            downloadEvent.setException(ioe);
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
    }

    @Override
    public String getUrlPostfix() {
        return fileNameOverride;
    }
}

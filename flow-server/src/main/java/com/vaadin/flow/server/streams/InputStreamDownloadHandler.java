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
import java.io.InputStream;
import java.io.OutputStream;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for serving an input stream for client download.
 *
 * @since 24.8
 */
public class InputStreamDownloadHandler
        extends AbstractDownloadHandler<InputStreamDownloadHandler> {

    private final InputStreamDownloadCallback callback;

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

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        DownloadResponse download = callback.complete(downloadEvent);
        VaadinResponse response = downloadEvent.getResponse();
        if (download.hasError()) {
            response.setStatus(download.getError());
            return;
        }

        String downloadName = download.getFileName();
        String contentType = download.getContentType() == null
                ? getContentType(downloadName, response)
                : download.getContentType();
        downloadEvent.setContentType(contentType);

        if (!isInline()) {
            downloadEvent.setFileName(downloadName);
        } else {
            downloadEvent.getResponse().setHeader("Content-Disposition",
                    "inline");
        }

        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = download.getInputStream()) {
            TransferUtil.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
    }
}

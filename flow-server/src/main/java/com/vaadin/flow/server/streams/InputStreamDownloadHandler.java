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
import java.io.UncheckedIOException;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for serving an input stream for client download.
 *
 * @since 24.8
 */
public class InputStreamDownloadHandler
        extends AbstractDownloadHandler<InputStreamDownloadHandler> {

    private final SerializableFunction<DownloadEvent, DownloadResponse> handler;
    private final String name;

    /**
     * Create a input stream download handler for given event -> response
     * function.
     *
     * @param handler
     *            serializable function for handling download
     */
    public InputStreamDownloadHandler(
            SerializableFunction<DownloadEvent, DownloadResponse> handler) {
        this(handler, null);
    }

    /**
     * Create an input stream download handler for given event -> response
     * function.
     * <p>
     * The downloaded file name and download URL postfix will be set to
     * <code>name</code>.
     *
     * @param handler
     *            serializable function for handling download
     * @param name
     *            name to use as the url postfix as download response is not
     *            generated before postfix
     */
    public InputStreamDownloadHandler(
            SerializableFunction<DownloadEvent, DownloadResponse> handler,
            String name) {
        this.handler = handler;
        this.name = name;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent) {
        String resourceName = getUrlPostfix();
        downloadEvent.setContentType(
                getContentType(resourceName, downloadEvent.getResponse()));
        if (!isInline()) {
            downloadEvent.setFileName(resourceName);
        }
        DownloadResponse download = handler.apply(downloadEvent);
        VaadinResponse response = downloadEvent.getResponse();
        if (download.hasError()) {
            response.setStatus(download.getError());
            return;
        }

        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = download.getInputStream()) {
            TransferProgressListener.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            notifyError(downloadEvent, ioe);
            throw new UncheckedIOException(ioe);
        }
    }

    @Override
    public String getUrlPostfix() {
        return name;
    }
}

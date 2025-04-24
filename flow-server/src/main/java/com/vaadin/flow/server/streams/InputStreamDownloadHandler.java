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

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for serving an input stream for client download.
 *
 * @since 24.8
 */
public class InputStreamDownloadHandler extends AbstractDownloadHandler {

    private final SerializableFunction<DownloadRequest, DownloadResponse> handler;
    private final String name;

    /**
     * Create a input stream download handler for given event -> response
     * function.
     *
     * @param handler
     *            serializable function for handling download
     */
    public InputStreamDownloadHandler(
            SerializableFunction<DownloadRequest, DownloadResponse> handler) {
        this(handler, null);
    }

    /**
     * Create a input stream download handler for given event -> response
     * function.
     *
     * @param handler
     *            serializable function for handling download
     * @param name
     *            name to use as the url postfix as download response is not
     *            generated before postfix
     */
    public InputStreamDownloadHandler(
            SerializableFunction<DownloadRequest, DownloadResponse> handler,
            String name) {
        this.handler = handler;
        this.name = name;
    }

    @Override
    public void handleDownloadRequest(DownloadRequest event) {
        DownloadResponse download = handler.apply(event);
        VaadinResponse response = event.getResponse();
        if (download.hasError()) {
            response.setStatus(download.getError());
            return;
        }

        final int BUFFER_SIZE = 1024;
        try (OutputStream outputStream = event.getOutputStream()
                .orElseThrow(() -> new IOException("No output stream"));
                InputStream inputStream = download.getInputStream()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = read(event.getSession(), inputStream, buf)) >= 0) {
                outputStream.write(buf, 0, n);
            }
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            throw new RuntimeException(ioe);
        }

        response.setContentType(download.getContentType());
        response.setContentLength(download.getSize());
        response.setHeader("Content-Disposition",
                "attachment;filename=" + download.getFileName());
    }

    @Override
    public String getUrlPostfix() {
        return name;
    }
}

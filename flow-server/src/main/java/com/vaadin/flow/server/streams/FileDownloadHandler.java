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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for use with a given File that will be read and written as
 * binary data to the response.
 *
 * @since 24.8
 */
public class FileDownloadHandler extends AbstractDownloadHandler {

    private final File file;
    private final String name;

    /**
     * Create a download handler for given file. Url postfix will be used as
     * {@code file.getName()}
     *
     * @param file
     *            file to download
     */
    public FileDownloadHandler(File file) {
        this(file, null);
    }

    /**
     * Create a download handler for given file.
     *
     * @param file
     *            file to download
     * @param name
     *            url postfix name to use instead of file name
     */
    public FileDownloadHandler(File file, String name) {
        this.file = file;
        this.name = name;
    }

    /**
     * Create a download handler for given file and desired download name also
     * adding a given progress listener.
     *
     * @param file
     *            file to download
     * @param name
     *            url postfix name to use instead of file name
     * @param listener
     *            listener for transfer progress events
     */
    public FileDownloadHandler(File file, String name,
            TransferProgressListener listener) {
        this(file, name);
        addTransferProgressListener(listener);
    }

    @Override
    public void handleTransfer(DownloadRequest downloadRequest) {
        VaadinResponse response = downloadRequest.getResponse();
        long length = file.length();
        try (OutputStream outputStream = downloadRequest.getOutputStream();
                FileInputStream inputStream = new FileInputStream(file)) {
            TransferProgressListener.transfer(inputStream, outputStream,
                    getTransferContext(downloadRequest), getListeners(),
                    length);
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            notifyError(downloadRequest, ioe);
            throw new UncheckedIOException(ioe);
        }
        response.setContentType(downloadRequest.getContentType());
        response.setContentLength(Math.toIntExact(length));
    }

    @Override
    public String getUrlPostfix() {
        if (name != null) {
            return name;
        }
        return file.getName();
    }
}

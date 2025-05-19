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

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;

/**
 * Download handler for use with a given File that will be read and written as
 * binary data to the response.
 *
 * @since 24.8
 */
public class FileDownloadHandler
        extends AbstractDownloadHandler<FileDownloadHandler> {

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

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        VaadinResponse response = downloadEvent.getResponse();
        try (OutputStream outputStream = downloadEvent.getOutputStream();
                FileInputStream inputStream = new FileInputStream(file)) {
            TransferProgressListener.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
        response.setContentType(downloadEvent.getContentType());
        response.setContentLengthLong(file.length());
    }

    @Override
    public String getUrlPostfix() {
        if (name != null) {
            return name;
        }
        return file.getName();
    }

    @Override
    protected TransferContext getTransferContext(DownloadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.session(),
                transferEvent.fileName(), transferEvent.owningElement(),
                file.length());
    }
}

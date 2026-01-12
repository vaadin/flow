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

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.communication.TransferUtil;

/**
 * Download handler for use with a given File that will be read and written as
 * binary data to the response.
 *
 * @since 24.8
 */
public class FileDownloadHandler
        extends AbstractDownloadHandler<FileDownloadHandler> {

    private final File file;
    private final String fileNameOverride;

    /**
     * Create a download handler for given file. Url postfix will be used as
     * {@code file.getName()}.
     * <p>
     * The downloaded file name and download URL postfix will be set to
     * <code>file.getName()</code>. If you want to use a different file name,
     * use {@link #FileDownloadHandler(File, String)} instead.
     *
     * @param file
     *            file to download
     */
    public FileDownloadHandler(File file) {
        this(file, null);
    }

    /**
     * Create a download handler for given file.
     * <p>
     * The downloaded file fileNameOverride and download URL postfix will be set
     * to <code>fileNameOverride</code>.
     *
     * @param file
     *            file to download
     * @param fileNameOverride
     *            used as a downloaded file name (overrides
     *            <code>file.getName()</code>) and also as a download request
     *            URL postfix, e.g.
     *            <code>/VAADIN/dynamic/resource/0/5298ee8b-9686-4a5a-ae1d-b38c62767d6a/my-file.txt</code>
     */
    public FileDownloadHandler(File file, String fileNameOverride) {
        this.file = file;
        this.fileNameOverride = fileNameOverride;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        setTransferUI(downloadEvent.getUI());
        VaadinResponse response = downloadEvent.getResponse();
        try (OutputStream outputStream = downloadEvent.getOutputStream();
                FileInputStream inputStream = new FileInputStream(file)) {
            String resourceName = getUrlPostfix();
            if (isInline()) {
                downloadEvent.inline(resourceName);
            } else {
                downloadEvent.setFileName(resourceName);
            }
            downloadEvent
                    .setContentType(getContentType(resourceName, response));
            downloadEvent.setContentLength(file.length());
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
        if (fileNameOverride != null) {
            return fileNameOverride;
        }
        return file.getName();
    }

    @Override
    protected TransferContext getTransferContext(DownloadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                getUrlPostfix(), transferEvent.getOwningElement(),
                file.length(), transferEvent.getException());
    }
}

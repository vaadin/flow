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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.HttpStatusCode;

/**
 * Download handler for serving a class resource.
 * <p>
 * For instance for the file {@code resources/com/example/ui/MyData.json} and
 * class {@code com.example.ui.MyData} the definition would be
 * {@code forClassResource(MyData.class, "MyData.json")}
 *
 * @since 24.8
 */
public class ClassDownloadHandler
        extends AbstractDownloadHandler<ClassDownloadHandler> {

    private final Class<?> clazz;
    private final String resourceName;
    private String fileName;

    /**
     * Create a class resource download handler with the resource name as the
     * url postfix (file name).
     * <p>
     * The downloaded file name and download URL postfix will be set to
     * <code>resourceName</code>. If you want to use a different file name, use
     * {@link #ClassDownloadHandler(Class, String, String)} instead.
     *
     * @param clazz
     *            class to use for getting resource
     * @param resourceName
     *            resource to get
     */
    public ClassDownloadHandler(Class<?> clazz, String resourceName) {
        this(clazz, resourceName, null);
    }

    /**
     * Create a class resource download handler with the given file name as the
     * url postfix.
     * <p>
     * The downloaded file name and download URL postfix will be set to
     * <code>fileName</code>.
     *
     * @param clazz
     *            class to use for getting resource
     * @param resourceName
     *            resource to get
     * @param fileName
     *            download file name that overrides <code>resourceName</code>
     *            and also used as a download request URL postfix
     */
    public ClassDownloadHandler(Class<?> clazz, String resourceName,
            String fileName) {
        this.clazz = clazz;
        this.resourceName = resourceName;
        this.fileName = fileName;

        if (clazz.getResource(resourceName) == null) {
            LoggerFactory.getLogger(ClassDownloadHandler.class).warn(
                    "No resource found for '{}'. The resource will receive a 404 not found response.",
                    resourceName);
        }
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        setTransferUI(downloadEvent.getUI());
        if (clazz.getResource(resourceName) == null) {
            LoggerFactory.getLogger(ClassDownloadHandler.class)
                    .warn("No resource found for '{}'", resourceName);
            downloadEvent.getResponse()
                    .setStatus(HttpStatusCode.NOT_FOUND.getCode());
            return;
        }
        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = clazz
                        .getResourceAsStream(resourceName)) {
            String resourceName = getUrlPostfix();
            downloadEvent.setContentType(
                    getContentType(resourceName, downloadEvent.getResponse()));
            if (!isInline()) {
                downloadEvent.setFileName(resourceName);
            } else {
                downloadEvent.inline(resourceName);
            }
            TransferUtil.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            downloadEvent.getResponse()
                    .setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            downloadEvent.setException(ioe);
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
    }

    @Override
    public String getUrlPostfix() {
        if (fileName != null) {
            return fileName;
        }
        if (resourceName.contains("/")) {
            return resourceName.substring(resourceName.lastIndexOf('/') + 1);
        }
        return resourceName;
    }
}

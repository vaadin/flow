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

import com.vaadin.flow.server.DownloadRequest;
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
public class ClassDownloadHandler extends AbstractDownloadHandler {

    private final Class<?> clazz;
    private final String resourceName;
    private String fileName;

    /**
     * Create a class resource download handler with the resource name as the
     * url postfix (file name).
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
     *
     * @param clazz
     *            class to use for getting resource
     * @param resourceName
     *            resource to get
     * @param fileName
     *            name to use as url postfix
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
    public void handleDownloadRequest(DownloadRequest event) {
        if (clazz.getResource(resourceName) == null) {
            event.getResponse().setStatus(HttpStatusCode.NOT_FOUND.getCode());
            return;
        }
        final int BUFFER_SIZE = 1024;
        try (OutputStream outputStream = event.getOutputStream()
                .orElseThrow(() -> new IOException("No output stream"));
                InputStream inputStream = clazz
                        .getResourceAsStream(resourceName)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = read(event.getSession(), inputStream, buf)) >= 0) {
                outputStream.write(buf, 0, n);
            }
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            event.getResponse()
                    .setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            throw new RuntimeException(ioe);
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

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

package com.vaadin.flow.server;

import java.io.File;
import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.streams.ClassDownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.FileDownloadHandler;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.flow.server.streams.ServletResourceDownloadHandler;

/**
 * Interface for handling download of data from the server to the client.
 *
 * @since 24.8
 */
@FunctionalInterface
public interface DownloadHandler extends ElementRequestHandler {

    /**
     * Method that is called when the client wants to download from the url
     * stored for this specific handler registration.
     *
     * @param event
     *            download event containing the necessary data for writing the
     *            response
     */
    void handleDownloadRequest(DownloadRequest event);

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        String fileName = getUrlPostfix() == null ? "" : getUrlPostfix();

        DownloadRequest event = new DownloadRequest(request, response, session,
                fileName);
        event.withOwningComponent(owner)
                .withContentType(Optional
                        .ofNullable(response.getService().getMimeType(fileName))
                        .orElse("application/octet-stream"));

        handleDownloadRequest(event);
    }

    /**
     * Get a download handler for serving given {@link File}.
     *
     * @param file
     *            file to server for download
     * @return DownloadHandler instance for file
     */
    static DownloadHandler forFile(File file) {
        return new FileDownloadHandler(file);
    }

    /**
     * Get a download handler for serving given {@link File}.
     *
     * @param file
     *            file to server for download
     * @param name
     *            download name to use
     * @return DownloadHandler instance for file
     */
    static DownloadHandler forFile(File file, String name) {
        return new FileDownloadHandler(file, name);
    }

    /**
     * Generate a download handler for class resource.
     * <p>
     * For instance for the file {@code resources/com/example/ui/MyData.json}
     * and class {@code com.example.ui.MyData} the definition would be
     * {@code forClassResource(MyData.class, "MyData.json")}
     *
     * @param clazz
     *            class for resource module
     * @param resourceName
     *            name of class resource
     * @return DownloadHandler instance for class resource
     */
    static DownloadHandler forClassResource(Class<?> clazz,
            String resourceName) {
        return new ClassDownloadHandler(clazz, resourceName);
    }

    /**
     * Generate a download handler for class resource.
     * <p>
     * For instance for the file {@code resources/com/example/ui/MyData.json}
     * and class {@code com.example.ui.MyData} the definition would be
     * {@code forClassResource(MyData.class, "MyData.json", "Data.json")}
     *
     * @param clazz
     *            class for resource module
     * @param resourceName
     *            name of class resource
     * @param fileName
     *            download resourceName to use
     * @return DownloadHandler instance for class resource
     */
    static DownloadHandler forClassResource(Class<?> clazz, String resourceName,
            String fileName) {
        return new ClassDownloadHandler(clazz, resourceName, fileName);
    }

    /**
     * Generate a download handler for a servlet resource.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}
     *
     * @param path
     *            the servlet path to the file
     * @return DownloadHandler instance for servlet resource
     */
    static DownloadHandler forServletResource(String path) {
        return new ServletResourceDownloadHandler(path);
    }

    /**
     * Generate a download handler for a servlet resource.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}
     * <p>
     * Name is appended to the download url as the logical name of the target
     * file.
     *
     * @param path
     *            the servlet path to the file
     * @param name
     *            resource name
     * @return DownloadHandler instance for servlet resource
     */
    static DownloadHandler forServletResource(String path, String name) {
        return new ServletResourceDownloadHandler(path, name);
    }

    /**
     * Generate a function for downloading from a generated inputStream.
     *
     * @param handler
     *            handler function that will be called on download
     * @return DownloadHandler instance for inputStream
     */
    static DownloadHandler fromInputStream(
            SerializableFunction<DownloadRequest, DownloadResponse> handler) {
        return new InputStreamDownloadHandler(handler);
    }

    /**
     * Generate a function for downloading from a generated inputStream.
     *
     * @param handler
     *            handler function that will be called on download
     * @param name
     *            resource name
     * @return DownloadHandler instance for inputStream
     */
    static DownloadHandler fromInputStream(
            SerializableFunction<DownloadRequest, DownloadResponse> handler,
            String name) {
        return new InputStreamDownloadHandler(handler, name);
    }
}

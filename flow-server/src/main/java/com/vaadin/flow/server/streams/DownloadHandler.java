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
import java.io.IOException;
import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

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
     * @throws IOException
     *             if an IO error occurred during download
     */
    void handleDownloadRequest(DownloadEvent event) throws IOException;

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) throws IOException {
        String fileName = getUrlPostfix() == null ? "" : getUrlPostfix();

        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, fileName,
                Optional.ofNullable(response.getService().getMimeType(fileName))
                        .orElse("application/octet-stream"),
                owner);

        handleDownloadRequest(downloadEvent);
    }

    /**
     * Get a download handler for serving given {@link File}.
     *
     * @param file
     *            file to server for download
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file) {
        return new FileDownloadHandler(file);
    }

    /**
     * Get a download handler for serving given {@link File} with the given
     * download name.
     *
     * @param file
     *            file to server for download
     * @param name
     *            download name to use
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file, String name) {
        return new FileDownloadHandler(file, name);
    }

    /**
     * Get a download handler for serving given {@link File} with the given
     * download name and progress listener.
     *
     * @param file
     *            file to server for download
     * @param name
     *            download name to use
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file, String name,
            TransferProgressListener listener) {
        FileDownloadHandler handler = new FileDownloadHandler(file, name);
        handler.addTransferProgressListener(listener);
        return handler;
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
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName) {
        return new ClassDownloadHandler(clazz, resourceName);
    }

    /**
     * Generate a download handler for class resource with the given download
     * name.
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
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, String fileName) {
        return new ClassDownloadHandler(clazz, resourceName, fileName);
    }

    /**
     * Generate a download handler for class resource with the given download
     * name and progress listener.
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
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, String fileName,
            TransferProgressListener listener) {
        ClassDownloadHandler handler = new ClassDownloadHandler(clazz,
                resourceName, fileName);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a download handler for a servlet resource.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}
     *
     * @param path
     *            the servlet path to the file
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path) {
        return new ServletResourceDownloadHandler(path);
    }

    /**
     * Generate a download handler for a servlet resource with the given
     * download name.
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
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path,
            String name) {
        return new ServletResourceDownloadHandler(path, name);
    }

    /**
     * Generate a download handler for a servlet resource with the given
     * download name and progress listener.
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
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path,
            String name, TransferProgressListener listener) {
        ServletResourceDownloadHandler handler = new ServletResourceDownloadHandler(
                path, name);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a function for downloading from a generated inputStream.
     *
     * @param handler
     *            handler function that will be called on download
     * @return DownloadHandler implementation for download from an input stream
     */
    static InputStreamDownloadHandler fromInputStream(
            SerializableFunction<DownloadEvent, DownloadResponse> handler) {
        return new InputStreamDownloadHandler(handler);
    }

    /**
     * Generate a function for downloading from a generated inputStream with the
     * given download name.
     *
     * @param handler
     *            handler function that will be called on download
     * @param name
     *            resource name
     * @return DownloadHandler implementation for download from an input stream
     */
    static InputStreamDownloadHandler fromInputStream(
            SerializableFunction<DownloadEvent, DownloadResponse> handler,
            String name) {
        return new InputStreamDownloadHandler(handler, name);
    }

    /**
     * Generate a function for downloading from a generated inputStream with the
     * given download name and progress listener.
     *
     * @param handler
     *            handler function that will be called on download
     * @param name
     *            resource name
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download from an input stream
     */
    static InputStreamDownloadHandler fromInputStream(
            SerializableFunction<DownloadEvent, DownloadResponse> handler,
            String name, TransferProgressListener listener) {
        InputStreamDownloadHandler downloadHandler = new InputStreamDownloadHandler(
                handler, name);
        downloadHandler.addTransferProgressListener(listener);
        return downloadHandler;
    }
}

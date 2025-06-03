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
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Provides a flexible high-level abstraction for implementing file and
 * arbitrary content downloads from server to client in Vaadin applications.
 * <p>
 * This interface can be implemented in two ways:
 * <ul>
 * <li>By creating a lambda expression that implements the
 * {@link #handleDownloadRequest(DownloadEvent)} method</li>
 * <li>By creating a child or anonymous class that implements this
 * interface</li>
 * </ul>
 * <p>
 * The interface provides several factory methods for common download scenarios:
 * <ul>
 * <li>{@link #forFile(File)} - for downloading files from the server</li>
 * <li>{@link #forClassResource(Class, String)} - for downloading class
 * resources</li>
 * <li>{@link #forServletResource(String)} - for downloading servlet
 * resources</li>
 * <li>{@link #fromInputStream(InputStreamDownloadCallback)} - for downloading from
 * input streams</li>
 * </ul>
 * Example:
 *
 * <pre>
 * DownloadHandler.forFile(new File("/path/to/file.txt"));
 * </pre>
 *
 * All factory methods have overloads that allow adding a download progress
 * listener and set a custom file name and URL postfix:
 *
 * <pre>
 * DownloadHandler.forClassResource(MyView.class, "attachment-XYZ.txt",
 *         "attachment.txt", new TransferProgressListener() {
 *             &#064;Override
 *             public void onComplete(TransferContext context,
 *                     long transferredBytes) {
 *                 // update UI on complete
 *             }
 *         });
 * </pre>
 * <p>
 * If you need to write directly to an OutputStream, you can use a lambda
 * expression with {@code event.getOutputStream()} to access the output stream:
 *
 * <pre>
 * DownloadHandler handler = event -> {
 *     try (OutputStream out = event.getOutputStream()) {
 *         // Write your data to the output stream
 *         out.write(yourData);
 *     }
 * };
 * </pre>
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
        DownloadEvent downloadEvent = new DownloadEvent(request, response,
                session, owner);

        handleDownloadRequest(downloadEvent);
    }

    /**
     * Get a download handler for serving given {@link File}.
     * <p>
     * The downloaded file name is resolved as <code>file.getName()</code>.
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
     * download file name.
     *
     * @param file
     *            file to server for download
     * @param fileNameOverride
     *            download file name that overrides <code>file.getName()</code>
     *            and also used as a download request URL postfix
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file, String fileNameOverride) {
        return new FileDownloadHandler(file, fileNameOverride);
    }

    /**
     * Get a download handler for serving given {@link File} with the given
     * download file name and progress listener.
     *
     * @param file
     *            file to server for download
     * @param fileNameOverride
     *            download file name that overrides <code>file.getName()</code>
     *            and also used as a download request URL postfix
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file, String fileNameOverride,
            TransferProgressListener listener) {
        FileDownloadHandler handler = new FileDownloadHandler(file,
                fileNameOverride);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Get a download handler for serving given {@link File} with the given
     * progress listener.
     * <p>
     * The downloaded file name is resolved as <code>file.getName()</code>.
     *
     * @param file
     *            file to server for download
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a file
     */
    static FileDownloadHandler forFile(File file,
            TransferProgressListener listener) {
        FileDownloadHandler handler = new FileDownloadHandler(file);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a download handler for class resource.
     * <p>
     * For instance for the file {@code resources/com/example/ui/MyData.json}
     * and class {@code com.example.ui.MyData} the definition would be
     * {@code forClassResource(MyData.class, "MyData.json")}.
     * <p>
     * The downloaded file name is resolved as <code>resourceName</code>.
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
     * @param fileNameOverride
     *            download file name that overrides <code>resourceName</code>
     *            and also used as a download request URL postfix
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, String fileNameOverride) {
        return new ClassDownloadHandler(clazz, resourceName, fileNameOverride);
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
     * @param fileNameOverride
     *            download file name that overrides <code>resourceName</code>
     *            and also used as a download request URL postfix
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, String fileNameOverride,
            TransferProgressListener listener) {
        ClassDownloadHandler handler = new ClassDownloadHandler(clazz,
                resourceName, fileNameOverride);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a download handler for class resource with the given progress
     * listener.
     * <p>
     * For instance for the file {@code resources/com/example/ui/MyData.json}
     * and class {@code com.example.ui.MyData} the definition would be
     * {@code forClassResource(MyData.class, "MyData.json", "Data.json")}.
     * <p>
     * The downloaded file name is resolved as <code>resourceName</code>.
     *
     * @param clazz
     *            class for resource module
     * @param resourceName
     *            name of class resource
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download a class resource
     */
    static ClassDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, TransferProgressListener listener) {
        ClassDownloadHandler handler = new ClassDownloadHandler(clazz,
                resourceName);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a download handler for a servlet resource.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}.
     * <p>
     * The downloaded file name is resolved as the last segment in
     * <code>path</code>.
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
     * download file name.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}
     * <p>
     * File name override is appended to the download url as the logical name of
     * the target file.
     *
     * @param path
     *            the servlet path to the file
     * @param fileNameOverride
     *            download file name that overrides the name taken from
     *            <code>path</code> and also used as a download request URL
     *            postfix
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path,
            String fileNameOverride) {
        return new ServletResourceDownloadHandler(path, fileNameOverride);
    }

    /**
     * Generate a download handler for a servlet resource with the given
     * download fileNameOverride and progress listener.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}
     * <p>
     * File name override is appended to the download url as the logical name of
     * the target file.
     *
     * @param path
     *            the servlet path to the file
     * @param fileNameOverride
     *            download file name that overrides the name taken from
     *            <code>path</code> and also used as a download request URL
     *            postfix
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path,
            String fileNameOverride, TransferProgressListener listener) {
        ServletResourceDownloadHandler handler = new ServletResourceDownloadHandler(
                path, fileNameOverride);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a download handler for a servlet resource with the given
     * progress listener.
     * <p>
     * For instance for the file {@code webapp/WEB-INF/servlet.json} the path
     * would be {@code /WEB-INF/servlet.json}.
     *
     * @param path
     *            the servlet path to the file
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for downloading a servlet resource
     */
    static ServletResourceDownloadHandler forServletResource(String path,
            TransferProgressListener listener) {
        ServletResourceDownloadHandler handler = new ServletResourceDownloadHandler(
                path);
        handler.addTransferProgressListener(listener);
        return handler;
    }

    /**
     * Generate a function for downloading from a generated InputStream.
     * <p>
     * <code>DownloadResponse</code> instances can be created using various
     * factory methods or with new operator.
     *
     * @param callback
     *            a function that will be called on download
     * @return DownloadHandler implementation for download from an input stream
     */
    static InputStreamDownloadHandler fromInputStream(
            InputStreamDownloadCallback callback) {
        return new InputStreamDownloadHandler(callback);
    }

    /**
     * Generate a function for downloading from a generated InputStream with the
     * given progress listener.
     * <p>
     * <code>DownloadResponse</code> instances can be created using various
     * factory methods or with new operator.
     *
     * @param callback
     *            a function that will be called on download
     * @param listener
     *            listener for transfer progress events
     * @return DownloadHandler implementation for download from an input stream
     */
    static InputStreamDownloadHandler fromInputStream(
            InputStreamDownloadCallback callback,
            TransferProgressListener listener) {
        InputStreamDownloadHandler downloadHandler = new InputStreamDownloadHandler(
                callback);
        downloadHandler.addTransferProgressListener(listener);
        return downloadHandler;
    }
}

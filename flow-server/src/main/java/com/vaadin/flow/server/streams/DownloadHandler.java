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
import java.util.Optional;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.ElementRequestHandler;
import com.vaadin.flow.server.TransferProgressAware;
import com.vaadin.flow.server.TransferProgressListener;
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
     */
    void handleDownloadRequest(DownloadRequest event);

    default void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner) {
        String fileName = getUrlPostfix() == null ? "" : getUrlPostfix();

        DownloadRequest downloadRequest = new DownloadRequest(request, response,
                session, fileName,
                Optional.ofNullable(response.getService().getMimeType(fileName))
                        .orElse("application/octet-stream"),
                owner);

        handleDownloadRequest(downloadRequest);
    }

    /**
     * Get a download handler for serving given {@link File}.
     *
     * @param file
     *            file to server for download
     * @return DownloadHandler instance for file
     */
    static AbstractDownloadHandler forFile(File file) {
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
    static AbstractDownloadHandler forFile(File file, String name) {
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
     * @return DownloadHandler instance for file
     */
    static AbstractDownloadHandler forFile(File file, String name,
            TransferProgressListener listener) {
        return new FileDownloadHandler(file, name, listener);
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
    static AbstractDownloadHandler forClassResource(Class<?> clazz,
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
    static AbstractDownloadHandler forClassResource(Class<?> clazz,
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
     * @return DownloadHandler instance for class resource
     */
    static AbstractDownloadHandler forClassResource(Class<?> clazz,
            String resourceName, String fileName,
            TransferProgressListener listener) {
        return new ClassDownloadHandler(clazz, resourceName, fileName,
                listener);
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
    static AbstractDownloadHandler forServletResource(String path) {
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
    static AbstractDownloadHandler forServletResource(String path,
            String name) {
        return new ServletResourceDownloadHandler(path, name);
    }

    static AbstractDownloadHandler forServletResource(String path, String name,
            TransferProgressListener listener) {
        return new ServletResourceDownloadHandler(path, name, listener);
    }

    /**
     * Generate a function for downloading from a generated inputStream.
     *
     * @param handler
     *            handler function that will be called on download
     * @return DownloadHandler instance for inputStream
     */
    static AbstractDownloadHandler fromInputStream(
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
    static AbstractDownloadHandler fromInputStream(
            SerializableFunction<DownloadRequest, DownloadResponse> handler,
            String name) {
        return new InputStreamDownloadHandler(handler, name);
    }

    static AbstractDownloadHandler fromInputStream(
            SerializableFunction<DownloadRequest, DownloadResponse> handler,
            String name, TransferProgressListener listener) {
        return new InputStreamDownloadHandler(handler, name, listener);
    }

    /**
     * Generate a download handler using a callback function.
     * <p>
     * This helper is useful when you want to chaing the handling lambda and
     * progress listeners.
     *
     * @param handler
     *            handler function that will be called on download
     * @return DownloadHandler instance for inputStream
     */
    static AbstractDownloadHandler fromCallback(
            SerializableConsumer<DownloadRequest> handler) {
        return new AbstractDownloadHandler() {
            @Override
            public void handleTransfer(DownloadRequest event) {
                handler.accept(event);
            }
        };
    }

    public static void main(String[] args) {

        DownloadHandler.fromCallback(event -> {
            System.out.println("Download handler logic ...");
        }).whenStart(() -> System.out.println("Started"))
                .whenComplete((transferred) -> {
                    System.out.println(
                            "Completed successfully, bytes transferred: "
                                    + transferred);
                });
        TransferProgressAware handler = DownloadHandler
                .forFile(new File("test.txt")).onProgress(
                        (transferredBytes,
                                totalBytes) -> System.out.println("Progress: "
                                        + transferredBytes + "/" + totalBytes),
                        1024);

        handler.unsubscribe();

        AbstractDownloadHandler handler2 = DownloadHandler
                .forServletResource("some/path");
        handler2.addTransferProgressListener(new TransferProgressListener() {
            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                System.out.println("Transfer completed with " + transferredBytes
                        + " bytes");
            }
        });

        AbstractDownloadHandler handler1 = new AbstractDownloadHandler() {
            @Override
            public void handleTransfer(DownloadRequest event) {
                System.out.println("Download handler logic ...");
            }
        };
    }
}

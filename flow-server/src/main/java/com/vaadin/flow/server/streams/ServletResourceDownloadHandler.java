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
import java.io.UncheckedIOException;

import com.vaadin.flow.server.DownloadEvent;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Download handler for serving a servlet resource for client download.
 * <p>
 * For instance for the file {@code webapp/WEB-INF/servlet.json} the path would
 * be {@code /WEB-INF/servlet.json}
 *
 * @since 24.8
 */
public class ServletResourceDownloadHandler
        extends AbstractDownloadHandler<ServletResourceDownloadHandler> {

    private final String path;
    private final String name;

    /**
     * Create download handler for servlet resource. Uses url postfix as file
     * name from path
     *
     * @param path
     *            path of servlet resource
     */
    public ServletResourceDownloadHandler(String path) {
        this(path, null);
    }

    /**
     * Create download handler for servlet resource.
     *
     * @param path
     *            path of servlet resource
     * @param name
     *            name to use as url postfix (can be given as empty string)
     */
    public ServletResourceDownloadHandler(String path, String name) {
        this.path = path;
        this.name = name;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent) {
        VaadinService service = downloadEvent.getRequest().getService();
        if (service instanceof VaadinServletService servletService) {
            try (OutputStream outputStream = downloadEvent.getOutputStream();
                    InputStream inputStream = servletService.getServlet()
                            .getServletContext().getResourceAsStream(path)) {
                TransferProgressListener.transfer(inputStream, outputStream,
                        getTransferContext(downloadEvent), getListeners());
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                downloadEvent.getResponse().setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                notifyError(downloadEvent, ioe);
                throw new UncheckedIOException(ioe);
            }

            downloadEvent.getResponse()
                    .setContentType(downloadEvent.getContentType());
        }
    }

    @Override
    public String getUrlPostfix() {
        if (name != null) {
            return name;
        }
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }
}

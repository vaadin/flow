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

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;
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
    private final String fileNameOverride;

    /**
     * Create download handler for servlet resource. Uses url postfix as file
     * name from path.
     * <p>
     * The downloaded file name and download URL postfix will be set to the file
     * name from <code>path</code>. If you want to use a different file name,
     * use {@link #ServletResourceDownloadHandler(String, String)} instead.
     *
     * @param path
     *            path of servlet resource
     */
    public ServletResourceDownloadHandler(String path) {
        this(path, null);
    }

    /**
     * Create download handler for servlet resource.
     * <p>
     * The downloaded file fileNameOverride and download URL postfix will be set
     * to <code>fileNameOverride</code>.
     *
     * @param path
     *            path of servlet resource
     * @param fileNameOverride
     *            download file name that overrides the name taken from
     *            <code>path</code> and also used as a download request URL
     *            postfix
     */
    public ServletResourceDownloadHandler(String path,
            String fileNameOverride) {
        this.path = path;
        this.fileNameOverride = fileNameOverride;
    }

    @Override
    public void handleDownloadRequest(DownloadEvent downloadEvent)
            throws IOException {
        setTransferUI(downloadEvent.getUI());
        VaadinService service = downloadEvent.getRequest().getService();
        VaadinResponse response = downloadEvent.getResponse();
        if (service instanceof VaadinServletService servletService) {
            try (OutputStream outputStream = downloadEvent.getOutputStream();
                    InputStream inputStream = servletService.getServlet()
                            .getServletContext().getResourceAsStream(path)) {
                String resourceName = getUrlPostfix();
                downloadEvent
                        .setContentType(getContentType(resourceName, response));
                if (!isInline()) {
                    downloadEvent.setFileName(resourceName);
                } else {
                    downloadEvent.getResponse().setHeader("Content-Disposition",
                            "inline");
                }
                TransferUtil.transfer(inputStream, outputStream,
                        getTransferContext(downloadEvent), getListeners());
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                notifyError(downloadEvent, ioe);
                throw ioe;
            }
        }
    }

    @Override
    public String getUrlPostfix() {
        if (fileNameOverride != null) {
            return fileNameOverride;
        }
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }
}

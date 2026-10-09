/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.net.URL;
import java.net.URLConnection;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Download handler for serving a servlet resource for client download.
 * <p>
 * For instance for the file {@code webapp/WEB-INF/servlet.json} the path would
 * be {@code /WEB-INF/servlet.json}
 * <p>
 * Byte range requests, which media players use to seek and browsers use to
 * resume a download, are only answered after {@link #enableRangeRequests()}.
 * Use {@link DownloadHandler#forFile(java.io.File)} to serve seekable media
 * efficiently.
 *
 * @since 24.8
 */
public class ServletResourceDownloadHandler
        extends AbstractDownloadHandler<ServletResourceDownloadHandler> {

    private final String path;
    private final String fileNameOverride;
    private boolean rangeRequestsEnabled;

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
            URL resource = servletService.getServlet().getServletContext()
                    .getResource(path);
            if (resource == null) {
                response.setStatus(HttpStatusCode.NOT_FOUND.getCode());
                return;
            }
            URLConnection connection = resource.openConnection();
            try (OutputStream outputStream = downloadEvent.getOutputStream();
                    InputStream inputStream = connection.getInputStream()) {
                String resourceName = getUrlPostfix();
                downloadEvent
                        .setContentType(getContentType(resourceName, response));
                if (isInline()) {
                    downloadEvent.inline(resourceName);
                } else {
                    downloadEvent.setFileName(resourceName);
                }
                transferContent(downloadEvent, inputStream, outputStream,
                        connection.getContentLengthLong(),
                        rangeRequestsEnabled
                                ? SeekableContent.ofResource(resource,
                                        connection)
                                : null);
            } catch (RangeRequestException e) {
                // Not reported again: a cancel is not an error, and a smaller
                // range was never reported as started
                if (!e.isCancelled()) {
                    throw e;
                }
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                downloadEvent.setException(ioe);
                notifyError(downloadEvent, ioe);
                throw ioe;
            }
        }
    }

    /**
     * Enables answering byte range requests, which media players use to seek
     * and browsers use to resume a download. Safari does not play audio or
     * video without them.
     * <p>
     * Each range is read by skipping the resource up to its start. For a file
     * on disk that is a seek, but a resource inside a packaged war may have to
     * be read, and inflated if compressed, from its start for every range,
     * depending on the servlet container. That is costly for large media that a
     * player fetches in many small ranges.
     *
     * @return this instance for method chaining
     */
    public ServletResourceDownloadHandler enableRangeRequests() {
        rangeRequestsEnabled = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Range requests are answered after {@link #enableRangeRequests()}.
     */
    @Override
    public boolean isRangeRequestsEnabled() {
        return rangeRequestsEnabled;
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

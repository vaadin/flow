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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.HttpStatusCode;

/**
 * Download handler for serving a class resource.
 * <p>
 * For instance for the file {@code resources/com/example/ui/MyData.json} and
 * class {@code com.example.ui.MyData} the definition would be
 * {@code forClassResource(MyData.class, "MyData.json")}
 * <p>
 * Byte range requests, which media players use to seek and browsers use to
 * resume a download, are only answered after {@link #enableRangeRequests()}.
 * Use {@link DownloadHandler#forFile(java.io.File)} to serve seekable media
 * efficiently.
 *
 * @since 24.8
 */
public class ClassDownloadHandler
        extends AbstractDownloadHandler<ClassDownloadHandler> {

    private final Class<?> clazz;
    private final String resourceName;
    private String fileName;
    private boolean rangeRequestsEnabled;

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
        URL resource = clazz.getResource(resourceName);
        if (resource == null) {
            LoggerFactory.getLogger(ClassDownloadHandler.class)
                    .warn("No resource found for '{}'", resourceName);
            downloadEvent.getResponse()
                    .setStatus(HttpStatusCode.NOT_FOUND.getCode());
            return;
        }
        URLConnection connection = resource.openConnection();
        try (OutputStream outputStream = downloadEvent.getOutputStream();
                InputStream inputStream = connection.getInputStream()) {
            String resourceName = getUrlPostfix();
            downloadEvent.setContentType(
                    getContentType(resourceName, downloadEvent.getResponse()));
            if (isInline()) {
                downloadEvent.inline(resourceName);
            } else {
                downloadEvent.setFileName(resourceName);
            }
            transferContent(downloadEvent, inputStream, outputStream,
                    connection.getContentLengthLong(),
                    rangeRequestsEnabled
                            ? SeekableContent.ofResource(resource, connection)
                            : null);
        } catch (RangeRequestException e) {
            // Not reported again: a cancel is not an error, and a smaller
            // range was never reported as started
            if (!e.isCancelled()) {
                throw e;
            }
        } catch (IOException ioe) {
            // Set status before output is closed (see #8740)
            downloadEvent.getResponse()
                    .setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            downloadEvent.setException(ioe);
            notifyError(downloadEvent, ioe);
            throw ioe;
        }
    }

    /**
     * Enables answering byte range requests, which media players use to seek
     * and browsers use to resume a download. Safari does not play audio or
     * video without them.
     * <p>
     * Each range is read by skipping the resource up to its start. For a file
     * on disk that is a seek, but an entry of a packaged jar is usually
     * compressed and has to be inflated from its start for every range, which
     * is costly for large media that a player fetches in many small ranges.
     *
     * @return this instance for method chaining
     */
    public ClassDownloadHandler enableRangeRequests() {
        rangeRequestsEnabled = true;
        return this;
    }

    /**
     * Returns whether byte range requests are answered.
     *
     * @return {@code true} if byte range requests are answered
     * @see #enableRangeRequests()
     */
    public boolean isRangeRequestsEnabled() {
        return rangeRequestsEnabled;
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

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

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.communication.TransferUtil;

/**
 * Abstract class for common methods used in pre-made download handlers.
 *
 * @param <R>
 *            the type of the subclass implementing this abstract class
 * @since 24.8
 */
public abstract class AbstractDownloadHandler<R extends AbstractDownloadHandler>
        extends TransferProgressAwareHandler<DownloadEvent, R>
        implements DownloadHandler {

    // Content-Disposition: attachment by default
    private boolean inline = false;

    @Override
    protected TransferContext getTransferContext(DownloadEvent transferEvent) {
        return new TransferContext(transferEvent.getRequest(),
                transferEvent.getResponse(), transferEvent.getSession(),
                transferEvent.getFileName(), transferEvent.getOwningElement(),
                transferEvent.getContentLength(), transferEvent.getException());
    }

    protected String getContentType(String fileName, VaadinResponse response) {
        return Optional.ofNullable(response.getService().getMimeType(fileName))
                .orElse("application/octet-stream");
    }

    /**
     * Sets this download content to be displayed inside the Web page, or as the
     * Web page, e.g. as an image or inside an iframe.
     * <p>
     * Implementations of this class should ensure that the
     * 'Content-Disposition' attribute is 'inline', if this method is called.
     *
     * @return this instance for method chaining
     */
    public R inline() {
        inline = true;
        return (R) this;
    }

    /**
     * Returns if the download content to be displayed inside the Web page or
     * downloaded as a file.
     *
     * @return true if the content is to be displayed inline, false if it is to
     *         be downloaded as a file
     */
    public boolean isInline() {
        return inline;
    }

    /**
     * Writes the content to the response and notifies the transfer progress
     * listeners.
     *
     * @param downloadEvent
     *            the download event
     * @param inputStream
     *            the content, positioned at its first byte
     * @param outputStream
     *            the response output stream
     * @param contentLength
     *            the length of the content, or {@code -1} if unknown
     * @throws IOException
     *             if reading or writing the content fails
     */
    void transferContent(DownloadEvent downloadEvent, InputStream inputStream,
            OutputStream outputStream, long contentLength) throws IOException {
        downloadEvent.setContentLength(contentLength);
        TransferUtil.transfer(inputStream, outputStream,
                getTransferContext(downloadEvent), getListeners());
    }

    /**
     * Writes the content to the response like
     * {@link #transferContent(DownloadEvent, InputStream, OutputStream, long)},
     * but answers a {@code Range} request header when the content is a file.
     * <p>
     * Media players rely on range requests to seek: without
     * {@code Accept-Ranges} and {@code 206 Partial Content}, a browser cannot
     * jump ahead of what it has downloaded. Ranges are only served for a file,
     * whose length is known and which can be read from any position without
     * reading the bytes before it. The range is written the same way as for a
     * static file. A request with an {@code If-Range} condition gets the whole
     * content, since no validator is sent that the condition could match.
     * <p>
     * A partial response is not reported to the transfer progress listeners: a
     * media player sends many overlapping, often cancelled range requests,
     * which do not describe a download of the content. Errors while writing it
     * are logged, as for static files.
     *
     * @param downloadEvent
     *            the download event
     * @param inputStream
     *            the content, positioned at its first byte
     * @param outputStream
     *            the response output stream
     * @param contentLength
     *            the length of the content, or {@code -1} if unknown
     * @param contentUrl
     *            the URL the content was read from, ranges are served only if
     *            it is a {@code file:} URL
     * @throws IOException
     *             if reading or writing the whole content fails
     */
    void transferContent(DownloadEvent downloadEvent, InputStream inputStream,
            OutputStream outputStream, long contentLength, URL contentUrl)
            throws IOException {
        if ("file".equals(contentUrl.getProtocol()) && contentLength >= 0
                && downloadEvent
                        .getResponse() instanceof HttpServletResponse response) {
            VaadinRequest request = downloadEvent.getRequest();
            String range = request.getHeader("Range");
            if (range != null && request.getHeader("If-Range") == null
                    && writeRange(range, contentUrl, contentLength, response)) {
                return;
            }
            response.setHeader("Accept-Ranges", "bytes");
        }
        transferContent(downloadEvent, inputStream, outputStream,
                contentLength);
    }

    private boolean writeRange(String range, URL contentUrl, long contentLength,
            HttpServletResponse response) {
        try {
            return ResponseWriter.writeRangeResponse(range, contentUrl,
                    contentLength, response);
        } catch (IOException e) {
            LoggerFactory.getLogger(AbstractDownloadHandler.class)
                    .debug("Error writing a range of {}", contentUrl, e);
            return true;
        }
    }
}

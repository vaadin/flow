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
import java.util.Optional;

import com.vaadin.flow.internal.streams.ByteRangeUtil;
import com.vaadin.flow.internal.streams.ByteRangeUtil.ByteRange;
import com.vaadin.flow.server.HttpStatusCode;
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
     * Writes the content to the response, sending only the requested part when
     * the request asks for a single byte range.
     * <p>
     * Media players rely on range requests to seek: without
     * {@code Accept-Ranges} and {@code 206 Partial Content}, a browser cannot
     * jump ahead of what it has downloaded. Ranges are only served when the
     * content length is known. A request with several ranges, a malformed range
     * or an {@code If-Range} condition gets the whole content, which RFC 9110
     * allows; the handlers send no validator that {@code If-Range} could match.
     *
     * @param downloadEvent
     *            the download event
     * @param inputStream
     *            the content, positioned at its first byte
     * @param outputStream
     *            the response output stream
     * @param contentLength
     *            the length of the whole content, or {@code -1} if unknown
     * @throws IOException
     *             if reading or writing the content fails
     */
    void transferContent(DownloadEvent downloadEvent, InputStream inputStream,
            OutputStream outputStream, long contentLength) throws IOException {
        if (contentLength >= 0) {
            downloadEvent.getResponse().setHeader("Accept-Ranges", "bytes");
        }
        Optional<ByteRange> range = ByteRangeUtil
                .parseRange(downloadEvent.getRequest(), contentLength);
        if (range.isPresent()) {
            transferRange(downloadEvent, inputStream, outputStream, range.get(),
                    contentLength);
        } else {
            downloadEvent.setContentLength(contentLength);
            TransferUtil.transfer(inputStream, outputStream,
                    getTransferContext(downloadEvent), getListeners());
        }
    }

    private void transferRange(DownloadEvent downloadEvent,
            InputStream inputStream, OutputStream outputStream, ByteRange range,
            long contentLength) throws IOException {
        VaadinResponse response = downloadEvent.getResponse();
        if (range.start() >= contentLength) {
            response.setStatus(
                    HttpStatusCode.REQUESTED_RANGE_NOT_SATISFIABLE.getCode());
            response.setHeader("Content-Range", "bytes */" + contentLength);
            downloadEvent.setContentLength(0);
            return;
        }
        response.setStatus(HttpStatusCode.PARTIAL_CONTENT.getCode());
        response.setHeader("Content-Range", "bytes " + range.start() + "-"
                + range.end() + "/" + contentLength);
        downloadEvent.setContentLength(range.length());
        inputStream.skipNBytes(range.start());
        TransferUtil.transfer(ByteRangeUtil.limit(inputStream, range.length()),
                outputStream, getTransferContext(downloadEvent),
                getListeners());
    }
}

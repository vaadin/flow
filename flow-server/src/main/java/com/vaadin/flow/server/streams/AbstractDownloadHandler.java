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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.flow.server.HttpStatusCode;
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
     * reading the bytes before it. The ranges are parsed and written the same
     * way as for a static file.
     * <p>
     * The response carries a strong {@code ETag} derived from the modification
     * time and length of the file. A request whose {@code If-Range} does not
     * match it gets the whole content, so a client cannot join parts of a file
     * that was replaced between its requests.
     * <p>
     * A range that covers the whole content, such as the {@code bytes=0-} that
     * media elements send first, is reported to the transfer progress listeners
     * like a normal transfer. A smaller range is not reported as started,
     * progressed or completed: a media player sends many overlapping, often
     * cancelled range requests, which do not describe a download of the
     * content.
     * <p>
     * A range request that the client cancels, as media players do on every
     * seek, is only logged, as for static files: it is not reported as an
     * error, and a cancelled whole-content range is not reported as completed
     * either. A failure to read the content, such as a file shorter than its
     * length, is propagated like for any other transfer. If nothing has been
     * sent yet, the response becomes an empty server error without the range
     * headers.
     *
     * @param downloadEvent
     *            the download event
     * @param inputStream
     *            the content, positioned at its first byte
     * @param outputStream
     *            the response output stream
     * @param contentLength
     *            the length of the content, or {@code -1} if unknown
     * @param file
     *            the file the content is read from, or {@code null} if it is
     *            not a file, in which case ranges are not served
     * @throws IOException
     *             if reading or writing the content fails
     */
    void transferContent(DownloadEvent downloadEvent, InputStream inputStream,
            OutputStream outputStream, long contentLength, File file)
            throws IOException {
        if (file == null || contentLength < 0 || !(downloadEvent
                .getResponse() instanceof HttpServletResponse response)) {
            transferContent(downloadEvent, inputStream, outputStream,
                    contentLength);
            return;
        }
        response.setHeader("Accept-Ranges", "bytes");
        String etag = createETag(file, contentLength);
        if (etag != null) {
            response.setHeader("ETag", etag);
        }
        VaadinRequest request = downloadEvent.getRequest();
        String range = request.getHeader("Range");
        String ifRange = request.getHeader("If-Range");
        List<Pair<Long, Long>> ranges = range == null
                || (ifRange != null && !ifRange.equals(etag)) ? null
                        : ResponseWriter.parseRanges(range, contentLength,
                                file);
        if (ranges == null) {
            transferContent(downloadEvent, inputStream, outputStream,
                    contentLength);
            return;
        }
        String contentType = response.getContentType();
        ClientResponse clientResponse = new ClientResponse(response);
        try {
            if (ranges.size() == 1 && ranges.get(0).getFirst() == 0
                    && ranges.get(0).getSecond() == contentLength - 1) {
                response.setStatus(HttpStatusCode.PARTIAL_CONTENT.getCode());
                response.setHeader("Content-Range",
                        "bytes 0-" + (contentLength - 1) + "/" + contentLength);
                transferContent(downloadEvent, inputStream,
                        clientResponse.track(outputStream), contentLength);
            } else {
                ResponseWriter.writeRanges(ranges, contentLength, inputStream,
                        file.toURI().toURL(), clientResponse);
                // Send the body now so that a client that is gone fails here
                // and not when the handler closes the stream
                clientResponse.flushBuffer();
            }
        } catch (IOException e) {
            if (clientResponse.clientGone) {
                // Media players cancel range requests on every seek. Like for
                // static files, that is not an error of the download.
                LoggerFactory.getLogger(AbstractDownloadHandler.class).debug(
                        "Range request for {} cancelled by the client", file,
                        e);
                clientResponse.closeQuietly();
                return;
            }
            if (!response.isCommitted()) {
                // Nothing sent yet: make the response a server error instead
                // of an empty 206 or 200 when the handler closes the stream,
                // keeping the headers that were not set for the range
                response.resetBuffer();
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                response.setHeader("Content-Range", null);
                response.setHeader("Transfer-Encoding", null);
                response.setHeader("ETag", null);
                response.setContentType(contentType);
                response.setContentLengthLong(0);
            }
            throw e;
        }
    }

    /**
     * Returns the file a resource URL points to, or {@code null} if it is not a
     * file on disk, for example an entry in a jar or war.
     */
    static File toFile(URL resource) {
        if (!"file".equals(resource.getProtocol())) {
            return null;
        }
        try {
            return new File(resource.toURI());
        } catch (URISyntaxException | IllegalArgumentException e) {
            LoggerFactory.getLogger(AbstractDownloadHandler.class).debug(
                    "Resource {} is not a file, ranges are not served",
                    resource, e);
            return null;
        }
    }

    private static String createETag(File file, long contentLength) {
        long lastModified = file.lastModified();
        if (lastModified == 0) {
            return null;
        }
        return "\"" + Long.toHexString(lastModified) + "-"
                + Long.toHexString(contentLength) + "\"";
    }

    /**
     * Response wrapper that records whether writing to the client failed, which
     * means that the client went away, unlike a failure to read the content.
     */
    private static final class ClientResponse
            extends HttpServletResponseWrapper {
        private boolean clientGone;
        private OutputStream delegate;
        private ServletOutputStream outputStream;

        private ClientResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                track(super.getOutputStream());
            }
            return outputStream;
        }

        @Override
        public void flushBuffer() throws IOException {
            try {
                super.flushBuffer();
            } catch (IOException e) {
                clientGone = true;
                throw e;
            }
        }

        private OutputStream track(OutputStream output) {
            delegate = output;
            outputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    try {
                        delegate.write(b);
                    } catch (IOException e) {
                        clientGone = true;
                        throw e;
                    }
                }

                @Override
                public void write(byte[] b, int off, int len)
                        throws IOException {
                    try {
                        delegate.write(b, off, len);
                    } catch (IOException e) {
                        clientGone = true;
                        throw e;
                    }
                }

                @Override
                public void flush() throws IOException {
                    try {
                        delegate.flush();
                    } catch (IOException e) {
                        clientGone = true;
                        throw e;
                    }
                }

                @Override
                public boolean isReady() {
                    return !(delegate instanceof ServletOutputStream servlet)
                            || servlet.isReady();
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                    if (delegate instanceof ServletOutputStream servlet) {
                        servlet.setWriteListener(writeListener);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }
            };
            return outputStream;
        }

        private void closeQuietly() {
            if (delegate == null) {
                return;
            }
            // Close the stream here, where a failure on the lost connection
            // is expected, rather than first in the handler
            try {
                delegate.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(AbstractDownloadHandler.class)
                        .debug("Error closing a cancelled range response", e);
            }
        }
    }
}

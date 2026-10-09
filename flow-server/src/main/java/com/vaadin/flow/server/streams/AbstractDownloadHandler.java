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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
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
     * but answers a {@code Range} request header when the content is seekable.
     * <p>
     * Media players rely on range requests to seek: without
     * {@code Accept-Ranges} and {@code 206 Partial Content}, a browser cannot
     * jump ahead of what it has downloaded, and Safari does not play audio or
     * video at all. A download that was cut off can only be resumed with a
     * range request too. Ranges are only served for content that is the same on
     * every request and whose length is known. The stream is skipped to the
     * start of a range, which is a seek for a file and reads the preceding
     * bytes for other streams, such as an entry of a compressed jar. Large
     * sequential content is instead extracted to a temporary file once, when a
     * range smaller than the content is first requested, and served from there.
     * The ranges are parsed and written the same way as for a static file.
     * <p>
     * The response carries the strong {@code ETag} of the content, if it has
     * one. A request whose {@code If-Range} does not match it gets the whole
     * content, so a client cannot join parts of content that changed between
     * its requests.
     * <p>
     * A range that covers the whole content, such as the {@code bytes=0-} that
     * media elements send first, is reported to the transfer progress listeners
     * like a normal transfer. A smaller range is not reported as started,
     * progressed or completed: a media player sends many overlapping, often
     * cancelled range requests, which do not describe a download of the
     * content.
     * <p>
     * A smaller range that the client cancels, as media players do on every
     * seek, is only logged, as for static files. A cancelled whole-content
     * range, which was reported as started, is reported as an error, like a
     * cancelled download without a {@code Range} header. In both cases a
     * {@link RangeRequestException} is thrown, which the handler ends the
     * request on without reporting it again.
     * <p>
     * A failure to read the content, such as content shorter than its length or
     * a resource that cannot be opened again for multipart ranges, is
     * propagated. For a whole-content range it is reported to the listeners
     * like for any other transfer. A smaller range, which the listeners never
     * saw start, is not reported to them: a {@link RangeRequestException} is
     * thrown, which the handler propagates without notifying the listeners. If
     * nothing has been sent yet, the response becomes an empty server error
     * without the range headers.
     *
     * @param downloadEvent
     *            the download event
     * @param inputStream
     *            the content, positioned at its first byte
     * @param outputStream
     *            the response output stream
     * @param contentLength
     *            the length of the content, or {@code -1} if unknown, in which
     *            case ranges are not served
     * @param content
     *            the seekable content the stream reads, or {@code null} if it
     *            is not seekable, in which case ranges are not served
     * @throws RangeRequestException
     *             if the client cancelled a range request, or reading a range
     *             smaller than the content failed
     * @throws IOException
     *             if reading or writing the content fails
     */
    void transferContent(DownloadEvent downloadEvent, InputStream inputStream,
            OutputStream outputStream, long contentLength,
            SeekableContent content) throws IOException {
        if (content == null || contentLength < 0 || !(downloadEvent
                .getResponse() instanceof HttpServletResponse response)) {
            transferContent(downloadEvent, inputStream, outputStream,
                    contentLength);
            return;
        }
        response.setHeader("Accept-Ranges", "bytes");
        String etag = content.eTag();
        if (etag != null) {
            response.setHeader("ETag", etag);
        }
        VaadinRequest request = downloadEvent.getRequest();
        String range = request.getHeader("Range");
        String ifRange = request.getHeader("If-Range");
        List<Pair<Long, Long>> ranges = range == null
                || (ifRange != null && !ifRange.equals(etag)) ? null
                        : ResponseWriter.parseRanges(range, contentLength,
                                content.resource());
        if (ranges != null && content.url() == null && !isAscending(ranges)) {
            // Answering would need the content from the start again, which
            // only a URL can provide. A server may ignore a Range header.
            LoggerFactory.getLogger(AbstractDownloadHandler.class).debug(
                    "Ignoring ranges not in ascending order for {}",
                    content.resource());
            ranges = null;
        }
        if (ranges == null) {
            transferContent(downloadEvent, inputStream, outputStream,
                    contentLength);
            return;
        }
        String contentType = response.getContentType();
        ClientOutputStream clientOutput = new ClientOutputStream(outputStream);
        boolean wholeContent = ranges.size() == 1
                && ranges.get(0).getFirst() == 0
                && ranges.get(0).getSecond() == contentLength - 1;
        try {
            if (wholeContent) {
                response.setStatus(HttpStatusCode.PARTIAL_CONTENT.getCode());
                response.setHeader("Content-Range",
                        "bytes 0-" + (contentLength - 1) + "/" + contentLength);
                transferContent(downloadEvent, inputStream, clientOutput,
                        contentLength);
            } else {
                Path extracted = content.sequential() ? ExtractedResources
                        .extract(downloadEvent.getRequest().getService(),
                                content, contentLength)
                        : null;
                if (extracted == null) {
                    ResponseWriter.writeRanges(ranges, contentLength,
                            inputStream, content.url(), clientOutput, response);
                } else {
                    try (InputStream extractedStream = new FileInputStream(
                            extracted.toFile())) {
                        ResponseWriter.writeRanges(ranges, contentLength,
                                extractedStream, extracted.toUri().toURL(),
                                clientOutput, response);
                    }
                }
                // Send the body now so that a client that is gone fails here
                // and not when the handler closes the stream
                clientOutput.flush();
            }
        } catch (IOException e) {
            if (clientOutput.failed) {
                // Media players cancel range requests on every seek. Like for
                // static files, that is not an error of the content.
                LoggerFactory.getLogger(AbstractDownloadHandler.class).debug(
                        "Range request for {} cancelled by the client",
                        content.resource(), e);
                if (wholeContent) {
                    // Reported as started, so it ends like any cancelled
                    // download
                    downloadEvent.setException(e);
                    notifyError(downloadEvent, e);
                }
                throw new RangeRequestException(e, true);
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
            // Listeners only see a range that covers the whole content
            throw wholeContent ? e : new RangeRequestException(e, false);
        }
    }

    private static boolean isAscending(List<Pair<Long, Long>> ranges) {
        long position = 0;
        for (Pair<Long, Long> range : ranges) {
            if (range.getFirst() < position) {
                return false;
            }
            position = range.getSecond() + 1;
        }
        return true;
    }

    /**
     * Content that ranges can be served from, because it is the same on every
     * request.
     *
     * @param resource
     *            what the content is read from, for log messages
     * @param eTag
     *            the strong entity tag of the content, or {@code null} if it
     *            has none
     * @param url
     *            the URL to read the content again from, for multipart ranges
     *            that are not in ascending order, or {@code null} if it cannot
     *            be read again, in which case such ranges are ignored
     * @param sequential
     *            whether the content can only be read from its start, so that
     *            it is extracted to a temporary file to serve a range from
     */
    record SeekableContent(Object resource, String eTag, URL url,
            boolean sequential) {

        /**
         * The smallest resource that is extracted to a temporary file to serve
         * ranges from when it cannot be read from a position. A smaller one is
         * read from its start for every range, which costs little.
         */
        static final long MIN_EXTRACTED_LENGTH = 4L * 1024 * 1024;

        /**
         * Describes a file, tagged with its modification time and length.
         */
        static SeekableContent ofFile(File file) throws IOException {
            return new SeekableContent(file,
                    createETag(file.lastModified(), file.length()),
                    file.toURI().toURL(), false);
        }

        /**
         * Describes a resource read through the given connection, such as a
         * file, or an entry in a jar or war. A jar entry is tagged with its
         * checksum, because reproducible builds fix the modification time of
         * every entry. Other resources are tagged with their modification time.
         * <p>
         * A resource larger than {@link #MIN_EXTRACTED_LENGTH} is sequential
         * unless it is a file or an uncompressed jar entry. Any other resource
         * may be a compressed entry of an archive, possibly one that the
         * servlet container opens its own way, such as a packed war, which
         * cannot be read from a position without inflating everything before
         * it. A media player fetches a large resource in many small ranges, so
         * serving them from the resource would inflate it over and over again.
         */
        static SeekableContent ofResource(URL resource,
                URLConnection connection) throws IOException {
            long length = connection.getContentLengthLong();
            String eTag;
            boolean seekable;
            if (connection instanceof JarURLConnection jarConnection) {
                JarEntry entry = jarConnection.getJarEntry();
                long crc = entry.getCrc();
                eTag = crc < 0 ? null : createETag(crc, length);
                seekable = entry.getMethod() == ZipEntry.STORED;
            } else {
                eTag = createETag(connection.getLastModified(), length);
                seekable = "file".equals(resource.getProtocol());
            }
            return new SeekableContent(resource, eTag, resource,
                    !seekable && length > MIN_EXTRACTED_LENGTH);
        }

        private static String createETag(long version, long length) {
            if (version == 0) {
                return null;
            }
            return "\"" + Long.toHexString(version) + "-"
                    + Long.toHexString(length) + "\"";
        }
    }

    /**
     * Copies of sequential resources in a temporary directory, which ranges are
     * served from. A resource is extracted once for all sessions, when a range
     * is first requested from it, and the copies are deleted when the service
     * is destroyed. A resource does not change while the application is
     * deployed, and its entity tag is part of the key in case it does.
     */
    static final class ExtractedResources {
        private final Map<String, CompletableFuture<Path>> files = new ConcurrentHashMap<>();
        private Path directory;

        /**
         * Returns a copy of the content in a temporary file, extracting it if
         * it has not been extracted yet. Requests for a resource that is being
         * extracted wait for the extraction.
         *
         * @return the copy, or {@code null} if there is no service to tie the
         *         copy to or extracting the content failed, in which case the
         *         content is read from its start
         */
        static Path extract(VaadinService service, SeekableContent content,
                long contentLength) {
            VaadinContext context = service == null ? null
                    : service.getContext();
            if (context == null) {
                return null;
            }
            ExtractedResources extractedResources = context
                    .getAttribute(ExtractedResources.class, () -> {
                        ExtractedResources created = new ExtractedResources();
                        service.addServiceDestroyListener(event -> {
                            context.removeAttribute(ExtractedResources.class);
                            created.deleteFiles();
                        });
                        return created;
                    });
            return extractedResources.extract(content, contentLength);
        }

        private Path extract(SeekableContent content, long contentLength) {
            String key = content.url() + " " + content.eTag();
            CompletableFuture<Path> extraction = new CompletableFuture<>();
            CompletableFuture<Path> existing = files.putIfAbsent(key,
                    extraction);
            if (existing != null) {
                Path file = existing.join();
                if (file == null || Files.isRegularFile(file)) {
                    return file;
                }
                // Removed by a cleanup of the temporary directory
                files.remove(key, existing);
                return extract(content, contentLength);
            }
            Path file = null;
            try {
                file = copy(content.url(), contentLength);
            } catch (IOException | RuntimeException e) {
                LoggerFactory.getLogger(AbstractDownloadHandler.class).warn(
                        "Failed to extract {} to a temporary file, ranges are read from its start",
                        content.resource(), e);
            } finally {
                // Also on an error, so that waiting requests do not hang
                if (file == null) {
                    files.remove(key, extraction);
                }
                extraction.complete(file);
            }
            return file;
        }

        private Path copy(URL url, long contentLength) throws IOException {
            Path file = Files.createTempFile(getDirectory(), "resource",
                    ".tmp");
            file.toFile().deleteOnExit();
            try (InputStream inputStream = url.openStream()) {
                long copied = Files.copy(inputStream, file,
                        StandardCopyOption.REPLACE_EXISTING);
                if (copied != contentLength) {
                    throw new IOException("Expected " + contentLength
                            + " bytes but extracted " + copied);
                }
                return file;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(file);
                throw e;
            }
        }

        private synchronized Path getDirectory() throws IOException {
            if (directory == null || !Files.isDirectory(directory)) {
                directory = Files.createTempDirectory("vaadin-downloads");
                directory.toFile().deleteOnExit();
            }
            return directory;
        }

        private synchronized void deleteFiles() {
            files.clear();
            FileIOUtils.deleteQuietly(directory);
        }
    }

    /**
     * Thrown when a range request fails in a way that the handler must not
     * report to the transfer progress listeners, because it has already been
     * reported or the listeners never saw the range start. A handler ends the
     * request quietly if the client cancelled it, and otherwise propagates this
     * exception. Closing the response stream afterwards may fail too, which is
     * then suppressed by this exception.
     */
    static final class RangeRequestException extends IOException {
        private final boolean cancelled;

        private RangeRequestException(IOException cause, boolean cancelled) {
            super(cancelled ? "Range request cancelled by the client"
                    : "Range request failed", cause);
            this.cancelled = cancelled;
        }

        /**
         * Returns whether the client cancelled the request, which is not an
         * error.
         *
         * @return {@code true} if the client cancelled the request
         */
        boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * Records whether writing to the client failed, which means that the client
     * went away, unlike a failure to read the content.
     */
    private static final class ClientOutputStream extends FilterOutputStream {
        private boolean failed;

        private ClientOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            try {
                out.write(b);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                out.write(b, off, len);
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }

        @Override
        public void flush() throws IOException {
            try {
                out.flush();
            } catch (IOException e) {
                failed = true;
                throw e;
            }
        }
    }
}

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
package com.vaadin.flow.internal;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;

import static com.vaadin.flow.server.Constants.VAADIN_BUILD_FILES_PATH;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;

/**
 * The class that handles writing the response data into the response.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ResponseWriter implements Serializable {
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile(
            "^bytes=((\\d*-\\d*\\s*,\\s*)*\\d*-\\d*\\s*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BYTE_RANGE_PATTERN = Pattern
            .compile("(\\d*)-(\\d*)");

    /**
     * Maximum number of ranges accepted in a single Range header. Remaining
     * ranges will be ignored.
     */
    private static final int MAX_RANGE_COUNT = 16;

    /**
     * Maximum number of overlapping ranges allowed. The request will be denied
     * if above this threshold.
     */
    private static final int MAX_OVERLAPPING_RANGE_COUNT = 2;

    private static final ConcurrentHashMap<String, Integer> utf8EncodingByDefault = new ConcurrentHashMap<>();
    static {
        utf8EncodingByDefault.put("application/json", 1);
        utf8EncodingByDefault.put("application/javascript", 1);
        utf8EncodingByDefault.put("application/xml", 1);
    }

    private final int bufferSize;
    private final boolean brotliEnabled;

    /**
     * Create a response writer with the given deployment configuration.
     *
     * @param deploymentConfiguration
     *            the deployment configuration to use, not <code>null</code>
     * @since 1.3
     */
    public ResponseWriter(DeploymentConfiguration deploymentConfiguration) {
        this(DEFAULT_BUFFER_SIZE, deploymentConfiguration.isBrotli());
    }

    private ResponseWriter(int bufferSize, boolean brotliEnabled) {
        this.brotliEnabled = brotliEnabled;
        this.bufferSize = bufferSize;
    }

    /**
     * Writes the contents and content type (if available) of the given
     * resourceUrl to the response.
     * <p>
     * WARNING: note that this should not be used for a {@code resourceUrl} that
     * represents a directory! For security reasons, the directory contents
     * should not be ever written into the {@code response}, and the
     * implementation which is used for setting the content length relies on
     * {@link URLConnection#getContentLengthLong()} method which returns
     * incorrect values for directories.
     *
     * @param filenameWithPath
     *            the name of the file being sent
     * @param resourceUrl
     *            the URL to the file, reported by the servlet container
     * @param request
     *            the request object to read from
     * @param response
     *            the response object to write to
     * @throws IOException
     *             if the servlet container threw an exception while locating
     *             the resource
     */
    public void writeResponseContents(String filenameWithPath, URL resourceUrl,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        writeContentType(filenameWithPath, request, response);

        URL url = null;
        URLConnection connection = null;
        InputStream dataStream = null;

        if (brotliEnabled && acceptsBrotliResource(request)) {
            String brotliFilenameWithPath = filenameWithPath + ".br";
            try {
                url = getPrecompressedResource(request, resourceUrl,
                        brotliFilenameWithPath, ".br");
                if (url != null) {
                    connection = url.openConnection();
                    dataStream = connection.getInputStream();
                    response.setHeader("Content-Encoding", "br");
                }
            } catch (Exception e) {
                getLogger().debug(
                        "Unexpected exception looking for Brotli resource {}",
                        brotliFilenameWithPath, e);
            }
        }

        if (dataStream == null && acceptsGzippedResource(request)) {
            // try to serve a gzipped version if available
            String gzippedFilenameWithPath = filenameWithPath + ".gz";
            try {
                url = getPrecompressedResource(request, resourceUrl,
                        gzippedFilenameWithPath, ".gz");
                if (url != null) {
                    connection = url.openConnection();
                    dataStream = connection.getInputStream();
                    response.setHeader("Content-Encoding", "gzip");
                }
            } catch (Exception e) {
                getLogger().debug(
                        "Unexpected exception looking for gzipped resource {}",
                        gzippedFilenameWithPath, e);
            }
        }

        if (dataStream == null) {
            // compressed resource not available, get non compressed
            url = resourceUrl;
            connection = resourceUrl.openConnection();
            dataStream = connection.getInputStream();
        } else {
            response.setHeader("Vary", "Accept-Encoding");
        }

        try {
            final long contentLength = connection.getContentLengthLong();
            String range = request.getHeader("Range");
            List<Pair<Long, Long>> ranges = range == null ? null
                    : parseRanges(range, contentLength, url);
            if (ranges != null) {
                writeRangeContents(ranges, contentLength, dataStream, url,
                        response);
            } else {
                if (0 <= contentLength) {
                    setContentLength(response, contentLength);
                }
                writeStream(response.getOutputStream(), dataStream,
                        Long.MAX_VALUE);
            }
        } catch (IOException e) {
            getLogger().debug("Error writing static file to user", e);
        } finally {
            closeStream(dataStream);
        }
    }

    private static void closeStream(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            getLogger().debug("Error closing input stream for resource", e);
        }
    }

    /**
     * Parses the value of a {@code Range} request header the same way as
     * {@link #writeResponseContents} does for static files.
     * <p>
     * Returns {@code null} if the header should be ignored and the whole
     * content sent, which RFC 9110 allows: when it is not a valid {@code bytes}
     * range, or the length of the resource is unknown. Otherwise returns the
     * requested ranges as inclusive first and last byte positions, clamped to
     * the length. Ranges that start past the end are dropped, so an empty list
     * means that none of them is satisfiable. Ranges beyond the limits on count
     * and overlap are dropped as well.
     *
     * @param range
     *            the value of the {@code Range} request header, not
     *            {@code null}
     * @param resourceLength
     *            the length of the resource, or {@code -1} if unknown
     * @param resource
     *            the resource, used in log messages
     * @return the ranges to send, or {@code null} if the header is ignored
     */
    public static List<Pair<Long, Long>> parseRanges(String range,
            long resourceLength, Object resource) {
        Matcher headerMatcher = RANGE_HEADER_PATTERN.matcher(range.trim());
        if (resourceLength < 0 || !headerMatcher.matches()) {
            getLogger().debug("ignoring range '{}' for resource '{}'", range,
                    resource);
            return null;
        }
        Matcher rangeMatcher = BYTE_RANGE_PATTERN
                .matcher(headerMatcher.group(1));

        List<Pair<Long, Long>> ranges = new ArrayList<>();
        boolean limitReached = false;
        while (rangeMatcher.find()) {
            String startGroup = rangeMatcher.group(1);
            String endGroup = rangeMatcher.group(2);
            long start;
            long end;
            try {
                if (startGroup.isEmpty()) {
                    // suffix range: the last N bytes
                    long suffixLength = Long.parseLong(endGroup);
                    start = Math.max(0L, resourceLength - suffixLength);
                    end = suffixLength == 0 ? -1L : resourceLength - 1;
                } else {
                    start = Long.parseLong(startGroup);
                    end = endGroup.isEmpty() ? Long.MAX_VALUE
                            : Long.parseLong(endGroup);
                }
            } catch (NumberFormatException e) {
                getLogger().info("received a malformed range '{}'",
                        rangeMatcher.group());
                return null;
            }
            if (end < start && !startGroup.isEmpty()) {
                getLogger().info(
                        "received an illegal range '{}' for resource '{}'",
                        rangeMatcher.group(), resource);
                return null;
            }
            end = Math.min(end, resourceLength - 1);
            if (limitReached || start > end) {
                // past the limits, or not satisfiable
                continue;
            }
            ranges.add(new Pair<>(start, end));
            if (!verifyRangeLimits(ranges)) {
                ranges.remove(ranges.size() - 1);
                limitReached = true;
                getLogger().info(
                        "serving only {} ranges for resource '{}' even though more were requested",
                        ranges.size(), resource);
            }
        }
        return ranges;
    }

    /**
     * Writes a range response for ranges returned by
     * {@link #parseRanges(String, long, Object)}, the same way as
     * {@link #writeResponseContents} does for static files: {@code 206 Partial
     * Content} with a single range or a {@code multipart/byteranges} body, or
     * {@code 416 Range Not Satisfiable} if the list is empty.
     * <p>
     * The content is read from the given stream, skipping to each range. The
     * resource is opened again from its URL only if the ranges are not in
     * ascending order. The status and headers of a single range are set only
     * after skipping to its start, so content shorter than its length fails
     * before the response is committed.
     *
     * @param ranges
     *            the ranges to send, not {@code null}
     * @param resourceLength
     *            the length of the resource
     * @param dataStream
     *            the content of the resource, positioned at its first byte, not
     *            {@code null}; not closed by this method
     * @param resourceUrl
     *            the URL of the resource, to read it again for ranges that are
     *            not in ascending order, not {@code null}
     * @param response
     *            the response to write to, not {@code null}
     * @throws IOException
     *             if reading the resource or writing the response fails, or the
     *             resource ends before a range does
     */
    public static void writeRanges(List<Pair<Long, Long>> ranges,
            long resourceLength, InputStream dataStream, URL resourceUrl,
            HttpServletResponse response) throws IOException {
        new ResponseWriter(DEFAULT_BUFFER_SIZE, false).writeRangeContents(
                ranges, resourceLength, dataStream, resourceUrl, response);
    }

    /**
     * Handle a "Header:" request. The handling logic is splits on single or
     * multiple ranges: for a single range, send a regular response with
     * Content-Length; for multiple ranges, send a "Content-Type:
     * multipart/byteranges" response. If the byte ranges are satisfiable, the
     * response code is 206, otherwise it is 416. See e.g.
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests for
     * protocol details.
     */
    private void writeRangeContents(List<Pair<Long, Long>> ranges,
            long resourceLength, InputStream dataStream, URL resourceURL,
            HttpServletResponse response) throws IOException {
        response.setHeader("Accept-Ranges", "bytes");

        if (ranges.isEmpty()) {
            getLogger().info(
                    "received an unsatisfiable range for resource '{}'",
                    resourceURL);
            response.setContentLengthLong(0L);
            response.setHeader("Content-Range", "bytes */" + resourceLength);
            response.setStatus(416); // Range Not Satisfiable
            return;
        }

        if (ranges.size() == 1) {
            // single range: calculate Content-Length
            long start = ranges.get(0).getFirst();
            long end = ranges.get(0).getSecond();
            dataStream.skipNBytes(start);

            response.setStatus(206);
            setContentLength(response, end - start + 1);
            response.setHeader("Content-Range",
                    createContentRangeHeader(start, end, resourceLength));
            writeStream(response.getOutputStream(), dataStream,
                    end - start + 1);
        } else {
            response.setStatus(206);
            writeMultipartRangeContents(ranges, resourceLength, dataStream,
                    resourceURL, response);
        }
    }

    /**
     * Write a multi-part request with MIME type "multipart/byteranges",
     * separated by boundaries and use "Transfer-Encoding: chunked" mode to
     * avoid computing "Content-Length".
     */
    private void writeMultipartRangeContents(List<Pair<Long, Long>> ranges,
            long resourceLength, InputStream dataStream, URL resourceURL,
            HttpServletResponse response) throws IOException {
        String partBoundary = UUID.randomUUID().toString();
        String mimeType = response.getContentType();
        response.setContentType(String
                .format("multipart/byteranges; boundary=%s", partBoundary));
        response.setHeader("Transfer-Encoding", "chunked");

        long position = 0L;
        InputStream currentStream = dataStream;
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            for (Pair<Long, Long> rangePair : ranges) {
                outputStream.write(String.format("\r\n--%s\r\n", partBoundary)
                        .getBytes(StandardCharsets.UTF_8));
                long start = rangePair.getFirst();
                long end = rangePair.getSecond();
                if (mimeType != null) {
                    outputStream.write(
                            String.format("Content-Type: %s\r\n", mimeType)
                                    .getBytes(StandardCharsets.UTF_8));
                }
                outputStream.write(String
                        .format("Content-Range: %s\r\n\r\n",
                                createContentRangeHeader(start, end,
                                        resourceLength))
                        .getBytes(StandardCharsets.UTF_8));

                if (position > start) {
                    // out-of-sequence range -> open new stream to the file
                    // alternative: use single stream with mark / reset
                    if (currentStream != dataStream) {
                        closeStream(currentStream);
                    }
                    currentStream = resourceURL.openStream();
                    position = 0L;
                }
                currentStream.skipNBytes(start - position);
                writeStream(outputStream, currentStream, end - start + 1);
                position = end + 1;
            }
        } finally {
            if (currentStream != dataStream) {
                closeStream(currentStream);
            }
        }
        outputStream.write(String.format("\r\n--%s", partBoundary)
                .getBytes(StandardCharsets.UTF_8));
    }

    private String createContentRangeHeader(long start, long end, long size) {
        String lengthString = size >= 0 ? Long.toString(size) : "*";
        return String.format("bytes %d-%d/%s", start, end, lengthString);
    }

    private void setContentLength(HttpServletResponse response,
            long contentLength) {
        try {
            response.setContentLengthLong(contentLength);
        } catch (Exception e) {
            getLogger().debug("Error setting the content length", e);
        }
    }

    /**
     * Returns true if the number of ranges in <code>ranges</code> is less than
     * the upper limit and the number that overlap (= have at least one byte in
     * common) with the range <code>[start, end]</code> are less than the upper
     * limit.
     */
    private static boolean verifyRangeLimits(List<Pair<Long, Long>> ranges) {
        if (ranges.size() > MAX_RANGE_COUNT) {
            getLogger().info("more than {} ranges requested", MAX_RANGE_COUNT);
            return false;
        }
        int count = 0;
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(i).getFirst() <= ranges.get(j).getSecond()
                        && ranges.get(j).getFirst() <= ranges.get(i)
                                .getSecond()) {
                    count++;
                }
            }
        }
        if (count > MAX_OVERLAPPING_RANGE_COUNT) {
            getLogger().info("more than {} overlapping ranges requested",
                    MAX_OVERLAPPING_RANGE_COUNT);
            return false;
        }
        return true;
    }

    private URL getResource(HttpServletRequest request, String resource)
            throws MalformedURLException {
        URL url = request.getServletContext().getResource(resource);
        if (url != null) {
            return url;
        } else if (resource.startsWith("/" + VAADIN_BUILD_FILES_PATH)
                && isAllowedVAADINBuildUrl(resource)) {
            url = request.getServletContext().getClassLoader().getResource(
                    VAADIN_WEBAPP_RESOURCES + resource.replaceFirst("^/", ""));
        }
        return url;
    }

    /**
     * Resolves a precompressed variant ({@code .br}/{@code .gz}) of a resource.
     * <p>
     * The compressed sibling is resolved relative to the already resolved
     * {@code resourceUrl} of the uncompressed resource, so it is located the
     * same way the original was, regardless of how the servlet container maps
     * {@link jakarta.servlet.ServletContext#getResource(String)}. This matters
     * for static resources served from the classpath {@code META-INF/resources}
     * of a packaged application (for example a Spring Boot executable jar),
     * where a path based lookup of the sibling returns {@code null} even though
     * the original resource is found. Falls back to a path based lookup so the
     * previous behavior is preserved when the sibling cannot be derived from
     * the resource URL.
     *
     * @param request
     *            the request, used for the path based fallback lookup
     * @param resourceUrl
     *            the resolved URL of the uncompressed resource
     * @param compressedFilenameWithPath
     *            the request path of the compressed resource, for the fallback
     * @param suffix
     *            the compression suffix, {@code .br} or {@code .gz}
     * @return the URL of the precompressed resource, or {@code null} if none
     */
    private URL getPrecompressedResource(HttpServletRequest request,
            URL resourceUrl, String compressedFilenameWithPath, String suffix)
            throws MalformedURLException {
        URL sibling = getCompressedSibling(resourceUrl, suffix);
        if (sibling != null) {
            return sibling;
        }
        return getResource(request, compressedFilenameWithPath);
    }

    private URL getCompressedSibling(URL resourceUrl, String suffix) {
        if (resourceUrl == null) {
            return null;
        }
        URL sibling;
        try {
            sibling = new URL(resourceUrl.toString() + suffix);
        } catch (MalformedURLException e) {
            return null;
        }
        // Only serve the sibling if it actually exists next to the resource.
        try (InputStream probe = sibling.openStream()) {
            return sibling;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Check if it is ok to serve the requested file from the classpath.
     * <p>
     * ClassLoader is applicable for use when we are in npm mode and are serving
     * from the VAADIN/build folder with no folder changes in path.
     *
     * @param filenameWithPath
     *            requested filename containing path
     * @return true if we are ok to try serving the file
     */
    private boolean isAllowedVAADINBuildUrl(String filenameWithPath) {
        // Check that we target VAADIN/build and do not have '/../'
        if (!filenameWithPath.startsWith("/" + VAADIN_BUILD_FILES_PATH)
                || filenameWithPath.contains("/../")) {
            getLogger().info("Blocked attempt to access file: {}",
                    filenameWithPath);
            return false;
        }

        return true;
    }

    private void writeStream(ServletOutputStream outputStream,
            InputStream dataStream, long count) throws IOException {
        final byte[] buffer = new byte[bufferSize];

        long bytesTotal = 0L;
        int bytes;
        while (bytesTotal < count && (bytes = dataStream.read(buffer, 0,
                (int) Long.min(bufferSize, count - bytesTotal))) >= 0) {
            outputStream.write(buffer, 0, bytes);
            bytesTotal += bytes;
        }
        if (count != Long.MAX_VALUE && bytesTotal < count) {
            // the declared Content-Length can no longer be met
            throw new EOFException("Resource ended after " + bytesTotal + " of "
                    + count + " bytes");
        }
    }

    /**
     * Returns whether it is ok to serve a gzipped version of the given
     * resource.
     * <p>
     * If this method returns true, the browser is ok with receiving a gzipped
     * version of the resource. In other cases, an uncompressed file must be
     * sent.
     *
     * @param request
     *            the request for the resource
     * @return true if the servlet should attempt to serve a gzipped version of
     *         the resource, false otherwise
     */
    protected boolean acceptsGzippedResource(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }

    /**
     * Returns whether it is ok to serve a Brotli version of the given resource.
     * <p>
     * If this method returns true, the browser is ok with receiving a Brotli
     * version of the resource. In other cases, an uncompressed or gzipped file
     * must be sent.
     *
     * @param request
     *            the request for the resource
     * @return true if the servlet should attempt to serve a Brotli version of
     *         the resource, false otherwise
     * @since 1.3
     */
    protected boolean acceptsBrotliResource(HttpServletRequest request) {
        return acceptsEncoding(request, "br");
    }

    private static boolean acceptsEncoding(HttpServletRequest request,
            String encodingName) {
        String accept = request.getHeader("Accept-Encoding");
        if (accept == null) {
            return false;
        }

        accept = accept.replace(" ", "");
        // Browser denies gzip compression if it reports
        // gzip;q=0
        //
        // Browser accepts gzip compression if it reports
        // "gzip"
        // "gzip;q=[notzero]"
        // "*"
        // "*;q=[not zero]"
        if (accept.contains(encodingName)) {
            return !isQualityValueZero(accept, encodingName);
        }
        return accept.contains("*") && !isQualityValueZero(accept, "*");
    }

    void writeContentType(String filenameWithPath, ServletRequest request,
            ServletResponse response) {
        // Set type mime type if we can determine it based on the filename
        String mimetype = request.getServletContext()
                .getMimeType(filenameWithPath);
        if (mimetype != null) {
            response.setContentType(mimetype);
            String lowerCaseMimeType = mimetype.toLowerCase(Locale.ENGLISH);
            if (!lowerCaseMimeType.contains("charset=")) {
                if (lowerCaseMimeType.startsWith("text/")
                        || utf8EncodingByDefault
                                .containsKey(lowerCaseMimeType)) {
                    response.setCharacterEncoding("utf-8");
                }
            }
        }
    }

    /**
     * Check the quality value of the encoding. If the value is zero the
     * encoding is disabled and not accepted.
     *
     * @param acceptEncoding
     *            Accept-Encoding header from request
     * @param encoding
     *            encoding to check
     * @return true if quality value is Zero
     */
    private static boolean isQualityValueZero(String acceptEncoding,
            String encoding) {
        String qPrefix = encoding + ";q=";
        int qValueIndex = acceptEncoding.indexOf(qPrefix);
        if (qValueIndex == -1) {
            return false;
        }

        // gzip;q=0.123 or gzip;q=0.123,compress...
        String qValue = acceptEncoding
                .substring(qValueIndex + qPrefix.length());
        int endOfQValue = qValue.indexOf(',');
        if (endOfQValue != -1) {
            qValue = qValue.substring(0, endOfQValue);
        }

        return Double.valueOf(0.000).equals(Double.valueOf(qValue));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ResponseWriter.class.getName());
    }
}

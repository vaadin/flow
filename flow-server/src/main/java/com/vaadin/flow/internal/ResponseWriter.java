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
package com.vaadin.flow.internal;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
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

    private static final Pattern RANGE_HEADER_PATTERN = Pattern
            .compile("^bytes=((\\d*-\\d*\\s*,\\s*)*\\d*-\\d*\\s*)$");
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
                url = getResource(request, brotliFilenameWithPath);
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
                url = getResource(request, gzippedFilenameWithPath);
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
            String range = request.getHeader("Range");
            if (range != null) {
                closeStream(dataStream);
                dataStream = null;
                writeRangeContents(range, response, url);
            } else {
                final long contentLength = connection.getContentLengthLong();
                if (0 <= contentLength) {
                    setContentLength(response, contentLength);
                }
                writeStream(response.getOutputStream(), dataStream,
                        Long.MAX_VALUE);
            }
        } catch (IOException e) {
            getLogger().debug("Error writing static file to user", e);
        } finally {
            if (dataStream != null) {
                closeStream(dataStream);
            }
        }
    }

    private void closeStream(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            getLogger().debug("Error closing input stream for resource", e);
        }
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
    private void writeRangeContents(String range, HttpServletResponse response,
            URL resourceURL) throws IOException {
        response.setHeader("Accept-Ranges", "bytes");

        URLConnection connection = resourceURL.openConnection();

        Matcher headerMatcher = RANGE_HEADER_PATTERN.matcher(range);
        if (!headerMatcher.matches()) {
            response.setContentLengthLong(0L);
            response.setStatus(416); // Range Not Satisfiable
            return;
        }
        String byteRanges = headerMatcher.group(1);

        long resourceLength = connection.getContentLengthLong();
        Matcher rangeMatcher = BYTE_RANGE_PATTERN.matcher(byteRanges);

        Stack<Pair<Long, Long>> ranges = new Stack<>();
        while (rangeMatcher.find() && ranges.size() < MAX_RANGE_COUNT) {
            String startGroup = rangeMatcher.group(1);
            String endGroup = rangeMatcher.group(2);
            if (startGroup.isEmpty() && endGroup.isEmpty()) {
                response.setContentLengthLong(0L);
                response.setStatus(416); // Range Not Satisfiable
                getLogger().info("received a malformed range: '{}'",
                        rangeMatcher.group());
                return;
            }
            long start = startGroup.isEmpty() ? 0L : Long.parseLong(startGroup);
            long end = endGroup.isEmpty() ? Long.MAX_VALUE
                    : Long.parseLong(endGroup);
            if (end < start
                    || (resourceLength >= 0 && start >= resourceLength)) {
                // illegal range -> 416
                getLogger().info(
                        "received an illegal range '{}' for resource '{}'",
                        rangeMatcher.group(), resourceURL);
                response.setContentLengthLong(0L);
                response.setStatus(416);
                return;
            }
            ranges.push(new Pair<>(start, end));

            if (!verifyRangeLimits(ranges)) {
                ranges.pop();
                getLogger().info(
                        "serving only {} ranges for resource '{}' even though more were requested",
                        ranges.size(), resourceURL);
                break;
            }
        }

        response.setStatus(206);

        if (ranges.size() == 1) {
            ServletOutputStream outputStream = response.getOutputStream();

            // single range: calculate Content-Length
            long start = ranges.get(0).getFirst();
            long end = ranges.get(0).getSecond();
            if (resourceLength >= 0) {
                end = Math.min(end, resourceLength - 1);
            }
            setContentLength(response, end - start + 1);
            response.setHeader("Content-Range",
                    createContentRangeHeader(start, end, resourceLength));

            final InputStream dataStream = connection.getInputStream();
            try {
                long skipped = dataStream.skip(start);
                assert (skipped == start);
                writeStream(outputStream, dataStream, end - start + 1);
            } finally {
                closeStream(dataStream);
            }
        } else {
            writeMultipartRangeContents(ranges, connection, response,
                    resourceURL);
        }
    }

    /**
     * Write a multi-part request with MIME type "multipart/byteranges",
     * separated by boundaries and use "Transfer-Encoding: chunked" mode to
     * avoid computing "Content-Length".
     */
    private void writeMultipartRangeContents(List<Pair<Long, Long>> ranges,
            URLConnection connection, HttpServletResponse response,
            URL resourceURL) throws IOException {
        String partBoundary = UUID.randomUUID().toString();
        response.setContentType(String
                .format("multipart/byteranges; boundary=%s", partBoundary));
        response.setHeader("Transfer-Encoding", "chunked");

        long position = 0L;
        String mimeType = response.getContentType();
        InputStream dataStream = connection.getInputStream();
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
                                        connection.getContentLengthLong()))
                        .getBytes(StandardCharsets.UTF_8));

                if (position > start) {
                    // out-of-sequence range -> open new stream to the file
                    // alternative: use single stream with mark / reset
                    closeStream(connection.getInputStream());
                    connection = resourceURL.openConnection();
                    dataStream = connection.getInputStream();
                    position = 0L;
                }
                long skipped = dataStream.skip(start - position);
                assert (skipped == start - position);
                writeStream(outputStream, dataStream, end - start + 1);
                position = end + 1;
            }
        } finally {
            closeStream(dataStream);
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
    private boolean verifyRangeLimits(List<Pair<Long, Long>> ranges) {
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

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getName());
    }
}

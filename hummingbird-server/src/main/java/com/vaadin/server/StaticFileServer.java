/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles sending of resources from the WAR root (web content) or
 * META-INF/resources in the case that {@link VaadinServlet} is mapped using
 * "/*".
 * <p>
 * This class is primarily meant to be used during developing time. For a
 * production mode site you should consider serving static resources directly
 * from the servlet (using a default servlet if such exists) or through a stand
 * alone static file server.
 *
 * @author Vaadin
 * @since
 */
public class StaticFileServer implements Serializable {

    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    /**
     * Constructs a file server.
     */
    public StaticFileServer() {
        // Empty default constructor
    }

    /**
     * Checks if a static resource can be found for the requested path.
     *
     * @param request
     *            the request to check
     * @return true if a static resource exists and can be sent as a response to
     *         this request, false otherwise
     */
    public boolean isStaticResourceRequest(HttpServletRequest request) {
        URL resource;
        try {
            resource = request.getServletContext()
                    .getResource(getRequestFilename(request));
        } catch (MalformedURLException e) {
            return false;
        }
        return resource != null;
    }

    /**
     * Serves a static resource for the requested path if a resource can be
     * found.
     * <p>
     *
     * @param request
     *            the request object to read from
     * @param response
     *            the response object to write to
     * @return true if a file was served and the request has been handled, false
     *         otherwise.
     * @throws IOException
     *             if the underlying servlet container reports an exception
     */
    public boolean serveStaticResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String filenameWithPath = getRequestFilename(request);
        URL resourceUrl = request.getServletContext()
                .getResource(filenameWithPath);

        if (resourceUrl == null) {
            // Not found in webcontent or in META-INF/resources in some JAR
            return false;
        }

        // There is a resource!

        // Intentionally writing cache headers also for 304 responses
        writeCacheHeaders(filenameWithPath, request, response);

        long timestamp = writeModificationTimestamp(resourceUrl, request,
                response);
        if (browserHasNewestVersion(request, timestamp)) {
            // Browser is up to date, nothing further to do than set the
            // response code
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }
        writeContentType(filenameWithPath, request, response);
        writeData(filenameWithPath, resourceUrl, request, response);
        return true;
    }

    /**
     * Writes the content type for the file into the response.
     *
     * @param filenameWithPath
     *            the name of the file being sent
     * @param request
     *            the request object
     * @param response
     *            the response object
     */
    protected void writeContentType(String filenameWithPath,
            HttpServletRequest request, HttpServletResponse response) {
        // Set type mime type if we can determine it based on the filename
        final String mimetype = request.getServletContext()
                .getMimeType(filenameWithPath);
        if (mimetype != null) {
            response.setContentType(mimetype);
        }
    }

    /**
     * Writes the modification timestamp info for the file into the response.
     *
     * @param resourceUrl
     *            the internal URL of the file
     * @param request
     *            the request object
     * @param response
     *            the response object
     * @return the written timestamp or -1 if no timestamp was written
     */
    protected long writeModificationTimestamp(URL resourceUrl,
            HttpServletRequest request, HttpServletResponse response) {
        // Find the modification timestamp
        long lastModifiedTime = -1;
        URLConnection connection = null;
        try {
            connection = resourceUrl.openConnection();
            lastModifiedTime = connection.getLastModified();
            // Remove milliseconds to avoid comparison problems (milliseconds
            // are not returned by the browser in the "If-Modified-Since"
            // header).
            lastModifiedTime = lastModifiedTime - lastModifiedTime % 1000;
            response.setDateHeader("Last-Modified", lastModifiedTime);
            return lastModifiedTime;
        } catch (Exception e) {
            getLogger().log(Level.FINEST,
                    "Failed to find out last modified timestamp. Continuing without it.",
                    e);
        } finally {
            try {
                // Explicitly close the input stream to prevent it
                // from remaining hanging
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700
                if (connection != null) {
                    InputStream is = connection.getInputStream();
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (IOException e) {
                getLogger().log(Level.WARNING,
                        "Error closing URLConnection input stream", e);
            }
        }
        return -1L;
    }

    /**
     * Writes cache headers for the file into the response.
     *
     * @param filenameWithPath
     *            the name and path of the file being sent
     * @param request
     *            the request object
     * @param response
     *            the response object
     */
    protected void writeCacheHeaders(String filenameWithPath,
            HttpServletRequest request, HttpServletResponse response) {
        int resourceCacheTime = getCacheTime(filenameWithPath);
        String cacheControl;
        if (resourceCacheTime > 0) {
            cacheControl = "max-age=" + resourceCacheTime;
        } else {
            cacheControl = "public, max-age=0, must-revalidate";
        }
        response.setHeader("Cache-Control", cacheControl);
    }

    /**
     * Returns the (decoded) requested file name, relative to the context path.
     * <p>
     * Package private for testing purposes.
     *
     * @param request
     *            the request object
     * @return the requested file name, starting with a {@literal /}
     */
    String getRequestFilename(HttpServletRequest request) {
        // http://localhost:8888/context/servlet/folder/file.js
        // ->
        // /servlet/folder/file.js

        String servletPath; // Starts with "/"
        if ("".equals(request.getServletPath())) {
            // /* mapped servlet
            servletPath = "";
        } else {
            // /something or /something/* mapped servlet
            servletPath = request.getServletPath();
        }

        if (request.getPathInfo() == null) {
            return servletPath;
        } else {
            return servletPath + request.getPathInfo();
        }
    }

    /**
     * Calculates the cache lifetime for the given filename in seconds.
     * <p>
     * By default filenames containing ".nocache." return 0, filenames
     * containing ".cache." return one year and all other files return 1 hour.
     *
     * @param filenameWithPath
     *            the name of the file being sent
     * @return cache lifetime for the given filename in seconds
     */
    protected int getCacheTime(String filenameWithPath) {
        /*
         * GWT conventions:
         *
         * - files containing .nocache. will not be cached.
         *
         * - files containing .cache. will be cached for one year.
         *
         * https://developers.google.com/web-toolkit/doc/latest/
         * DevGuideCompilingAndDebugging#perfect_caching
         */
        if (filenameWithPath.contains(".nocache.")) {
            return 0;
        }
        if (filenameWithPath.contains(".cache.")) {
            return 60 * 60 * 24 * 365;
        }
        /*
         * For all other files, the browser is allowed to cache for 1 hour
         * without checking if the file has changed.
         */
        return 3600;
    }

    /**
     * Writes the contents of the given resourceUrl to the response.
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
    protected void writeData(String filenameWithPath, URL resourceUrl,
            HttpServletRequest request, HttpServletResponse response)
                    throws IOException {

        URLConnection connection = null;
        InputStream dataStream = null;

        if (acceptsGzippedResource(request)) {
            // try to serve a gzipped version if available
            String gzippedFilenameWithPath = filenameWithPath + ".gz";
            try {
                URL url = request.getServletContext()
                        .getResource(gzippedFilenameWithPath);
                if (url != null) {
                    connection = url.openConnection();
                    dataStream = connection.getInputStream();
                    response.setHeader("Content-Encoding", "gzip");
                }
            } catch (Exception e) {
                getLogger().log(Level.FINE,
                        "Unexpected exception looking for gzipped resource "
                                + gzippedFilenameWithPath,
                        e);
            }
        }
        if (dataStream == null) {
            // gzipped resource not available, get non compressed
            connection = resourceUrl.openConnection();
            dataStream = connection.getInputStream();
        }

        try {
            long length = connection.getContentLengthLong();
            if (length >= 0L) {
                response.setContentLengthLong(length);
            }
        } catch (Exception e) {
            getLogger().log(Level.FINE, "Error setting the content length", e);
        }

        try {
            writeStream(response.getOutputStream(), dataStream);
        } catch (IOException e) {
            getLogger().log(Level.FINE, "Error writing static file to user", e);
        } finally {
            try {
                dataStream.close();
            } catch (IOException e) {
                getLogger().log(Level.FINE,
                        "Error closing input stream for resource", e);
            }
        }
    }

    private static void writeStream(ServletOutputStream outputStream,
            InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while ((bytes = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, bytes);
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
     * @return true if the servlet should attempt to serve a precompressed
     *         version of the resource, false otherwise
     */
    protected boolean acceptsGzippedResource(HttpServletRequest request) {
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
        if (accept.contains("gzip")) {
            return !isQZero(accept, "gzip");
        } else if (accept.contains("*")) {
            return !isQZero(accept, "*");
        }

        return false;
    }

    private static boolean isQZero(String acceptEncoding, String encoding) {
        String qPrefix = encoding + ";q=";
        int qValueIndex = acceptEncoding.indexOf(qPrefix);
        if (qValueIndex == -1) {
            return false;
        }

        // gzip;q=0.123 or gzip;q=0.123,compress...
        String qValue = acceptEncoding
                .substring(qValueIndex + qPrefix.length());
        int endOfQValue = qValue.indexOf(",");
        if (endOfQValue != -1) {
            qValue = qValue.substring(0, endOfQValue);
        }
        return "0".equals(qValue) || "0.0".equals(qValue)
                || "0.00".equals(qValue) || "0.000".equals(qValue);
    }

    /**
     * Checks if the browser has an up to date cached version of requested
     * resource using the "If-Modified-Since" header.
     *
     * @param request
     *            The HttpServletRequest from the browser.
     * @param resourceLastModifiedTimestamp
     *            The timestamp when the resource was last modified. -1 if the
     *            last modification time is unknown.
     * @return true if the If-Modified-Since header tells the cached version in
     *         the browser is up to date, false otherwise
     */
    protected boolean browserHasNewestVersion(HttpServletRequest request,
            long resourceLastModifiedTimestamp) {
        assert resourceLastModifiedTimestamp >= -1;

        if (resourceLastModifiedTimestamp == -1) {
            // We do not know when it was modified so the browser cannot have an
            // up-to-date version
            return false;
        }
        /*
         * The browser can request the resource conditionally using an
         * If-Modified-Since header. Check this against the last modification
         * time.
         */
        try {
            // If-Modified-Since represents the timestamp of the version cached
            // in the browser
            long headerIfModifiedSince = request
                    .getDateHeader("If-Modified-Since");

            if (headerIfModifiedSince >= resourceLastModifiedTimestamp) {
                // Browser has this an up-to-date version of the resource
                return true;
            }
        } catch (Exception e) {
            // Failed to parse header.
            getLogger().log(Level.FINEST, "Unable to parse If-Modified-Since",
                    e);
        }
        return false;
    }

    private static Logger getLogger() {
        return Logger.getLogger(StaticFileServer.class.getName());
    }

}

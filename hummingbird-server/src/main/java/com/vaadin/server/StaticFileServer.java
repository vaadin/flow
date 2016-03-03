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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StaticFileServer {

    private int resourceCacheTime = 3600;
    private ServletContext servletContext;
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    private String folderToServe = "/VAADIN/"; // Must end in slash
    private boolean firstTime = true;

    public void init(ServletContext servletContext) throws ServletException {
        this.servletContext = servletContext;

        String resourceCacheTimeParameter = servletContext.getInitParameter(
                Constants.SERVLET_PARAMETER_RESOURCE_CACHE_TIME);
        if (resourceCacheTimeParameter != null) {
            try {
                resourceCacheTime = Integer
                        .parseInt(resourceCacheTimeParameter);
            } catch (NumberFormatException e) {
                getLogger().warning(
                        Constants.WARNING_RESOURCE_CACHING_TIME_NOT_NUMERIC);
            }
        }
    }

    protected boolean isStaticResourceRequest(HttpServletRequest request) {
        return request.getRequestURI()
                .startsWith(request.getContextPath() + folderToServe);
    }

    /**
     * Check if this is a request for a static resource and, if it is, serve the
     * resource to the client.
     *
     * @param request
     * @param response
     * @return true if a file was served and the request has been handled, false
     *         otherwise.
     * @throws IOException
     * @throws ServletException
     */
    public boolean serveStaticResources(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        if (firstTime) {
            firstTime = false;
            getLogger().info("Using " + getClass().getName()
                    + " to serve static resources");
        }
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            return false;
        }

        String decodedRequestURI = URLDecoder.decode(request.getRequestURI(),
                "UTF-8");
        if ((request.getContextPath() != null)
                && (decodedRequestURI.startsWith(folderToServe))) {
            serveStaticResourcesInVAADIN(decodedRequestURI, request, response);
            return true;
        }

        String decodedContextPath = URLDecoder.decode(request.getContextPath(),
                "UTF-8");
        if (decodedRequestURI.startsWith(decodedContextPath + folderToServe)) {
            serveStaticResourcesInVAADIN(
                    decodedRequestURI.substring(decodedContextPath.length()),
                    request, response);
            return true;
        }

        return false;
    }

    /**
     * Serve resources from VAADIN directory.
     *
     * @param filename
     *            The filename to serve. Should always start with
     *            {@link #folderToServe}.
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private void serveStaticResourcesInVAADIN(String filename,
            HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {

        URL resourceUrl = findResourceURL(filename, servletContext);

        if (resourceUrl == null) {
            // cannot serve requested file
            getLogger().log(Level.INFO,
                    "Requested resource [{0}] not found from filesystem or through class loader.",
                    filename);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // security check: do not permit navigation out of the VAADIN
        // directory
        if (!isAllowedVAADINResourceUrl(request, resourceUrl)) {
            getLogger().log(Level.INFO,
                    "Requested resource [{0}] not accessible in the VAADIN directory or access to it is forbidden.",
                    filename);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String cacheControl = "public, max-age=0, must-revalidate";
        int resourceCacheTime = getCacheTime(filename);
        if (resourceCacheTime > 0) {
            cacheControl = "max-age=" + String.valueOf(resourceCacheTime);
        }
        response.setHeader("Cache-Control", cacheControl);
        response.setDateHeader("Expires",
                System.currentTimeMillis() + (resourceCacheTime * 1000));

        // Find the modification timestamp
        long lastModifiedTime = 0;
        URLConnection connection = null;
        try {
            connection = resourceUrl.openConnection();
            lastModifiedTime = connection.getLastModified();
            // Remove milliseconds to avoid comparison problems (milliseconds
            // are not returned by the browser in the "If-Modified-Since"
            // header).
            lastModifiedTime = lastModifiedTime - lastModifiedTime % 1000;
            response.setDateHeader("Last-Modified", lastModifiedTime);

            if (browserHasNewestVersion(request, lastModifiedTime)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        } catch (Exception e) {
            // Failed to find out last modified timestamp. Continue without it.
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
            } catch (FileNotFoundException e) {
                // Not logging when the file does not exist.
            } catch (IOException e) {
                getLogger().log(Level.INFO,
                        "Error closing URLConnection input stream", e);
            }
        }

        // Set type mime type if we can determine it based on the filename
        final String mimetype = servletContext.getMimeType(filename);
        if (mimetype != null) {
            response.setContentType(mimetype);
        }

        writeStaticResourceResponse(request, response, resourceUrl);
    }

    /**
     * Calculates the cache lifetime for the given filename in seconds. By
     * default filenames containing ".nocache." return 0, filenames containing
     * ".cache." return one year, all other return the value defined in the
     * web.xml using resourceCacheTime (defaults to 1 hour).
     *
     * @param filename
     * @return cache lifetime for the given filename in seconds
     */
    protected int getCacheTime(String filename) {
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
        if (filename.contains(".nocache.")) {
            return 0;
        }
        if (filename.contains(".cache.")) {
            return 60 * 60 * 24 * 365;
        }
        /*
         * For all other files, the browser is allowed to cache for 1 hour
         * without checking if the file has changed. This forces browsers to
         * fetch a new version when the Vaadin version is updated. This will
         * cause more requests to the servlet than without this but for high
         * volume sites the static files should never be served through the
         * servlet.
         */
        return resourceCacheTime;
    }

    /**
     * Writes the contents of the given resourceUrl in the response. Can be
     * overridden to add/modify response headers and similar.
     *
     * @param request
     *            The request for the resource
     * @param response
     *            The response
     * @param resourceUrl
     *            The url to send
     * @throws IOException
     */
    protected void writeStaticResourceResponse(HttpServletRequest request,
            HttpServletResponse response, URL resourceUrl) throws IOException {

        URLConnection connection = null;
        InputStream is = null;
        String urlStr = resourceUrl.toExternalForm();

        if (allowServePrecompressedResource(request, urlStr)) {
            // try to serve a precompressed version if available
            try {
                connection = new URL(urlStr + ".gz").openConnection();
                is = connection.getInputStream();
                // set gzip headers
                response.setHeader("Content-Encoding", "gzip");
            } catch (IOException e) {
                // NOP: will be still tried with non gzipped version
            } catch (Exception e) {
                getLogger().log(Level.FINE,
                        "Unexpected exception looking for gzipped version of resource "
                                + urlStr,
                        e);
            }
        }
        if (is == null) {
            // precompressed resource not available, get non compressed
            connection = resourceUrl.openConnection();
            try {
                is = connection.getInputStream();
            } catch (FileNotFoundException e) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        try {
            int length = connection.getContentLength();
            if (length >= 0) {
                response.setContentLength(length);
            }
        } catch (Exception e) {
            // This can be ignored, content length header is not required.
            // Need to close the input stream because of
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700 to
            // prevent it from hanging, but that is done below.
        }

        try {
            streamContent(response, is);
        } finally {
            is.close();
        }
    }

    /**
     * Returns whether this servlet should attempt to serve a precompressed
     * version of the given static resource. If this method returns true, the
     * suffix {@code .gz} is appended to the URL and the corresponding resource
     * is served if it exists. It is assumed that the compression method used is
     * gzip. If this method returns false or a compressed version is not found,
     * the original URL is used.
     *
     * The base implementation of this method returns true if and only if the
     * request indicates that the client accepts gzip compressed responses and
     * the filename extension of the requested resource is .js, .css, or .html.
     *
     * @since 7.5.0
     *
     * @param request
     *            the request for the resource
     * @param url
     *            the URL of the requested resource
     * @return true if the servlet should attempt to serve a precompressed
     *         version of the resource, false otherwise
     */
    protected boolean allowServePrecompressedResource(
            HttpServletRequest request, String url) {
        String accept = request.getHeader("Accept-Encoding");
        return accept != null && accept.contains("gzip") && (url.endsWith(".js")
                || url.endsWith(".css") || url.endsWith(".html"));
    }

    private void streamContent(HttpServletResponse response, InputStream is)
            throws IOException {
        final OutputStream os = response.getOutputStream();
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytes;
        while ((bytes = is.read(buffer)) >= 0) {
            os.write(buffer, 0, bytes);
        }
    }

    private URL findResourceURL(String filename, ServletContext sc)
            throws MalformedURLException {
        URL resourceUrl = sc.getResource(filename);
        if (resourceUrl == null) {
            // try if requested file is found from classloader

            // strip leading "/" otherwise stream from JAR wont work
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }

            resourceUrl = getClass().getClassLoader().getResource(filename);
        }
        return resourceUrl;
    }

    /**
     * Check whether a URL obtained from a classloader refers to a valid static
     * resource in the directory VAADIN.
     *
     * Warning: Overriding of this method is not recommended, but is possible to
     * support non-default classloaders or servers that may produce URLs
     * different from the normal ones. The method prototype may change in the
     * future. Care should be taken not to expose class files or other resources
     * outside the VAADIN directory if the method is overridden.
     *
     * @param request
     * @param resourceUrl
     * @return
     *
     * @since 6.6.7
     *
     * @deprecated As of 7.0. Will likely change or be removed in a future
     *             version
     */
    @Deprecated
    protected boolean isAllowedVAADINResourceUrl(HttpServletRequest request,
            URL resourceUrl) {
        if ("jar".equals(resourceUrl.getProtocol())) {
            // This branch is used for accessing resources directly from the
            // Vaadin JAR in development environments and in similar cases.

            // Inside a JAR, a ".." would mean a real directory named ".." so
            // using it in paths should just result in the file not being found.
            // However, performing a check in case some servers or class loaders
            // try to normalize the path by collapsing ".." before the class
            // loader sees it.

            if (!resourceUrl.getPath().contains("!" + folderToServe)) {
                getLogger().log(Level.INFO,
                        "Blocked attempt to access a JAR entry not starting with "
                                + folderToServe + ": {0}",
                        resourceUrl);
                return false;
            }
            getLogger().log(Level.FINE,
                    "Accepted access to a JAR entry using a class loader: {0}",
                    resourceUrl);
            return true;
        } else {
            // Some servers such as GlassFish extract files from JARs (file:)
            // and e.g. JBoss 5+ use protocols vsf: and vfsfile: .

            // Check that the URL is in the correct directory and does not
            // contain
            // "/../"
            if (!resourceUrl.getPath().contains(folderToServe)
                    || resourceUrl.getPath().contains("/../")) {
                getLogger().log(Level.INFO,
                        "Blocked attempt to access file: {0}", resourceUrl);
                return false;
            }
            getLogger().log(Level.FINE,
                    "Accepted access to a file using a class loader: {0}",
                    resourceUrl);
            return true;
        }
    }

    /**
     * Checks if the browser has an up to date cached version of requested
     * resource. Currently the check is performed using the "If-Modified-Since"
     * header. Could be expanded if needed.
     *
     * @param request
     *            The HttpServletRequest from the browser.
     * @param resourceLastModifiedTimestamp
     *            The timestamp when the resource was last modified. 0 if the
     *            last modification time is unknown.
     * @return true if the If-Modified-Since header tells the cached version in
     *         the browser is up to date, false otherwise
     */
    private boolean browserHasNewestVersion(HttpServletRequest request,
            long resourceLastModifiedTimestamp) {
        if (resourceLastModifiedTimestamp < 1) {
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
            // Failed to parse header. Fail silently - the browser does not have
            // an up-to-date version in its cache.
        }
        return false;
    }

    private static Logger getLogger() {
        return Logger.getLogger(StaticFileServer.class.getName());
    }

}

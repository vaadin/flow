/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;

import static com.vaadin.flow.server.Constants.VAADIN_BUILD_FILES_PATH;

/**
 * The class that handles writing the response data into the response.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ResponseWriter implements Serializable {
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    private final int bufferSize;
    private final boolean brotliEnabled;
    private final boolean compatibilityMode;

    /**
     * Create a response writer with buffer size equal to
     * {@link ResponseWriter#DEFAULT_BUFFER_SIZE}.
     *
     * @deprecated Use {@link #ResponseWriter(DeploymentConfiguration)} instead.
     */
    @Deprecated
    public ResponseWriter() {
        this(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a response writer with custom buffer size.
     * <p>
     * This will always mark us as compatibility mode and not accept loading
     * resources from the classpath. To enable compressed resources use {@link
     * #ResponseWriter(DeploymentConfiguration)}.
     *
     * @param bufferSize
     *            custom buffer size
     * @deprecated This constructor is never used internally and might be
     *             removed.
     */
    @Deprecated
    public ResponseWriter(int bufferSize) {
        this(bufferSize, false, true);
    }

    /**
     * Create a response writer with the given deployment configuration.
     *
     * @param deploymentConfiguration
     *            the deployment configuration to use, not <code>null</code>
     */
    public ResponseWriter(DeploymentConfiguration deploymentConfiguration) {
        this(DEFAULT_BUFFER_SIZE, deploymentConfiguration.isBrotli(), deploymentConfiguration.isCompatibilityMode());
    }

    private ResponseWriter(int bufferSize, boolean brotliEnabled, boolean compatibilityMode) {
        this.brotliEnabled = brotliEnabled;
        this.bufferSize = bufferSize;
        this.compatibilityMode = compatibilityMode;
    }

    /**
     * Writes the contents and content type (if available) of the given
     * resourceUrl to the response.
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

        URLConnection connection = null;
        InputStream dataStream = null;

        if (brotliEnabled && acceptsBrotliResource(request)) {
            String brotliFilenameWithPath = filenameWithPath + ".br";
            try {
                URL url = getResource(request, brotliFilenameWithPath);
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
                URL url = getResource(request, gzippedFilenameWithPath);
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
            connection = resourceUrl.openConnection();
            dataStream = connection.getInputStream();
        } else {
            response.setHeader("Vary", "Accept-Encoding");
        }

        try {
            long length = connection.getContentLengthLong();
            if (length >= 0L) {
                response.setContentLengthLong(length);
            }
        } catch (Exception e) {
            getLogger().debug("Error setting the content length", e);
        }

        try {
            writeStream(response.getOutputStream(), dataStream);
        } catch (IOException e) {
            getLogger().debug("Error writing static file to user", e);
        } finally {
            try {
                dataStream.close();
            } catch (IOException e) {
                getLogger().debug("Error closing input stream for resource", e);
            }
        }
    }

    private URL getResource(HttpServletRequest request, String resource )
            throws MalformedURLException {
        URL url = request.getServletContext()
                .getResource(resource);
        if (url != null) {
            return url;
        } else if (resource.startsWith("/" + VAADIN_BUILD_FILES_PATH)
                && isAllowedVAADINBuildUrl(resource)) {
            url = request.getServletContext().getClassLoader()
                    .getResource("META-INF" + resource);
        }
        return url;
    }
    /**
     * Check if it is ok to serve the requested file from the classpath.
     * <p>
     * ClassLoader is applicable for use when we are in NPM mode and
     * are serving from the VAADIN/build folder with no folder changes in path.
     *
     * @param filenameWithPath requested filename containing path
     * @return true if we are ok to try serving the file
     */
    private boolean isAllowedVAADINBuildUrl(String filenameWithPath) {
        if (compatibilityMode) {
            getLogger().trace("Serving from the classpath in legacy "
                            + "mode is not accepted. "
                            + "Letting request for '{}' go to servlet context.",
                    filenameWithPath);
            return false;
        }
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
            InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[bufferSize];
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
        }
    }
    /**
     * Check the quality value of the encoding. If the value is zero the
     * encoding is disabled and not accepted.
     *
     * @param acceptEncoding
     *         Accept-Encoding header from request
     * @param encoding
     *         encoding to check
     * @return true if quality value is Zero
     */
    private static boolean isQualityValueZero(String acceptEncoding, String encoding) {
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

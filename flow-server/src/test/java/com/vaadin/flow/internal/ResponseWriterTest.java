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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.MockDeploymentConfiguration;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ResponseWriterTest {
    private static final String PATH_JS = "/static/file.js";
    private static final String PATH_GZ = "/static/file.js.gz";
    private static final String PATH_BR = "/static/file.js.br";
    private static final String CLASS_PATH_JS = "/VAADIN/build/file.js";
    private static final String CLASS_PATH_GZ = "/VAADIN/build/file.js.gz";
    private static final String FAULTY_CLASS_PATH_JS = "/VAADIN/config/file.js";
    private static final String FAULTY_CLASS_PATH_GZ = "/VAADIN/config/file.js.gz";

    private static final byte[] fileJsContents = "File.js contents"
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] fileJsGzippedContents = gzip(fileJsContents);
    // Dummy contents since we don't have a brotli encoder on the test
    // classpath
    private static final byte[] fileJsBrotliContents = "Fake brotli".getBytes();

    private static final Map<String, URL> pathToUrl = new HashMap<>();
    static {
        pathToUrl.put(PATH_JS,
                createFileURLWithDataAndLength(PATH_JS, fileJsContents));
        pathToUrl.put(PATH_GZ,
                createFileURLWithDataAndLength(PATH_GZ, fileJsGzippedContents));
        pathToUrl.put(PATH_BR,
                createFileURLWithDataAndLength(PATH_BR, fileJsBrotliContents));
        pathToUrl.put(CLASS_PATH_JS,
                createFileURLWithDataAndLength(CLASS_PATH_JS, fileJsContents));
        pathToUrl.put(CLASS_PATH_GZ,
                createFileURLWithDataAndLength(CLASS_PATH_GZ, fileJsGzippedContents));
        pathToUrl.put(FAULTY_CLASS_PATH_JS,
                createFileURLWithDataAndLength(FAULTY_CLASS_PATH_JS, fileJsContents));
        pathToUrl.put(FAULTY_CLASS_PATH_GZ,
                createFileURLWithDataAndLength(FAULTY_CLASS_PATH_GZ, fileJsGzippedContents));
    }

    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private AtomicLong responseContentLength;
    private OverrideableResponseWriter responseWriter;

    private static class OverrideableResponseWriter extends ResponseWriter {
        private Boolean overrideAcceptsGzippedResource;
        private Boolean overrideAcceptsBrotliResource;

        public OverrideableResponseWriter(
                DeploymentConfiguration deploymentConfiguration) {
            super(deploymentConfiguration);
        }

        @Override
        protected boolean acceptsGzippedResource(HttpServletRequest request) {
            if (overrideAcceptsGzippedResource != null) {
                return overrideAcceptsGzippedResource;
            }
            return super.acceptsGzippedResource(request);
        }

        @Override
        protected boolean acceptsBrotliResource(HttpServletRequest request) {
            if (overrideAcceptsBrotliResource != null) {
                return overrideAcceptsBrotliResource.booleanValue();
            }
            return super.acceptsBrotliResource(request);
        }
    }

    public static class CapturingServletOutputStream
            extends ServletOutputStream {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        public byte[] getOutput() {
            return baos.toByteArray();
        }
    };

    @Before
    public void setUp() {
        MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
        deploymentConfiguration.setBrotli(true);

        responseWriter = new OverrideableResponseWriter(
                deploymentConfiguration);
        servletContext = Mockito.mock(ServletContext.class);
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        // No header == getDateHeader returns -1 (Mockito default is 0)
        Mockito.when(request.getDateHeader(Matchers.anyString()))
                .thenReturn(-1L);

        response = Mockito.mock(HttpServletResponse.class);
        responseContentLength = new AtomicLong(-1L);
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(Matchers.anyLong());

        Assert.assertNull(VaadinService.getCurrent());
    }

    @After
    public void tearDown() {
        Assert.assertNull(VaadinService.getCurrent());
    }

    @Test
    public void contentType() {
        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Matchers.anyString());

        Mockito.when(servletContext.getMimeType("/file.png"))
                .thenReturn("image/png");

        responseWriter.writeContentType("/file.png", request, response);

        Assert.assertEquals("image/png", contentType.get());
    }

    @Test
    public void noContentType() {
        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Matchers.anyString());

        Mockito.when(servletContext.getMimeType("/file")).thenReturn(null);

        responseWriter.writeContentType("/file", request, response);

        Assert.assertNull(contentType.get());
    }

    @Test
    public void acceptsGzippedResource() {
        Assert.assertTrue(acceptsGzippedResource("compress, gzip"));
        Assert.assertTrue(acceptsGzippedResource("brotli, gzip"));
        Assert.assertTrue(acceptsGzippedResource("gzip"));
        Assert.assertTrue(acceptsGzippedResource("gzip;"));
        Assert.assertTrue(acceptsGzippedResource("gzip;q"));
        Assert.assertFalse(acceptsGzippedResource("compress; q=1 , gzip;q=0"));
        Assert.assertFalse(acceptsGzippedResource(""));
        Assert.assertFalse(acceptsGzippedResource("compress"));

        Assert.assertTrue(
                acceptsGzippedResource("compress;q = 0.5, gzip;q=0.6"));
        Assert.assertTrue(
                acceptsGzippedResource("gzip;q=1.0, identity;q=0.5, *;q=0"));
        Assert.assertTrue(acceptsGzippedResource("*"));
        Assert.assertTrue(acceptsGzippedResource("*;q=0;gzip"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.0"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.00"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.000"));
    }

    private boolean acceptsGzippedResource(String acceptEncodingHeader) {
        Mockito.when(request.getHeader("Accept-Encoding"))
                .thenReturn(acceptEncodingHeader);
        return responseWriter.acceptsGzippedResource(request);
    }

    @Test
    public void acceptsBrotliResource() {
        // Not testing all the same cases as for gzip since most of those
        // variants are effectively testing the same parser functionality
        Assert.assertTrue(acceptsBrotliResource("compress, brotli"));
        Assert.assertFalse(acceptsBrotliResource("gzip"));

        Assert.assertTrue(
                acceptsBrotliResource("compress;q = 0.5, brotli;q=0.6"));
        Assert.assertTrue(acceptsBrotliResource("*"));
        Assert.assertFalse(acceptsBrotliResource("*;q=0"));
    }

    private boolean acceptsBrotliResource(String acceptEncodingHeader) {
        Mockito.when(request.getHeader("Accept-Encoding"))
                .thenReturn(acceptEncodingHeader);
        return responseWriter.acceptsBrotliResource(request);
    }

    @Test
    public void writeDataGzipped() throws IOException {
        responseWriter.overrideAcceptsGzippedResource = true;

        makePathsAvailable(PATH_JS, PATH_GZ);

        assertResponse(fileJsGzippedContents);
    }

    @Test
    public void writeDataGzippedClassPathResource() throws IOException {
        responseWriter.overrideAcceptsGzippedResource = true;

        makePathsAvailable(CLASS_PATH_JS);
        makeClassPathAvailable(CLASS_PATH_GZ);

        assertResponse(CLASS_PATH_JS, fileJsGzippedContents);
    }

    @Test
    public void writeDataNotGzippedClassPathNotAcceptedPath() throws IOException {
        responseWriter.overrideAcceptsGzippedResource = true;

        makePathsAvailable(FAULTY_CLASS_PATH_JS);
        makeClassPathAvailable(FAULTY_CLASS_PATH_GZ);

        assertResponse(FAULTY_CLASS_PATH_JS, fileJsContents);
    }

    @Test
    public void writeDataNoGzippedVersion() throws IOException {
        responseWriter.overrideAcceptsGzippedResource = true;

        makePathsAvailable(PATH_JS);

        assertResponse(fileJsContents);
    }

    @Test
    public void writeDataBrowserDoesNotAcceptGzippedVersion()
            throws IOException {
        responseWriter.overrideAcceptsGzippedResource = false;

        makePathsAvailable(PATH_JS, PATH_GZ);

        assertResponse(fileJsContents);
    }

    @Test
    public void writeDataBrotli() throws IOException {
        responseWriter.overrideAcceptsBrotliResource = Boolean.TRUE;

        // Enable gzip as well to see that Brotli takes priority over gzip in
        // case both are accepted and available
        responseWriter.overrideAcceptsGzippedResource = Boolean.TRUE;

        makePathsAvailable(PATH_JS, PATH_GZ, PATH_BR);

        assertResponse(fileJsBrotliContents);
    }

    @Test
    public void writeDataNoBrotliVersion() throws IOException {
        responseWriter.overrideAcceptsBrotliResource = Boolean.TRUE;

        makePathsAvailable(PATH_JS);

        assertResponse(fileJsContents);
    }

    @Test
    public void writeDataBrowserDoesNotAcceptBrotli() throws IOException {
        responseWriter.overrideAcceptsBrotliResource = Boolean.FALSE;

        makePathsAvailable(PATH_JS, PATH_BR);

        assertResponse(fileJsContents);
    }

    @Test
    public void writeDataBrotliDisabled() throws IOException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setBrotli(false);

        responseWriter = new OverrideableResponseWriter(configuration);

        responseWriter.overrideAcceptsBrotliResource = Boolean.TRUE;

        makePathsAvailable(PATH_JS, PATH_BR);

        assertResponse(fileJsContents);
    }

    private void assertResponse(byte[] expectedResponse) throws IOException {
        assertResponse(PATH_JS, expectedResponse);
    }

    private void assertResponse(String path, byte[] expectedResponse) throws IOException {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        responseWriter.writeResponseContents(path, pathToUrl.get(path),
                request, response);

        Assert.assertArrayEquals(expectedResponse, out.getOutput());
        Assert.assertEquals(expectedResponse.length,
                responseContentLength.get());
    }

    private void makePathsAvailable(String... paths)
            throws MalformedURLException {
        for (String path : paths) {
            URL url = pathToUrl.get(path);
            if (url == null) {
                throw new IllegalArgumentException("Unsupported path: " + path);
            }
            Mockito.when(servletContext.getResource(path)).thenReturn(url);
        }
    }
    private void makeClassPathAvailable(String... paths) {
        for (String path : paths) {
            URL url = pathToUrl.get(path);
            if (url == null) {
                throw new IllegalArgumentException("Unsupported path: " + path);
            }
            ClassLoader classLoader = Mockito.mock(ClassLoader.class);
            Mockito.when(servletContext.getClassLoader()).thenReturn(classLoader);
            Mockito.when(classLoader.getResource("META-INF" + path)).thenReturn(url);
        }
    }

    private static byte[] gzip(byte[] input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream stream = new GZIPOutputStream(baos)) {
            stream.write(input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    private static URL createFileURLWithDataAndLength(String name,
            byte[] data) {
        return createFileURLWithDataAndLength(name, data, -1);
    }

    private static URL createFileURLWithDataAndLength(String name, byte[] data,
            long lastModificationTime) {
        try {
            return new URL("file", "", -1, name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u)
                        throws IOException {
                    URLConnection connection = Mockito
                            .mock(URLConnection.class);
                    Mockito.when(connection.getInputStream())
                            .thenReturn(new ByteArrayInputStream(data));
                    Mockito.when(connection.getContentLengthLong())
                            .thenReturn((long) data.length);
                    Mockito.when(connection.getLastModified())
                            .thenReturn(lastModificationTime);
                    return connection;
                }
            });
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }
}

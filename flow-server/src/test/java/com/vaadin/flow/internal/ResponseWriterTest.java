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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
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
        pathToUrl.put(CLASS_PATH_GZ, createFileURLWithDataAndLength(
                CLASS_PATH_GZ, fileJsGzippedContents));
        pathToUrl.put(FAULTY_CLASS_PATH_JS, createFileURLWithDataAndLength(
                FAULTY_CLASS_PATH_JS, fileJsContents));
        pathToUrl.put(FAULTY_CLASS_PATH_GZ, createFileURLWithDataAndLength(
                FAULTY_CLASS_PATH_GZ, fileJsGzippedContents));
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
        Mockito.when(request.getDateHeader(ArgumentMatchers.anyString()))
                .thenReturn(-1L);

        response = Mockito.mock(HttpServletResponse.class);
        responseContentLength = new AtomicLong(-1L);
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(ArgumentMatchers.anyLong());
    }

    @Test
    public void contentType() {
        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(ArgumentMatchers.anyString());

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
        }).when(response).setContentType(ArgumentMatchers.anyString());

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
    public void writeDataNotGzippedClassPathNotAcceptedPath()
            throws IOException {
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

    @Test
    public void writeByteRangeFromStart() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=0-1"));

        assertResponse(Arrays.copyOfRange(fileJsContents, 0, 2));
        assertResponseHeaders(new Pair<>("Accept-Ranges", "bytes"), new Pair<>(
                "Content-Range", "bytes 0-1/" + fileJsContents.length));
        Assert.assertEquals(2L, responseContentLength.get());
        assertStatus(206);
    }

    @Test
    public void writeByteRangeSubset() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=10-11"));
        assertResponse(Arrays.copyOfRange(fileJsContents, 10, 12));
        assertResponseHeaders(new Pair<>("Accept-Ranges", "bytes"), new Pair<>(
                "Content-Range", "bytes 10-11/" + fileJsContents.length));
        Assert.assertEquals(2L, responseContentLength.get());
        assertStatus(206);
    }

    @Test
    public void writeByteRangeStartOmitted() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=-10"));
        assertResponse(Arrays.copyOfRange(fileJsContents, 0, 11));
        assertResponseHeaders(new Pair<>("Accept-Ranges", "bytes"), new Pair<>(
                "Content-Range", "bytes 0-10/" + fileJsContents.length));
        Assert.assertEquals(11L, responseContentLength.get());
        assertStatus(206);
    }

    @Test
    public void writeByteRangeEndOmitted() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=10-"));
        assertResponse(
                Arrays.copyOfRange(fileJsContents, 10, fileJsContents.length));
        assertResponseHeaders(new Pair<>("Accept-Ranges", "bytes"), new Pair<>(
                "Content-Range", "bytes 10-15/" + fileJsContents.length));
        Assert.assertEquals(6L, responseContentLength.get());
        assertStatus(206);
    }

    @Test
    public void writeByteRangePastFileSize() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=10-100000"));
        assertResponse(
                Arrays.copyOfRange(fileJsContents, 10, fileJsContents.length));
        assertResponseHeaders(new Pair<>("Accept-Ranges", "bytes"), new Pair<>(
                "Content-Range", "bytes 10-15/" + fileJsContents.length));
        Assert.assertEquals(6L, responseContentLength.get());
        assertStatus(206);
    }

    @Test
    public void writeByteRangeEmpty() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=10-9"));
        assertResponse(new byte[] {});
        assertStatus(416);
    }

    @Test
    public void writeByteRangeMalformed() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "f-d-d___"));
        assertResponse(new byte[] {});
        assertStatus(416);
    }

    @Test
    public void writeByteRangeBothEndsOpen() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "-"));
        assertResponse(new byte[] {});
        assertStatus(416);
    }

    @Test
    public void writeByteRangeMultiPartSequential() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=1-4, 5-6, 10-12"));
        // "File.js contents"
        // ^0123456789ABCDEF^
        assertMultipartResponse(PATH_JS, Arrays.asList(
                new Pair<>(new String[] { "Content-Range: bytes 1-4/16" },
                        "ile.".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 5-6/16" },
                        "js".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 10-12/16" },
                        "nte".getBytes())));
        assertStatus(206);
    }

    @Test
    public void writeByteRangeMultiPartNonSequential() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=10-12, 1-4, 5-6"));
        // "File.js contents"
        // ^0123456789ABCDEF^
        assertMultipartResponse(PATH_JS,
                Arrays.asList(
                        new Pair<>(
                                new String[] {
                                        "Content-Range: bytes 10-12/16" },
                                "nte".getBytes()),
                        new Pair<>(
                                new String[] { "Content-Range: bytes 1-4/16" },
                                "ile.".getBytes()),
                        new Pair<>(
                                new String[] { "Content-Range: bytes 5-6/16" },
                                "js".getBytes())));
        assertStatus(206);
    }

    @Test
    public void writeByteRangeMultiPartOverlapping() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=0-15, 1-4"));
        // "File.js contents"
        // ^0123456789ABCDEF^
        assertMultipartResponse(PATH_JS, Arrays.asList(
                new Pair<>(new String[] { "Content-Range: bytes 0-15/16" },
                        "File.js contents".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 1-4/16" },
                        "ile.".getBytes())));
        assertStatus(206);
    }

    @Test
    public void writeByteRangeMultiPartTooManyRequested() throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range",
                "bytes=0-0, 0-0, 1-1, 2-2, 3-3, 4-4, 5-5, 6-6, 7-7, 8-8, 9-9, 10-10, 11-11, 12-12, 13-13, 14-14, 15-15, 16-16"));
        // "File.js contents"
        // ^0123456789ABCDEF^
        assertMultipartResponse(PATH_JS, Arrays.asList(
                new Pair<>(new String[] { "Content-Range: bytes 0-0/16" },
                        "F".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 0-0/16" },
                        "F".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 1-1/16" },
                        "i".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 2-2/16" },
                        "l".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 3-3/16" },
                        "e".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 4-4/16" },
                        ".".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 5-5/16" },
                        "j".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 6-6/16" },
                        "s".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 7-7/16" },
                        " ".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 8-8/16" },
                        "c".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 9-9/16" },
                        "o".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 10-10/16" },
                        "n".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 11-11/16" },
                        "t".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 12-12/16" },
                        "e".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 13-13/16" },
                        "n".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 14-14/16" },
                        "t".getBytes())));
        assertStatus(206);
    }

    @Test
    public void writeByteRangeMultiPartTooManyOverlappingRequested()
            throws IOException {
        makePathsAvailable(PATH_JS);
        mockRequestHeaders(new Pair<>("Range", "bytes=2-4, 0-4, 3-14"));
        // "File.js contents"
        // ^0123456789ABCDEF^
        assertMultipartResponse(PATH_JS, Arrays.asList(
                new Pair<>(new String[] { "Content-Range: bytes 2-4/16" },
                        "le.".getBytes()),
                new Pair<>(new String[] { "Content-Range: bytes 0-4/16" },
                        "File.".getBytes())));
        assertStatus(206);
    }

    private void assertResponse(byte[] expectedResponse) throws IOException {
        assertResponse(PATH_JS, expectedResponse);
    }

    private void assertResponse(String path, byte[] expectedResponse)
            throws IOException {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        responseWriter.writeResponseContents(path, pathToUrl.get(path), request,
                response);

        Assert.assertArrayEquals(expectedResponse, out.getOutput());
        Assert.assertEquals(expectedResponse.length,
                responseContentLength.get());
    }

    private void assertMultipartResponse(String path,
            List<Pair<String[], byte[]>> expectedHeadersAndBytes)
            throws IOException {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(ArgumentMatchers.anyString());

        responseWriter.writeResponseContents(path, pathToUrl.get(path), request,
                response);
        final byte[] output = out.getOutput();

        Assert.assertNotNull(contentType.get());
        Assert.assertTrue(contentType.get()
                .startsWith("multipart/byteranges; boundary="));

        String boundary = contentType.get()
                .substring(contentType.get().indexOf("=") + 1);

        SimpleMultipartParser parser = new SimpleMultipartParser(output,
                boundary);
        for (Pair<String[], byte[]> expected : expectedHeadersAndBytes) {
            String[] expectedHeaders = expected.getFirst();
            String actualHeaders = parser.readHeaders();
            for (String expectedHeader : expectedHeaders) {
                Assert.assertTrue(
                        String.format("Headers:\n%s\ndid not contain:\n%s",
                                actualHeaders, expectedHeader),
                        actualHeaders.contains(expectedHeader));
            }
            byte[] expectedBytes = expected.getSecond();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            parser.readBodyData(outputStream);
            byte[] bytes = outputStream.toByteArray();
            Assert.assertArrayEquals(expectedBytes, bytes);
        }

        // check that there are no excess parts
        try {
            parser.readHeaders();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            parser.readBodyData(outputStream);
            Assert.assertTrue("excess bytes in multipart response",
                    outputStream.toByteArray().length == 0);
        } catch (IOException ioe) {
            // all is well, stream ended
        }
    }

    private void assertStatus(int status) {
        Mockito.verify(response).setStatus(status);
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
            Mockito.when(servletContext.getClassLoader())
                    .thenReturn(classLoader);
            Mockito.when(
                    classLoader.getResource("META-INF/VAADIN/webapp" + path))
                    .thenReturn(url);
        }
    }

    @SafeVarargs
    private final void assertResponseHeaders(Pair<String, String>... headers) {
        for (Pair<String, String> header : headers) {
            Mockito.verify(response).setHeader(header.getFirst(),
                    header.getSecond());
        }
    }

    @SafeVarargs
    private final void mockRequestHeaders(Pair<String, String>... headers) {
        for (Pair<String, String> header : headers) {
            Mockito.when(request.getHeader(header.getFirst()))
                    .thenReturn(header.getSecond());
            Mockito.when(request.getHeaders(header.getFirst()))
                    .thenReturn(Collections.enumeration(
                            Collections.singleton(header.getSecond())));
        }
        Mockito.when(request.getHeaderNames())
                .thenReturn(Collections.enumeration(Arrays.stream(headers)
                        .map(Pair::getFirst).collect(Collectors.toList())));
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

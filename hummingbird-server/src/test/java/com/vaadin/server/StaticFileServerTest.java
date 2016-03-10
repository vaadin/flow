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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class StaticFileServerTest implements Serializable {

    private static class CapturingServletOutputStream
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

    private static URL createFileURLWithDataAndLength(String name, byte[] data)
            throws MalformedURLException {
        return createFileURLWithDataAndLength(name, data, -1);
    }

    private static URL createFileURLWithDataAndLength(String name, byte[] data,
            long lastModificationTime) throws MalformedURLException {
        return new URL("file", "", -1, name, new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                URLConnection connection = Mockito.mock(URLConnection.class);
                Mockito.when(connection.getInputStream())
                        .thenReturn(new ByteArrayInputStream(data));
                Mockito.when(connection.getContentLengthLong())
                        .thenReturn((long) data.length);
                Mockito.when(connection.getLastModified())
                        .thenReturn(lastModificationTime);
                return connection;
            }
        });

    }

    private static class OverrideableStaticFileServer extends StaticFileServer {
        private Boolean overrideBrowserHasNewestVersion;
        private Integer overrideCacheTime;
        private Boolean overrideAcceptsGzippedResource;

        @Override
        protected boolean browserHasNewestVersion(HttpServletRequest request,
                long resourceLastModifiedTimestamp) {
            if (overrideBrowserHasNewestVersion != null) {
                return overrideBrowserHasNewestVersion;
            }

            return super.browserHasNewestVersion(request,
                    resourceLastModifiedTimestamp);
        }

        @Override
        protected int getCacheTime(String filenameWithPath) {
            if (overrideCacheTime != null) {
                return overrideCacheTime;
            }
            return super.getCacheTime(filenameWithPath);
        }

        @Override
        protected boolean acceptsGzippedResource(HttpServletRequest request) {
            if (overrideAcceptsGzippedResource != null) {
                return overrideAcceptsGzippedResource;
            }
            return super.acceptsGzippedResource(request);
        }
    };

    private OverrideableStaticFileServer fileServer;
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, String> headers;
    private Map<String, Long> dateHeaders;
    private AtomicInteger responseCode;
    private AtomicLong responseContentLength;

    @Before
    public void setUp() {
        servletContext = Mockito.mock(ServletContext.class);
        fileServer = new OverrideableStaticFileServer();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        // No header == getDateHeader returns -1 (Mockito default is 0)
        Mockito.when(request.getDateHeader(Mockito.anyString()))
                .thenReturn(-1L);

        responseCode = new AtomicInteger(-1);
        responseContentLength = new AtomicLong(-1L);
        headers = new HashMap<>();
        dateHeaders = new HashMap<>();

        Mockito.doAnswer(invocation -> {
            headers.put((String) invocation.getArguments()[0],
                    (String) invocation.getArguments()[1]);
            return null;
        }).when(response).setHeader(Mockito.anyString(), Mockito.anyString());
        Mockito.doAnswer(invocation -> {
            dateHeaders.put((String) invocation.getArguments()[0],
                    (Long) invocation.getArguments()[1]);
            return null;
        }).when(response).setDateHeader(Mockito.anyString(), Mockito.anyLong());
        Mockito.doAnswer(invocation -> {
            responseCode.set((int) invocation.getArguments()[0]);
            return null;
        }).when(response).setStatus(Mockito.anyInt());
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(Mockito.anyLong());

    }

    @Test
    public void getRequestFilename() {
        // Context path should not affect the filename in any way
        for (String contextPath : new String[] { "", "/foo", "/foo/bar" }) {
            // /* servlet
            Assert.assertEquals("", getRequestFilename(contextPath, "", null));
            Assert.assertEquals("/bar.js",
                    getRequestFilename(contextPath, "", "/bar.js"));
            Assert.assertEquals("/foo/bar.js",
                    getRequestFilename(contextPath, "", "/foo/bar.js"));

            // /foo servlet
            Assert.assertEquals("/foo",
                    getRequestFilename(contextPath, "/foo", null));
            Assert.assertEquals("/foo/bar.js",
                    getRequestFilename(contextPath, "/foo", "/bar.js"));
            Assert.assertEquals("/foo/bar/baz.js",
                    getRequestFilename(contextPath, "/foo", "/bar/baz.js"));

            // /foo/bar servlet
            Assert.assertEquals("/foo/bar",
                    getRequestFilename(contextPath, "/foo/bar", null));
            Assert.assertEquals("/foo/bar/baz.js",
                    getRequestFilename(contextPath, "/foo/bar", "/baz.js"));
            Assert.assertEquals("/foo/bar/baz/baz.js",
                    getRequestFilename(contextPath, "/foo/bar", "/baz/baz.js"));
        }
    }

    private String getRequestFilename(String encodedContextPath,
            String servletPath, String pathInfo) {
        setupRequestURI(encodedContextPath, servletPath, pathInfo);
        return fileServer.getRequestFilename(request);
    }

    private void setupRequestURI(String encodedContextPath, String servletPath,
            String pathInfo) {
        assert !encodedContextPath.equals("/") : "root context is always \"\"";
        assert encodedContextPath.equals("") || encodedContextPath
                .startsWith("/") : "context always starts with /";
        assert !encodedContextPath.endsWith(
                "/") : "context path should start with / but not end with /";
        assert !servletPath.equals("/") : "a /* mapped servlet has path \"\"";
        assert servletPath.equals("") || servletPath
                .startsWith("/") : "servlet path always starts with /";
        assert !servletPath.endsWith(
                "/") : "servlet path should start with / but not end with /";
        assert pathInfo == null || pathInfo.startsWith("/");

        String requestURI = "";
        if (!encodedContextPath.isEmpty()) {
            requestURI += encodedContextPath;
        }
        if (!servletPath.isEmpty()) {
            requestURI += servletPath;
        }
        if (pathInfo != null) {
            requestURI += pathInfo;
        }

        Mockito.when(request.getContextPath()).thenReturn(encodedContextPath);
        Mockito.when(request.getServletPath()).thenReturn(servletPath);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getRequestURI()).thenReturn(requestURI);
    }

    @Test
    public void isResourceRequest() throws Exception {
        setupRequestURI("", "/static", "/file.png");
        Mockito.when(servletContext.getResource("/static/file.png"))
                .thenReturn(new URL("file:///static/file.png"));
        Assert.assertTrue(fileServer.isStaticResourceRequest(request));
    }

    @Test
    public void isResourceRequestWithContextPath() throws Exception {
        setupRequestURI("/foo", "/static", "/file.png");
        Mockito.when(servletContext.getResource("/static/file.png"))
                .thenReturn(new URL("file:///static/file.png"));
        Assert.assertTrue(fileServer.isStaticResourceRequest(request));
    }

    @Test
    public void isNotResourceRequest() throws Exception {
        setupRequestURI("", "", null);
        Mockito.when(servletContext.getResource("/")).thenReturn(null);
        Assert.assertFalse(fileServer.isStaticResourceRequest(request));
    }

    @Test
    public void isNotResourceRequestWithContextPath() throws Exception {
        setupRequestURI("/context", "", "/");
        Mockito.when(servletContext.getResource("/"))
                .thenReturn(new URL("file", "", -1,
                        "hummingbird/hummingbird-tests/non-root-context-test/src/main/webapp/",
                        new URLStreamHandler() {

                            @Override
                            protected URLConnection openConnection(URL u)
                                    throws IOException {
                                URLConnection mock = Mockito
                                        .mock(URLConnection.class);
                                return mock;
                            }
                        }));

        Assert.assertFalse(fileServer.isStaticResourceRequest(request));
    }

    @Test
    public void contentType() {
        AtomicReference<String> contentType = new AtomicReference<String>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Mockito.anyString());

        Mockito.when(servletContext.getMimeType("/file.png"))
                .thenReturn("image/png");

        fileServer.writeContentType("/file.png", request, response);

        Assert.assertEquals("image/png", contentType.get());
    }

    @Test
    public void noContentType() {
        AtomicReference<String> contentType = new AtomicReference<String>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Mockito.anyString());

        Mockito.when(servletContext.getMimeType("/file")).thenReturn(null);

        fileServer.writeContentType("/file", request, response);

        Assert.assertNull(contentType.get());
    }

    @Test
    public void writeModificationTimestampBrowserHasLatest()
            throws MalformedURLException {
        fileServer.overrideBrowserHasNewestVersion = true;
        Long modificationTimestamp = writeModificationTime();
        Assert.assertEquals(modificationTimestamp,
                dateHeaders.get("Last-Modified"));
    }

    @Test
    public void writeModificationTimestampBrowserDoesNotHaveLatest()
            throws MalformedURLException {
        fileServer.overrideBrowserHasNewestVersion = false;
        Long modificationTimestamp = writeModificationTime();
        Assert.assertEquals(modificationTimestamp,
                dateHeaders.get("Last-Modified"));

    }

    private Long writeModificationTime() throws MalformedURLException {
        LocalDateTime modificationTime = LocalDateTime.of(2016, 2, 2, 0, 0, 0);
        Long modificationTimestamp = modificationTime
                .toEpochSecond(ZoneOffset.UTC) * 1000;
        String filename = "modified-1d-ago.txt";
        URL resourceUrl = new URL("file", "", -1, filename,
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u)
                            throws IOException {
                        URLConnection mock = Mockito.mock(URLConnection.class);
                        Mockito.when(mock.getLastModified())
                                .thenReturn(modificationTimestamp);
                        return mock;
                    }
                });

        fileServer.writeModificationTimestamp(resourceUrl, request, response);
        return modificationTimestamp;
    }

    @Test
    public void browserHasNewestVersionUnknownModificiationTime() {
        Assert.assertFalse(fileServer.browserHasNewestVersion(request, -1));
    }

    @Test(expected = AssertionError.class)
    public void browserHasNewestVersionInvalidModificiationTime() {
        fileServer.browserHasNewestVersion(request, -2);
    }

    @Test
    public void browserHasNewestVersionNoIfModifiedSinceHeader() {
        long fileModifiedTime = 0;

        Assert.assertFalse(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void browserHasNewestVersionOlderIfModifiedSinceHeader() {
        long browserIfModifiedSince = 123L;
        long fileModifiedTime = 124L;
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserIfModifiedSince);

        Assert.assertFalse(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void browserHasNewestVersionNewerIfModifiedSinceHeader() {
        long browserIfModifiedSince = 125L;
        long fileModifiedTime = 124L;
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserIfModifiedSince);

        Assert.assertTrue(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void writeCacheHeadersCacheResource() {
        fileServer.overrideCacheTime = 12;
        fileServer.writeCacheHeaders("/folder/myfile.txt", request, response);
        Assert.assertTrue(headers.get("Cache-Control").contains("max-age=12"));
    }

    @Test
    public void writeCacheHeadersDoNotCacheResource() {
        fileServer.overrideCacheTime = 0;
        fileServer.writeCacheHeaders("/folder/myfile.txt", request, response);
        Assert.assertTrue(headers.get("Cache-Control").contains("max-age=0"));
        Assert.assertTrue(
                headers.get("Cache-Control").contains("must-revalidate"));
    }

    @Test
    public void getCacheTime() {
        int oneYear = 60 * 60 * 24 * 365;
        Assert.assertEquals(oneYear,
                fileServer.getCacheTime("somefile.cache.js"));
        Assert.assertEquals(oneYear,
                fileServer.getCacheTime("folder/somefile.cache.js"));
        Assert.assertEquals(0, fileServer.getCacheTime("somefile.nocache.js"));
        Assert.assertEquals(0,
                fileServer.getCacheTime("folder/somefile.nocache.js"));
        Assert.assertEquals(3600, fileServer.getCacheTime("randomfile.js"));
        Assert.assertEquals(3600,
                fileServer.getCacheTime("folder/randomfile.js"));
    }

    @Test
    public void acceptsGzippedResource() {
        Assert.assertTrue(acceptsGzippedResource("compress, gzip"));
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
        return fileServer.acceptsGzippedResource(request);
    }

    @Test
    public void writeDataGzipped() throws IOException {
        fileServer.overrideAcceptsGzippedResource = true;
        String fileJsContents = "File.js contents";
        byte[] fileJsGzippedContents = gzip(fileJsContents);

        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContents.getBytes(StandardCharsets.UTF_8));
        URL fileJsGzURL = createFileURLWithDataAndLength("/static/file.js.gz",
                fileJsGzippedContents);

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);
        Mockito.when(servletContext.getResource("/static/file.js.gz"))
                .thenReturn(fileJsGzURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        fileServer.writeData("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsGzippedContents, out.getOutput());
        Assert.assertEquals(fileJsGzippedContents.length,
                responseContentLength.get());
    }

    @Test
    public void writeDataNoGzippedVersion() throws IOException {
        String fileJsContents = "File.js contents";

        byte[] fileJsContentsBytes = fileJsContents
                .getBytes(StandardCharsets.UTF_8);
        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContentsBytes);

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        fileServer.writeData("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsContentsBytes, out.getOutput());
        Assert.assertEquals(fileJsContentsBytes.length,
                responseContentLength.get());
    }

    @Test
    public void writeDataBrowserDoesNotAcceptGzippedVersion()
            throws IOException {
        fileServer.overrideAcceptsGzippedResource = false;
        String fileJsContents = "File.js contents";
        byte[] fileJsContentsBytes = fileJsContents
                .getBytes(StandardCharsets.UTF_8);

        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContentsBytes);
        URL fileJsGzURL = createFileURLWithDataAndLength("/static/file.js.gz",
                gzip(fileJsContents));

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);
        Mockito.when(servletContext.getResource("/static/file.js.gz"))
                .thenReturn(fileJsGzURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        fileServer.writeData("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsContentsBytes, out.getOutput());
        Assert.assertEquals(fileJsContentsBytes.length,
                responseContentLength.get());
    }

    private byte[] gzip(String input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream stream = new GZIPOutputStream(baos);
        stream.write(input.getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    @Test
    public void serveNonExistingStaticResource() throws IOException {
        setupRequestURI("", "", "/nonexisting/file.js");

        Assert.assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void serveStaticResource() throws IOException {
        setupRequestURI("", "/some", "/file.js");
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(servletContext.getResource("/some/file.js")).thenReturn(
                createFileURLWithDataAndLength("/some/file.js", fileData));
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void serveStaticResourceBrowserHasLatest() throws IOException {
        long browserLatest = 123L;
        long fileModified = 123L;

        setupRequestURI("", "/some", "/file.js");
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserLatest);

        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(servletContext.getResource("/some/file.js"))
                .thenReturn(createFileURLWithDataAndLength("/some/file.js",
                        fileData, fileModified));

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertEquals(0, out.getOutput().length);
        Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED,
                responseCode.get());
    }

}

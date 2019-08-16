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
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;

import static com.vaadin.flow.server.Constants.POLYFILLS_DEFAULT_VALUE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

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
    }

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

        OverrideableStaticFileServer(VaadinServletService servletService) {
            super(servletService);
        }

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
    }

    private OverrideableStaticFileServer fileServer;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, String> headers;
    private Map<String, Long> dateHeaders;
    private AtomicInteger responseCode;
    private AtomicLong responseContentLength;

    private VaadinServletService servletService = Mockito.mock(VaadinServletService.class);
    private DeploymentConfiguration configuration;
    private ServletContext servletContext;

    @Before
    public void setUp() throws IOException {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        // No header == getDateHeader returns -1 (Mockito default is 0)
        Mockito.when(request.getDateHeader(Matchers.anyString()))
                .thenReturn(-1L);

        responseCode = new AtomicInteger(-1);
        responseContentLength = new AtomicLong(-1L);
        headers = new HashMap<>();
        dateHeaders = new HashMap<>();

        Mockito.doAnswer(invocation -> {
            headers.put((String) invocation.getArguments()[0],
                    (String) invocation.getArguments()[1]);
            return null;
        }).when(response).setHeader(Matchers.anyString(), Matchers.anyString());
        Mockito.doAnswer(invocation -> {
            dateHeaders.put((String) invocation.getArguments()[0],
                    (Long) invocation.getArguments()[1]);
            return null;
        }).when(response).setDateHeader(Matchers.anyString(),
                Matchers.anyLong());
        Mockito.doAnswer(invocation -> {
            responseCode.set((int) invocation.getArguments()[0]);
            return null;
        }).when(response).setStatus(Matchers.anyInt());
        Mockito.doAnswer(invocation -> {
            responseCode.set((int) invocation.getArguments()[0]);
            return null;
        }).when(response).sendError(Matchers.anyInt());
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(Matchers.anyLong());

        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);

        Mockito.when(servletService.getDeploymentConfiguration()).thenReturn(configuration);
        fileServer = new OverrideableStaticFileServer(servletService);

        // Required by the ResponseWriter
        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
    }

    @After
    public void tearDown() {
        Assert.assertNull(VaadinService.getCurrent());
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
        Mockito.when(servletService.getStaticResource("/static/file.png"))
                .thenReturn(new URL("file:///static/file.png"));
        Assert.assertTrue(fileServer.isStaticResourceRequest(request));
    }

    @Test
    public void isResourceRequestWithContextPath() throws Exception {
        setupRequestURI("/foo", "/static", "/file.png");
        Mockito.when(servletService.getStaticResource("/static/file.png"))
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
        Mockito.when(servletContext.getResource("/")).thenReturn(new URL("file",
                "", -1,
                "flow/flow-tests/non-root-context-test/src/main/webapp/",
                new URLStreamHandler() {

                    @Override
                    protected URLConnection openConnection(URL u)
                            throws IOException {
                        URLConnection mock = Mockito.mock(URLConnection.class);
                        return mock;
                    }
                }));

        Assert.assertFalse(fileServer.isStaticResourceRequest(request));
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
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        Assert.assertTrue(headers.get("Cache-Control").contains("max-age=12"));
    }

    @Test
    public void nonProductionMode_writeCacheHeadersCacheResource_noCache() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        fileServer.overrideCacheTime = 12;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        Assert.assertTrue(headers.get("Cache-Control").equals("no-cache"));
    }

    @Test
    public void writeCacheHeadersDoNotCacheResource() {
        fileServer.overrideCacheTime = 0;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        Assert.assertTrue(headers.get("Cache-Control").contains("max-age=0"));
        Assert.assertTrue(
                headers.get("Cache-Control").contains("must-revalidate"));
    }

    @Test
    public void nonProductionMode_writeCacheHeadersDoNotCacheResource() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        fileServer.overrideCacheTime = 0;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        Assert.assertTrue(headers.get("Cache-Control").equals("no-cache"));
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
    public void serveNonExistingStaticResource() throws IOException {
        setupRequestURI("", "", "/nonexisting/file.js");

        fileServer.serveStaticResource(request, response);
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND,
                responseCode.get());
    }

    @Test
    public void serveStaticResource() throws IOException {
        setupRequestURI("", "/some", "/file.js");
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(servletService.getStaticResource("/some/file.js")).thenReturn(
                createFileURLWithDataAndLength("/some/file.js", fileData));
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void serveStaticPolyfillBuildResource() throws IOException {
        String pathInfo = "/VAADIN/build/webcomponentsjs/webcomponents-loader.js";
        setupRequestURI("/context", "/servlet", pathInfo);
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource("META-INF" + pathInfo)).thenReturn(
                createFileURLWithDataAndLength("META-INF" + pathInfo,
                        fileData));
        mockConfigurationPolyfills();

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void contextAndServletPath_serveStaticBundleBuildResource() throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("/context", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void ServletPath_serveStaticBundleBuildResource() throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void contextPath_serveStaticBundleBuildResource() throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("/context", "", pathInfo);
        assertBundleBuildResource(pathInfo);
    }
    @Test
    public void serveStaticBundleBuildResource() throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("", "", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    public void assertBundleBuildResource(String pathInfo) throws IOException {
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource("META-INF" + pathInfo)).thenReturn(
                createFileURLWithDataAndLength("META-INF" + pathInfo,
                        fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void staticBuildResourceWithDirectoryChange_nothingServed()
            throws IOException {
        String pathInfo = "/VAADIN/build/../vaadin-bundle-1234.cache.js";
        setupRequestURI("/context", "/servlet", pathInfo);
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource("META-INF" + pathInfo)).thenReturn(
                createFileURLWithDataAndLength("META-INF" + pathInfo,
                        fileData));

        // have data available for /VAADIN/vaadin-bundle-1234.cache.js
        Mockito.when(mockLoader.getResource("META-INF" + pathInfo.replace("build/../", ""))).thenReturn(
                createFileURLWithDataAndLength("META-INF" + pathInfo.replace("build/../", ""),
                        fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertEquals(0, out.getOutput().length);
    }

    @Test
    public void customStaticBuildResource_isServed()
            throws IOException {
        String pathInfo = "/VAADIN/build/my-text.txt";
        setupRequestURI("", "", pathInfo);
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource("META-INF" + pathInfo)).thenReturn(
                createFileURLWithDataAndLength("META-INF" + pathInfo,
                        fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void nonexistingStaticBuildResource_returnsNotFound()
            throws IOException {
        String pathInfo = "/VAADIN/build/my-text.txt";
        setupRequestURI("", "", pathInfo);
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, responseCode.get());
    }

    @Test
    public void customStatsJson_isServedFromServlet() throws IOException {

        String pathInfo = "/VAADIN/build/stats.json";
        setupRequestURI("", "/servlet", pathInfo);
        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);

        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);
        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        Mockito.when(servletService.getStaticResource(pathInfo))
                .thenReturn(createFileURLWithDataAndLength(pathInfo, fileData));

        CapturingServletOutputStream out = new CapturingServletOutputStream();

        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    public void mockConfigurationPolyfills() {
        Mockito.when(configuration.getPolyfills()).thenReturn(
                Arrays.asList(POLYFILLS_DEFAULT_VALUE.split("[, ]+")));
    }

    public void mockStatsBundles(ClassLoader mockLoader) {
        Mockito.when(configuration
                .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                        VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn("META-INF/VAADIN/config/stats.json");
        Mockito.when(mockLoader
                .getResourceAsStream("META-INF/VAADIN/config/stats.json"))
                .thenReturn(new ByteArrayInputStream(getStatsData()));
    }

    /**
     * Returns a byte array for a valid stats.json containing only chunks
     * @return
     */
    public byte[] getStatsData() {
        return ("{ "
                    + "\"assetsByChunkName\" :{ "
                    + "\"index\": \"build/vaadin-bundle-1234.cache.js\", "
                    + "\"index.es5\": \"build/vaadin-bundle.es5-1234.cache.js\" "
                    + "} }")
                .getBytes(StandardCharsets.UTF_8);
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
        Mockito.when(servletService.getStaticResource("/some/file.js"))
                .thenReturn(createFileURLWithDataAndLength("/some/file.js",
                        fileData, fileModified));

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertEquals(0, out.getOutput().length);
        Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED,
                responseCode.get());
    }

    @Test
    public void serveStaticResourceFromWebjarWithIncorrectPath()
            throws IOException {
        Mockito.when(configuration.getBooleanProperty(
                StaticFileServer.PROPERTY_FIX_INCORRECT_WEBJAR_PATHS, false))
                .thenReturn(true);

        byte[] fileData = "function() {eval('foo');};"
                .getBytes(StandardCharsets.UTF_8);
        Mockito.when(servletService.getStaticResource("/webjars/foo/bar.js"))
                .thenReturn(createFileURLWithDataAndLength(
                        "/webjars/foo/bar.js", fileData));

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);

        setupRequestURI("", "", "/frontend/src/webjars/foo/bar.js");

        Assert.assertTrue(fileServer.isStaticResourceRequest(request));
        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertArrayEquals(fileData, out.getOutput());
    }

    @Test
    public void serveStaticResourceFromWebjarWithIncorrectPathAndFixingDisabled()
            throws IOException {
        Mockito.when(configuration.getBooleanProperty(
                StaticFileServer.PROPERTY_FIX_INCORRECT_WEBJAR_PATHS, false))
                .thenReturn(false);

        Mockito.when(servletService
                .getStaticResource("/frontend/src/webjars/foo/bar.js"))
                .thenReturn(null);

        setupRequestURI("", "", "/frontend/src/webjars/foo/bar.js");

        Assert.assertFalse(fileServer.isStaticResourceRequest(request));
        Assert.assertTrue(fileServer.serveStaticResource(request, response));
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND,
                responseCode.get());
    }
}

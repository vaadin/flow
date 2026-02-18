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
package com.vaadin.flow.server;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.WarURLStreamHandlerFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ResponseWriter;
import com.vaadin.tests.util.TestUtil;

import static com.vaadin.flow.internal.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.internal.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.Constants.POLYFILLS_DEFAULT_VALUE;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@NotThreadSafe
class StaticFileServerTest implements Serializable {

    private OverrideableStaticFileServer fileServer;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Map<String, String> headers;
    private Map<String, Long> dateHeaders;
    private AtomicInteger responseCode;
    private AtomicLong responseContentLength;
    private VaadinServletService servletService = Mockito
            .mock(VaadinServletService.class);
    private DeploymentConfiguration configuration;
    private ServletContext servletContext;

    private static final String WEBAPP_RESOURCE_PREFIX = "META-INF/VAADIN/webapp";
    private CapturingServletOutputStream out;
    @TempDir
    Path temporaryFolder;

    private static URL createFileURLWithDataAndLength(String name, String data)
            throws MalformedURLException {
        return createFileURLWithDataAndLength(name, data, -1);
    }

    private static URL createFileURLWithDataAndLength(String name, String data,
            long lastModificationTime) throws MalformedURLException {
        return new URL("file", "", -1, name, new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                URLConnection connection = Mockito.mock(URLConnection.class);
                Mockito.when(connection.getInputStream())
                        .thenReturn(new ByteArrayInputStream(bytes));
                Mockito.when(connection.getContentLengthLong())
                        .thenReturn((long) bytes.length);
                Mockito.when(connection.getLastModified())
                        .thenReturn(lastModificationTime);
                return connection;
            }
        });

    }

    @BeforeAll
    public static void beforeClass() {
        // must be cleared before running this class
        CurrentInstance.clearAll();
    }

    @BeforeEach
    public void setUp() throws IOException {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        // No header == getDateHeader returns -1 (Mockito default is 0)
        Mockito.when(request.getDateHeader(ArgumentMatchers.anyString()))
                .thenReturn(-1L);

        responseCode = new AtomicInteger(-1);
        responseContentLength = new AtomicLong(-1L);
        headers = new HashMap<>();
        dateHeaders = new HashMap<>();

        Mockito.doAnswer(invocation -> {
            headers.put((String) invocation.getArguments()[0],
                    (String) invocation.getArguments()[1]);
            return null;
        }).when(response).setHeader(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString());
        Mockito.doAnswer(invocation -> {
            dateHeaders.put((String) invocation.getArguments()[0],
                    (Long) invocation.getArguments()[1]);
            return null;
        }).when(response).setDateHeader(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyLong());
        Mockito.doAnswer(invocation -> {
            responseCode.set((int) invocation.getArguments()[0]);
            return null;
        }).when(response).setStatus(ArgumentMatchers.anyInt());
        Mockito.doAnswer(invocation -> {
            responseCode.set((int) invocation.getArguments()[0]);
            return null;
        }).when(response).sendError(ArgumentMatchers.anyInt());
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(ArgumentMatchers.anyLong());

        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(configuration.getMode()).thenAnswer(q -> {
            if (configuration.isProductionMode()) {
                return Mode.PRODUCTION_CUSTOM;
            } else {
                return Mode.DEVELOPMENT_BUNDLE;
            }
        });
        Mockito.when(servletService.getDeploymentConfiguration())
                .thenReturn(configuration);

        // Use test class loader to enable reading `manifest.json` resource
        Mockito.doAnswer(
                invocationOnMock -> servletService.getClass().getClassLoader())
                .when(servletService).getClassLoader();

        fileServer = new OverrideableStaticFileServer(servletService,
                configuration);

        // Required by the ResponseWriter
        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);

        out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
    }

    @Test
    public void getRequestFilename() {
        // Context path should not affect the filename in any way
        for (String contextPath : new String[] { "", "/foo", "/foo/bar" }) {
            // /* servlet
            assertEquals("", getRequestFilename(contextPath, "", null));
            assertEquals("/bar.js",
                    getRequestFilename(contextPath, "", "/bar.js"));
            assertEquals("/foo/bar.js",
                    getRequestFilename(contextPath, "", "/foo/bar.js"));

            // /foo servlet
            assertEquals("/foo", getRequestFilename(contextPath, "/foo", null));
            assertEquals("/foo/bar.js",
                    getRequestFilename(contextPath, "/foo", "/bar.js"));
            assertEquals("/foo/bar/baz.js",
                    getRequestFilename(contextPath, "/foo", "/bar/baz.js"));

            // /foo/bar servlet
            assertEquals("/foo/bar",
                    getRequestFilename(contextPath, "/foo/bar", null));
            assertEquals("/foo/bar/baz.js",
                    getRequestFilename(contextPath, "/foo/bar", "/baz.js"));
            assertEquals("/foo/bar/baz/baz.js",
                    getRequestFilename(contextPath, "/foo/bar", "/baz/baz.js"));
        }
    }

    @Test
    public void getRequestFilename_shouldAlwaysBeResolvedAsRootResourceForServiceWorkerRequest() {
        for (String swFile : new String[] { "/sw.js", "/sw.js.gz" }) {
            assertEquals(swFile, getRequestFilename("", "", swFile));
            assertEquals(swFile, getRequestFilename("", "/foo", swFile));
            assertEquals(swFile, getRequestFilename("", "/foo/bar", swFile));
            assertEquals(swFile, getRequestFilename("/ctx", "", swFile));
            assertEquals(swFile, getRequestFilename("/ctx", "/foo", swFile));
            assertEquals(swFile,
                    getRequestFilename("/ctx", "/foo/bar", swFile));
            assertEquals(swFile, getRequestFilename("/ctx/sub", "", swFile));
            assertEquals(swFile,
                    getRequestFilename("/ctx/sub", "/foo", swFile));
            assertEquals(swFile,
                    getRequestFilename("/ctx/sub", "/foo/bar", swFile));
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
        assert encodedContextPath.equals("")
                || encodedContextPath.startsWith("/")
                : "context always starts with /";
        assert !encodedContextPath.endsWith("/")
                : "context path should start with / but not end with /";
        assert !servletPath.equals("/") : "a /* mapped servlet has path \"\"";
        assert servletPath.equals("") || servletPath.startsWith("/")
                : "servlet path always starts with /";
        assert !servletPath.endsWith("/")
                : "servlet path should start with / but not end with /";
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
        fileServer.writeResponse = false;
        setupRequestURI("", "/static", "/file.png");
        Mockito.when(servletService.getStaticResource("/static/file.png"))
                .thenReturn(new URL("file:///static/file.png"));
        assertTrue(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void isResourceRequestWithContextPath() throws Exception {
        fileServer.writeResponse = false;
        setupRequestURI("/foo", "/static", "/file.png");
        Mockito.when(servletService.getStaticResource("/static/file.png"))
                .thenReturn(new URL("file:///static/file.png"));
        assertTrue(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void isNotResourceRequest() throws Exception {
        setupRequestURI("", "", null);
        Mockito.when(servletContext.getResource("/")).thenReturn(null);
        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void directoryIsNotResourceRequest() throws Exception {
        fileServer.writeResponse = false;
        final Path folder = Files.createTempDirectory("test");

        setupRequestURI("", "", "/frontend");
        // generate URL so it is not ending with / so that we test the correct
        // method
        String rootAbsolutePath = folder.toFile().getAbsolutePath()
                .replaceAll("\\\\", "/");
        if (rootAbsolutePath.endsWith("/")) {
            rootAbsolutePath = rootAbsolutePath.substring(0,
                    rootAbsolutePath.length() - 1);
        }
        final URL folderPath = new URL("file:///" + rootAbsolutePath);

        Mockito.when(servletService.getStaticResource("/frontend"))
                .thenReturn(folderPath);
        assertFalse(fileServer.serveStaticResource(request, response),
                "Folder on disk should not be a static resource.");

        // Test any path ending with / to be seen as a directory
        setupRequestURI("", "", "/fake");
        Mockito.when(servletService.getStaticResource("/fake"))
                .thenReturn(new URL("file:///fake/"));
        assertFalse(fileServer.serveStaticResource(request, response),
                "Fake should not check the file system nor be a static resource.");

        Path tempArchive = generateZipArchive(folder);

        setupRequestURI("", "", "/frontend/.");
        Mockito.when(servletService.getStaticResource("/frontend/."))
                .thenReturn(new URL("jar:file:///"
                        + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/frontend"));
        assertFalse(fileServer.serveStaticResource(request, response),
                "Folder 'frontend' in jar should not be a static resource.");
        setupRequestURI("", "", "/file.txt");
        Mockito.when(servletService.getStaticResource("/file.txt"))
                .thenReturn(new URL("jar:file:///"
                        + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/file.txt"));
        assertTrue(fileServer.serveStaticResource(request, response),
                "File 'file.txt' inside jar should be a static resource.");

    }

    @Test
    public void isStaticResource_jarWarFileScheme_detectsAsStaticResources()
            throws IOException {
        fileServer.writeResponse = false;
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Can not run concurrently with other test");

        final Path folder = Files.createTempDirectory("test");

        File archiveFile = new File(folder.toFile(), "fake.jar");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();
        File warFile = new File(folder.toFile(), "war.jar");
        warFile.createNewFile();
        Path warArchive = warFile.toPath();

        generateJarInJar(archiveFile, tempArchive, warArchive);

        // Instantiate URL stream handler factory to be able to handle war:
        WarURLStreamHandlerFactory.getInstance();

        final URL folderResourceURL = new URL(
                "jar:war:" + warFile.toURI().toURL() + "!/"
                        + archiveFile.getName() + "!/frontend");

        setupRequestURI("", "", "/frontend/.");
        Mockito.when(servletService.getStaticResource("/frontend/."))
                .thenReturn(folderResourceURL);

        assertTrue(fileServer.serveStaticResource(request, response),
                "Request should return as static request as we can not determine non file resources in jar files.");

    }

    @Test
    public void isStaticResource_jarInAJar_detectsAsStaticResources()
            throws IOException {
        fileServer.writeResponse = false;
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Can not run concurrently with other test");

        final Path folder = Files.createTempDirectory("test");

        File archiveFile = new File(folder.toFile(), "fake.jar");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        File warFile = new File(folder.toFile(), "war.jar");
        warFile.createNewFile();
        Path warArchive = warFile.toPath();

        generateJarInJar(archiveFile, tempArchive, warArchive);

        setupRequestURI("", "", "/frontend/.");
        Mockito.when(servletService.getStaticResource("/frontend/."))
                .thenReturn(new URL("jar:" + warFile.toURI().toURL() + "!/"
                        + archiveFile.getName() + "!/frontend"));
        assertTrue(fileServer.serveStaticResource(request, response),
                "Request should return as static request as we can not determine non file resources in jar files.");
        setupRequestURI("", "", "/file.txt");
        Mockito.when(servletService.getStaticResource("/file.txt"))
                .thenReturn(new URL("jar:" + warFile.toURI().toURL() + "!/"
                        + archiveFile.getName() + "!/file.txt"));
        assertTrue(fileServer.serveStaticResource(request, response),
                "Request should return as static request as we can not determine non file resources in jar files.");

    }

    private Path generateZipArchive(Path folder) throws IOException {
        File archiveFile = new File(folder.toFile(), "fake.jar");
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempArchive))) {
            // Create a file to the zip
            zipOutputStream.putNextEntry(new ZipEntry("/file"));
            zipOutputStream.closeEntry();
            // Create a directory to the zip
            zipOutputStream.putNextEntry(new ZipEntry("frontend/"));
            zipOutputStream.closeEntry();
        }
        return tempArchive;
    }

    private void generateJarInJar(File archiveFile, Path tempArchive,
            Path warArchive) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempArchive))) {
            // Create a file to the zip
            zipOutputStream.putNextEntry(new ZipEntry("/file"));
            zipOutputStream.closeEntry();
            // Create a directory to the zip
            zipOutputStream.putNextEntry(new ZipEntry("frontend/"));
            zipOutputStream.closeEntry();
        }

        try (ZipOutputStream warOutputStream = new ZipOutputStream(
                Files.newOutputStream(warArchive))) {
            // Create a file to the zip
            warOutputStream.putNextEntry(new ZipEntry(archiveFile.getName()));
            warOutputStream.write(Files.readAllBytes(tempArchive));

            warOutputStream.closeEntry();
        }
    }

    @Test
    public void openFileServerExistsForZip_openingNewDoesNotFail()
            throws IOException, URISyntaxException {
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Can not run concurrently with other test");

        final Path folder = Files.createTempDirectory("test");

        Path tempArchive = generateZipArchive(folder);

        final FileSystem fileSystem = FileSystems
                .newFileSystem(new URL("jar:file:///"
                        + tempArchive.toString().replaceAll("\\\\", "/") + "!/")
                        .toURI(), Collections.emptyMap());

        final URL folderResourceURL = new URL(
                "jar:file:///" + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/frontend");

        try {
            fileServer.getFileSystem(folderResourceURL.toURI());
        } finally {
            fileServer.closeFileSystem(folderResourceURL.toURI());
            fileSystem.close();
        }

    }

    @Test
    public void openingJarFileSystemForDifferentFilesInSameJar_existingFileSystemIsUsed()
            throws IOException, URISyntaxException {
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Can not run concurrently with other test");

        final Path folder = Files.createTempDirectory("test");

        Path tempArchive = generateZipArchive(folder);

        final URL folderResourceURL = new URL(
                "jar:file:///" + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/frontend");

        final URL fileResourceURL = new URL(
                "jar:file:///" + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/file.txt");

        fileServer.getFileSystem(folderResourceURL.toURI());
        fileServer.getFileSystem(fileResourceURL.toURI());

        assertEquals((Integer) 2,
                StaticFileServer.openFileSystems.entrySet().iterator().next()
                        .getValue(),
                "Same file should be marked for both resources");
        fileServer.closeFileSystem(folderResourceURL.toURI());
        assertEquals((Integer) 1,
                StaticFileServer.openFileSystems.entrySet().iterator().next()
                        .getValue(),
                "Closing resource should be removed from jar uri");
        fileServer.closeFileSystem(fileResourceURL.toURI());
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Closing last resource should clear marking");

        try {
            FileSystems.getFileSystem(folderResourceURL.toURI());
            fail("Jar FileSystem should have been closed");
        } catch (FileSystemNotFoundException fsnfe) {
            // This should happen as we should not have an open FileSystem here.
        }
    }

    @Test
    public void concurrentRequestsToJarResources_checksAreCorrect()
            throws IOException, InterruptedException, ExecutionException,
            URISyntaxException {
        fileServer.writeResponse = false;
        assertTrue(StaticFileServer.openFileSystems.isEmpty(),
                "Can not run concurrently with other test");

        final Path folder = Files.createTempDirectory("test");

        Path tempArchive = generateZipArchive(folder);

        setupRequestURI("", "", "/frontend/.");
        final URL folderResourceURL = new URL(
                "jar:file:///" + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/frontend");
        Mockito.when(servletService.getStaticResource("/frontend/."))
                .thenReturn(folderResourceURL);

        int THREADS = 5;

        List<Callable<Result>> folderNotResource = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<Result> callable = () -> {
                        try {
                            if (fileServer.serveStaticResource(request,
                                    response)) {
                                throw new IllegalArgumentException(
                                        "Folder 'frontend' in jar should not be a static resource.");
                            }
                        } catch (Exception e) {
                            return new Result(e);
                        }
                        return new Result(null);
                    };
                    return callable;
                }).collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Future<Result>> futures = executor.invokeAll(folderNotResource);
        List<String> exceptions = new ArrayList<>();

        executor.shutdown();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.exception != null) {
                exceptions.add(result.exception.getMessage());
            }
        }

        assertTrue(exceptions.isEmpty(),
                "There were exceptions in concurrent requests {" + exceptions
                        + "}");

        assertFalse(
                StaticFileServer.openFileSystems
                        .containsKey(folderResourceURL.toURI()),
                "Folder URI should have been cleared");
        try {
            FileSystems.getFileSystem(folderResourceURL.toURI());
            fail("FileSystem for folder resource should be closed");
        } catch (FileSystemNotFoundException fsnfe) {
            // This should happen as we should not have an open FileSystem here.
        }

        setupRequestURI("", "", "/file.txt");
        final URL fileResourceURL = new URL(
                "jar:file:///" + tempArchive.toString().replaceAll("\\\\", "/")
                        + "!/file.txt");
        Mockito.when(servletService.getStaticResource("/file.txt"))
                .thenReturn(fileResourceURL);

        List<Callable<Result>> fileIsResource = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<Result> callable = () -> {
                        try {
                            if (!fileServer.serveStaticResource(request,
                                    response)) {
                                throw new IllegalArgumentException(
                                        "File 'file.txt' inside jar should be a static resource.");
                            }
                        } catch (Exception e) {
                            return new Result(e);
                        }
                        return new Result(null);
                    };
                    return callable;
                }).collect(Collectors.toList());

        executor = Executors.newFixedThreadPool(THREADS);
        futures = executor.invokeAll(fileIsResource);
        exceptions = new ArrayList<>();

        executor.shutdown();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.exception != null) {
                exceptions.add(result.exception.getMessage());
            }
        }

        assertTrue(exceptions.isEmpty(),
                "There were exceptions in concurrent requests {" + exceptions
                        + "}");

        assertFalse(
                fileServer.openFileSystems.containsKey(fileResourceURL.toURI()),
                "URI should have been cleared");
        try {
            FileSystems.getFileSystem(fileResourceURL.toURI());
            fail("FileSystem for file resource should be closed");
        } catch (FileSystemNotFoundException fsnfe) {
            // This should happen as we should not have an open FileSystem here.
        }

    }

    private static class Result {
        final Exception exception;

        Result(Exception exception) {
            this.exception = exception;
        }
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

        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void writeModificationTimestampBrowserHasLatest()
            throws MalformedURLException {
        fileServer.overrideBrowserHasNewestVersion = true;
        Long modificationTimestamp = writeModificationTime();
        assertEquals(modificationTimestamp, dateHeaders.get("Last-Modified"));
    }

    @Test
    public void writeModificationTimestampBrowserDoesNotHaveLatest()
            throws MalformedURLException {
        fileServer.overrideBrowserHasNewestVersion = false;
        Long modificationTimestamp = writeModificationTime();
        assertEquals(modificationTimestamp, dateHeaders.get("Last-Modified"));

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
        assertFalse(fileServer.browserHasNewestVersion(request, -1));
    }

    @Test
    public void browserHasNewestVersionInvalidModificiationTime() {
        assertThrows(AssertionError.class, () -> {
            fileServer.browserHasNewestVersion(request, -2);
        });
    }

    @Test
    public void browserHasNewestVersionNoIfModifiedSinceHeader() {
        long fileModifiedTime = 0;

        assertFalse(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void browserHasNewestVersionOlderIfModifiedSinceHeader() {
        long browserIfModifiedSince = 123L;
        long fileModifiedTime = 124L;
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserIfModifiedSince);

        assertFalse(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void browserHasNewestVersionNewerIfModifiedSinceHeader() {
        long browserIfModifiedSince = 125L;
        long fileModifiedTime = 124L;
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserIfModifiedSince);

        assertTrue(
                fileServer.browserHasNewestVersion(request, fileModifiedTime));
    }

    @Test
    public void writeCacheHeadersCacheResource() {
        fileServer.overrideCacheTime = 12;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        assertTrue(headers.get("Cache-Control").contains("max-age=12"));
    }

    @Test
    public void nonProductionMode_writeCacheHeadersCacheResource_noCache() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        fileServer.overrideCacheTime = 12;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        assertTrue(headers.get("Cache-Control").equals("no-cache"));
    }

    @Test
    public void writeCacheHeadersDoNotCacheResource() {
        fileServer.overrideCacheTime = 0;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        assertTrue(headers.get("Cache-Control").contains("max-age=0"));
        assertTrue(headers.get("Cache-Control").contains("must-revalidate"));
    }

    @Test
    public void nonProductionMode_writeCacheHeadersDoNotCacheResource() {
        Mockito.when(configuration.isProductionMode()).thenReturn(false);

        fileServer.overrideCacheTime = 0;
        fileServer.writeCacheHeaders("/folder/myfile.txt", response);
        assertTrue(headers.get("Cache-Control").equals("no-cache"));
    }

    @Test
    public void productionMode_writeCacheHeaders_withVersionParam_oneYearCache() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(request.getParameter("v")).thenReturn("abcd1234");

        fileServer.writeCacheHeaders("/folder/myfile.css", request, response);
        assertEquals("max-age=31536000", headers.get("Cache-Control"));
    }

    @Test
    public void getCacheTime() {
        int oneYear = 60 * 60 * 24 * 365;
        assertEquals(oneYear, fileServer.getCacheTime("somefile.cache.js"));
        assertEquals(oneYear,
                fileServer.getCacheTime("folder/somefile.cache.js"));
        assertEquals(0, fileServer.getCacheTime("somefile.nocache.js"));
        assertEquals(0, fileServer.getCacheTime("folder/somefile.nocache.js"));
        assertEquals(3600, fileServer.getCacheTime("randomfile.js"));
        assertEquals(3600, fileServer.getCacheTime("folder/randomfile.js"));
    }

    @Test
    public void serveNonExistingStaticResource() throws IOException {
        setupRequestURI("", "", "/nonexisting/file.js");

        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void serveStaticResource() throws IOException {
        setupRequestURI("", "/some", "/file.js");
        String fileData = "function() {eval('foo');};";

        Mockito.when(servletService.getStaticResource("/some/file.js"))
                .thenReturn(createFileURLWithDataAndLength("/some/file.js",
                        fileData));

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    @Test
    public void contextAndServletPath_serveStaticBundleBuildResource()
            throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("/context", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void ServletPath_serveStaticBundleBuildResource()
            throws IOException {
        String pathInfo = "/VAADIN/build/vaadin-bundle-1234.cache.js";
        setupRequestURI("", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void contextPath_serveStaticBundleBuildResource()
            throws IOException {
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

    @Test
    public void contextAndServletPath_serveStaticFileResource()
            throws IOException {
        String pathInfo = "/VAADIN/static/img/bg.jpg";
        setupRequestURI("/context", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void ServletPath_serveStaticFileResource() throws IOException {
        String pathInfo = "/VAADIN/static/img/bg.jpg";
        setupRequestURI("", "/servlet", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void contextPath_serveStaticFileResource() throws IOException {
        String pathInfo = "/VAADIN/static/img/bg.jpg";
        setupRequestURI("/context", "", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    @Test
    public void serveStaticFileResource() throws IOException {
        String pathInfo = "/VAADIN/static/img/bg.jpg";
        setupRequestURI("", "", pathInfo);
        assertBundleBuildResource(pathInfo);
    }

    public void assertBundleBuildResource(String pathInfo) throws IOException {
        String fileData = "function() {eval('foo');};";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    private void staticBuildResourceWithDirectoryChange_nothingServed(
            String pathInfo) throws IOException {
        setupRequestURI("/context", "/servlet", pathInfo);
        String fileData = "function() {eval('foo');};";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        // have data available for /VAADIN/vaadin-bundle-1234.cache.js
        Mockito.when(mockLoader.getResource(
                WEBAPP_RESOURCE_PREFIX + pathInfo.replace("build/../", "")))
                .thenReturn(
                        createFileURLWithDataAndLength(
                                WEBAPP_RESOURCE_PREFIX
                                        + pathInfo.replace("build/../", ""),
                                fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(0, out.getOutput().length);
        assertEquals(HttpStatusCode.BAD_REQUEST.getCode(), responseCode.get());
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeWithSlash_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/../vaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeWithBackslash_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something\\..\\vaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeWithEncodedBackslashUpperCase_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something%5C..%5Cvaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeWithEncodedBackslashLowerCase_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something%5c..%5cvaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeInTheEndWithSlash_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/..");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeInTheEndWithBackslash_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something\\..");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeInTheEndWithEncodedBackslashUpperCase_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something%5C..");
    }

    @Test
    public void serveStaticResource_uriWithDirectoryChangeInTheEndWithEncodedBackslashLowerCase_returnsImmediatelyAndSetsBadRequestStatus()
            throws IOException {
        staticBuildResourceWithDirectoryChange_nothingServed(
                "/VAADIN/build/something%5c..");
    }

    @Test
    public void serveStaticResource_uriWithPercent_isServed()
            throws IOException {
        String pathInfo = "/VAADIN/build/100%.pdf";
        setupRequestURI("", "", pathInfo);
        String fileData = "contents";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    @Test
    public void customStaticBuildResource_isServed() throws IOException {
        String pathInfo = "/VAADIN/build/my-text.txt";
        setupRequestURI("", "", pathInfo);
        String fileData = "function() {eval('foo');};";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    @Test
    public void nonexistingStaticBuildResource_notServed() throws IOException {
        String pathInfo = "/VAADIN/build/my-text.txt";
        setupRequestURI("", "", pathInfo);
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void staticManifestPathResource_isServed() throws IOException {
        String pathInfo = "/sw.js";
        setupRequestURI("", "", pathInfo);
        String fileData = "function() {eval('foo');};";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    @Test
    public void staticManifestPathIndexHtmlResource_notServed()
            throws IOException {
        String pathInfo = "/index.html";
        setupRequestURI("", "", pathInfo);
        String fileData = "function() {eval('foo');};";
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);

        Mockito.when(mockLoader.getResource(WEBAPP_RESOURCE_PREFIX + pathInfo))
                .thenReturn(createFileURLWithDataAndLength(
                        "/" + WEBAPP_RESOURCE_PREFIX + pathInfo, fileData));

        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void customStatsJson_isServedFromServlet() throws IOException {
        String pathInfo = "/VAADIN/build/stats.json";
        setupRequestURI("", "/servlet", pathInfo);
        String fileData = "function() {eval('foo');};";

        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(servletService.getClassLoader()).thenReturn(mockLoader);
        mockStatsBundles(mockLoader);
        mockConfigurationPolyfills();

        Mockito.when(servletService.getStaticResource(pathInfo))
                .thenReturn(createFileURLWithDataAndLength(pathInfo, fileData));

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
    }

    public void mockConfigurationPolyfills() {
        Mockito.when(configuration.getPolyfills()).thenReturn(
                Arrays.asList(POLYFILLS_DEFAULT_VALUE.split("[, ]+")));
    }

    public void mockStatsBundles(ClassLoader mockLoader) {
        Mockito.when(configuration.getStringProperty(
                SERVLET_PARAMETER_STATISTICS_JSON,
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn("META-INF/VAADIN/config/stats.json");
        Mockito.when(mockLoader
                .getResourceAsStream("META-INF/VAADIN/config/stats.json"))
                .thenReturn(new ByteArrayInputStream(getStatsData()));
    }

    /**
     * Returns a byte array for a valid stats.json containing only chunks
     *
     * @return
     */
    public byte[] getStatsData() {
        return ("{ " + "\"assetsByChunkName\" :{ "
                + "\"index\": \"build/vaadin-bundle-1234.cache.js\"} }")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void serveStaticResourceBrowserHasLatest() throws IOException {
        long browserLatest = 123L;
        long fileModified = 123L;

        setupRequestURI("", "/some", "/file.js");
        Mockito.when(request.getDateHeader("If-Modified-Since"))
                .thenReturn(browserLatest);

        String fileData = "function() {eval('foo');};";
        Mockito.when(servletService.getStaticResource("/some/file.js"))
                .thenReturn(createFileURLWithDataAndLength("/some/file.js",
                        fileData, fileModified));

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(0, out.getOutput().length);
        assertEquals(HttpStatusCode.NOT_MODIFIED.getCode(), responseCode.get());
    }

    @Test
    public void serveStaticResourceFromWebjarWithIncorrectPath()
            throws IOException {
        Mockito.when(configuration.getBooleanProperty(
                StaticFileServer.PROPERTY_FIX_INCORRECT_WEBJAR_PATHS, false))
                .thenReturn(true);
        String fileData = "function() {eval('foo');};";
        Mockito.when(servletService.getStaticResource("/webjars/foo/bar.js"))
                .thenReturn(createFileURLWithDataAndLength(
                        "/webjars/foo/bar.js", fileData));

        setupRequestURI("", "", "/frontend/src/webjars/foo/bar.js");

        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(fileData, out.getOutputString());
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

        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void getStaticResource_delegateToVaadinService()
            throws MalformedURLException {
        URL url = new URL("http://bar");
        Mockito.when(servletService.getStaticResource("foo")).thenReturn(url);
        URL result = fileServer.getStaticResource("foo");

        Mockito.verify(servletService).getStaticResource("foo");
        assertSame(url, result);
    }

    @Test
    public void serveStaticResource_projectThemeResourceRequest_serveFromFrontend()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        final String styles = "body { background: black; }";
        TestUtil.createStyleCssStubInFrontend(projectRootFolder, "my-theme",
                styles);

        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Mockito.when(configuration.getProjectFolder())
                .thenReturn(projectRootFolder);
        Mockito.when(configuration.getBuildFolder()).thenReturn("target");
        Mockito.when(configuration.getFrontendFolder())
                .thenReturn(new File(projectRootFolder, DEFAULT_FRONTEND_DIR));

        setupRequestURI("", "", "/VAADIN/themes/my-theme/styles.css");
        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(styles, out.getOutputString());
    }

    @Test
    public void serveStaticResource_externalThemeResourceRequest_serveFromBundle()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        final String styles = "body { background: black; }";
        TestUtil.createStylesCssStubInBundle(projectRootFolder, "my-theme",
                styles);

        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        Mockito.when(configuration.getProjectFolder())
                .thenReturn(projectRootFolder);
        Mockito.when(configuration.getBuildFolder()).thenReturn("target");
        Mockito.when(configuration.getStringProperty(PARAM_FRONTEND_DIR,
                DEFAULT_FRONTEND_DIR)).thenReturn(DEFAULT_FRONTEND_DIR);

        setupRequestURI("", "", "/VAADIN/themes/my-theme/styles.css");
        assertTrue(fileServer.serveStaticResource(request, response));
        assertEquals(styles, out.getOutputString());
    }

    @Test
    public void serveStaticResource_themeResourceRequest_productionMode_notServeFromBundleNorFromFrontend()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        final String styles = "body { background: black; }";
        TestUtil.createStylesCssStubInBundle(projectRootFolder, "my-theme",
                styles);

        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(configuration.getProjectFolder())
                .thenReturn(projectRootFolder);
        Mockito.when(configuration.getBuildFolder()).thenReturn("target");

        setupRequestURI("", "", "/themes/my-theme/styles.css");
        assertFalse(fileServer.serveStaticResource(request, response));
    }

    @Test
    public void frameworkStaticFolder_withoutEndingSlash_doesNotServeStaticResource()
            throws IOException {
        // Test framework static folders without / at the end,
        // that they do not return a result
        for (String publicInternalFolderPath : HandlerHelper
                .getPublicInternalFolderPaths()) {
            assertTrue(publicInternalFolderPath.startsWith("/"));
            assertFalse(publicInternalFolderPath.endsWith("/**"));

            setupRequestURI("", "", publicInternalFolderPath);
            Mockito.when(
                    servletService.getStaticResource(publicInternalFolderPath))
                    .thenReturn(URI
                            .create("file:///" + publicInternalFolderPath + "/")
                            .toURL());

            assertFalse(fileServer.serveStaticResource(request, response),
                    publicInternalFolderPath
                            + " should not be a static resource.");
        }
    }

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

        public String getOutputString() {
            return new String(getOutput(), StandardCharsets.UTF_8);
        }
    }

    private static class OverrideableStaticFileServer extends StaticFileServer {
        public boolean writeResponse = true;
        private Boolean overrideBrowserHasNewestVersion;
        private Integer overrideCacheTime;
        private DeploymentConfiguration configuration;

        OverrideableStaticFileServer(VaadinServletService servletService,
                DeploymentConfiguration configuration) {
            super(servletService);
            this.configuration = configuration;
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

        @Override
        public boolean serveStaticResource(HttpServletRequest request,
                HttpServletResponse response) throws IOException {
            if (!writeResponse)
                try {
                    {
                        ResponseWriter fakeWriter = new ResponseWriter(
                                configuration) {
                            @Override
                            public void writeResponseContents(
                                    String filenameWithPath, URL resourceUrl,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
                            }
                        };
                        Field f = StaticFileServer.class
                                .getDeclaredField("responseWriter");
                        f.setAccessible(true);
                        f.set(this, fakeWriter);
                    }
                } catch (IllegalArgumentException | IllegalAccessException
                        | NoSuchFieldException | SecurityException e) {
                    throw new RuntimeException(e);
                }
            return super.serveStaticResource(request, response);
        }

    }
}

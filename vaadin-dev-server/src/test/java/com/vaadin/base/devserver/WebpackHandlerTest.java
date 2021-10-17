/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import static com.vaadin.base.devserver.WebpackHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.testutil.FrontendStubs.WEBPACK_TEST_OUT_FILE;
import static com.vaadin.flow.testutil.FrontendStubs.createStubWebpackServer;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.base.devserver.startup.AbstractDevModeTest;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
@SuppressWarnings("restriction")
public class WebpackHandlerTest extends AbstractDevModeTest {

    private HttpServer httpServer;
    private int responseStatus;
    private int responseError;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static class CustomRuntimeException extends RuntimeException {

    }

    public abstract static class TestAppConfig
            implements ApplicationConfiguration {

    }

    @Override
    public void setup() throws Exception {
        super.setup();
        new File(baseDir, FrontendUtils.WEBPACK_CONFIG).createNewFile();
        createStubWebpackServer("Compiled", 100, baseDir, true);
    }

    @Override
    public void teardown() {
        super.teardown();
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    public static void removeDevModeHandlerInstance(Lookup lookup)
            throws Exception {
        // Reset unique instance of DevModeHandler
        Field atomicHandler = WebpackHandler.class
                .getDeclaredField("atomicHandler");
        atomicHandler.setAccessible(true);
        AtomicReference<?> reference = (AtomicReference<?>) atomicHandler
                .get(null);
        reference.set(null);
    }

    @Test
    public void should_CreateInstanceAndRunWebPack_When_DevModeAndNpmInstalled()
            throws Exception {
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNotNull(handler);
        waitForDevServer();
        assertTrue(new File(baseDir,
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
        assertNull(handler.getFailedOutput());
        assertTrue(0 < getDevServerPort());
        Thread.sleep(150); // NOSONAR
    }

    @Test
    public void avoidStoringPortOfFailingWebPackDevServer_failWebpackStart_startWebPackSucessfullyAfter()
            throws Exception {
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));

        waitForDevServer();

        removeDevModeHandlerInstance(lookup);
        // dev mode handler should fail because of non-existent npm
        // folder: it
        // means the port number should not have been written

        // use non-existent folder for as npmFolder, it should fail the
        // validation (which means server instance won't be reused)
        WebpackHandler newhHandler = WebpackHandler.start(lookup,
                new File(npmFolder, UUID.randomUUID().toString()),
                CompletableFuture.completedFuture(null));

        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(response.getOutputStream())
                .thenReturn(new ByteArrayOutputStream());
        boolean proceed = true;
        Throwable cause = null;
        while (proceed) {
            try {
                proceed = newhHandler.handleRequest(
                        Mockito.mock(VaadinSession.class),
                        Mockito.mock(VaadinRequest.class), response);
            } catch (IllegalStateException ise) {
                proceed = false;
                cause = ise.getCause();
            }
        }
        Assert.assertNotNull(cause);
        Assert.assertTrue(cause instanceof ExecutionFailedException);
    }

    @Test
    @Ignore("Ignored due to failing rate on CI")
    public void should_Fail_When_WebpackPrematurelyExit() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Webpack exited prematurely");

        createStubWebpackServer("Foo", 0, baseDir);
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
    }

    @Test
    public void should_CaptureWebpackOutput_When_Failed() throws Exception {
        configuration.setApplicationOrSystemProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT, "100");
        createStubWebpackServer("Failed to compile", 300, baseDir, true);
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNotNull(handler);
        waitForDevServer();
        int port = getDevServerPort();
        assertTrue(port > 0);

        /*
         * Wait for server to stop running before checking the output stream
         */
        Thread.sleep(350); // NOSONAR
        assertNotNull(
                "Got no output for the failed output even though expected output.",
                handler.getFailedOutput());
    }

    @Test
    public void shouldNot_CreateInstance_When_ProductionMode()
            throws Exception {
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNull(handler);
    }

    @Test
    public void enableDevServerFalse_shouldNotCreateInstance()
            throws Exception {
        Mockito.when(appConfig.enableDevServer()).thenReturn(false);
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNull(handler);
    }

    @Test
    public void shouldNot_RunWebpack_When_WebpackRunning() throws Exception {
        final String manifestJsonResponse = "{}";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);
        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertFalse(new File(baseDir,
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
    }

    @Test
    public void webpackNotInstalled_throws() throws Exception {
        exception.expectCause(CoreMatchers.isA(ExecutionFailedException.class));
        new File(baseDir, WEBPACK_SERVER).delete();
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
    }

    @Test
    public void webpackIsNotExecutable_throws() {
        /*
         * The set executable doesn't work in Windows and will always return
         * false
         */
        boolean systemImplementsExecutable = new File(baseDir, WEBPACK_SERVER)
                .setExecutable(false);
        if (systemImplementsExecutable) {
            exception.expectCause(
                    CoreMatchers.isA(ExecutionFailedException.class));
            handler = WebpackHandler.start(lookup, npmFolder,
                    CompletableFuture.completedFuture(null));
            waitForDevServer();
        }
    }

    @Test
    public void webpackNotConfigured_throws() {
        exception.expectCause(CoreMatchers.isA(ExecutionFailedException.class));
        new File(baseDir, FrontendUtils.WEBPACK_CONFIG).delete();
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
    }

    @Test
    public void should_HandleJavaScriptRequests() {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.js");
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertTrue(handler.isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleNonVaadinRequests() {
        HttpServletRequest request = prepareRequest("/foo.js");
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleOtherRequests() {
        HttpServletRequest request = prepareRequest("/foo/VAADIN//foo.bar");
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void isDevModeRequest_dynamicResourcesAreNotDevModeRequest() {
        HttpServletRequest request = prepareRequest(
                "/" + StreamRequestHandler.DYN_RES_PREFIX + "foo");
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void should_HandleAnyAssetInVaadin() {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.bar");
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertTrue(handler.isDevModeRequest(request));
    }

    @Test
    public void should_HandleAnyAssetInManifestPaths() throws Exception {
        final String manifestJsonResponse = "{ \"sw.js\": "
                + "\"sw.js\", \"index.html\": \"index.html\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        HttpServletRequest request = prepareRequest("/sw.js");
        assertTrue(handler.isDevModeRequest(request));
    }

    @Test
    public void shouldNot_Handle_IndexHtmlInManifestPaths() throws Exception {
        final String manifestJsonResponse = "{ \"sw.js\": "
                + "\"sw.js\", \"index.html\": \"index.html\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        HttpServletRequest request = prepareRequest("/index.html");
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test(expected = ConnectException.class)
    public void should_ThrowAnException_When_WebpackNotListening()
            throws IOException {
        createStubWebpackServer("Compiled", 3000, baseDir, false);
        HttpServletRequest request = prepareRequest("/VAADIN//foo.js");
        handler = WebpackHandler.start(0, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        handler.serveDevModeRequest(request, null);
    }

    @Test
    public void webpack_forDifferentRequests_shouldHaveCorrectResponse()
            throws Exception {
        HttpServletRequest request = prepareRequest("/VAADIN//foo.js");
        HttpServletResponse response = prepareResponse();
        final String manifestJsonResponse = "{ \"VAADIN//foo.js\": "
                + "\"VAADIN//foo.js\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertTrue(handler.serveDevModeRequest(request, response));
        assertEquals(HTTP_OK, responseStatus);

        httpServer.stop(0);
        prepareHttpServer(port, HTTP_NOT_FOUND, "");
        assertFalse(handler.serveDevModeRequest(request, response));
        assertEquals(200, responseStatus);

        httpServer.stop(0);
        prepareHttpServer(port, HTTP_UNAUTHORIZED, "");
        assertTrue(handler.serveDevModeRequest(request, response));
        assertEquals(HTTP_UNAUTHORIZED, responseError);

        httpServer.stop(0);
        exception.expect(ConnectException.class);
        handler.serveDevModeRequest(request, null);
    }

    @Test
    public void vaadinServlet_forDifferentRequests_shouldHaveCorrectResponse()
            throws Exception {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.js");
        HttpServletResponse response = prepareResponse();
        final String manifestJsonResponse = "{ \"VAADIN/foo.js\": "
                + "\"VAADIN/foo.js\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        VaadinServlet servlet = prepareServlet(port);
        servlet.service(request, response);
        assertEquals(HTTP_OK, responseStatus);

        httpServer.stop(0);
        prepareHttpServer(port, HTTP_NOT_MODIFIED, "");
        servlet.service(request, response);
        assertEquals(HTTP_NOT_MODIFIED, responseStatus);

        httpServer.stop(0);
        exception.expect(ConnectException.class);
        servlet.service(request, response);
    }

    @Test
    public void should_GetStatsJson_From_Webpack() throws Exception {

        String statsContent = "{}";
        int port = prepareHttpServer(0, HTTP_OK, statsContent);
        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        assertEquals(statsContent,
                FrontendUtils.getStatsContent(vaadinService));
    }

    @Test
    public void should_reuseWebpackPort_AfterRestart() throws Exception {
        final String manifestJsonContents = "{}";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonContents);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertNotNull(handler);
        assertEquals(port, getDevServerPort());

        removeDevModeHandlerInstance(lookup);

        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertNotNull(handler);
        assertEquals(port, getDevServerPort());
    }

    @Test
    public void startDevModeHandler_vaadinHomeNodeIsAFolder_throws()
            throws IOException {
        exception.expectCause(CoreMatchers.isA(IllegalStateException.class));
        String userHome = "user.home";
        String originalHome = System.getProperty(userHome);
        File home = temporaryFolder.newFolder();
        System.setProperty(userHome, home.getPath());
        try {
            File homeDir = new File(home, ".vaadin");
            File node = new File(homeDir,
                    FrontendUtils.isWindows() ? "node/node.exe" : "node/node");
            FileUtils.forceMkdir(node);

            Mockito.when(appConfig.getBooleanProperty(
                    InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, false))
                    .thenReturn(true);
            handler = WebpackHandler.start(lookup, npmFolder,
                    CompletableFuture.completedFuture(null));
            waitForDevServer();
        } finally {
            System.setProperty(userHome, originalHome);
        }
    }

    @Test(expected = CustomRuntimeException.class)
    public void startDevModeHandler_prepareTasksThrows_handleThrows()
            throws Exception {
        CompletableFuture<Void> throwFuture = new CompletableFuture<>();
        throwFuture.completeExceptionally(new CustomRuntimeException());
        final String manifestJsonResponse = "{}";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);
        handler = WebpackHandler.start(port, lookup, npmFolder, throwFuture);
        try {
            waitForDevServer();
        } catch (CompletionException ignore) {
            /*
             * this is an expected exception thrown on join for the handler
             */

        }
        handler.handleRequest(Mockito.mock(VaadinSession.class),
                Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class));
    }

    @Test
    public void serveDevModeRequest_prepareTasksThrows_serveDevModeReturnsFalseAndDoesNotThrow()
            throws IOException {
        CompletableFuture<Void> throwFuture = new CompletableFuture<>();
        throwFuture.completeExceptionally(new CustomRuntimeException());
        handler = WebpackHandler.start(0, lookup, npmFolder, throwFuture);
        try {
            waitForDevServer();
        } catch (CompletionException ignore) {
            /*
             * this is an expected exception thrown on join for the handler
             */

        }
        try {
            handler.handleRequest(Mockito.mock(VaadinSession.class),
                    Mockito.mock(VaadinRequest.class),
                    Mockito.mock(VaadinResponse.class));
        } catch (CustomRuntimeException ignore) {
            // this is expected and we just ignore it
        }
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        /*
         * The following throws an exception without considering the fact that
         * dev mode start was unsuccessful and there is no need to serve
         * requests via serveDevModeRequest. In the current impl it doesn't
         * throw and just return false
         */
        Assert.assertFalse(handler.serveDevModeRequest(request, response));
    }

    @Test
    public void start_twoTimes_onlyOneHandlerInstanceIsCreated() {
        handler = WebpackHandler.start(0, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        /*
         * "start" one more time: there should not be another instance of dev
         * mode handler created
         */
        WebpackHandler anotherHandler = WebpackHandler.start(0, lookup,
                npmFolder, CompletableFuture.completedFuture(null));
        waitForDevServer(anotherHandler);

        /*
         * The handler instances are the same but there should be no attempt to
         * create another instance (which won't be stored anywhere), see below
         */
        Assert.assertSame(handler, anotherHandler);
    }

    @Test
    public void start_serverPortDoesNotWork_throws() throws Exception {
        exception.expect(CompletionException.class);
        exception.expectCause(Matchers.instanceOf(IllegalStateException.class));
        int port = WebpackHandler.getFreePort();
        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
    }

    @Test
    public void devModeNotReady_handleRequest_returnsHtml() throws Exception {
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(stream);
        handler.handleRequest(Mockito.mock(VaadinSession.class),
                Mockito.mock(VaadinRequest.class), response);
        String devModeNotReadyContents = stream.toString("UTF-8");
        Document document = Jsoup.parse(devModeNotReadyContents);
        Assert.assertTrue("expected a head child",
                document.head().children().size() > 0);
        Assert.assertTrue("expected a body child",
                document.body().children().size() > 0);
        Mockito.verify(response).setContentType("text/html;charset=utf-8");
    }

    @Test
    public void serveDevModeRequest_uriForDevmodeGizmo_goesToWebpack()
            throws Exception {
        HttpServletRequest request = prepareRequest(
                "/VAADIN/build/vaadin-devmodeGizmo-f679dbf313191ec3d018.cache.js");
        HttpServletResponse response = prepareResponse();

        final String manifestJsonResponse = "{ \"sw.js\": "
                + "\"sw.js\", \"index.html\": \"index.html\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        assertTrue(handler.serveDevModeRequest(request, response));
        assertEquals(HTTP_OK, responseStatus);
    }

    @Test
    public void serveDevModeRequest_uriWithScriptInjected_returnsImmediatelyAndSetsForbiddenStatus()
            throws Exception {
        HttpServletRequest request = prepareRequest(
                "/VAADIN/build/vaadin-devmodeGizmo-f679dbf313191ec3d018.cache%3f%22onload=%22alert(1)");
        HttpServletResponse response = prepareResponse();

        final String manifestJsonResponse = "{ \"sw.js\": "
                + "\"sw.js\", \"index.html\": \"index.html\" }";
        int port = prepareHttpServer(0, HTTP_OK, manifestJsonResponse);

        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();

        assertTrue(handler.serveDevModeRequest(request, response));
        assertEquals(HTTP_FORBIDDEN, responseStatus);
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeWithSlash_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/../vaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeWithBackslash_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something\\..\\vaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeWithEncodedBackslashUpperCase_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something%5C..%5Cvaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeWithEncodedBackslashLowerCase_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something%5c..%5cvaadin-bundle-1234.cache.js");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeInTheEndWithSlash_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/..");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeInTheEndWithBackslash_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something\\..");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeInTheEndWithEncodedBackslashUpperCase_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something%5C..");
    }

    @Test
    public void serveDevModeRequest_uriWithDirectoryChangeInTheEndWithEncodedBackslashLowerCase_returnsImmediatelyAndSetsForbiddenStatus()
            throws IOException {
        verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
                "/VAADIN/build/something%5c..");
    }

    private void verifyServeDevModeRequestReturnsTrueAndSetsProperStatusCode(
            String uri) throws IOException {
        HttpServletRequest request = prepareRequest(uri);
        HttpServletResponse response = prepareResponse();
        handler = WebpackHandler.start(lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        assertTrue(handler.serveDevModeRequest(request, response));

        Assert.assertEquals(HTTP_FORBIDDEN, responseStatus);
    }

    private VaadinServlet prepareServlet(int port)
            throws ServletException, IOException {
        handler = WebpackHandler.start(port, lookup, npmFolder,
                CompletableFuture.completedFuture(null));
        waitForDevServer();
        VaadinServlet servlet = new VaadinServlet();
        ServletConfig cfg = mock(ServletConfig.class);
        Mockito.when(cfg.getServletContext()).thenReturn(servletContext);

        List<String> paramNames = new ArrayList<>();
        paramNames.add(FrontendUtils.PARAM_TOKEN_FILE);

        Mockito.doAnswer(invocation -> Collections.enumeration(paramNames))
                .when(cfg).getInitParameterNames();

        File tokenFile = new File(temporaryFolder.getRoot(),
                "flow-build-info.json");
        FileUtils.write(tokenFile, "{}", StandardCharsets.UTF_8);
        Mockito.doAnswer(invocation -> tokenFile.getPath()).when(cfg)
                .getInitParameter(FrontendUtils.PARAM_TOKEN_FILE);

        servlet.init(cfg);
        return servlet;
    }

    private HttpServletRequest prepareRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> uri).when(request).getPathInfo();
        Mockito.doAnswer(invocation -> uri).when(request).getRequestURI();
        Mockito.doAnswer(invocation -> "/app").when(request).getServletPath();
        Mockito.doAnswer(invocation -> "GET").when(request).getMethod();
        Mockito.doAnswer(
                invocation -> Collections.enumeration(Arrays.asList("foo")))
                .when(request).getHeaderNames();
        Mockito.doAnswer(invocation -> "bar").when(request).getHeader("foo");
        return request;
    }

    private HttpServletResponse prepareResponse() throws IOException {
        responseStatus = 200; // The default response code is 200
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream output = mock(ServletOutputStream.class);
        Mockito.doAnswer(invocation -> output).when(response).getOutputStream();
        Mockito.doAnswer(invocation -> responseStatus = (int) invocation
                .getArguments()[0]).when(response).setStatus(Mockito.anyInt());
        Mockito.doAnswer(invocation -> responseError = (int) invocation
                .getArguments()[0]).when(response).sendError(Mockito.anyInt());
        return response;
    }

    private int prepareHttpServer(int port, int status, String response)
            throws Exception {
        if (port == 0) {
            port = WebpackHandler.getFreePort();
        }
        httpServer = createStubWebpackTcpListener(port, status, response);
        return port;
    }

    public static HttpServer createStubWebpackTcpListener(int port, int status,
            String response) throws Exception {
        try {
            HttpServer httpServer = HttpServer
                    .create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", exchange -> {
                exchange.sendResponseHeaders(status, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });
            httpServer.start();
            return httpServer;
        } catch (BindException e) {
            throw new IllegalArgumentException(
                    "Tried to create a server on port " + port
                            + " but it was already in use",
                    e);
        }
    }
}

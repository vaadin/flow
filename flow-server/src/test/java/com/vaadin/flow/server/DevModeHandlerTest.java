/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.WEBPACK_TEST_OUT_FILE;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;
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

@NotThreadSafe
@SuppressWarnings("restriction")
public class DevModeHandlerTest {

    private MockDeploymentConfiguration configuration;

    private HttpServer httpServer;
    private int responseStatus;
    private int responseError;
    private File npmFolder;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static class CustomRuntimeException extends RuntimeException {

    }

    private String baseDir;

    @Before
    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot().getAbsolutePath();

        npmFolder = temporaryFolder.getRoot();
        configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);

        new File(baseDir, FrontendUtils.WEBPACK_CONFIG).createNewFile();
        createStubWebpackServer("Compiled", 100, baseDir);
    }

    @After
    public void teardown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        if (handler != null) {
            handler.stop();
        }
    }

    public static void removeDevModeHandlerInstance() throws Exception {
        // Reset unique instance of DevModeHandler
        Field atomicHandler = DevModeHandler.class
                .getDeclaredField("atomicHandler");
        atomicHandler.setAccessible(true);
        AtomicReference<?> reference = (AtomicReference<?>) atomicHandler
                .get(null);
        reference.set(null);
    }

    @Ignore
    @Test
    public void should_CreateInstanceAndRunWebPack_When_DevModeAndNpmInstalled()
            throws Exception {
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNotNull(handler);
        handler.join();
        assertTrue(new File(baseDir,
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
        assertNull(DevModeHandler.getDevModeHandler().getFailedOutput());
        assertTrue(0 < DevModeHandler.getDevModeHandler().getPort());
        Thread.sleep(150); // NOSONAR
    }

    @Test
    @Ignore("Ignored due to failing rate on CI")
    public void should_Fail_When_WebpackPrematurelyExit() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Webpack exited prematurely");

        createStubWebpackServer("Foo", 0, baseDir);
        DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
    }

    @Test
    public void should_CaptureWebpackOutput_When_Failed() throws Exception {
        configuration.setApplicationOrSystemProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT, "100");
        createStubWebpackServer("Failed to compile", 300, baseDir);
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNotNull(handler);
        handler.join();
        int port = DevModeHandler.getDevModeHandler().getPort();
        assertTrue(port > 0);

        // Wait for server to stop running before checking the output stream
        Thread.sleep(350); // NOSONAR
        assertNotNull(
                "Got no output for the failed output even though expected output.",
                DevModeHandler.getDevModeHandler().getFailedOutput());
    }

    @Test
    public void shouldNot_CreateInstance_When_ProductionMode()
            throws Exception {
        configuration.setProductionMode(true);
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNull(handler);
    }

    @Test
    public void enableDevServerFalse_shouldNotCreateInstance()
            throws Exception {
        configuration.setEnableDevServer(false);
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertNull(handler);
    }

    @Test
    public void shouldNot_RunWebpack_When_WebpackRunning() throws Exception {
        int port = prepareHttpServer(0, HTTP_OK, "bar");
        DevModeHandler handler = DevModeHandler.start(port, configuration,
                npmFolder, CompletableFuture.completedFuture(null));
        handler.join();
        assertFalse(new File(baseDir,
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
    }

    @Test
    public void webpackNotInstalled_throws() throws Exception {
        exception.expectCause(CoreMatchers.isA(ExecutionFailedException.class));
        new File(baseDir, WEBPACK_SERVER).delete();
        DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
    }

    @Ignore
    @Test
    public void webpackIsNotExecutable_throws() {
        exception.expectCause(CoreMatchers.isA(ExecutionFailedException.class));
        // The set executable doesn't work in Windows and will always return
        // false
        boolean systemImplementsExecutable = new File(baseDir, WEBPACK_SERVER)
                .setExecutable(false);
        if (systemImplementsExecutable) {
            DevModeHandler.start(configuration, npmFolder,
                    CompletableFuture.completedFuture(null)).join();
        }
    }

    @Test
    public void webpackNotConfigured_throws() {
        exception.expectCause(CoreMatchers.isA(ExecutionFailedException.class));
        new File(baseDir, FrontendUtils.WEBPACK_CONFIG).delete();
        DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
    }

    @Test
    public void should_HandleJavaScriptRequests() {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.js");
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertTrue(handler.isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleNonVaadinRequests() {
        HttpServletRequest request = prepareRequest("/foo.js");
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleOtherRequests() {
        HttpServletRequest request = prepareRequest("/foo/VAADIN//foo.bar");
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void isDevModeRequest_dynamicResourcesAreNotDevModeRequest() {
        HttpServletRequest request = prepareRequest(
                "/" + StreamRequestHandler.DYN_RES_PREFIX + "foo");
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertFalse(handler.isDevModeRequest(request));
    }

    @Test
    public void should_HandleAnyAssetInVaadin() {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.bar");
        DevModeHandler handler = DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        assertTrue(handler.isDevModeRequest(request));
    }

    @Test(expected = ConnectException.class)
    public void should_ThrowAnException_When_WebpackNotListening()
            throws IOException {
        HttpServletRequest request = prepareRequest("/VAADIN//foo.js");
        DevModeHandler handler = DevModeHandler.start(0, configuration,
                npmFolder, CompletableFuture.completedFuture(null));
        handler.join();
        handler.serveDevModeRequest(request, null);
    }

    @Test
    public void webpack_forDifferentRequests_shouldHaveCorrectResponse()
            throws Exception {
        HttpServletRequest request = prepareRequest("/VAADIN//foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(0, HTTP_OK, "bar");

        DevModeHandler devModeHandler = DevModeHandler.start(port,
                configuration, npmFolder,
                CompletableFuture.completedFuture(null));
        devModeHandler.join();
        assertTrue(devModeHandler.serveDevModeRequest(request, response));
        assertEquals(HTTP_OK, responseStatus);

        httpServer.stop(0);
        prepareHttpServer(port, HTTP_NOT_FOUND, "");
        assertFalse(devModeHandler.serveDevModeRequest(request, response));
        assertEquals(200, responseStatus);

        httpServer.stop(0);
        prepareHttpServer(port, HTTP_UNAUTHORIZED, "");
        assertTrue(devModeHandler.serveDevModeRequest(request, response));
        assertEquals(HTTP_UNAUTHORIZED, responseError);

        httpServer.stop(0);
        exception.expect(ConnectException.class);
        devModeHandler.serveDevModeRequest(request, null);
    }

    @Test
    public void vaadinServlet_forDifferentRequests_shouldHaveCorrectResponse()
            throws Exception {
        HttpServletRequest request = prepareRequest("/VAADIN/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(0, HTTP_OK, "");

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
        VaadinService vaadinService = mock(VaadinService.class);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(configuration);

        String statsContent = "{}";
        int port = prepareHttpServer(0, HTTP_OK, statsContent);
        DevModeHandler.start(port, configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();

        assertEquals(statsContent,
                FrontendUtils.getStatsContent(vaadinService));
    }

    @Test
    public void should_reuseWebpackPort_AfterRestart() throws Exception {
        int port = prepareHttpServer(0, HTTP_OK, "foo");

        DevModeHandler.start(port, configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
        assertNotNull(DevModeHandler.getDevModeHandler());
        assertEquals(port, DevModeHandler.getDevModeHandler().getPort());

        removeDevModeHandlerInstance();
        assertNull(DevModeHandler.getDevModeHandler());

        DevModeHandler.start(configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
        assertNotNull(DevModeHandler.getDevModeHandler());
        assertEquals(port, DevModeHandler.getDevModeHandler().getPort());
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

            configuration.setApplicationOrSystemProperty(
                    InitParameters.REQUIRE_HOME_NODE_EXECUTABLE,
                    Boolean.TRUE.toString());
            DevModeHandler.start(configuration, npmFolder,
                    CompletableFuture.completedFuture(null)).join();
        } finally {
            System.setProperty(userHome, originalHome);
        }
    }

    @Test(expected = CustomRuntimeException.class)
    public void startDevModeHandler_prepareTasksThrows_handleThrows()
            throws IOException {
        CompletableFuture<Void> throwFuture = new CompletableFuture<>();
        throwFuture.completeExceptionally(new CustomRuntimeException());
        DevModeHandler handler = DevModeHandler.start(0, configuration,
                npmFolder, throwFuture);
        handler.handleRequest(Mockito.mock(VaadinSession.class),
                Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class));
    }

    @Test
    public void serveDevModeRequest_prepareTasksThrows_serveDevModeReturnsFalseAndDoesNotThrow()
            throws IOException {
        CompletableFuture<Void> throwFuture = new CompletableFuture<>();
        throwFuture.completeExceptionally(new CustomRuntimeException());
        DevModeHandler handler = DevModeHandler.start(0, configuration,
                npmFolder, throwFuture);
        try {
            handler.handleRequest(Mockito.mock(VaadinSession.class),
                    Mockito.mock(VaadinRequest.class),
                    Mockito.mock(VaadinResponse.class));
        } catch (CustomRuntimeException ignore) {
            // this is expected and we just ignore it
        }
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        // The following throws an exception without considering the
        // fact that dev mode start was unsuccessful and there is no need to
        // serve requests via serveDevModeRequest. In the current impl it
        // doesn't throw and just return false
        Assert.assertFalse(handler.serveDevModeRequest(request, response));
    }

    private VaadinServlet prepareServlet(int port)
            throws ServletException, IOException {
        DevModeHandler.start(port, configuration, npmFolder,
                CompletableFuture.completedFuture(null)).join();
        VaadinServlet servlet = new VaadinServlet();
        ServletConfig cfg = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        Mockito.doAnswer(invocation -> ctx.getClass().getClassLoader())
                .when(ctx).getClassLoader();
        Mockito.doAnswer(invocation -> ctx).when(cfg).getServletContext();

        List<String> paramNames = new ArrayList<>();
        paramNames.add(FrontendUtils.PARAM_TOKEN_FILE);

        Mockito.doAnswer(invocation -> Collections.enumeration(paramNames))
                .when(cfg).getInitParameterNames();

        File tokenFile = new File(temporaryFolder.getRoot(),
                "flow-build-info.json");
        FileUtils.write(tokenFile, "{}", StandardCharsets.UTF_8);
        Mockito.doAnswer(invocation -> tokenFile.getPath()).when(cfg)
                .getInitParameter(FrontendUtils.PARAM_TOKEN_FILE);

        Mockito.doAnswer(
                invocation -> Collections.enumeration(Collections.emptyList()))
                .when(ctx).getInitParameterNames();
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
            port = DevModeHandler.getFreePort();
        }
        httpServer = createStubWebpackTcpListener(port, status, response);
        return port;
    }

    public static HttpServer createStubWebpackTcpListener(int port, int status,
            String response) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port),
                0);
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }
}

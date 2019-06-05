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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.WEBPACK_TEST_OUT_FILE;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
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

    private MockServletContext servletContext;
    private VaadinContext vaadinContext;
    private HttpServer httpServer;
    private int responseStatus;
    private File npmFolder;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        npmFolder = temporaryFolder.getRoot();
        System.setProperty("user.dir", npmFolder.getAbsolutePath());
        servletContext = new MockServletContext();
        vaadinContext = new VaadinServletContext(servletContext);
        configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);

        new File(getBaseDir(), FrontendUtils.WEBPACK_CONFIG).createNewFile();
        createStubWebpackServer("Compiled", 100);
    }

    @After
    public void teardown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }

        // Reset unique instance in DevModeHandler
        Field atomicHandler = DevModeHandler.class
                .getDeclaredField("atomicHandler");
        atomicHandler.setAccessible(true);
        atomicHandler.set(null, new AtomicReference<>());
    }

    @Test
    public void should_CreateInstanceAndRunWebPack_When_DevModeAndNpmInstalled()
            throws Exception {
        assertNotNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
        assertTrue(new File(getBaseDir(),
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
        assertNull(DevModeHandler.getDevModeHandler().getFailedOutput());
        Thread.sleep(150); // NOSONAR
    }

    @Test
    @Ignore("Ignored due to failing rate on CI")
    public void should_Fail_When_WebpackPrematurelyExit() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Webpack exited prematurely");

        createStubWebpackServer("Foo", 0);
        DevModeHandler.start(vaadinContext, configuration, npmFolder);
    }

    @Test
    public void should_CreateInstance_After_TimeoutWaitingForPattern()
            throws Exception {
        configuration.setApplicationOrSystemProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT, "100");
        createStubWebpackServer("Foo", 300);
        assertNotNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
        assertTrue(DevModeHandler.getRunningPort(vaadinContext) > 0);
        Thread.sleep(350); // NOSONAR
    }

    @Test
    public void should_CaptureWebpackOutput_When_Failed() throws Exception {
        configuration.setApplicationOrSystemProperty(
                SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT, "100");
        createStubWebpackServer("Failed to compile", 300);
        assertNotNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
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
        assertNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
    }

    @Test
    public void shouldNot_CreateInstance_When_BowerMode() throws Exception {
        configuration.setProductionMode(true);
        assertNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
        Thread.sleep(150); // NOSONAR
    }

    @Test
    public void should_RunWebpack_When_WebpackNotListening() throws Exception {
        DevModeHandler.start(vaadinContext, configuration, npmFolder);
        assertTrue(new File(getBaseDir(),
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
        Thread.sleep(150); // NOSONAR
    }

    @Test
    public void shouldNot_RunWebpack_When_WebpackRunning() throws Exception {
        prepareHttpServer(HTTP_OK, "bar");
        DevModeHandler.start(vaadinContext, configuration, npmFolder);
        assertFalse(new File(getBaseDir(),
                FrontendUtils.DEFAULT_NODE_DIR + WEBPACK_TEST_OUT_FILE)
                        .canRead());
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackNotInstalled()
            throws Exception {
        new File(getBaseDir(), WEBPACK_SERVER).delete();
        assertNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackIsNotExecutable() {
        // The set executable doesn't work in Windows and will always return
        // false
        boolean systemImplementsExecutable = new File(getBaseDir(),
                WEBPACK_SERVER).setExecutable(false);
        if (systemImplementsExecutable) {
            assertNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
        }
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackNotConfigured() {
        new File(getBaseDir(), FrontendUtils.WEBPACK_CONFIG).delete();
        assertNull(DevModeHandler.start(vaadinContext, configuration, npmFolder));
    }

    @Test
    public void should_HandleJavaScriptRequests() {
        HttpServletRequest request = prepareRequest("/foo.js");
        assertTrue(new DevModeHandler(0).isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleOtherRequests() {
        HttpServletRequest request = prepareRequest("/foo.bar");
        assertFalse(new DevModeHandler(0).isDevModeRequest(request));
    }

    @Test(expected = ConnectException.class)
    public void should_ThrowAnException_When_WebpackNotListening()
            throws IOException {
        HttpServletRequest request = prepareRequest("/foo.js");
        new DevModeHandler(0).serveDevModeRequest(request, null);
    }

    @Test
    public void should_ReturnTrue_When_WebpackResponseOK() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_OK, "bar");

        assertTrue(new DevModeHandler(port).serveDevModeRequest(request,
                response));
        assertEquals(HTTP_OK, responseStatus);
    }

    @Test
    public void should_ReturnFalse_When_WebpackResponseNotFound()
            throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_NOT_FOUND, "");

        assertFalse(new DevModeHandler(port).serveDevModeRequest(request,
                response));
        assertEquals(200, responseStatus);
    }

    @Test
    public void should_ReturnTrue_When_OtherResponseCodes() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_UNAUTHORIZED, "");

        assertTrue(new DevModeHandler(port).serveDevModeRequest(request,
                response));
        assertEquals(HTTP_UNAUTHORIZED, responseStatus);
    }

    @Test(expected = ConnectException.class)
    public void servlet_should_ThrowAnException_When_WebpackNotListening()
            throws Exception {
        VaadinServlet servlet = prepareServlet();
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        servlet.service(request, response);
        Thread.sleep(150); // NOSONAR
    }

    @Test
    public void servlet_should_GetValidResponse_When_WebpackListening()
            throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        prepareHttpServer(HTTP_OK, "");

        prepareServlet().service(request, response);
        assertEquals(HTTP_OK, responseStatus);
    }

    @Test
    public void stopDevMode_devModeHanlderNull() throws InterruptedException {
        DevModeHandler handler = DevModeHandler.start(vaadinContext, configuration, npmFolder);
        handler.stop();
        assertNull(DevModeHandler.getDevModeHandler());
        Thread.sleep(150); // NOSONAR
    }

    private VaadinServlet prepareServlet() throws ServletException {
        DevModeHandler.start(vaadinContext, configuration, npmFolder);
        VaadinServlet servlet = new VaadinServlet();
        ServletConfig cfg = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        Mockito.doAnswer(invocation -> ctx.getClass().getClassLoader()).when(ctx).getClassLoader();
        Mockito.doAnswer(invocation -> ctx).when(cfg).getServletContext();
        Mockito.doAnswer(
                invocation -> Collections.enumeration(Collections.emptyList()))
                .when(cfg).getInitParameterNames();
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
                .getArguments()[0]).when(response).sendError(Mockito.anyInt());
        return response;
    }

    private int prepareHttpServer(int status, String response)
            throws Exception {
        int port = DevModeHandler.getFreePort();
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        httpServer.start();
        DevModeHandler.setRunningPort(vaadinContext, port);
        return port;
    }
}

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT;
import static com.vaadin.flow.server.DevModeHandler.WEBAPP_FOLDER;
import static com.vaadin.flow.server.DevModeHandler.WEBPACK_CONFIG;
import static com.vaadin.flow.server.DevModeHandler.WEBPACK_SERVER;
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

    private static final String TEST_FILE = "webpack-out.test";
    private HttpServer httpServer;
    private int responseStatus;


    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() throws IOException {
        configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);
        configuration.setApplicationOrSystemProperty(SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS, "true");
        createWebpackScript("Compiled", 100);
    }

    private void createWebpackScript(String readyString, int milliSecondsToRun) throws IOException {
        File serverFile = new File(WEBPACK_SERVER);
        serverFile.getParentFile().mkdirs();
        serverFile.createNewFile();
        serverFile.setExecutable(true);
        Files.write(Paths.get(serverFile.toURI()), (
            "#!/usr/bin/env node\n" +
            "const fs = require('fs');\n" +
            "const args = String(process.argv);\n" +
            "fs.writeFileSync('" + TEST_FILE + "', args);\n" +
            "console.log(args + '\\n[wps]: Compiled.');\n" +
            "setTimeout(() => {}, " + milliSecondsToRun + ");\n").getBytes());
        new File(WEBPACK_CONFIG).createNewFile();
    }

    @After
    public void teardown() throws Exception {
        FileUtils.deleteDirectory(new File("node_modules"));
        FileUtils.deleteQuietly(new File(PACKAGE_JSON));
        FileUtils.deleteQuietly(new File(WEBPACK_CONFIG));
        FileUtils.deleteQuietly(new File(WEBAPP_FOLDER + TEST_FILE));
        if (httpServer != null) {
            httpServer.stop(0);
        }

        // Reset unique instance in DevModeHandler
        Field atomicHandler = DevModeHandler.class.getDeclaredField("atomicHandler");
        atomicHandler.setAccessible(true);
        atomicHandler.set(null, new AtomicReference<>());
    }

    @Test
    public void should_CreateInstanceAndRunWebPack_When_DevModeAndNpmInstalled() throws Exception {
        assertNotNull(DevModeHandler.start(configuration));
        assertTrue(new File(WEBAPP_FOLDER + TEST_FILE).canRead());
        Thread.sleep(150); //NOSONAR
    }

    @Test
    @Ignore("Ignored due to failing rate on CI")
    public void should_Fail_When_WebpackPrematurelyExit() throws Exception {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Webpack exited prematurely");

        createWebpackScript("Foo", 0);
        DevModeHandler.start(configuration);
    }

    @Test
    public void should_CreateInstance_After_TimeoutWaitingForPattern() throws Exception {
        configuration.setApplicationOrSystemProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT, "100");
        createWebpackScript("Foo", 300);
        assertNotNull(DevModeHandler.start(configuration));
        assertTrue(Integer.getInteger("vaadin." + SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, 0) > 0);
        Thread.sleep(350); //NOSONAR
    }

    @Test
    public void shouldNot_CreateInstance_When_ProductionMode() throws Exception {
        configuration.setProductionMode(true);
        assertNull(DevModeHandler.start(configuration));
    }

    @Test
    public void shouldNot_CreateInstance_When_BowerMode() throws Exception {
        configuration.setProductionMode(true);
        assertNull(DevModeHandler.start(configuration));
        Thread.sleep(150); //NOSONAR
    }

    @Test
    public void should_RunWebpack_When_WebpackNotListening() throws Exception {
        DevModeHandler.start(configuration);
        assertTrue(new File(WEBAPP_FOLDER + TEST_FILE).canRead());
        Thread.sleep(150); //NOSONAR
    }

    @Test
    public void shouldNot_RunWebpack_When_WebpackRunning() throws Exception {
        prepareHttpServer(HTTP_OK, "bar");
        DevModeHandler.start(configuration);
        assertFalse(new File(WEBAPP_FOLDER + TEST_FILE).canRead());
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackNotInstalled() throws Exception {
        new File(WEBPACK_SERVER).delete();
        assertNull(DevModeHandler.start(configuration));
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackIsNotExecutable()  {
        // The set executable doesn't work in Windows and will always return false
        boolean systemImplementsExecutable = new File(WEBPACK_SERVER).setExecutable(false);
        if(systemImplementsExecutable) {
            assertNull(DevModeHandler.start(configuration));
        }
    }

    @Test
    public void shouldNot_CreateInstance_When_WebpackNotConfigured()  {
        new File(WEBPACK_CONFIG).delete();
        assertNull(DevModeHandler.start(configuration));
    }

    @Test
    public void should_HandleJavaScriptRequests() {
        HttpServletRequest request = prepareRequest("/foo.js");
        assertTrue(new DevModeHandler(configuration, 0).isDevModeRequest(request));
    }

    @Test
    public void shouldNot_HandleOtherRequests() {
        HttpServletRequest request = prepareRequest("/foo.bar");
        assertFalse(new DevModeHandler(configuration, 0).isDevModeRequest(request));
    }

    @Test(expected = ConnectException.class)
    public void should_ThrowAnException_When_WebpackNotListening() throws IOException {
        HttpServletRequest request = prepareRequest("/foo.js");
        new DevModeHandler(configuration, 0).serveDevModeRequest(request, null);
    }

    @Test
    public void should_ReturnTrue_When_WebpackResponseOK() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_OK, "bar");

        assertTrue(new DevModeHandler(configuration, port).serveDevModeRequest(request, response));
        assertEquals(HTTP_OK, responseStatus);
    }

    @Test
    public void should_ReturnFalse_When_WebpackResponseNotFound() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_NOT_FOUND, "");

        assertFalse(new DevModeHandler(configuration, port).serveDevModeRequest(request, response));
        assertEquals(0, responseStatus);
    }

    @Test
    public void should_ReturnTrue_When_OtherResponseCodes() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        int port = prepareHttpServer(HTTP_UNAUTHORIZED, "");

        assertTrue(new DevModeHandler(configuration, port).serveDevModeRequest(request, response));
        assertEquals(HTTP_UNAUTHORIZED, responseStatus);
    }

    @Test(expected = ConnectException.class)
    public void servlet_should_ThrowAnException_When_WebpackNotListening() throws Exception {
        VaadinServlet servlet = prepareServlet();
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        servlet.service(request, response);
        Thread.sleep(150); //NOSONAR
    }

    @Test
    public void servlet_should_GetValidResponse_When_WebpackListening() throws Exception {
        HttpServletRequest request = prepareRequest("/foo.js");
        HttpServletResponse response = prepareResponse();
        prepareHttpServer(HTTP_OK, "");

        prepareServlet().service(request, response);
        assertEquals(HTTP_OK, responseStatus);
    }

    private VaadinServlet prepareServlet() throws ServletException {
        DevModeHandler.start(configuration);
        VaadinServlet servlet = new VaadinServlet();
        ServletConfig cfg = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        Mockito.doAnswer(invocation -> ctx).when(cfg).getServletContext();
        Mockito.doAnswer(invocation -> Collections.enumeration(Collections.emptyList())).when(cfg)
                .getInitParameterNames();
        Mockito.doAnswer(invocation -> Collections.enumeration(Collections.emptyList())).when(ctx)
                .getInitParameterNames();
        servlet.init(cfg);
        return servlet;
    }

    private HttpServletRequest prepareRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> uri).when(request).getPathInfo();
        Mockito.doAnswer(invocation -> uri).when(request).getRequestURI();
        Mockito.doAnswer(invocation -> "/app").when(request).getServletPath();
        Mockito.doAnswer(invocation -> "GET").when(request).getMethod();
        Mockito.doAnswer(invocation -> Collections.enumeration(Arrays.asList("foo"))).when(request).getHeaderNames();
        Mockito.doAnswer(invocation -> "bar").when(request).getHeader("foo");
        return request;
    }

    private HttpServletResponse prepareResponse() throws IOException {
        responseStatus = 0;
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream output = mock(ServletOutputStream.class);
        Mockito.doAnswer(invocation -> output).when(response).getOutputStream();
        Mockito.doAnswer(invocation -> responseStatus = (int) invocation.getArguments()[0]).when(response)
                .sendError(Mockito.anyInt());
        return response;
    }

    private int prepareHttpServer(int status, String response) throws Exception {
        int port = DevModeHandler.getFreePort();
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        httpServer.start();
        configuration.setApplicationOrSystemProperty(SERVLET_PARAMETER_DEVMODE_WEBPACK_RUNNING_PORT, String.valueOf(port));
        return port;
    }
}

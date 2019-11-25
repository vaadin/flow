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
package com.vaadin.flow.server.communication;

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;
import static com.vaadin.flow.server.DevModeHandlerTest.createStubWebpackTcpListener;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;

public class VaadinAppShellTest {

    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private VaadinSession session;
    private IndexHtmlRequestHandler indexHtmlRequestHandler;
    private VaadinResponse response;
    private ByteArrayOutputStream responseOutput;
    private MockDeploymentConfiguration deploymentConfiguration;
    private HttpServer httpServer;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        session = mocks.getSession();
        response = Mockito.mock(VaadinResponse.class);
        responseOutput = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(responseOutput);
        deploymentConfiguration = mocks.getDeploymentConfiguration();
        deploymentConfiguration.setEnableDevServer(false);
        deploymentConfiguration.setClientSideMode(true);
        indexHtmlRequestHandler = new IndexHtmlRequestHandler();
    }

    @After
    public void tearDown() {
        session.unlock();
        mocks.cleanup();
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        if (handler != null) {
            handler.stop();
        }
    }    

    
    @Test
    public void serveIndexHtml_requestWithRootPath_serveContentFromTemplate()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(
                "Response should have content from the index.html template",
                indexHtml.contains("index.html template content"));
        Assert.assertTrue(
                "Response should have styles for the system-error dialogs",
                indexHtml.contains(".v-system-error"));
    }

    @Test
    public void should_attachWebpackErrors() throws Exception {
        // Create a webpack-dev-server command that should fail the compilation
        File npmFolder = temporaryFolder.getRoot();
        String baseDir = npmFolder.getAbsolutePath();
        new File(baseDir, FrontendUtils.WEBPACK_CONFIG).createNewFile();
        createStubWebpackServer("Failed to compile", 300, baseDir);

        // Create a DevModeHandler
        deploymentConfiguration.setEnableDevServer(true);
        deploymentConfiguration.setProductionMode(false);
        DevModeHandler handler = DevModeHandler.start(0, deploymentConfiguration, npmFolder);
        // Ask to the DevModeHandler for the computed random port
        Method runningPort = DevModeHandler.class.getDeclaredMethod("getRunningDevServerPort");
        runningPort.setAccessible(true);
        int port = (Integer)runningPort.invoke(handler);

        // Configure webpack-dev-server tcp listener to return the `index.html` content
        httpServer = createStubWebpackTcpListener(port, 200, "<html></html>");

        // Send the request
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(
                "Should have a system error dialog",
                indexHtml.contains("<div class=\"v-system-error\">"));
        Assert.assertTrue(
                "Should show webpack failure error",
                indexHtml.contains("Failed to compile"));
    }

    private VaadinServletRequest createVaadinRequest(String pathInfo) {
        HttpServletRequest request = createRequest(pathInfo);
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        Mockito.doAnswer(invocation -> pathInfo).when(request).getPathInfo();
        Mockito.doAnswer(invocation -> new StringBuffer(pathInfo)).when(request)
                .getRequestURL();
        return request;
    }

}

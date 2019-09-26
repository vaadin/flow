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

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.tests.util.MockDeploymentConfiguration;

public class ClientIndexHandlerTest {

    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private VaadinSession session;
    private ClientIndexHandler clientIndexBootstrapHandler;
    private VaadinResponse response;
    private ByteArrayOutputStream responseOutput;

    @Before
    public void setUp() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        session = mocks.getSession();
        response = Mockito.mock(VaadinResponse.class);
        responseOutput = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(responseOutput);
        MockDeploymentConfiguration deploymentConfiguration = mocks
                .getDeploymentConfiguration();
        deploymentConfiguration.setEnableDevServer(false);
        deploymentConfiguration.setClientSideMode(true);
        clientIndexBootstrapHandler = new ClientIndexHandler();
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_serveContentFromTemplate()
            throws IOException {
        clientIndexBootstrapHandler
                .synchronizedHandleRequest(session, createVaadinRequest("/"),
                        response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(
                "Response should have content from the index.html template",
                indexHtml.contains("index.html template content"));
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_hasBaseHrefElement()
            throws IOException {
        clientIndexBootstrapHandler
                .synchronizedHandleRequest(session, createVaadinRequest("/"),
                        response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Response should have correct base href",
                indexHtml.contains("<base href=\".\""));
    }

    @Test
    public void serveIndexHtml_requestWithSomePath_hasBaseHrefElement()
            throws IOException {
        clientIndexBootstrapHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/some/path"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Response should have correct base href",
                indexHtml.contains("<base href=\"./..\""));
    }

    @Test
    public void canHandleRequest_requestWithRootPath_handleRequest() {
        boolean canHandleRequest = clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/"));
        Assert.assertTrue("The handler should handle a root path request",
                canHandleRequest);
    }

    @Test
    public void canHandleRequest_requestWithRoute_handleRequest() {
        Assert.assertTrue(
                "The handler should handle a route with " + "parameter",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/some/route")));
        Assert.assertTrue("The handler should handle a normal route",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/myroute")));
        Assert.assertTrue("The handler should handle a directory request",
                clientIndexBootstrapHandler.canHandleRequest(
                createVaadinRequest("/myroute/ends/withslash/")));
        Assert.assertTrue(
                "The handler should handle a request if it has "
                        + "extension pattern in the middle of the path",
                clientIndexBootstrapHandler.canHandleRequest(
                        createVaadinRequest("/documentation/10.0.x1/flow")));
    }

    @Test
    public void canHandleRequest_requestWithExtension_ignoreRequest() {
        Assert.assertFalse(
                "The handler should not handle request with extension",
                clientIndexBootstrapHandler.canHandleRequest(
                        createVaadinRequest("/nested/picture.png")));
        Assert.assertFalse(
                "The handler should not handle request with capital extension",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/nested/CAPITAL.PNG")));
        Assert.assertFalse(
                "The handler should not handle request with extension",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/script.js")));
        Assert.assertFalse(
                "The handler should not handle request with extension",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/music.mp3")));
        Assert.assertFalse(
                "The handler should not handle request with only extension",
                clientIndexBootstrapHandler
                .canHandleRequest(createVaadinRequest("/.htaccess")));
    }

    @Test
    public void bootstrapListener_addListener_responseIsModified()
            throws IOException {
        service.addClientIndexBootstrapListener(evt -> evt.getDocument().head()
                .getElementsByTag("script").remove());
        service.addClientIndexBootstrapListener(evt -> {
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.1");
        });
        service.addClientIndexBootstrapListener(evt -> evt.getDocument().head()
                .appendElement("script").attr("src", "testing.2"));

        clientIndexBootstrapHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);
        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(2, scripts.size());
        Assert.assertEquals("testing.1", scripts.get(0).attr("src"));
        Assert.assertEquals("testing.2", scripts.get(1).attr("src"));
    }

    @Test
    public void should_add_initialUidl()
            throws IOException {

        // Test that ClientIndexInitialListener is in the configuration file:
        // `resources/META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener`

        clientIndexBootstrapHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("", scripts.get(0).attr("initial"));
    }

    @After
    public void tearDown() {
        session.unlock();
        mocks.cleanup();
    }

    private VaadinServletRequest createVaadinRequest(String pathInfo) {
        HttpServletRequest request = createRequest(pathInfo);
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        Mockito.doAnswer(invocation -> pathInfo).when(request).getPathInfo();
        Mockito.doAnswer(invocation -> new StringBuffer(pathInfo)).when(request).getRequestURL();
        return request;
    }
}

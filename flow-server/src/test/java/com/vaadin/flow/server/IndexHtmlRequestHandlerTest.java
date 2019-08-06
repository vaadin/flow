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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.tests.util.MockDeploymentConfiguration;

public class IndexHtmlRequestHandlerTest {

    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private VaadinSession session;
    private IndexHtmlRequestHandler indexHtmlRequestHandler;
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
        deploymentConfiguration.setClientSideBootstrapMode(true);
        indexHtmlRequestHandler = new IndexHtmlRequestHandler();
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_serveContentFromTemplate()
            throws IOException {
        indexHtmlRequestHandler
                .synchronizedHandleRequest(session, createVaadinRequest(""),
                        response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(indexHtml.contains("index.html template content"));
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler
                .synchronizedHandleRequest(session, createVaadinRequest(""),
                        response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(indexHtml.contains("<base href=\".\""));
    }

    @Test
    public void serveIndexHtml_requestWithSomePath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/some/path"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(indexHtml.contains("<base href=\"./..\""));
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_hasInjectedBundles()
            throws IOException {
        indexHtmlRequestHandler
                .synchronizedHandleRequest(session, createVaadinRequest(""),
                        response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue(indexHtml.contains(
                "<script type=\"module\" defer src=\"./VAADIN/build/index-1111.cache.js\"></script>"));
        Assert.assertTrue(indexHtml.contains(
                "<script type=\"text/javascript\" defer src=\"./VAADIN/build/index.es5-2222.cache.js\" nomodule></script>"));
    }

    @Test
    public void canHandleRequest_requestWithRootPath_handleRequest() {
        boolean canHandleRequest = indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest(""));
        Assert.assertTrue(canHandleRequest);
    }

    @Test
    public void canHandleRequest_requestWithRoute_handleRequest() {
        Assert.assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/some/route")));
        Assert.assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/myroute")));
        Assert.assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createVaadinRequest("/myroute/ends/withslash/")));
        Assert.assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createVaadinRequest("/documentation/10.0.10/flow")));
    }

    @Test
    public void canHandleRequest_requestWithExtension_ignoreRequest() {
        Assert.assertFalse(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/picture.png")));
        Assert.assertFalse(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/CAPITAL.PNG")));
        Assert.assertFalse(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/script.js")));
        Assert.assertFalse(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/music.mp3")));
        Assert.assertFalse(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/.htaccess")));
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
        return request;
    }
}

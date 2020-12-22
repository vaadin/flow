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
package com.vaadin.flow.server.communication;

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinAppShellInitializerTest.AppShellWithPWA;
import com.vaadin.flow.server.startup.VaadinAppShellInitializerTest.MyAppShellWithConfigurator;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;
import static com.vaadin.flow.server.DevModeHandlerTest.createStubWebpackTcpListener;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class IndexHtmlRequestHandlerTest {
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
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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
        deploymentConfiguration.useDeprecatedV14Bootstrapping(false);
        indexHtmlRequestHandler = new IndexHtmlRequestHandler();
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
    public void serveNotFoundIndexHtml_requestWithRootPath_failsWithIOException()
            throws IOException {
        ClassLoader mockLoader = Mockito.mock(ClassLoader.class);
        VaadinServletService vaadinService = Mockito
                .mock(VaadinServletService.class);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(vaadinService.getClassLoader()).thenReturn(mockLoader);
        Mockito.when(mockLoader.getResourceAsStream(Mockito.anyString()))
                .thenReturn(null);

        VaadinServletRequest vaadinRequest = Mockito
                .mock(VaadinServletRequest.class);
        Mockito.when(vaadinRequest.getService()).thenReturn(vaadinService);

        String path;
        if (DEFAULT_FRONTEND_DIR.endsWith(File.separator)) {
            path = DEFAULT_FRONTEND_DIR + "index.html";
        } else {
            path = DEFAULT_FRONTEND_DIR + File.separatorChar + "/index.html";
        }
        String expectedError = String
                .format("Failed to load content of '%1$s'. "
                        + "It is required to have '%1$s' file when "
                        + "using client side bootstrapping.", path);

        exceptionRule.expect(IOException.class);
        exceptionRule.expectMessage(expectedError);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                vaadinRequest, response);
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Response should have correct base href",
                indexHtml.contains("<base href=\".\""));
    }

    @Test
    public void serveIndexHtml_requestWithSomePath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/some/path"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Response should have correct base href",
                indexHtml.contains("<base href=\"./..\""));
    }

    @Test
    public void canHandleRequest_requestWithRootPath_handleRequest() {
        boolean canHandleRequest = indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/"));
        Assert.assertTrue("The handler should handle a root path request",
                canHandleRequest);
    }

    @Test
    public void canHandleRequest_withoutBootstrapUrlPredicate() {
        Assert.assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/picture.png")));
        Assert.assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/CAPITAL.PNG")));
        Assert.assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("com.foo.MyTest")));
    }

    @Test
    public void canHandleRequest_withBootstrapUrlPredicate() {

        service.setBootstrapUrlPredicate(req -> {
            // refuse any request with extension
            return !req.getPathInfo().matches(".+\\.[A-z][A-z\\d]+$");
        });

        Assert.assertTrue(
                "The handler should handle a route with " + "parameter",
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/some/route")));
        Assert.assertTrue("The handler should handle a normal route",
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/myroute")));
        Assert.assertTrue("The handler should handle a directory request",
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/myroute/ends/withslash/")));
        Assert.assertTrue(
                "The handler should handle a request if it has "
                        + "extension pattern in the middle of the path",
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/documentation/10.0.x1/flow")));

        Assert.assertFalse(
                "The handler should not handle request with extension",
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/nested/picture.png")));
        Assert.assertFalse(
                "The handler should not handle request with capital extension",
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/nested/CAPITAL.PNG")));
        Assert.assertFalse(
                "The handler should not handle request with extension",
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/script.js")));

        Assert.assertFalse(
                "The handler should not handle request with extension",
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/music.mp3")));
        Assert.assertFalse(
                "The handler should not handle request with only extension",
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/.htaccess")));
    }

    @Test
    public void bootstrapListener_addListener_responseIsModified()
            throws IOException {
        service.addIndexHtmlRequestListener(evt -> evt.getDocument().head()
                .getElementsByTag("script").remove());
        service.addIndexHtmlRequestListener(evt -> {
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.1");
        });
        service.addIndexHtmlRequestListener(evt -> evt.getDocument().head()
                .appendElement("script").attr("src", "testing.2"));

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
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
    public void should_add_initialUidl_when_includeInitialBootstrapUidl()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("", scripts.get(0).attr("initial"));
        Assert.assertTrue(
                scripts.get(0).toString().contains("Could not navigate"));

        Mockito.verify(session, Mockito.times(1)).setAttribute(SERVER_ROUTING,
                Boolean.TRUE);
    }

    @Test
    public void should_not_add_initialUidl_when_not_includeInitialBootstrapUidl()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("window.Vaadin = {TypeScript: {}};",
                scripts.get(0).childNode(0).toString());
        Assert.assertEquals("", scripts.get(0).attr("initial"));

        Mockito.verify(session, Mockito.times(0)).setAttribute(SERVER_ROUTING,
                Boolean.TRUE);
    }

    @Test
    public void should_initialize_UI_and_add_initialUidl_when_valid_route()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        service.setBootstrapInitialPredicate(request -> {
            return request.getPathInfo().equals("/");
        });

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("", scripts.get(0).attr("initial"));
        String scriptContent = scripts.get(0).toString();
        Assert.assertTrue(scriptContent.contains("Could not navigate"));
        Assert.assertFalse("Initial object content should not be escaped",
                scriptContent.contains("&lt;")
                        || scriptContent.contains("&gt;"));
        Assert.assertNotNull(UI.getCurrent());
    }

    @Test
    public void should_not_initialize_UI_and_add_initialUidl_when_invalid_route()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        service.setBootstrapInitialPredicate(request -> {
            return request.getPathInfo().equals("/");
        });

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/foo"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertEquals("window.Vaadin = {TypeScript: {}};",
                scripts.get(0).childNode(0).toString());
        Assert.assertEquals("", scripts.get(0).attr("initial"));
        Assert.assertNull(UI.getCurrent());
    }

    @Test
    public void should_getter_UI_return_not_empty_when_includeInitialBootstrapUidl()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        VaadinRequest request = createVaadinRequest("/");

        indexHtmlRequestHandler.synchronizedHandleRequest(session,

                request, response);

        ArgumentCaptor<IndexHtmlResponse> captor = ArgumentCaptor
                .forClass(IndexHtmlResponse.class);

        verify(request.getService()).modifyIndexHtmlResponse(captor.capture());

        Assert.assertNotNull(captor.getValue().getUI());
    }

    @Test
    public void should_getter_UI_return_empty_when_not_includeInitialBootstrapUidl()
            throws IOException {
        VaadinRequest request = createVaadinRequest("/");

        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);

        ArgumentCaptor<IndexHtmlResponse> captor = ArgumentCaptor
                .forClass(IndexHtmlResponse.class);

        verify(request.getService()).modifyIndexHtmlResponse(captor.capture());

        Assert.assertEquals(Optional.empty(), captor.getValue().getUI());
    }

    @Test
    public void should_include_token_in_dom_when_return_not_null_csrfToken_in_session()
            throws IOException {
        Mockito.when(session.getCsrfToken()).thenReturn("foo");
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertTrue(scripts.get(0).childNode(0).toString().contains(
                "window.Vaadin = {TypeScript: {\"csrfToken\":\"foo\""));
        Assert.assertEquals("", scripts.get(0).attr("initial"));
    }

    @Test
    public void should_not_include_token_in_dom_when_return_null_csrfToken_in_session()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Assert.assertEquals(1, scripts.size());
        Assert.assertFalse(scripts.get(0).childNode(0).toString()
                .contains("window.Vaadin = {Flow: {\"csrfToken\":"));
        Assert.assertEquals("", scripts.get(0).attr("initial"));
    }

    @Test
    public void should_use_client_routing_when_there_is_a_router_call()
            throws IOException {

        deploymentConfiguration.setEagerServerLoad(true);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        Mockito.verify(session, Mockito.times(1)).setAttribute(SERVER_ROUTING,
                Boolean.TRUE);
        Mockito.verify(session, Mockito.times(0)).setAttribute(SERVER_ROUTING,
                Boolean.FALSE);

        ((JavaScriptBootstrapUI) UI.getCurrent()).connectClient("foo", "bar",
                "/foo", "");

        Mockito.verify(session, Mockito.times(1)).setAttribute(SERVER_ROUTING,
                Boolean.FALSE);
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

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        mockApplicationConfiguration(appConfig);

        DevModeHandler handler = DevModeHandler.start(0,
                Lookup.of(appConfig, ApplicationConfiguration.class), npmFolder,
                CompletableFuture.completedFuture(null));
        Method join = DevModeHandler.class.getDeclaredMethod("join");
        join.setAccessible(true);
        join.invoke(handler);
        // Ask to the DevModeHandler for the computed random port
        Method runningPort = DevModeHandler.class
                .getDeclaredMethod("getRunningDevServerPort", File.class);
        runningPort.setAccessible(true);
        int port = (Integer) runningPort.invoke(handler, npmFolder);

        // Configure webpack-dev-server tcp listener to return the `index.html`
        // content
        httpServer = createStubWebpackTcpListener(port, 200, "<html></html>");

        // Send the request
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Should have a system error dialog",
                indexHtml.contains(
                        "<div class=\"v-system-error\" onclick=\"this.parentElement.removeChild(this)\">"));
        Assert.assertTrue("Should show webpack failure error",
                indexHtml.contains("Failed to compile"));
    }

    @Test
    public void should_not_add_metaElements_when_not_appShellPresent()
            throws Exception {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        // the template used in clientSide mode already has two metas
        // see: src/main/resources/com/vaadin/flow/server/frontend/index.html
        Elements elements = document.head().getElementsByTag("meta");
        assertEquals(2, elements.size());
    }

    @Test
    public void should_add_metaAndPwa_Inline_Elements_when_appShellPresent()
            throws Exception {
        // Set class in context and do not call initializer
        AppShellRegistry registry = new AppShellRegistry();
        registry.setShell(AppShellWithPWA.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements elements = document.head().getElementsByTag("meta");
        assertEquals(5, elements.size());
        assertEquals("viewport", elements.get(1).attr("name"));
        assertEquals("my-viewport", elements.get(1).attr("content"));
        assertEquals("apple-mobile-web-app-capable",
                elements.get(2).attr("name"));
        assertEquals("yes", elements.get(2).attr("content"));
        assertEquals("theme-color", elements.get(3).attr("name"));
        assertEquals("#ffffff", elements.get(3).attr("content"));
        assertEquals("apple-mobile-web-app-status-bar-style",
                elements.get(4).attr("name"));
        assertEquals("#ffffff", elements.get(4).attr("content"));

        Elements headInlineAndStyleElements = document.head()
                .getElementsByTag("style");
        assertEquals(2, headInlineAndStyleElements.size());
        assertEquals("text/css",
                headInlineAndStyleElements.get(1).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}",
                headInlineAndStyleElements.get(1).childNode(0).toString());
    }

    @Test
    public void should_add_elements_when_appShellWithConfigurator()
            throws Exception {
        // Set class in context and do not call initializer
        AppShellRegistry registry = new AppShellRegistry();
        registry.setShell(MyAppShellWithConfigurator.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements elements = document.head().getElementsByTag("meta");
        assertEquals(4, elements.size());
        // already in the index.html used as template
        assertEquals("UTF-8", elements.get(0).attr("charset"));

        // replaced ones in the index.html template by configurator ones
        assertEquals("viewport", elements.get(1).attr("name"));
        assertEquals("my-viewport", elements.get(1).attr("content"));

        // added by configurator
        assertEquals("foo", elements.get(2).attr("name"));
        assertEquals("bar", elements.get(2).attr("content"));
        assertEquals("lorem", elements.get(3).attr("name"));
        assertEquals("ipsum", elements.get(3).attr("content"));

        assertEquals("my-title", document.head().getElementsByTag("title")
                .get(0).childNode(0).toString());

        Elements headInlineAndStyleElements = document.head()
                .getElementsByTag("style");
        assertEquals(3, headInlineAndStyleElements.size());
        assertEquals("text/css",
                headInlineAndStyleElements.get(2).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}",
                headInlineAndStyleElements.get(2).childNode(0).toString());

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        assertEquals(3, bodyInlineElements.size());
    }

    @Test
    public void should_export_usage_statistics_in_development_mode()
            throws IOException {
        deploymentConfiguration.setProductionMode(false);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        assertEquals(2, bodyInlineElements.size());

        String entries = UsageStatistics.getEntries().map(entry -> {
            JsonObject json = Json.createObject();

            json.put("is", entry.getName());
            json.put("version", entry.getVersion());

            return json.toString();
        }).collect(Collectors.joining(","));

        String expected = StringUtil
                .normaliseWhitespace("window.Vaadin = window.Vaadin || {}; "
                        + "window.Vaadin.registrations = window.Vaadin.registrations || [];\n"
                        + "window.Vaadin.registrations.push(" + entries + ");");

        assertEquals(StringUtil.normaliseWhitespace(expected),
                bodyInlineElements.get(1).childNode(0).outerHtml());
    }

    @Test
    public void should_NOT_export_usage_statistics_in_production_mode()
            throws IOException {
        deploymentConfiguration.setProductionMode(true);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        assertEquals(1, bodyInlineElements.size());
    }

    @Test
    public void should_store_IndexHtmltitleToUI_When_LoadingServerEagerly()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        assertEquals("Flow Test CCDM",
                UI.getCurrent().getInternals().getAppShellTitle());
    }

    @After
    public void tearDown() throws Exception {
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

    private VaadinServletRequest createVaadinRequest(String pathInfo) {
        HttpServletRequest request = createRequest(pathInfo);
        return new VaadinServletRequest(request, Mockito.spy(service));
    }

    private HttpServletRequest createRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        Mockito.doAnswer(invocation -> pathInfo).when(request).getPathInfo();
        Mockito.doAnswer(invocation -> new StringBuffer(pathInfo)).when(request)
                .getRequestURL();
        return request;
    }

    private void mockApplicationConfiguration(
            ApplicationConfiguration appConfig) {
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);
        Mockito.when(appConfig.enableDevServer()).thenReturn(true);

        Mockito.when(appConfig.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgumentAt(1,
                        String.class));
        Mockito.when(appConfig.getBooleanProperty(Mockito.anyString(),
                Mockito.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgumentAt(1,
                        Boolean.class));
    }
}

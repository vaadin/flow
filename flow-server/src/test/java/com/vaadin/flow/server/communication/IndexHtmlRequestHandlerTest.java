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
package com.vaadin.flow.server.communication;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinAppShellInitializerTest.AppShellWithPWA;
import com.vaadin.flow.server.startup.VaadinAppShellInitializerTest.MyAppShellWithConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.TestUtil;

import static com.vaadin.flow.internal.FrontendUtils.INDEX_HTML;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_REMOTE_ADDRESS_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexHtmlRequestHandlerTest {
    private static final String SPRING_CSRF_ATTRIBUTE_IN_SESSION = "org.springframework.security.web.csrf.CsrfToken";
    private static final String SPRING_CSRF_ATTRIBUTE = "_csrf";
    private static final String INITIAL_UIDL_SEARCH_STRING = "window.Vaadin.TypeScript= ";
    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private VaadinSession session;
    private IndexHtmlRequestHandler indexHtmlRequestHandler;
    private VaadinResponse response;
    private ByteArrayOutputStream responseOutput;
    private MockDeploymentConfiguration deploymentConfiguration;
    private VaadinContext context;

    private String springTokenString;
    private final String springTokenHeaderName = "x-CSRF-TOKEN";
    private final String springTokenParamName = SPRING_CSRF_ATTRIBUTE_IN_SESSION;
    @TempDir
    Path temporaryFolder;

    @BeforeEach
    public void setUp() throws Exception {

        UsageStatistics.resetEntries();
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        session = mocks.getSession();
        response = Mockito.mock(VaadinResponse.class);
        responseOutput = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(responseOutput);
        deploymentConfiguration = mocks.getDeploymentConfiguration();
        deploymentConfiguration.setProductionMode(true);
        indexHtmlRequestHandler = new IndexHtmlRequestHandler();
        context = service.getContext();
        springTokenString = UUID.randomUUID().toString();

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(context.getAttribute(ApplicationConfiguration.class))
                .thenReturn(applicationConfiguration);

        MenuRegistry.clearFileRoutesCache();
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_serveContentFromTemplate()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("index.html template content"),
                "Response should have content from the index.html template");
        assertTrue(indexHtml.contains(".v-system-error"),
                "Response should have styles for the system-error dialogs");
    }

    @Test
    public void serveNotFoundIndexHtml_requestWithRootPath_failsWithIOException()
            throws IOException {
        VaadinServletService vaadinService = Mockito
                .mock(VaadinServletService.class);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);

        Mockito.when(vaadinService.getContext()).thenReturn(context);
        final Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        URL resource = Mockito.mock(URL.class);
        Mockito.when(resourceProvider
                .getApplicationResource(VAADIN_WEBAPP_RESOURCES + INDEX_HTML))
                .thenReturn(resource);
        when(resource.openStream()).thenReturn(null);

        VaadinServletRequest vaadinRequest = Mockito
                .mock(VaadinServletRequest.class);
        Mockito.when(vaadinRequest.getService()).thenReturn(vaadinService);

        String expectedError = "java.io.IOException: Unable to find index.html. "
                + "It should be available on the classpath when running in production mode";

        UncheckedIOException expectedException = assertThrows(
                UncheckedIOException.class,
                () -> indexHtmlRequestHandler.synchronizedHandleRequest(session,
                        vaadinRequest, response));
        assertEquals(expectedError, expectedException.getMessage());
    }

    @Test
    public void serveIndexHtml_language_attribute_is_present()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("<html lang"),
                "Response should have a language attribute");
    }

    @Test
    public void serveIndexHtml_requestWithRootPath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("<base href=\".\""),
                "Response should have correct base href");
    }

    @Test
    public void serveIndexHtml_requestWithSomePath_hasBaseHrefElement()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/some/path"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains("<base href=\"./..\""),
                "Response should have correct base href");
    }

    @Test
    public void serveIndexHtml_featureFlagsSetter_isPresent()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        assertTrue(indexHtml.contains(
                "window.Vaadin.featureFlagsUpdaters.push((activator) => {"),
                "Response should have Feature Flags updater function");
    }

    @Test
    public void canHandleRequest_requestWithRootPath_handleRequest() {
        boolean canHandleRequest = indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/"));
        assertTrue(canHandleRequest,
                "The handler should handle a root path request");
    }

    @Test
    public void canHandleRequest_withoutBootstrapUrlPredicate() {
        assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/picture.png")));
        assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("/nested/CAPITAL.PNG")));
        assertTrue(indexHtmlRequestHandler
                .canHandleRequest(createVaadinRequest("com.foo.MyTest")));
    }

    @Test
    public void canHandleRequest_withBootstrapUrlPredicate() {

        service.setBootstrapUrlPredicate(req -> {
            // refuse any request with extension
            return !req.getPathInfo().matches(".+\\.[A-z][A-z\\d]+$");
        });

        assertTrue(
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/some/route")),
                "The handler should handle a route with " + "parameter");
        assertTrue(
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/myroute")),
                "The handler should handle a normal route");
        assertTrue(
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/myroute/ends/withslash/")),
                "The handler should handle a directory request");
        assertTrue(
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/documentation/10.0.x1/flow")),
                "The handler should handle a request if it has "
                        + "extension pattern in the middle of the path");

        assertFalse(
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/nested/picture.png")),
                "The handler should not handle request with extension");
        assertFalse(
                indexHtmlRequestHandler.canHandleRequest(
                        createVaadinRequest("/nested/CAPITAL.PNG")),
                "The handler should not handle request with capital extension");
        assertFalse(
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/script.js")),
                "The handler should not handle request with extension");

        assertFalse(
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/music.mp3")),
                "The handler should not handle request with extension");
        assertFalse(
                indexHtmlRequestHandler
                        .canHandleRequest(createVaadinRequest("/.htaccess")),
                "The handler should not handle request with only extension");
    }

    @Test
    public void canHandleRequest_allow_oldBrowser() {
        assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", null, null)));
    }

    @Test
    public void canHandleRequest_handle_indexHtmlRequest() {
        assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "document", "navigate")));
    }

    @Test
    public void canHandleRequest_doNotHandle_scriptRequest() {
        assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "script", "no-cors")));
    }

    @Test
    public void canHandleRequest_doNotHandle_imageRequest() {
        assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "image", "no-cors")));
    }

    @Test
    public void canHandleRequest_doNotHandle_vaadinStaticResources() {
        assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/VAADIN/foo.js", null, null)));
    }

    @Test
    public void canHandleRequest_doNotHandle_vaadinReservedFolders() {
        for (String reservedFolder : HandlerHelper
                .getPublicInternalFolderPaths()) {
            assertFalse(indexHtmlRequestHandler.canHandleRequest(
                    createRequestWithDestination(reservedFolder, null, null)),
                    reservedFolder
                            + " was handled even though it should not init index handler");
        }
    }

    @Test
    public void canHandleRequest_handle_serviceWorkerDocumentRequest() {
        assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "empty", "same-origin")));
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
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);
        Elements scripts = document.head().getElementsByTag("script");
        int expectedScripts = 2;
        assertEquals(expectedScripts, scripts.size());
        assertEquals("testing.1", scripts.get(expectedScripts - 2).attr("src"));
        assertEquals("testing.2", scripts.get(expectedScripts - 1).attr("src"));
    }

    @Test
    public void should_add_initialUidl_when_includeInitialBootstrapUidl()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        assertEquals("", initialUidlScript.attr("initial"));
    }

    private static Element findScript(Elements scripts, String needle) {
        for (Element script : scripts) {
            if (script.toString().contains(needle)) {
                return script;
            }
        }
        return null;
    }

    @Test
    public void should_not_add_initialUidl_when_not_includeInitialBootstrapUidl()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);

        assertEquals(
                "window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};",
                initialUidlScript.childNode(0).toString());
        assertEquals("", initialUidlScript.attr("initial"));
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
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);

        assertEquals("", initialUidlScript.attr("initial"));
        String scriptContent = initialUidlScript.toString();
        assertTrue(scriptContent.contains("Could not navigate"));
        assertFalse(
                scriptContent.contains("&lt;")
                        || scriptContent.contains("&gt;"),
                "Initial object content should not be escaped");
        assertNotNull(UI.getCurrent());
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
        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        assertEquals(
                "window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};",
                initialUidlScript.childNode(0).toString());
        assertEquals("", initialUidlScript.attr("initial"));
        assertNull(UI.getCurrent());
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

        assertNotNull(captor.getValue().getUI());
    }

    @Test
    public void eagerServerLoad_requestParameters_forwardedToLocationObject()
            throws IOException {
        deploymentConfiguration.setEagerServerLoad(true);

        Map<String, String[]> requestParams = new HashMap<>();
        requestParams.put("param1", new String[] { "a", "b" });
        requestParams.put("param2", new String[] { "2" });
        VaadinServletRequest request = createVaadinRequest("/view");
        Mockito.when(request.getHttpServletRequest().getParameterMap())
                .thenReturn(requestParams);

        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);

        ArgumentCaptor<IndexHtmlResponse> captor = ArgumentCaptor
                .forClass(IndexHtmlResponse.class);

        verify(request.getService()).modifyIndexHtmlResponse(captor.capture());

        Optional<UI> maybeUI = captor.getValue().getUI();
        assertNotNull(maybeUI);
        QueryParameters locationParams = maybeUI.get().getActiveViewLocation()
                .getQueryParameters();
        assertEquals(List.of("a", "b"), locationParams.getParameters("param1"));
        assertEquals(List.of("2"), locationParams.getParameters("param2"));
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

        assertEquals(Optional.empty(), captor.getValue().getUI());
    }

    @Test
    public void should_include_spring_csrf_token_in_meta_tags_when_return_not_null_spring_csrf_in_request()
            throws IOException {
        VaadinRequest request = createVaadinRequestWithSpringCsrfToken();
        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);
        assertSpringCsrfTokenIsAvailableAsMetaTagsInDom();
    }

    @Test
    public void should_not_include_token_in_dom_when_return_null_csrfToken_in_session()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        assertFalse(initialUidlScript.childNode(0).toString()
                .contains("window.Vaadin = {Flow: {\"csrfToken\":"));
        assertEquals("", initialUidlScript.attr("initial"));
    }

    @Test
    public void should_not_include_spring_csrf_token_in_meta_tags_when_return_null_spring_csrf_in_request()
            throws IOException {
        VaadinRequest request = createVaadinRequest("/");
        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertEquals(0, document.head()
                .getElementsByAttribute(SPRING_CSRF_ATTRIBUTE).size());
        assertEquals(0,
                document.head().getElementsByAttribute("_csrf_header").size());
    }

    @Test
    public void should_include_spring_token_in_dom_when_referer_is_service_worker()
            throws IOException {
        VaadinRequest request = createVaadinRequestWithSpringCsrfToken();
        Mockito.when(request.getHeader("referer"))
                .thenReturn("http://somewhere.test/sw.js");
        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);
        assertSpringCsrfTokenIsAvailableAsMetaTagsInDom();
    }

    @Test
    public void should_not_add_metaElements_when_not_appShellPresent()
            throws Exception {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
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
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(AppShellWithPWA.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements elements = document.head().getElementsByTag("meta");
        assertEquals(8, elements.size());

        Optional<Element> viewPort = findFirstElementByNameAttrEqualTo(elements,
                "viewport");
        assertTrue(viewPort.isPresent(), "'viewport' meta link should exist.");
        assertEquals("my-viewport", viewPort.get().attr("content"));

        Optional<Element> appleMobileWebAppCapable = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-capable");
        assertTrue(appleMobileWebAppCapable.isPresent(),
                "'apple-mobile-web-app-capable' meta link should exist.");
        assertEquals("yes", appleMobileWebAppCapable.get().attr("content"));

        Optional<Element> themeColor = findFirstElementByNameAttrEqualTo(
                elements, "theme-color");
        assertTrue(themeColor.isPresent(),
                "'theme-color' meta link should exists.");
        assertEquals("#ffffff", themeColor.get().attr("content"));

        Optional<Element> appleMobileWebAppStatusBar = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-status-bar-style");
        assertTrue(appleMobileWebAppStatusBar.isPresent(),
                "'apple-mobile-web-app-status-bar-style' meta link should exists.");
        assertEquals("#ffffff",
                appleMobileWebAppStatusBar.get().attr("content"));

        Optional<Element> mobileWebAppCapableElements = findFirstElementByNameAttrEqualTo(
                elements, "mobile-web-app-capable");
        assertTrue(mobileWebAppCapableElements.isPresent(),
                "'mobile-web-app-capable' meta link should exists.");
        assertEquals("yes", mobileWebAppCapableElements.get().attr("content"));

        Optional<Element> appleTouchFullScreenElements = findFirstElementByNameAttrEqualTo(
                elements, "apple-touch-fullscreen");
        assertTrue(appleTouchFullScreenElements.isPresent(),
                "'apple-touch-fullscreen' meta link should exist.");
        assertEquals("yes", appleTouchFullScreenElements.get().attr("content"));

        Optional<Element> appleMobileWebAppTitleElements = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-title");
        assertTrue(appleMobileWebAppTitleElements.isPresent(),
                "'apple-mobile-web-app-title' should exist.");
        assertEquals("n", appleMobileWebAppTitleElements.get().attr("content"));

        Elements headInlineAndStyleElements = document.head()
                .getElementsByTag("style");
        assertEquals(3, headInlineAndStyleElements.size());

        assertEquals("[hidden] { display: none !important; }",
                headInlineAndStyleElements.get(1).childNode(0).toString());

        assertEquals("text/css",
                headInlineAndStyleElements.get(2).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}",
                headInlineAndStyleElements.get(2).childNode(0).toString());
    }

    @Test
    public void should_add_elements_when_appShellWithConfigurator()
            throws Exception {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        TestUtil.createIndexHtmlStub(projectRootFolder);
        TestUtil.createStatsJsonStub(projectRootFolder);
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setProjectFolder(projectRootFolder);
        // Set class in context and do not call initializer
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(MyAppShellWithConfigurator.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
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
        assertEquals(4, headInlineAndStyleElements.size());

        assertEquals("[hidden] { display: none !important; }",
                headInlineAndStyleElements.get(2).childNode(0).toString());

        assertEquals("text/css",
                headInlineAndStyleElements.get(3).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}",
                headInlineAndStyleElements.get(3).childNode(0).toString());

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        // <script>window.Vaadin = window.Vaadin || {};window.Vaadin
        // .registrations = window.Vaadin.registrations ||
        // [];window.Vaadin.registrations.push({"is":"java","version":"17.0.2"});
        // </script>"
        // <script type="text/javascript">window.messages = window.messages
        // || [];window.messages.push("inline.js");
        // </script>"
        assertEquals(2, bodyInlineElements.size());
    }

    @Test
    public void should_export_usage_statistics_in_development_mode()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        TestUtil.createIndexHtmlStub(projectRootFolder);
        TestUtil.createStatsJsonStub(projectRootFolder);
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setProjectFolder(projectRootFolder);
        VaadinServletRequest request = createVaadinRequest("/");
        Mockito.when(request.getHttpServletRequest().getRemoteAddr())
                .thenReturn("127.0.0.1");
        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        // <script>window.Vaadin = window.Vaadin || {};
        // window.Vaadin.registrations = window.Vaadin.registrations || [];
        // window.Vaadin.registrations.push({"is":"java","version":"17.0.2"});
        // </script>
        assertEquals(1, bodyInlineElements.size());

        String entries = UsageStatistics.getEntries().map(entry -> {
            ObjectNode json = JacksonUtils.createObjectNode();

            json.put("is", entry.getName());
            json.put("version", entry.getVersion());

            return json.toString();
        }).collect(Collectors.joining(","));

        String expected = StringUtil
                .normaliseWhitespace("window.Vaadin = window.Vaadin || {}; "
                        + "window.Vaadin.registrations = window.Vaadin.registrations || [];\n"
                        + "window.Vaadin.registrations.push(" + entries + ");");

        assertTrue(isTokenPresent(indexHtml));

        String htmlContent = bodyInlineElements.get(0).childNode(0).outerHtml();
        htmlContent = htmlContent.replace("\r", "");
        htmlContent = htmlContent.replace("\n", " ");
        assertEquals(StringUtil.normaliseWhitespace(expected), htmlContent);
    }

    // Regular expression to match a UUID in the format 8-4-4-4-12
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\"token\":\"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\"");

    private static boolean isTokenPresent(String htmlContent) {
        Matcher matcher = UUID_PATTERN.matcher(htmlContent);
        return matcher.find();
    }

    // This test output index.html will contain the window.Vaadin.devToolsConf
    // (in production mode)
    // Is this ok (or deploymentConfiguration.setProductionMode(true) is not
    // working properly?)
    // window.Vaadin.devToolsConf =
    // {"enable":false,"url":"./nullVAADIN/push","liveReloadPort":35729,"token":"6ac38fcc-a66f-4283-9e46-6d38e1b7260b"};
    // @Test
    // public void dev_tools_token_should_not_be_in_production_mode()
    // throws IOException {
    // deploymentConfiguration.setProductionMode(true);
    //
    // indexHtmlRequestHandler.synchronizedHandleRequest(session,
    // createVaadinRequest("/"), response);
    //
    // String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
    //
    // assertFalse(isTokenPresent(indexHtml));
    // }

    @Test
    public void devTools_disable_stubPushFunctionRegistered()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        TestUtil.createIndexHtmlStub(projectRootFolder);
        TestUtil.createStatsJsonStub(projectRootFolder);
        deploymentConfiguration.setDevToolsEnabled(false);
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setProjectFolder(projectRootFolder);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertTrue(
                document.head().getElementsByTag("script").stream()
                        .map(Element::html)
                        .anyMatch(script -> script
                                .contains("window.Vaadin.devToolsPlugins = {")
                                && script.contains("push: function(plugin) {")),
                "Expected devToolsPlugins.push function when dev-tools are disabled");

    }

    @Test
    public void should_NOT_export_usage_statistics_in_production_mode()
            throws IOException {
        deploymentConfiguration.setProductionMode(true);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        assertEquals(1, bodyInlineElements.size());
    }

    @Theme(value = "mytheme", variant = "dark")
    public static class ClassWithDarkLumo implements AppShellConfigurator {

    }

    @Test
    public void should_apply_theme_variant() throws IOException {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(ClassWithDarkLumo.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertEquals("dark", document.head().parent().attr("theme"));
    }

    @ColorScheme(ColorScheme.Value.DARK)
    public static class ClassWithDarkColorScheme
            implements AppShellConfigurator {
    }

    @Test
    public void should_apply_colorScheme_dark() throws IOException {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(ClassWithDarkColorScheme.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertEquals("dark", document.head().parent().attr("theme"));
        assertEquals("color-scheme: dark;",
                document.head().parent().attr("style"));
    }

    @ColorScheme(ColorScheme.Value.LIGHT_DARK)
    public static class ClassWithLightDarkColorScheme
            implements AppShellConfigurator {
    }

    @Test
    public void should_apply_colorScheme_lightDark() throws IOException {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(ClassWithLightDarkColorScheme.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertEquals("light-dark", document.head().parent().attr("theme"));
        assertEquals("color-scheme: light dark;",
                document.head().parent().attr("style"));
    }

    @ColorScheme(ColorScheme.Value.NORMAL)
    public static class ClassWithNormalColorScheme
            implements AppShellConfigurator {
    }

    @Test
    public void should_not_apply_colorScheme_normal() throws IOException {
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(ClassWithNormalColorScheme.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        assertEquals("", document.head().parent().attr("theme"));
        assertEquals("", document.head().parent().attr("style"));
    }

    @Test
    public void should_append_colorScheme_to_existing_style()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();

        // Create custom index.html with existing style attribute on html
        // element
        String indexHtmlWithStyle = """
                <!DOCTYPE html>
                <html style="--custom-prop: value;">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                </head>
                <body>
                  <div id="outlet"></div>
                </body>
                </html>
                """;

        File frontendDir = new File(projectRootFolder, "frontend");
        frontendDir.mkdirs();
        File indexHtml = new File(frontendDir, "index.html");
        Files.writeString(indexHtml.toPath(), indexHtmlWithStyle);

        TestUtil.createStatsJsonStub(projectRootFolder);

        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setProjectFolder(projectRootFolder);

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(ClassWithDarkColorScheme.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtmlOutput = responseOutput
                .toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtmlOutput);

        assertEquals("dark", document.head().parent().attr("theme"));
        assertEquals("--custom-prop: value; color-scheme: dark;",
                document.head().parent().attr("style"));
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

    @AfterEach
    public void tearDown() throws Exception {
        session.unlock();
        mocks.cleanup();
    }

    private Optional<Element> findFirstElementByNameAttrEqualTo(
            Elements elements, String name) {
        return elements.stream()
                .filter(element -> name.equals(element.attr("name")))
                .findFirst();
    }

    private VaadinServletRequest createRequestWithDestination(String pathInfo,
            String fetchDest, String fetchMode) {
        VaadinServletRequest req = createVaadinRequest(pathInfo);
        Mockito.when(req.getHeader(Mockito.anyString())).thenAnswer(arg -> {
            if ("Sec-Fetch-Dest".equals(arg.getArgument(0))) {
                return fetchDest;
            } else if ("Sec-Fetch-Mode".equals(arg.getArgument(0))) {
                return fetchMode;
            }
            return null;
        });

        return req;
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

    @Test
    public void internal_request_no_bootstrap_page() {
        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(null);
        Mockito.when(request.getParameter("v-r")).thenReturn("hello-foo-bar");
        assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
        assertFalse(indexHtmlRequestHandler.canHandleRequest(request));

        Mockito.when(request.getParameter("v-r")).thenReturn("init");
        assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
        assertFalse(indexHtmlRequestHandler.canHandleRequest(request));
    }

    @Test
    public void synchronizedHandleRequest_badLocation_noUiCreated()
            throws IOException {
        final IndexHtmlRequestHandler bootstrapHandler = new IndexHtmlRequestHandler();

        final VaadinServletRequest request = Mockito
                .mock(VaadinServletRequest.class);
        Mockito.doAnswer(invocation -> "..**").when(request).getPathInfo();

        final MockServletServiceSessionSetup.TestVaadinServletResponse response = mocks
                .createResponse();

        final boolean value = bootstrapHandler.synchronizedHandleRequest(
                mocks.getSession(), request, response);
        assertTrue(value, "No further request handlers should be called");

        assertEquals(400, response.getErrorCode(),
                "Invalid status code reported");
        assertEquals(
                "Invalid location: Relative path cannot contain .. segments",
                response.getErrorMessage(), "Invalid message reported");
    }

    @Test
    public void serviceWorkerRequest_canNotHandleRequest() {
        IndexHtmlRequestHandler bootstrapHandler = new IndexHtmlRequestHandler();

        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);

        Mockito.when(request.getHeader(BootstrapHandler.SERVICE_WORKER_HEADER))
                .thenReturn("script");

        assertFalse(bootstrapHandler.canHandleRequest(request));
    }

    @Test
    public void servingStylesCss_productionMode_noLinkTagAdded()
            throws IOException {
        File projectRootFolder = Files
                .createTempDirectory(temporaryFolder, "temp").toFile();
        deploymentConfiguration.setProductionMode(true);
        deploymentConfiguration.setProjectFolder(projectRootFolder);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements linkElements = document.head().getElementsByTag("link");
        assertEquals(0, linkElements.size());
    }

    private VaadinRequest createVaadinRequestWithSpringCsrfToken() {
        VaadinRequest request = Mockito.spy(createVaadinRequest("/"));
        Map<String, String> csrfJsonMap = new HashMap<>();
        csrfJsonMap.put("token", springTokenString);
        csrfJsonMap.put("headerName", springTokenHeaderName);
        csrfJsonMap.put("parameterName", springTokenParamName);
        Mockito.when(request.getAttribute(SPRING_CSRF_ATTRIBUTE_IN_SESSION))
                .thenReturn(csrfJsonMap);
        return request;
    }

    private void assertSpringCsrfTokenIsAvailableAsMetaTagsInDom() {
        try {
            String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
            Document document = Jsoup.parse(indexHtml);

            Elements csrfMetaEelement = document.head()
                    .getElementsByAttributeValue("name", SPRING_CSRF_ATTRIBUTE);
            assertEquals(1, csrfMetaEelement.size());
            assertEquals(springTokenString,
                    csrfMetaEelement.first().attr("content"));

            Elements csrfHeaderMetaElement = document.head()
                    .getElementsByAttributeValue("name", "_csrf_header");
            assertEquals(1, csrfHeaderMetaElement.size());
            assertEquals(springTokenHeaderName,
                    csrfHeaderMetaElement.first().attr("content"));

            Elements csrfParameterMetaElement = document.head()
                    .getElementsByAttributeValue("name", "_csrf_parameter");
            assertEquals(1, csrfParameterMetaElement.size());
            assertEquals(springTokenParamName,
                    csrfParameterMetaElement.first().attr("content"));
        } catch (Exception e) {
            fail("Unable to parse the index html page");
        }
    }

    private boolean isAllowedDevToolsHost(String hostsAllowedProperty,
            String remoteAddr) {
        return isAllowedDevToolsHost(hostsAllowedProperty, remoteAddr,
                (List) null);
    }

    private boolean isAllowedDevToolsHost(String hostsAllowedProperty,
            String remoteAddr, String forwardedForHeader) {
        return isAllowedDevToolsHost(hostsAllowedProperty, remoteAddr,
                Collections.singletonList(forwardedForHeader));
    }

    private boolean isAllowedDevToolsHost(String hostsAllowedProperty,
            String remoteAddr, List<String> forwardedForHeader) {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddr);
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.doAnswer(q -> hostsAllowedProperty).when(configuration)
                .getStringProperty(SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED,
                        null);
        if (forwardedForHeader == null) {
            Mockito.when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            Mockito.when(request.getHeaders("X-Forwarded-For"))
                    .thenReturn(Collections.emptyEnumeration());
        } else {
            Mockito.when(request.getHeader("X-Forwarded-For"))
                    .thenReturn(forwardedForHeader.get(0));
            Mockito.when(request.getHeaders("X-Forwarded-For"))
                    .thenReturn(Collections.enumeration(forwardedForHeader));
        }
        return IndexHtmlRequestHandler.isAllowedDevToolsHost(configuration,
                request);

    }

    @Test
    public void devTools_loopbackAllowedByDefault() {
        assertTrue(isAllowedDevToolsHost(null, "127.0.0.1"));
        assertTrue(isAllowedDevToolsHost(null, "0:0:0:0:0:0:0:1"));
        assertTrue(isAllowedDevToolsHost(null, "::1"));

        assertTrue(isAllowedDevToolsHost("", "127.0.0.1"));
        assertTrue(isAllowedDevToolsHost("", "0:0:0:0:0:0:0:1"));
        assertTrue(isAllowedDevToolsHost("", "::1"));

        assertTrue(isAllowedDevToolsHost("   ", "127.0.0.1"));
        assertTrue(isAllowedDevToolsHost("   ", "0:0:0:0:0:0:0:1"));
        assertTrue(isAllowedDevToolsHost("   ", "::1"));
    }

    @Test
    public void devTools_externalOrNoIpDeniedByDefault() {
        assertFalse(isAllowedDevToolsHost(null, "192.168.1.1"));
        assertFalse(isAllowedDevToolsHost(null, "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost(null, null));

        assertFalse(isAllowedDevToolsHost("", "192.168.1.1"));
        assertFalse(isAllowedDevToolsHost("", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("", null));

        assertFalse(isAllowedDevToolsHost("   ", "192.168.1.1"));
        assertFalse(isAllowedDevToolsHost("   ", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("   ", null));
    }

    @Test
    public void devTools_allowedHostsMatchesIp() {
        assertTrue(isAllowedDevToolsHost("192.168.1.*", "192.168.1.1"));
        assertTrue(isAllowedDevToolsHost("192.168.1.*", "192.168.1.100"));
        assertFalse(isAllowedDevToolsHost("192.168.1.*", "192.168.100.100"));

        // Localhost is always allowed
        assertTrue(isAllowedDevToolsHost("192.168.1.*", "127.0.0.1"));

    }

    @Test
    public void devTools_allowedHostsMatchesIpAndForwardedFor() {
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("  ", "127.0.0.1", "1.2.3.4"));
        assertFalse(
                isAllowedDevToolsHost(null, "127.0.0.1", "1.2.3.4, 3.4.5.6"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "1.2.3.4, 3.4.5.6"));
        assertFalse(
                isAllowedDevToolsHost("   ", "127.0.0.1", "1.2.3.4, 3.4.5.6"));
        assertFalse(isAllowedDevToolsHost("1.2.3.4", "5.5.5.5",
                "1.2.3.4, 3.4.5.6"));

        // Local proxy
        assertTrue(isAllowedDevToolsHost("1.2.3.4", "127.0.0.1", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("1.2.3.4", "127.0.0.1",
                "1.2.3.4, 3.4.5.6"));
        assertFalse(isAllowedDevToolsHost("1.2.3.4", "127.0.0.1",
                "   1.2.3.4 , 3.4.5.6   "));
        assertTrue(isAllowedDevToolsHost("1.2.3.4,3.4.5.6", "127.0.0.1",
                "1.2.3.4, 3.4.5.6"));
        assertTrue(isAllowedDevToolsHost("1.2.3.4,3.4.5.6", "127.0.0.1",
                "   1.2.3.4 , 3.4.5.6   "));

        // Non local proxy
        assertTrue(isAllowedDevToolsHost("1.2.3.4, 5.5.5.5", "5.5.5.5",
                "   1.2.3.4    "));
        assertTrue(
                isAllowedDevToolsHost("1.2.3.4,5.5.*", "5.5.5.5", "1.2.3.4"));
        assertFalse(isAllowedDevToolsHost("1.2.3.4,5.5.*", "5.5.5.5",
                "1.2.3.4, 3.4.5.6"));
        assertFalse(isAllowedDevToolsHost("1.2.3.4,5.5.*", "5.5.5.5",
                "   1.2.3.4 , 3.4.5.6   "));
        assertTrue(isAllowedDevToolsHost("1.2.3.4,3.4.5.6,5.5.*", "5.5.5.5",
                "1.2.3.4, 3.4.5.6"));
        assertTrue(isAllowedDevToolsHost("1.2.3.4,3.4.5.6,5.5.*", "5.5.5.5",
                "   1.2.3.4 , 3.4.5.6   "));

        // Verify full chain
        String forwardedChain = "1.2.3.4,5.5.5.5,6.6.6.6,7.7.7.7";
        assertFalse(
                isAllowedDevToolsHost("1.2.3.4", "127.0.0.1", forwardedChain));
        assertFalse(isAllowedDevToolsHost("1.2.3.4,5.5.5.5", "127.0.0.1",
                forwardedChain));
        assertFalse(isAllowedDevToolsHost("1.2.3.4,5.5.5.5,6.6.6.6",
                "127.0.0.1", forwardedChain));
        assertFalse(isAllowedDevToolsHost("1.2.3.4,5.5.5.5,7.7.7.7",
                "127.0.0.1", forwardedChain));
        assertTrue(isAllowedDevToolsHost("1.2.3.4,5.5.5.5,6.6.6.6,7.7.7.7",
                "127.0.0.1", forwardedChain));

    }

    @Test
    public void devTools_forwardedForIsLocal_denyAccess() {
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", "127.0.0.1"));
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", "::1"));
        assertFalse(
                isAllowedDevToolsHost(null, "127.0.0.1", "0:0:0:0:0:0:0:1"));
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", "172.16.0.4"));

        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "127.0.0.1"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "::1"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "0:0:0:0:0:0:0:1"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "172.16.0.4"));

        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", "127.0.0.1"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "::1"));
        assertFalse(
                isAllowedDevToolsHost("   ", "127.0.0.1", "0:0:0:0:0:0:0:1"));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", "172.16.0.4"));

        // Access for local addresses in forwarded-for for should be denied
        // disregarding hostsAllow property
        assertFalse(
                isAllowedDevToolsHost("127.0.0.1", "127.0.0.1", "127.0.0.1"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "127.0.0.1"));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", "127.0.0.1"));

    }

    @Test
    public void devTools_forwardedForIsEmpty_denyAccess() {
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", ""));
        assertFalse(isAllowedDevToolsHost("127.0.0.1", "127.0.0.1", ""));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", ""));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", ""));

        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", "   "));
        assertFalse(isAllowedDevToolsHost("127.0.0.1", "127.0.0.1", "   "));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", "   "));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", "   "));

        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", ","));
        assertFalse(isAllowedDevToolsHost("127.0.0.1", "127.0.0.1", ","));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", ","));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", ","));

        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", ", ,, ,"));
        assertFalse(isAllowedDevToolsHost("127.0.0.1", "127.0.0.1", ", ,, ,"));
        assertFalse(isAllowedDevToolsHost("", "127.0.0.1", ", ,, ,"));
        assertFalse(isAllowedDevToolsHost("   ", "127.0.0.1", ", ,, ,"));
    }

    @Test
    public void devTools_multipleForwardedForHeader_allChecked() {
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1",
                List.of("1.1.1.1", "2.2.2.2")));
        assertFalse(isAllowedDevToolsHost("1.1.1.1", "127.0.0.1",
                List.of("1.1.1.1", "2.2.2.2")));
        assertFalse(isAllowedDevToolsHost("2.2.2.2", "127.0.0.1",
                List.of("1.1.1.1", "2.2.2.2")));

        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1", List.of("", "")));
        assertFalse(
                isAllowedDevToolsHost(null, "127.0.0.1", List.of("  ", "   ")));
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1",
                List.of("1.1.1.1", "", "2.2.2.2")));
        assertFalse(isAllowedDevToolsHost(null, "127.0.0.1",
                List.of("1.1.1.1", "  ", "2.2.2.2")));

        assertTrue(isAllowedDevToolsHost("1.1.1.1, 2.2.2.2", "127.0.0.1",
                List.of("1.1.1.1", "2.2.2.2")));

    }

    @Test
    public void devTools_customRemoteIPHeader_allowedIfIpMatches() {

        BiPredicate<String, String> verifier = (remoteIp,
                spoofedForwarderFor) -> {
            String customClientIpHeaderName = "Some-Proxy-Client-IP";

            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            ApplicationConfiguration configuration = Mockito
                    .mock(ApplicationConfiguration.class);
            Mockito.when(configuration.getStringProperty(
                    SERVLET_PARAMETER_DEVMODE_HOSTS_ALLOWED, null))
                    .thenAnswer(q -> "1.2.3.4,5.6.7.8");
            Mockito.when(configuration.getStringProperty(
                    eq(SERVLET_PARAMETER_DEVMODE_REMOTE_ADDRESS_HEADER), any()))
                    .thenAnswer(q -> customClientIpHeaderName);
            Mockito.when(request.getHeader(customClientIpHeaderName))
                    .thenReturn(remoteIp);
            Mockito.when(request.getHeader("X-Forwarded-For"))
                    .thenReturn(spoofedForwarderFor);
            return IndexHtmlRequestHandler.isAllowedDevToolsHost(configuration,
                    request);
        };

        // remote ip header is mandatory
        assertFalse(verifier.test(null, null));
        assertFalse(verifier.test(null, "1.2.3.4"));

        // Should verify only remote ip header value
        assertFalse(verifier.test("5.5.5.5", null));
        assertFalse(verifier.test("5.5.5.5", "1.2.3.4,5.5.5.5"));
        assertTrue(verifier.test("1.2.3.4", null));
        assertTrue(verifier.test("5.6.7.8", "5.5.5.5,5.6.7.8"));
    }

    @Test
    public void developmentMode_commercialBannerNeverApplied()
            throws IOException {
        assertHasCommercialBanner(false, false, true);
        assertHasCommercialBanner(false, false, false);
        assertHasCommercialBanner(false, false, null);
    }

    @Test
    public void productionMode_commercialBannerEnabled_commercialBannerApplied()
            throws IOException {
        assertHasCommercialBanner(true, true, true);
    }

    @Test
    public void productionMode_commercialBannerNotEnabled_commercialBannerNotApplied()
            throws IOException {
        assertHasCommercialBanner(false, true, false);
        assertHasCommercialBanner(false, true, null);
    }

    private void assertHasCommercialBanner(boolean expectBanner,
            boolean productionMode, Boolean commercialBannerFlag)
            throws IOException {
        if (!productionMode) {
            File projectRootFolder = Files
                    .createTempDirectory(temporaryFolder, "temp").toFile();
            TestUtil.createIndexHtmlStub(projectRootFolder);
            TestUtil.createStatsJsonStub(projectRootFolder);
            deploymentConfiguration.setProjectFolder(projectRootFolder);
        }
        deploymentConfiguration.setProductionMode(productionMode);
        if (commercialBannerFlag != null) {
            deploymentConfiguration.setApplicationOrSystemProperty(
                    Constants.COMMERCIAL_BANNER_TOKEN,
                    Boolean.toString(commercialBannerFlag));
        }

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput.toString(StandardCharsets.UTF_8);
        Document document = Jsoup.parse(indexHtml);

        Elements commercialBannerScript = document.head().select(
                "script[type=\"module\"]:containsData(<vaadin-commercial-banner></vaadin-commercial-banner>)");
        if (expectBanner) {
            assertEquals(1, commercialBannerScript.size(),
                    "Commercial banner should be applied in %s mode with commercial banner token %s"
                            .formatted(
                                    ((productionMode) ? "production"
                                            : "development"),
                                    commercialBannerFlag));
        } else {
            assertTrue(commercialBannerScript.isEmpty(),
                    "Commercial banner should not be applied in %s mode with commercial banner token %s"
                            .formatted(
                                    ((productionMode) ? "production"
                                            : "development"),
                                    commercialBannerFlag));
        }
    }

}

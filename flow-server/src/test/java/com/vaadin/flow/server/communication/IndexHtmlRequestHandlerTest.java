/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.BootstrapHandler;
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

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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
    private String springTokenHeaderName = "x-CSRF-TOKEN";
    private String springTokenParamName = SPRING_CSRF_ATTRIBUTE_IN_SESSION;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        session = mocks.getSession();
        response = Mockito.mock(VaadinResponse.class);
        responseOutput = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(responseOutput);
        deploymentConfiguration = mocks.getDeploymentConfiguration();
        deploymentConfiguration.setFrontendHotdeploy(false);
        deploymentConfiguration.setProductionMode(true);
        indexHtmlRequestHandler = new IndexHtmlRequestHandler();
        context = service.getContext();
        springTokenString = UUID.randomUUID().toString();

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(context.getAttribute(ApplicationConfiguration.class))
                .thenReturn(applicationConfiguration);
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
        Assert.assertEquals(expectedError, expectedException.getMessage());
    }

    @Test
    public void serveIndexHtml_language_attribute_is_present()
            throws IOException {
        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Assert.assertTrue("Response should have a language attribute",
                indexHtml.contains("<html lang"));
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
    public void canHandleRequest_allow_oldBrowser() {
        Assert.assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", null, null)));
    }

    @Test
    public void canHandleRequest_handle_indexHtmlRequest() {
        Assert.assertTrue(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "document", "navigate")));
    }

    @Test
    public void canHandleRequest_doNotHandle_scriptRequest() {
        Assert.assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "script", "no-cors")));
    }

    @Test
    public void canHandleRequest_doNotHandle_imageRequest() {
        Assert.assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/", "image", "no-cors")));
    }

    @Test
    public void canHandleRequest_doNotHandle_vaadinStaticResources() {
        Assert.assertFalse(indexHtmlRequestHandler.canHandleRequest(
                createRequestWithDestination("/VAADIN/foo.js", null, null)));
    }

    @Test
    public void canHandleRequest_handle_serviceWorkerDocumentRequest() {
        Assert.assertTrue(indexHtmlRequestHandler.canHandleRequest(
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
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);
        Elements scripts = document.head().getElementsByTag("script");
        int expectedScripts = 2;
        Assert.assertEquals(expectedScripts, scripts.size());
        Assert.assertEquals("testing.1",
                scripts.get(expectedScripts - 2).attr("src"));
        Assert.assertEquals("testing.2",
                scripts.get(expectedScripts - 1).attr("src"));
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
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        Assert.assertEquals("", initialUidlScript.attr("initial"));
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
        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);

        Assert.assertEquals(
                "window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};",
                initialUidlScript.childNode(0).toString());
        Assert.assertEquals("", initialUidlScript.attr("initial"));
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
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);

        Assert.assertEquals("", initialUidlScript.attr("initial"));
        String scriptContent = initialUidlScript.toString();
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
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        Assert.assertEquals(
                "window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};",
                initialUidlScript.childNode(0).toString());
        Assert.assertEquals("", initialUidlScript.attr("initial"));
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

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements scripts = document.head().getElementsByTag("script");
        Element initialUidlScript = findScript(scripts,
                INITIAL_UIDL_SEARCH_STRING);
        Assert.assertFalse(initialUidlScript.childNode(0).toString()
                .contains("window.Vaadin = {Flow: {\"csrfToken\":"));
        Assert.assertEquals("", initialUidlScript.attr("initial"));
    }

    @Test
    public void should_not_include_spring_csrf_token_in_meta_tags_when_return_null_spring_csrf_in_request()
            throws IOException {
        VaadinRequest request = createVaadinRequest("/");
        indexHtmlRequestHandler.synchronizedHandleRequest(session, request,
                response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Assert.assertEquals(0, document.head()
                .getElementsByAttribute(SPRING_CSRF_ATTRIBUTE).size());
        Assert.assertEquals(0,
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
        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.setShell(AppShellWithPWA.class);
        mocks.setAppShellRegistry(registry);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements elements = document.head().getElementsByTag("meta");
        assertEquals(8, elements.size());

        Optional<Element> viewPort = findFirstElementByNameAttrEqualTo(elements,
                "viewport");
        assertTrue("'viewport' meta link should exist.", viewPort.isPresent());
        assertEquals("my-viewport", viewPort.get().attr("content"));

        Optional<Element> appleMobileWebAppCapable = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-capable");
        assertTrue("'apple-mobile-web-app-capable' meta link should exist.",
                appleMobileWebAppCapable.isPresent());
        assertEquals("yes", appleMobileWebAppCapable.get().attr("content"));

        Optional<Element> themeColor = findFirstElementByNameAttrEqualTo(
                elements, "theme-color");
        assertTrue("'theme-color' meta link should exists.",
                themeColor.isPresent());
        assertEquals("#ffffff", themeColor.get().attr("content"));

        Optional<Element> appleMobileWebAppStatusBar = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-status-bar-style");
        assertTrue(
                "'apple-mobile-web-app-status-bar-style' meta link should exists.",
                appleMobileWebAppStatusBar.isPresent());
        assertEquals("#ffffff",
                appleMobileWebAppStatusBar.get().attr("content"));

        Optional<Element> mobileWebAppCapableElements = findFirstElementByNameAttrEqualTo(
                elements, "mobile-web-app-capable");
        assertTrue("'mobile-web-app-capable' meta link should exists.",
                mobileWebAppCapableElements.isPresent());
        assertEquals("yes", mobileWebAppCapableElements.get().attr("content"));

        Optional<Element> appleTouchFullScreenElements = findFirstElementByNameAttrEqualTo(
                elements, "apple-touch-fullscreen");
        assertTrue("'apple-touch-fullscreen' meta link should exist.",
                appleTouchFullScreenElements.isPresent());
        assertEquals("yes", appleTouchFullScreenElements.get().attr("content"));

        Optional<Element> appleMobileWebAppTitleElements = findFirstElementByNameAttrEqualTo(
                elements, "apple-mobile-web-app-title");
        assertTrue("'apple-mobile-web-app-title' should exist.",
                appleMobileWebAppTitleElements.isPresent());
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
        File projectRootFolder = temporaryFolder.newFolder();
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
        File projectRootFolder = temporaryFolder.newFolder();
        TestUtil.createIndexHtmlStub(projectRootFolder);
        TestUtil.createStatsJsonStub(projectRootFolder);
        deploymentConfiguration.setProductionMode(false);
        deploymentConfiguration.setProjectFolder(projectRootFolder);

        indexHtmlRequestHandler.synchronizedHandleRequest(session,
                createVaadinRequest("/"), response);

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        // <script>window.Vaadin = window.Vaadin || {};
        // window.Vaadin.registrations = window.Vaadin.registrations || [];
        // window.Vaadin.registrations.push({"is":"java","version":"17.0.2"});
        // </script>
        assertEquals(1, bodyInlineElements.size());

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

        String htmlContent = bodyInlineElements.get(0).childNode(0).outerHtml();
        htmlContent = htmlContent.replace("\r", "");
        htmlContent = htmlContent.replace("\n", " ");
        assertEquals(StringUtil.normaliseWhitespace(expected), htmlContent);
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

        String indexHtml = responseOutput
                .toString(StandardCharsets.UTF_8.name());
        Document document = Jsoup.parse(indexHtml);

        assertEquals("dark", document.head().parent().attr("theme"));
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
        Assert.assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
        Assert.assertFalse(indexHtmlRequestHandler.canHandleRequest(request));

        Mockito.when(request.getParameter("v-r")).thenReturn("init");
        Assert.assertTrue(BootstrapHandler.isFrameworkInternalRequest(request));
        Assert.assertFalse(indexHtmlRequestHandler.canHandleRequest(request));
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
        Assert.assertTrue("No further request handlers should be called",
                value);

        Assert.assertEquals("Invalid status code reported", 400,
                response.getErrorCode());
        Assert.assertEquals("Invalid message reported",
                "Invalid location: Relative path cannot contain .. segments",
                response.getErrorMessage());
    }

    @Test
    public void serviceWorkerRequest_canNotHandleRequest() {
        IndexHtmlRequestHandler bootstrapHandler = new IndexHtmlRequestHandler();

        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);

        Mockito.when(request.getHeader(BootstrapHandler.SERVICE_WORKER_HEADER))
                .thenReturn("script");

        Assert.assertFalse(bootstrapHandler.canHandleRequest(request));
    }

    @Test
    public void servingStylesCss_productionMode_noLinkTagAdded()
            throws IOException {
        File projectRootFolder = temporaryFolder.newFolder();
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
            String indexHtml = responseOutput
                    .toString(StandardCharsets.UTF_8.name());
            Document document = Jsoup.parse(indexHtml);

            Elements csrfMetaEelement = document.head()
                    .getElementsByAttributeValue("name", SPRING_CSRF_ATTRIBUTE);
            Assert.assertEquals(1, csrfMetaEelement.size());
            Assert.assertEquals(springTokenString,
                    csrfMetaEelement.first().attr("content"));

            Elements csrfHeaderMetaElement = document.head()
                    .getElementsByAttributeValue("name", "_csrf_header");
            Assert.assertEquals(1, csrfHeaderMetaElement.size());
            Assert.assertEquals(springTokenHeaderName,
                    csrfHeaderMetaElement.first().attr("content"));

            Elements csrfParameterMetaElement = document.head()
                    .getElementsByAttributeValue("name", "_csrf_parameter");
            Assert.assertEquals(1, csrfParameterMetaElement.size());
            Assert.assertEquals(springTokenParamName,
                    csrfParameterMetaElement.first().attr("content"));
        } catch (Exception e) {
            Assert.fail("Unable to parse the index html page");
        }
    }
}

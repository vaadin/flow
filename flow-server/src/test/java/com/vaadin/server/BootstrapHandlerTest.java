package com.vaadin.server;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.router.PageTitle;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Html;
import com.vaadin.ui.Text;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BootstrapHandlerTest {

    static final String UI_TITLE = "UI_TITLE";

    @PageTitle(UI_TITLE)
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "lazy.html", loadMode = LoadMode.LAZY)
    @JavaScript("eager.js")
    @StyleSheet("context://eager-relative.css")
    @StyleSheet("eager.css")
    @HtmlImport("eager.html")
    private class TestUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            super.init(request);
            add(new Html("<div foo=bar>foobar</div>"));
            add(new Text("Hello world"));
            add(new InlineTemplate("<div><script></script></div>"));
        }

    }

    private TestUI testUI;
    private BootstrapContext context;
    private VaadinRequest request;
    private VaadinSession session;
    private MockVaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration;
    private WebBrowser browser;

    @Before
    public void setup() {
        BootstrapHandler.clientEngineFile = "foobar";
        testUI = new TestUI();

        deploymentConfiguration = new MockDeploymentConfiguration();

        service = Mockito
                .spy(new MockVaadinServletService(deploymentConfiguration));

        session = Mockito.spy(new MockVaadinSession(service));
        session.lock();
        session.setConfiguration(deploymentConfiguration);
        testUI.getInternals().setSession(session);

        browser = Mockito.mock(WebBrowser.class);

        Mockito.when(browser.isEs6Supported()).thenReturn(false);
        Mockito.when(session.getBrowser()).thenReturn(browser);
    }

    @After
    public void tearDown() {
        session.unlock();
    }

    private void initUI(UI ui) {
        initUI(ui, createVaadinRequest());
    }

    private void initUI(UI ui, VaadinRequest request) {
        this.request = request;
        service.init();
        ui.doInit(request, 0);
        context = new BootstrapContext(request, null, session, ui);
    }

    @Test
    public void testInitialPageTitle_pageSetTitle_noExecuteJavascript() {
        initUI(testUI, createVaadinRequest());
        String overriddenPageTitle = "overridden";
        testUI.getPage().setTitle(overriddenPageTitle);

        assertEquals(overriddenPageTitle,
                BootstrapHandler.resolvePageTitle(context).get());

        assertEquals(0, testUI.getInternals().dumpPendingJavaScriptInvocations()
                .size());
    }

    @Test
    public void testInitialPageTitle_nullTitle_noTitle() {
        initUI(testUI, createVaadinRequest());
        assertFalse(BootstrapHandler.resolvePageTitle(context).isPresent());
    }

    @Test
    public void renderUI() throws IOException {
        TestUI anotherUI = new TestUI();
        initUI(testUI);
        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI);

        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);
        Element body = page.body();

        assertEquals(1, body.childNodeSize());
        assertEquals("noscript", body.child(0).tagName());
    }

    @Test // #1134
    public void testBody() throws Exception {
        initUI(testUI, createVaadinRequest());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Element body = page.head().nextElementSibling();

        assertEquals("body", body.tagName());
        assertEquals("html", body.parent().tagName());
        assertEquals(2, body.parent().childNodeSize());
    }

    @Test
    public void testBootstrapListener() throws ServiceException {
        List<BootstrapListener> listeners = new ArrayList<>(3);
        AtomicReference<VaadinUriResolver> resolver = new AtomicReference<>();
        listeners.add(evt -> evt.getDocument().head().getElementsByTag("script")
                .remove());
        listeners.add(evt -> {
            resolver.set(evt.getUriResolver());
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.1");
        });
        listeners.add(evt -> evt.getDocument().head().appendElement("script")
                .attr("src", "testing.2"));

        Mockito.when(service.createInstantiator())
                .thenReturn(new MockInstantiator(event -> listeners
                        .forEach(event::addBootstrapListener)));

        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI);
        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");
        assertEquals(2, scripts.size());
        assertEquals("testing.1", scripts.get(0).attr("src"));
        assertEquals("testing.2", scripts.get(1).attr("src"));

        Assert.assertNotNull(resolver.get());
        Assert.assertEquals(bootstrapContext.getUriResolver(), resolver.get());
    }

    @Test
    public void useDependencyFilters_removeDependenciesAndAddNewOnes()
            throws ServiceException {
        List<DependencyFilter> filters = new ArrayList<>(5);
        filters.add((list, context) -> {
            list.clear(); // remove everything
            return list;
        });
        filters.add((list, context) -> {
            list.add(new Dependency(Dependency.Type.HTML_IMPORT,
                    "imported-by-filter.html", LoadMode.EAGER));
            return list;
        });
        filters.add((list, context) -> {
            list.add(new Dependency(Dependency.Type.JAVASCRIPT,
                    "imported-by-filter.js", LoadMode.EAGER));
            list.add(new Dependency(Dependency.Type.JAVASCRIPT,
                    "imported-by-filter2.js", LoadMode.EAGER));
            return list;
        });
        filters.add((list, context) -> {
            list.remove(2); // removes the imported-by-filter2.js
            return list;
        });
        filters.add((list, context) -> {
            list.add(new Dependency(Dependency.Type.STYLESHEET,
                    "imported-by-filter.css", LoadMode.EAGER));
            return list;
        });

        Mockito.when(service.createInstantiator())
                .thenReturn(new MockInstantiator(
                        event -> filters.forEach(event::addDependencyFilter)));

        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI);
        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");
        boolean found = scripts.stream().anyMatch(element -> element.attr("src")
                .equals("./frontend/imported-by-filter.js"));
        Assert.assertTrue(
                "imported-by-filter.js should be in the head of the page",
                found);

        found = scripts.stream().anyMatch(element -> element.attr("src")
                .equals("./frontend/imported-by-filter2.js"));
        Assert.assertFalse(
                "imported-by-filter2.js shouldn't be in the head of the page",
                found);

        found = scripts.stream()
                .anyMatch(element -> element.attr("src").equals("./eager.js"));
        Assert.assertFalse("eager.js shouldn't be in the head of the page",
                found);

        Elements links = page.head().getElementsByTag("link");
        found = links.stream().anyMatch(element -> element.attr("href")
                .equals("./frontend/imported-by-filter.css"));
        Assert.assertTrue(
                "imported-by-filter.css should be in the head of the page",
                found);

        found = links.stream().anyMatch(element -> element.attr("href")
                .equals("./frontend/imported-by-filter.html"));
        Assert.assertTrue(
                "imported-by-filter.html should be in the head of the page",
                found);
    }

    @Test
    public void frontendProtocol_productionMode_useDifferentUrlsForEs5AndEs6() {
        initUI(testUI);
        deploymentConfiguration.setProductionMode(true);
        WebBrowser mockedWebBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedWebBrowser);

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(true);

        String resolvedContext = context.getUriResolver()
                .resolveVaadinUri(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);

        String urlES6 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals(Constants.FRONTEND_URL_ES6_DEFAULT_VALUE.replace(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, resolvedContext)
                + "foo", urlES6);

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(false);

        String urlES5 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals(Constants.FRONTEND_URL_ES5_DEFAULT_VALUE.replace(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX, resolvedContext)
                + "foo", urlES5);

        Mockito.verify(session, Mockito.times(3)).getBrowser();
    }

    @Test
    public void frontendProtocol_notInProductionMode_useDefaultFrontend() {
        initUI(testUI);
        deploymentConfiguration.setProductionMode(false);
        WebBrowser mockedWebBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedWebBrowser);

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(true);

        String resolvedContext = context.getUriResolver()
                .resolveVaadinUri(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);

        String urlES6 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals(resolvedContext + "frontend/foo", urlES6);

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(false);

        String urlES5 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals(resolvedContext + "frontend/foo", urlES5);

        Mockito.verify(session, Mockito.times(3)).getBrowser();
    }

    @Test
    public void frontendProtocol_notInProductionModeAndWithProperties_useProperties() {
        initUI(testUI);
        deploymentConfiguration.setProductionMode(false);
        WebBrowser mockedWebBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedWebBrowser);

        deploymentConfiguration.setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES6, "bar/es6/");
        deploymentConfiguration.setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES5, "bar/es5/");

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(true);

        String urlES6 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals("bar/es6/foo", urlES6);

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(false);

        String urlES5 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + "foo");

        assertEquals("bar/es5/foo", urlES5);

        Mockito.verify(session, Mockito.times(3)).getBrowser();
    }

    @Test
    public void es6IsNotSupported_es6CollectionsAreInlined()
            throws IOException {
        Assert.assertTrue(hasEs6Inlined());
    }

    @Test
    public void bootstrapPage_configJsonPatternIsReplacedBeforeInitialUidl() {
        TestUI anotherUI = new TestUI();
        initUI(testUI);

        SystemMessages messages = Mockito.mock(SystemMessages.class);
        Mockito.when(service.getSystemMessages(Matchers.any(Locale.class),
                Matchers.any(VaadinRequest.class))).thenReturn(messages);
        Mockito.when(messages.isSessionExpiredNotificationEnabled())
                .thenReturn(true);
        Mockito.when(session.getSession())
                .thenReturn(Mockito.mock(WrappedSession.class));

        String url = "http://{{CONFIG_JSON}}/file";
        Mockito.when(messages.getSessionExpiredURL()).thenReturn(url);

        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI);

        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);
        Element head = page.head();
        Assert.assertTrue(head.outerHtml().contains(url));
    }

    @Test
    public void es6IsSupported_noEs6ScriptInlined() throws IOException {
        Mockito.when(browser.isEs6Supported()).thenReturn(true);
        Assert.assertFalse(hasEs6Inlined());
    }

    private VaadinRequest createVaadinRequest() {
        HttpServletRequest request = createRequest();
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
    }

    private boolean hasEs6Inlined() throws IOException {
        TestUI anotherUI = new TestUI();
        initUI(testUI);
        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI);

        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);
        Element head = page.head();

        StringBuilder builder = new StringBuilder();
        try (InputStream stream = getClass()
                .getResourceAsStream("es6-collections.js")) {
            IOUtils.readLines(stream, StandardCharsets.UTF_8).stream()
                    .forEach(builder::append);

        }
        boolean hasEs6Inlined = head.getElementsByTag("script").stream()
                .anyMatch(script -> script.data().contains(builder.toString()));
        return hasEs6Inlined;
    }
}

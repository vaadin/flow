package com.vaadin.server;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.router.PageTitle;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.TestRouteRegistry;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.VaadinUriResolver;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Html;
import com.vaadin.ui.Tag;
import com.vaadin.ui.Text;
import com.vaadin.ui.UI;
import com.vaadin.ui.Viewport;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

    @Route("")
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class RootNavigationTarget extends Component {
    }

    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class Parent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    public static class MiddleParent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class RootWithParent extends Component {
    }

    @Route(value = "", layout = MiddleParent.class)
    @Tag(Tag.DIV)
    public static class RootWithParents extends Component {
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

        deploymentConfiguration = new MockDeploymentConfiguration("test/");

        service = Mockito
                .spy(new MockVaadinServletService(deploymentConfiguration));
        Mockito.when(service.getRouteRegistry())
                .thenReturn(new TestRouteRegistry());

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

    private void initUI(UI ui, VaadinRequest request,
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {

        service.getRouteRegistry().setNavigationTargets(navigationTargets);

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

    @Test // #2956
    public void head_has_ui_lang() throws Exception {
        initUI(testUI, createVaadinRequest());
        testUI.setLocale(Locale.FRENCH);

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Element body = page.head().nextElementSibling();

        assertEquals("Expected body element", "body", body.tagName());
        assertEquals("Expected html element as parent to body element", "html",
                body.parent().tagName());

        assertTrue("Html tag was missing lang attribute",
                body.parent().hasAttr("lang"));
        assertEquals("Lang did not have UI defined language",
                testUI.getLocale().getLanguage(), body.parent().attr("lang"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootNavigationTarget.class));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Assert.assertTrue(page.toString().contains(
                "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route_parent()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootWithParent.class));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Assert.assertTrue(page.toString().contains(
                "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route_top_parent()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootWithParents.class));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Assert.assertTrue(page.toString().contains(
                "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test
    public void headHasMetaTags() throws Exception {
        initUI(testUI, createVaadinRequest());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Element head = page.head();
        Elements metas = head.getElementsByTag("meta");

        Assert.assertEquals(2, metas.size());
        Element meta = metas.get(0);
        assertEquals("Content-Type", meta.attr("http-equiv"));
        assertEquals("text/html; charset=utf-8", meta.attr("content"));

        meta = metas.get(1);
        assertEquals("X-UA-Compatible", meta.attr("http-equiv"));
        assertEquals("IE=edge", meta.attr("content"));
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
    public void frontendProtocol_productionModeAndWithProperties_useProperties() {
        initUI(testUI);
        deploymentConfiguration.setProductionMode(true);
        WebBrowser mockedWebBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedWebBrowser);

        String es6Prefix = "bar/es6/";
        deploymentConfiguration.setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES6, es6Prefix);

        String es5Prefix = "bar/es5/";
        deploymentConfiguration.setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_ES5, es5Prefix);
        String urlPart = "foo";

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(true);
        String urlES6 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + urlPart);
        assertThat(String.format(
                "In development mode, es6 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_ES6), urlES6, is(es6Prefix + urlPart));

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(false);
        String urlES5 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + urlPart);
        assertThat(String.format(
                "In development mode, es5 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_ES5), urlES5, is(es5Prefix + urlPart));

        Mockito.verify(session, Mockito.times(3)).getBrowser();
        Mockito.verify(mockedWebBrowser, Mockito.times(2)).isEs6Supported();
    }

    @Test
    public void frontendProtocol_notInProductionModeAndWithProperties_useProperties() {
        initUI(testUI);
        deploymentConfiguration.setProductionMode(false);
        WebBrowser mockedWebBrowser = Mockito.mock(WebBrowser.class);
        Mockito.when(session.getBrowser()).thenReturn(mockedWebBrowser);

        String devPrefix = "bar/dev/";
        deploymentConfiguration.setApplicationOrSystemProperty(
                Constants.FRONTEND_URL_DEV, devPrefix);
        String urlPart = "foo";

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(true);
        String urlES6 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + urlPart);
        assertThat(String.format(
                "In development mode, es6 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_DEV), urlES6, is(devPrefix + urlPart));

        Mockito.when(mockedWebBrowser.isEs6Supported()).thenReturn(false);
        String urlES5 = context.getUriResolver().resolveVaadinUri(
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX + urlPart);
        assertThat(String.format(
                "In development mode, es5 prefix should be equal to '%s' parameter value",
                Constants.FRONTEND_URL_DEV), urlES5, is(devPrefix + urlPart));

        Mockito.verify(session, Mockito.times(3)).getBrowser();
        Mockito.verify(mockedWebBrowser, Mockito.times(2)).isEs6Supported();
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
    public void es6NotSupported_webcomponentsPolyfillBasePresent_polyfillsLoaded() {
        Mockito.when(browser.isEs6Supported()).thenReturn(false);

        Element head = initTestUI();

        checkInlinedScript(head, "es6-collections.js", true);
        checkInlinedScript(head, "babel-helpers.min.js", true);
    }

    @Test
    public void es6IsSupported_noPolyfillsLoaded() {
        Mockito.when(browser.isEs6Supported()).thenReturn(true);

        Element head = initTestUI();

        checkInlinedScript(head, "es6-collections.js", false);
        checkInlinedScript(head, "babel-helpers.min.js", false);
    }

    private Element initTestUI() {
        TestUI anotherUI = new TestUI();
        initUI(testUI);
        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI);
        return BootstrapHandler.getBootstrapPage(bootstrapContext).head();
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

    private void checkInlinedScript(Element head, String scriptName,
            boolean shouldBeInlined) {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = getClass().getResourceAsStream(scriptName)) {
            IOUtils.readLines(stream, StandardCharsets.UTF_8)
                    .forEach(builder::append);
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

        boolean inlined = head.getElementsByTag("script").stream()
                .anyMatch(script -> script.data().contains(builder.toString()));
        if (shouldBeInlined) {
            assertTrue(String.format(
                    "Expect the script '%s' to be inlined in document head",
                    scriptName), inlined);
        } else {
            assertFalse(String.format(
                    "Expect document head NOT to contain script '%s'",
                    scriptName), inlined);
        }
    }
}

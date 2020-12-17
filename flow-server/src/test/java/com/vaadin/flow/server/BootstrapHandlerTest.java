package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.BootstrapHandler.BootstrapContext;
import com.vaadin.flow.server.MockServletServiceSessionSetup.TestVaadinServletService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.Constants.VAADIN_MAPPING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BootstrapHandlerTest {

    static final String UI_TITLE = "UI_TITLE";

    private static final String EXPECTED_THEME_CONTENTS = "<script id=\"_theme-header-injection\">\n"
            + "function _inlineHeader(tag, content){\n"
            + "var customStyle = document.createElement(tag);\n"
            + "customStyle.innerHTML= content;\n"
            + "var firstScript=document.head.querySelector('script');\n"
            + "document.head.insertBefore(customStyle,firstScript);\n" + "}\n"
            + "_inlineHeader('custom-style','<style include=\"lumo-typography\"></style>');\n"
            + "var script = document.getElementById('_theme-header-injection');"
            + "if ( script ) { document.head.removeChild(script);}\n"
            + "</script>";

    @PageTitle(UI_TITLE)
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @JavaScript("eager.js")
    @StyleSheet("context://eager-relative.css")
    @StyleSheet("eager.css")
    protected static class TestUI extends UI {

        public TestUI() {
        }

        @Override
        protected void init(VaadinRequest request) {
            super.init(request);
            add(new Html("<div foo=bar>foobar</div>"));
            add(new Text("Hello world"));
        }

    }

    @Route("")
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class InitialPageConfiguratorViewportOverride
            extends Component implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.setViewport("width=100");
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorPrependContents extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineWithContents(InitialPageSettings.Position.PREPEND,
                    "window.messages = window.messages || [];\n"
                            + "window.messages.push(\"content script\");",
                    InitialPageSettings.WrapMode.JAVASCRIPT);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorPrependFile extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineFromFile(InitialPageSettings.Position.PREPEND,
                    "inline.js", InitialPageSettings.WrapMode.JAVASCRIPT);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorAppendFiles extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineFromFile("inline.js",
                    InitialPageSettings.WrapMode.JAVASCRIPT);
            settings.addInlineFromFile("inline.html",
                    InitialPageSettings.WrapMode.NONE);
            settings.addInlineFromFile("inline.css",
                    InitialPageSettings.WrapMode.STYLESHEET);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorLinks extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addLink("icons/favicon.ico",
                    new LinkedHashMap<String, String>() {
                        {
                            put("rel", "shortcut icon");
                        }
                    });
            settings.addLink("icons/icon-192.png",
                    new LinkedHashMap<String, String>() {
                        {
                            put("rel", "icon");
                            put("sizes", "192x192");
                        }
                    });
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorLinkShorthands extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addLink("shortcut icon", "icons/favicon.ico");
            settings.addFavIcon("icon", "icons/icon-192.png", "192x192");
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorMetaTag extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addMetaTag(InitialPageSettings.Position.PREPEND,
                    "theme-color", "#227aef");
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorBodyStyle extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineWithContents(
                    "body {width: 100%; height:100vh; margin:0;}",
                    InitialPageSettings.WrapMode.STYLESHEET);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    @BodySize(height = "10px", width = "20px")
    public static class BodySizeAnnotated extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @BodySize
    public static class EmptyBodySizeAnnotated extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @BodySize(height = "10px", width = "20px")
    @StyleSheet("bodysize.css")
    public static class BodySizeAnnotatedAndCss extends Component {
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

    @Route("")
    @RouteAlias(value = "alias", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class AliasLayout extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Inline("inline.js")
    @Inline("inline.html")
    @Inline("inline.css")
    public static class InlineAnnotations extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Inline(value = "inline.css", position = Inline.Position.PREPEND)
    @Inline(value = "inline.html", position = Inline.Position.PREPEND)
    @Inline(value = "inline.js", position = Inline.Position.PREPEND)
    public static class PrependInlineAnnotations extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Inline(value = "inline.css", target = TargetElement.BODY)
    @Inline(value = "inline.html", target = TargetElement.BODY)
    @Inline(value = "inline.js", target = TargetElement.BODY)
    public static class InlineAnnotationsBodyTarget extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Inline(value = "inline.css", target = TargetElement.BODY, position = Inline.Position.PREPEND)
    @Inline(value = "inline.html", target = TargetElement.BODY, position = Inline.Position.PREPEND)
    @Inline(value = "inline.js", target = TargetElement.BODY, position = Inline.Position.PREPEND)
    public static class PrependInlineAnnotationsBodyTarget extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    @Inline(value = "inline.js", wrapping = Inline.Wrapping.STYLESHEET)
    public static class ForcedWrapping extends Component {
    }

    @Tag(Tag.DIV)
    public static abstract class AbstractMain extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class ExtendingView extends AbstractMain {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class InitialPageConfiguratorRoute extends Component
            implements PageConfigurator {

        public static final int SECOND_DELAY = 700000;

        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.getLoadingIndicatorConfiguration()
                    .setApplyDefaultTheme(false);
            settings.getLoadingIndicatorConfiguration()
                    .setSecondDelay(SECOND_DELAY);

            settings.getPushConfiguration().setPushMode(PushMode.MANUAL);
        }
    }

    private TestUI testUI;
    private BootstrapContext context;
    private VaadinRequest request;
    private VaadinSession session;
    private TestVaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration;
    private MockServletServiceSessionSetup mocks;
    private BootstrapHandler.BootstrapPageBuilder pageBuilder = new BootstrapHandler.BootstrapPageBuilder();

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        TestRouteRegistry routeRegistry = new TestRouteRegistry();

        testUI = new TestUI();

        deploymentConfiguration = mocks.getDeploymentConfiguration();
        deploymentConfiguration.setEnableDevServer(false);

        service = mocks.getService();
        service.setRouteRegistry(routeRegistry);
        service.setRouter(new Router(routeRegistry) {
            @Override
            public void initializeUI(UI ui, VaadinRequest initRequest) {
                // Skip initial navigation during UI.init if no routes have been
                // injected
                if (routeRegistry.hasNavigationTargets()) {
                    super.initializeUI(ui, initRequest);
                }
            }
        });

        session = mocks.getSession();

        // Update sessionRegistry due to after init change of global registry
        SessionRouteRegistry sessionRegistry = new SessionRouteRegistry(
                session);
        Mockito.when(session.getAttribute(SessionRouteRegistry.class))
                .thenReturn(sessionRegistry);

        testUI.getInternals().setSession(session);
    }

    @After
    public void tearDown() {
        service.setDependencyFilters(null); // Reset to default
        session.unlock();
        mocks.cleanup();
    }

    private void initUI(UI ui) {
        initUI(ui, createVaadinRequest());
    }

    private void initUI(UI ui, VaadinRequest request) {
        this.request = request;

        ui.doInit(request, 0);
        ui.getInternals().getRouter().initializeUI(ui, request);
        context = new BootstrapContext(request, null, session, ui,
                this::contextRootRelativePath);
        ui.getInternals().setContextRoot(contextRootRelativePath(request));
    }

    private void initUI(UI ui, VaadinRequest request,
            Set<Class<? extends Component>> navigationTargets)
            throws InvalidRouteConfigurationException {

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(service.getRouteRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            navigationTargets.forEach(routeConfiguration::setAnnotatedRoute);
        });

        initUI(ui, request);
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
        anotherUI.getInternals().getRouter().initializeUI(anotherUI, request);
        anotherUI.getInternals()
                .setContextRoot(contextRootRelativePath(request));
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI, this::contextRootRelativePath);

        Document page = pageBuilder.getBootstrapPage(bootstrapContext);
        Element body = page.body();

        assertEquals(2, body.childNodeSize());
        assertEquals("noscript", body.child(0).tagName());
    }

    @Test // #1134
    public void testBody() throws Exception {
        initUI(testUI, createVaadinRequest());

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Element body = page.head().nextElementSibling();

        assertEquals("body", body.tagName());
        assertEquals("html", body.parent().tagName());
        assertEquals(2, body.parent().childNodeSize());
    }

    @Test // #2956
    public void head_has_ui_lang() throws Exception {
        initUI(testUI, createVaadinRequest());
        testUI.setLocale(Locale.FRENCH);

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

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

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Assert.assertTrue("Viewport meta tag was missing",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route_parent()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootWithParent.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Assert.assertTrue("Viewport meta tag was missing",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route_top_parent()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootWithParents.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Assert.assertTrue("Viewport meta tag was missing",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // #3008
    public void bootstrap_page_has_viewport_for_route_alias_parent()
            throws InvalidRouteConfigurationException {
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(httpRequest).getServletPath();
        VaadinServletRequest vaadinRequest = new VaadinServletRequest(
                httpRequest, service);

        initUI(testUI, vaadinRequest, Collections.singleton(AliasLayout.class));

        Document page = pageBuilder
                .getBootstrapPage(new BootstrapContext(vaadinRequest, null,
                        session, testUI, this::contextRootRelativePath));

        Assert.assertFalse("Viewport found even though not part of Route",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));

        Mockito.doAnswer(invocation -> "/alias").when(httpRequest)
                .getPathInfo();

        page = pageBuilder.getBootstrapPage(new BootstrapContext(vaadinRequest,
                null, session, testUI, this::contextRootRelativePath));

        Assert.assertTrue(
                "Viewport meta tag was missing even tough alias route parent has annotation",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));
    }

    @Test // 3036
    public void page_configurator_overrides_viewport()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(), Collections
                .singleton(InitialPageConfiguratorViewportOverride.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Assert.assertFalse(
                "Viewport annotation value found even if it should be overridden.",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=device-width\">"));

        Assert.assertTrue("Viewport annotation value not the expected one.",
                page.toString().contains(
                        "<meta name=\"viewport\" content=\"width=100\">"));
    }

    @Test // 3036
    public void page_configurator_inlines_javascript_from_content()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(), Collections
                .singleton(InitialPageConfiguratorPrependContents.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();
        // Note element 0 is the full head element.
        assertStringEquals(
                "Content javascript should have been prepended to head element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"content script\");</script>",
                allElements.get(1).toString());
    }

    @Test // 3036
    public void page_configurator_inlines_prepend_javascript_from_file()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(), Collections
                .singleton(InitialPageConfiguratorPrependFile.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();
        // Note element 0 is the full head element.
        assertStringEquals(
                "Content javascript should have been prepended to head element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(1).toString());
    }

    @Test // 3036
    public void page_configurator_append_inline_form_files()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(), Collections
                .singleton(InitialPageConfiguratorAppendFiles.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();
        // Note element 0 is the full head element.
        assertStringEquals(
                "File javascript should have been appended to head element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(allElements.size() - 3).toString());
        assertStringEquals(
                "File html should have been appended to head element",
                "<script type=\"text/javascript\">\n"
                        + "    // document.body might not yet be accessible, so just leave a message\n"
                        + "    window.messages = window.messages || [];\n"
                        + "    window.messages.push(\"inline.html\");\n"
                        + "</script>",
                allElements.get(allElements.size() - 2).toString());
        assertStringEquals("File css should have been appended to head element",
                "<style type=\"text/css\">/* inline.css */\n" + "\n"
                        + "#preloadedDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}\n" + "\n"
                        + "#inlineCssTestDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}</style>",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test // 3036
    public void page_configurator_adds_link()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InitialPageConfiguratorLinks.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Assert.assertEquals(
                "<link href=\"icons/favicon.ico\" rel=\"shortcut icon\">",
                allElements.get(allElements.size() - 2).toString());
        Assert.assertEquals(
                "<link href=\"icons/icon-192.png\" rel=\"icon\" sizes=\"192x192\">",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test // 3036
    public void page_configurator_adds_meta_tags()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InitialPageConfiguratorMetaTag.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Assert.assertEquals("<meta name=\"theme-color\" content=\"#227aef\">",
                allElements.get(1).toString());
    }

    @Test // 3203
    public void page_configurator_link_shorthands_are_added_correctly()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(), Collections
                .singleton(InitialPageConfiguratorLinkShorthands.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Assert.assertEquals(
                "<link href=\"icons/favicon.ico\" rel=\"shortcut icon\">",
                allElements.get(allElements.size() - 2).toString());
        Assert.assertEquals(
                "<link href=\"icons/icon-192.png\" rel=\"icon\" sizes=\"192x192\">",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test // 2344
    public void page_configurator_adds_styles_for_body()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InitialPageConfiguratorBodyStyle.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Assert.assertEquals(
                "<style type=\"text/css\">body {width: 100%; height:100vh; margin:0;}</style>",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test // 2344
    public void body_size_adds_styles_for_body()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(BodySizeAnnotated.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Optional<Element> styleTag = allElements.stream()
                .filter(element -> element.tagName().equals("style"))
                .findFirst();

        Assert.assertTrue("Expected a style element in head.",
                styleTag.isPresent());

        Assert.assertTrue(
                "The first style tag should start with body style from @BodySize",
                styleTag.get().toString().startsWith(
                        "<style type=\"text/css\">body {height:10px;width:20px;margin:0;}"));
    }

    @Test
    public void css_body_size_overrides_annotated_body_size()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(BodySizeAnnotatedAndCss.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Optional<Element> styleTag = allElements.stream()
                .filter(element -> element.tagName().equals("style"))
                .findFirst();

        Assert.assertTrue("Expected a style element in head.",
                styleTag.isPresent());

        Assert.assertTrue(
                "The first style tag should start with body style from @BodySize",
                styleTag.get().toString().startsWith(
                        "<style type=\"text/css\">body {height:10px;width:20px;margin:0;}"));

        Optional<Element> cssImportTag = allElements.stream().filter(
                element -> element.attr("href").contains("bodysize.css"))
                .findFirst();

        Assert.assertTrue("Expected import for bodysize.css in head.",
                cssImportTag.isPresent());

        Assert.assertTrue(
                "Styles defined with @BodySize should be imported before css-files in the head,"
                        + " so that body size defined in css overrides the annotated values.",
                allElements.indexOf(styleTag.get()) < allElements
                        .indexOf(cssImportTag.get()));
    }

    @Test // 3749
    public void no_body_size_or_page_configurator_adds_margin_and_full_size_for_body()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RootNavigationTarget.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Optional<Element> styleTag = allElements.stream()
                .filter(element -> element.tagName().equals("style"))
                .findFirst();

        Assert.assertTrue("Expected a style element in head.",
                styleTag.isPresent());

        Assert.assertTrue(
                "The first style tag should start with body style containing default body size and margin",
                styleTag.get().toString().startsWith(
                        "<style type=\"text/css\">body {height:100vh;width:100%;margin:0;}"));
    }

    @Test // 3749
    public void empty_body_size_adds_margin_but_no_size_for_body()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(EmptyBodySizeAnnotated.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Optional<Element> styleTag = allElements.stream()
                .filter(element -> element.tagName().equals("style"))
                .findFirst();

        Assert.assertTrue("Expected a style element in head.",
                styleTag.isPresent());

        Assert.assertTrue(
                "The first style tag should start with body style containing only margin",
                styleTag.get().toString().startsWith(
                        "<style type=\"text/css\">body {margin:0;}"));
    }

    @Test // 3010
    public void use_inline_to_append_files_to_head()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InlineAnnotations.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();
        assertStringEquals(
                "File javascript should have been appended to head element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(allElements.size() - 3).toString());
        assertStringEquals(
                "File html should have been appended to head element",
                "<script type=\"text/javascript\">\n"
                        + "    // document.body might not yet be accessible, so just leave a message\n"
                        + "    window.messages = window.messages || [];\n"
                        + "    window.messages.push(\"inline.html\");\n"
                        + "</script>",
                allElements.get(allElements.size() - 2).toString());
        assertStringEquals("File css should have been appended to head element",
                "<style type=\"text/css\">/* inline.css */\n" + "\n"
                        + "#preloadedDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}\n" + "\n"
                        + "#inlineCssTestDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}</style>",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test // 3010
    public void use_inline_to_prepend_files_to_head()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(PrependInlineAnnotations.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();
        // Note element 0 is the full head element.
        assertStringEquals(
                "File javascript should have been prepended to head element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(1).toString());
        assertStringEquals(
                "File html should have been prepended to head element",
                "<script type=\"text/javascript\">\n"
                        + "    // document.body might not yet be accessible, so just leave a message\n"
                        + "    window.messages = window.messages || [];\n"
                        + "    window.messages.push(\"inline.html\");\n"
                        + "</script>",
                allElements.get(2).toString());
        assertStringEquals(
                "File css should have been prepended to head element",
                "<style type=\"text/css\">/* inline.css */\n" + "\n"
                        + "#preloadedDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}\n" + "\n"
                        + "#inlineCssTestDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}</style>",
                allElements.get(3).toString());
    }

    @Test // 3010
    public void use_inline_to_append_files_to_body()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InlineAnnotationsBodyTarget.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.body().getAllElements();
        assertStringEquals("File css should have been appended to body element",
                "<style type=\"text/css\">/* inline.css */\n" + "\n"
                        + "#preloadedDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}\n" + "\n"
                        + "#inlineCssTestDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}</style>",
                allElements.get(allElements.size() - 4).toString());
        assertStringEquals(
                "File html should have been appended to body element",
                "<script type=\"text/javascript\">\n"
                        + "    // document.body might not yet be accessible, so just leave a message\n"
                        + "    window.messages = window.messages || [];\n"
                        + "    window.messages.push(\"inline.html\");\n"
                        + "</script>",
                allElements.get(allElements.size() - 3).toString());
        assertStringEquals(
                "File javascript should have been appended to body element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(allElements.size() - 2).toString());
    }

    @Test // 3010
    public void use_inline_to_prepend_files_to_body()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(), Collections
                .singleton(PrependInlineAnnotationsBodyTarget.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.body().getAllElements();
        // Note element 0 is the full head element.
        assertStringEquals(
                "File javascript should have been prepended to body element",
                "<script type=\"text/javascript\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</script>",
                allElements.get(1).toString());
        assertStringEquals(
                "File html should have been prepended to body element",
                "<script type=\"text/javascript\">\n"
                        + "    // document.body might not yet be accessible, so just leave a message\n"
                        + "    window.messages = window.messages || [];\n"
                        + "    window.messages.push(\"inline.html\");\n"
                        + "</script>",
                allElements.get(2).toString());
        assertStringEquals(
                "File css should have been prepended to body element",
                "<style type=\"text/css\">/* inline.css */\n" + "\n"
                        + "#preloadedDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}\n" + "\n"
                        + "#inlineCssTestDiv {\n"
                        + "    color: rgba(255, 255, 0, 1);\n" + "}</style>",
                allElements.get(3).toString());
    }

    @Test // 3010
    public void force_wrapping_of_file()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(ForcedWrapping.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        assertStringEquals(
                "File css should have been prepended to body element",
                "<style type=\"text/css\">window.messages = window.messages || [];\n"
                        + "window.messages.push(\"inline.js\");</style>",
                allElements.get(allElements.size() - 1).toString());
    }

    @Test
    public void index_appended_to_head_in_npm()
            throws InvalidRouteConfigurationException {

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(AliasLayout.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Elements allElements = page.head().getAllElements();

        Assert.assertTrue(
                "index.js should be added to head for ES6 browsers. (type module with crossorigin)",
                allElements.stream().map(Object::toString)
                        .anyMatch(element -> element
                                .equals("<script type=\"module\" src=\"./"
                                        + VAADIN_MAPPING
                                        + "build/vaadin-bundle-1111.cache.js\" data-app-id=\""
                                        + testUI.getInternals().getAppId()
                                        + "\" crossorigin></script>")));
    }

    @Test
    public void headHasMetaTags() throws Exception {
        initUI(testUI, createVaadinRequest());

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Element head = page.head();
        Elements metas = head.getElementsByTag("meta");

        Assert.assertEquals(3, metas.size());
        Element meta = metas.get(0);
        assertEquals("Content-Type", meta.attr("http-equiv"));
        assertEquals("text/html; charset=utf-8", meta.attr("content"));

        meta = metas.get(1);
        assertEquals("X-UA-Compatible", meta.attr("http-equiv"));
        assertEquals("IE=edge", meta.attr("content"));

        meta = metas.get(2);
        assertEquals(BootstrapHandler.VIEWPORT, meta.attr("name"));
        assertEquals(Viewport.DEFAULT,
                meta.attr(BootstrapHandler.CONTENT_ATTRIBUTE));
    }

    @Test
    public void bootstrapListener_isInvokedInServerSideMode()
            throws ServiceException {
        AtomicReference<VaadinUriResolver> resolver = new AtomicReference<>();
        service.addBootstrapListener(evt -> evt.getDocument().head()
                .getElementsByTag("script").remove());
        service.addBootstrapListener(evt -> {
            resolver.set(evt.getUriResolver());
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.1");
        });
        service.addBootstrapListener(evt -> evt.getDocument().head()
                .appendElement("script").attr("src", "testing.2"));

        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath);
        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");
        assertEquals(2, scripts.size());
        assertEquals("testing.1", scripts.get(0).attr("src"));
        assertEquals("testing.2", scripts.get(1).attr("src"));

        Assert.assertNotNull(resolver.get());
        Assert.assertEquals(bootstrapContext.getUriResolver(), resolver.get());
    }

    @Test(expected = IllegalStateException.class)
    public void bootstrapListener_throwsInClientSideMode()
            throws ServiceException {
        deploymentConfiguration.useDeprecatedV14Bootstrapping(false);

        ServiceInitEvent event = new ServiceInitEvent(service);
        event.addBootstrapListener(evt -> {
        });
    }

    @Test
    public void useDependencyFilters_removeDependenciesAndAddNewOnes()
            throws ServiceException {
        List<DependencyFilter> filters = Arrays.asList((list, context) -> {
            list.clear(); // remove everything
            return list;
        }, (list, context) -> {
            list.add(new Dependency(Dependency.Type.JAVASCRIPT,
                    "imported-by-filter.js", LoadMode.EAGER));
            list.add(new Dependency(Dependency.Type.JAVASCRIPT,
                    "imported-by-filter2.js", LoadMode.EAGER));
            return list;
        }, (list, context) -> {
            list.remove(1); // removes the imported-by-filter2.js
            return list;
        }, (list, context) -> {
            list.add(new Dependency(Dependency.Type.STYLESHEET,
                    "imported-by-filter.css", LoadMode.EAGER));
            return list;
        });
        service.setDependencyFilters(filters);

        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath);
        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");
        boolean found = scripts.stream().anyMatch(
                element -> element.attr("src").equals("imported-by-filter.js"));
        Assert.assertTrue(
                "imported-by-filter.js should be in the head of the page",
                found);

        found = scripts.stream().anyMatch(element -> element.attr("src")
                .equals("imported-by-filter2.js"));
        Assert.assertFalse(
                "imported-by-filter2.js shouldn't be in the head of the page",
                found);

        found = scripts.stream()
                .anyMatch(element -> element.attr("src").equals("./eager.js"));
        Assert.assertFalse("eager.js shouldn't be in the head of the page",
                found);

        Elements links = page.head().getElementsByTag("link");
        found = links.stream().anyMatch(element -> element.attr("href")
                .equals("imported-by-filter.css"));
        Assert.assertTrue(
                "imported-by-filter.css should be in the head of the page",
                found);
    }

    @Test
    public void bootstrapPage_configJsonPatternIsReplacedBeforeInitialUidl() {
        TestUI anotherUI = new TestUI();
        initUI(testUI);

        SystemMessages messages = Mockito.mock(SystemMessages.class);
        service.setSystemMessagesProvider(info -> messages);
        Mockito.when(messages.isSessionExpiredNotificationEnabled())
                .thenReturn(true);
        Mockito.when(session.getSession())
                .thenReturn(Mockito.mock(WrappedSession.class));

        String url = "http://{{CONFIG_JSON}}/file";
        Mockito.when(messages.getSessionExpiredURL()).thenReturn(url);

        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        anotherUI.getInternals().getRouter().initializeUI(anotherUI, request);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI, this::contextRootRelativePath);
        anotherUI.getInternals()
                .setContextRoot(contextRootRelativePath(request));

        Document page = pageBuilder.getBootstrapPage(bootstrapContext);
        Element head = page.head();
        Assert.assertTrue(head.outerHtml().contains(url));
    }

    @Test // UIInitListeners
    public void uiInitialization_allRegisteredListenersAreNotified() {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        AtomicReference<UI> uiReference = new AtomicReference<>();

        Registration registration = service.addUIInitListener(
                event -> Assert.assertTrue("Atomic reference was not empty.",
                        uiReference.compareAndSet(null, event.getUI())));
        final BootstrapContext context = bootstrapHandler.createAndInitUI(
                TestUI.class, createVaadinRequest(), response, session);

        Assert.assertEquals("Event UI didn't match initialized UI instance.",
                context.getUI(), uiReference.get());

        // unregister listener
        registration.remove();

        AtomicReference<UI> secondListenerReference = new AtomicReference<>();
        service.addUIInitListener(event -> Assert.assertTrue(
                "Atomic reference did not contain previous UI.",
                uiReference.compareAndSet(context.getUI(), event.getUI())));
        service.addUIInitListener(event -> Assert.assertTrue(
                "Atomic reference was not empty.",
                secondListenerReference.compareAndSet(null, event.getUI())));

        final BootstrapContext secondInit = bootstrapHandler.createAndInitUI(
                TestUI.class, createVaadinRequest(), response, session);

        Assert.assertEquals("Event UI didn't match initialized UI instance.",
                secondInit.getUI(), uiReference.get());
        Assert.assertEquals(
                "Second event UI didn't match initialized UI instance.",
                secondInit.getUI(), secondListenerReference.get());
    }

    @Test // UIInitListeners
    public void uiInitialization_changingListenersOnEventWorks() {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        AtomicReference<UI> uiReference = new AtomicReference<>();

        Registration registration = service.addUIInitListener(
                event -> service.addUIInitListener(laterEvent -> uiReference
                        .compareAndSet(null, laterEvent.getUI())));
        bootstrapHandler.createAndInitUI(TestUI.class, createVaadinRequest(),
                response, session);

        Assert.assertEquals("Event UI didn't match initialized UI instance.",
                null, uiReference.get());

        // unregister listener
        registration.remove();

        service.addUIInitListener(event -> registration.remove());

        final BootstrapContext secondInit = bootstrapHandler.createAndInitUI(
                TestUI.class, createVaadinRequest(), response, session);

        Assert.assertEquals("Event UI didn't match initialized UI instance.",
                secondInit.getUI(), uiReference.get());
    }

    private void bootstrapPage_productionModeTest(boolean productionMode) {
        mocks.setProductionMode(productionMode);
        TestUI anotherUI = new TestUI();
        initUI(testUI);

        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        anotherUI.getInternals().getRouter().initializeUI(anotherUI, request);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI, this::contextRootRelativePath);
        anotherUI.getInternals()
                .setContextRoot(contextRootRelativePath(request));

        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Element head = page.head();
        Assert.assertTrue(
                head.outerHtml().contains("mode = " + productionMode));
    }

    @Test
    public void bootstrapPage_productionModeTrueIsReplaced() {
        bootstrapPage_productionModeTest(true);
    }

    @Test
    public void bootstrapPage_productionModeFalseIsReplaced() {
        bootstrapPage_productionModeTest(false);
    }

    @Route("")
    @Tag(Tag.DIV)
    @Meta(name = "apple-mobile-web-app-capable", content = "yes")
    @Meta(name = "apple-mobile-web-app-status-bar-style", content = "black")
    public static class MetaAnnotations extends Component {
    }

    @Test
    public void addMultiMetaTagViaMetaAnnotation_MetaSizeCorrect_ContentCorrect()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(MetaAnnotations.class));

        Document page = pageBuilder.getBootstrapPage(new BootstrapContext(
                request, null, session, testUI, this::contextRootRelativePath));

        Element head = page.head();
        Elements metas = head.getElementsByTag("meta");

        Assert.assertEquals(5, metas.size());
        Element meta = metas.get(0);
        assertEquals("Content-Type", meta.attr("http-equiv"));
        assertEquals("text/html; charset=utf-8", meta.attr("content"));

        meta = metas.get(1);
        assertEquals("X-UA-Compatible", meta.attr("http-equiv"));
        assertEquals("IE=edge", meta.attr("content"));

        meta = metas.get(2);
        assertEquals(BootstrapHandler.VIEWPORT, meta.attr("name"));
        assertEquals(Viewport.DEFAULT,
                meta.attr(BootstrapHandler.CONTENT_ATTRIBUTE));

        meta = metas.get(3);
        assertEquals("apple-mobile-web-app-status-bar-style",
                meta.attr("name"));
        assertEquals("black", meta.attr(BootstrapHandler.CONTENT_ATTRIBUTE));

        meta = metas.get(4);
        assertEquals("apple-mobile-web-app-capable", meta.attr("name"));
        assertEquals("yes", meta.attr(BootstrapHandler.CONTENT_ATTRIBUTE));
    }

    @Route("")
    @Tag(Tag.DIV)
    @Meta(name = "", content = "yes")
    public static class MetaAnnotationsContainsNull extends Component {
    }

    @Test(expected = IllegalStateException.class)
    public void AnnotationContainsNullValue_ExceptionThrown()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(MetaAnnotationsContainsNull.class));

        pageBuilder.getBootstrapPage(new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath));
    }

    @Tag(Tag.DIV)
    @Meta(name = "apple-mobile-web-app-capable", content = "yes")
    public static class MetaAnnotationsWithoutRoute extends Component {
    }

    @Test(expected = InvalidRouteConfigurationException.class)
    public void AnnotationsWithoutRoute_ExceptionThrown()
            throws InvalidRouteConfigurationException {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(MetaAnnotationsWithoutRoute.class));

        pageBuilder.getBootstrapPage(new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath));
    }

    @Test
    public void getBootstrapPage_jsModulesDoNotContainDeferAttribute()
            throws ServiceException {
        List<DependencyFilter> filters = Arrays.asList((list, context) -> {
            list.clear(); // remove everything
            return list;
        }, (list, context) -> {
            list.add(new Dependency(Dependency.Type.JS_MODULE, "//module.js",
                    LoadMode.EAGER));
            return list;
        });
        service.setDependencyFilters(filters);

        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath);
        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");
        scripts.forEach(s -> System.err.println(s.outerHtml()));

        Element element = scripts.stream()
                .filter(elem -> elem.attr("src").equals("//module.js"))
                .findFirst().get();
        Assert.assertFalse(element.hasAttr("defer"));

        Element bundle = scripts.stream()
                .filter(el -> el.attr("src")
                        .equals("./VAADIN/build/vaadin-bundle-1111.cache.js"))
                .findFirst().get();
        Assert.assertFalse(bundle.hasAttr("defer"));
    }

    @Test
    public void getBootstrapPage_removesExportScript() throws ServiceException {
        initUI(testUI);

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath);
        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");

        Assert.assertTrue(scripts.stream()
                .filter(el -> el.attr("src")
                        .equals("./VAADIN/build/vaadin-bundle-1111.cache.js"))
                .findFirst().isPresent());
        Assert.assertFalse(scripts.stream()
                .filter(el -> el.attr("src")
                        .equals("./VAADIN/build/vaadin-export-2222.cache.js"))
                .findFirst().isPresent());
    }

    @Test // #7158
    public void getBootstrapPage_assetChunksIsAnARRAY_bootstrapParsesOk()
            throws ServiceException, IOException {

        initUI(testUI);

        String statsJson = "{\n" + " \"errors\": [],\n" + " \"warnings\": [],\n"
                + " \"assetsByChunkName\": {\n" + "  \"bundle\": [\n"
                + "    \"VAADIN/build/vaadin-bundle-e77008557c8d410bf0dc" +
                ".cache.js\",\n"
                + "    \"VAADIN/build/vaadin-bundle-e77008557c8d410bf0dc" +
                ".cache.js.map\"\n"
                + "  ],\n" + " }" + "}";

        File tmpFile = tmpDir.newFile();
        try (FileOutputStream stream = new FileOutputStream(tmpFile)) {
            IOUtils.write(statsJson, stream, StandardCharsets.UTF_8);
        }

        Lookup lookup = testUI.getSession().getService().getContext()
                .getAttribute(Lookup.class);
        ResourceProvider provider = lookup.lookup(ResourceProvider.class);
        Mockito.when(provider.getApplicationResource(Mockito.anyString()))
                .thenReturn(tmpFile.toURI().toURL());

        BootstrapContext bootstrapContext = new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath);
        Document page = pageBuilder.getBootstrapPage(bootstrapContext);

        Elements scripts = page.head().getElementsByTag("script");

        Element bundle = scripts.stream().filter(el -> el.attr("src").equals(
                "./VAADIN/build/vaadin-bundle-e77008557c8d410bf0dc.cache.js"))
                .findFirst().get();
        Assert.assertFalse(bundle.hasAttr("defer"));
    }

    private void assertStringEquals(String message, String expected,
            String actual) {
        Assert.assertThat(message,
                actual.replaceAll(System.getProperty("line.separator"), "\n"),
                CoreMatchers.equalTo(expected));
    }

    private Element initTestUI() {
        TestUI anotherUI = new TestUI();
        initUI(testUI);
        anotherUI.getInternals().setSession(session);
        VaadinServletRequest vaadinRequest = createVaadinRequest();
        anotherUI.doInit(vaadinRequest, 0);
        anotherUI.getInternals().getRouter().initializeUI(anotherUI, request);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI, this::contextRootRelativePath);
        anotherUI.getInternals()
                .setContextRoot(contextRootRelativePath(vaadinRequest));
        return pageBuilder.getBootstrapPage(bootstrapContext).head();
    }

    private String contextRootRelativePath(VaadinRequest request) {
        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        Mockito.doCallRealMethod().when(service)
                .getContextRootRelativePath(Mockito.any());
        return service.getContextRootRelativePath(request);
    }

    private VaadinServletRequest createVaadinRequest() {
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

    @Test
    public void defaultViewport() {
        initUI(testUI);
        Document page = pageBuilder.getBootstrapPage(context);
        Element head = page.head();
        Elements viewports = head.getElementsByAttributeValue("name",
                BootstrapHandler.VIEWPORT);
        Assert.assertEquals(1, viewports.size());
        Element viewport = viewports.get(0);
        Assert.assertEquals(Viewport.DEFAULT,
                viewport.attr(BootstrapHandler.CONTENT_ATTRIBUTE));

    }

    @Viewport("viewport-annotation-value")
    @Tag("div")
    @Route("")
    public static class RouteWithViewport extends Component {

    }

    @Test
    public void viewportAnnotationOverridesDefault() throws Exception {
        initUI(testUI, createVaadinRequest(),
                Collections.singleton(RouteWithViewport.class));
        Document page = pageBuilder.getBootstrapPage(context);
        Element head = page.head();
        Elements viewports = head.getElementsByAttributeValue("name",
                BootstrapHandler.VIEWPORT);
        Assert.assertEquals(1, viewports.size());
        Element viewport = viewports.get(0);
        Assert.assertEquals("viewport-annotation-value",
                viewport.attr(BootstrapHandler.CONTENT_ATTRIBUTE));

    }

    @Test
    public void testUIConfiguration_usingPageSettings() throws Exception {
        Assert.assertTrue("By default loading indicator is themed", testUI
                .getLoadingIndicatorConfiguration().isApplyDefaultTheme());

        initUI(testUI, createVaadinRequest(),
                Collections.singleton(InitialPageConfiguratorRoute.class));
        pageBuilder.getBootstrapPage(new BootstrapContext(request, null,
                session, testUI, this::contextRootRelativePath));

        Assert.assertFalse("Default indicator theme is not themed anymore",
                testUI.getLoadingIndicatorConfiguration()
                        .isApplyDefaultTheme());

        Assert.assertEquals(InitialPageConfiguratorRoute.SECOND_DELAY,
                testUI.getLoadingIndicatorConfiguration().getSecondDelay());

        Assert.assertEquals(PushMode.MANUAL,
                testUI.getPushConfiguration().getPushMode());
    }

}

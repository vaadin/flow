package com.vaadin.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.nodes.TextNode;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.flow.dom.Prerenderer;
import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.server.BootstrapHandler.PreRenderMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.Html;
import com.vaadin.ui.Text;
import com.vaadin.ui.UI;

public class BootstrapHandlerTest {

    static final String UI_TITLE = "UI_TITLE";

    @Title(UI_TITLE)
    @JavaScript(value = "lazy.js", blocking = false)
    @StyleSheet(value = "lazy.css", blocking = false)
    @HtmlImport(value = "lazy.html", blocking = false)
    @JavaScript("blocking.js")
    @StyleSheet("context://blocking-relative.css")
    @StyleSheet("blocking.css")
    @HtmlImport("blocking.html")
    private class TestUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            super.init(request);
            add(new Html("<div foo=bar>foobar</div>"));
            add(new Text("Hello world"));
            add(new InlineTemplate("<div><script></script></div>"));
        }

    }

    public static class PrerenderInlineTemplate extends InlineTemplate {

        public PrerenderInlineTemplate() {
            super("<div><script></script></div>");
        }

        @Override
        protected Optional<Node> prerender() {
            return Prerenderer.prerenderElementTree(createMeterElement());
        }

    }

    public static class NoPrerenderComponent extends Text {

        public NoPrerenderComponent() {
            super("No prerender");
        }

        @Override
        protected Optional<Node> prerender() {
            return Optional.empty();
        }

    }

    public static class ComponentMappingTemplate extends InlineTemplate {

        @Id("replaced")
        private CompositeWithCustomPrerender prerender;

        public ComponentMappingTemplate() {
            super("<div><div id='replaced'>foobar<div></div>");
        }

    }

    private class PrerenderText extends Text {

        public PrerenderText(String text) {
            super(text);
        }

        @Override
        protected Optional<Node> prerender() {
            Optional<Node> prerenderElement = super.prerender();
            ((TextNode) prerenderElement.get()).text("FOOBAR");
            return prerenderElement;
        }
    }

    public static class CompositeWithCustomPrerender extends Composite<Html> {

        @Override
        protected Html initContent() {
            return new Html("<div foo=bar>foobar</div>") {
                @Override
                protected Optional<Node> prerender() {
                    return Prerenderer
                            .prerenderElementTree(createMeterElement());
                }
            };
        }

        @Override
        protected Optional<Node> prerender() {
            Optional<Node> prerenderElement = super.prerender();
            prerenderElement.get().attr("bar", "baz");
            return prerenderElement;
        }
    }

    @Tag("vaadin-grid")
    public static class WebComponent extends Component {

    }

    public static class TemplateWithWebComponent extends InlineTemplate {

        public TemplateWithWebComponent() {
            super("<div><span>Grid below</span><vaadin-grid foo=bar /></div>");
        }

    }

    @Tag("div")
    public static class CustomElementsComponent extends Component {
        public CustomElementsComponent() {
            getElement().appendChild(
                    new com.vaadin.flow.dom.Element("vaadin-grid"));
        }

    }

    public static class CustomElementPrerenderOverrideComponent
            extends CustomElementsComponent {
        @Override
        protected Optional<Node> prerender() {
            return Prerenderer.prerenderElementTree(getElement(), true, false);
        }
    }

    private TestUI testUI;
    private UI prerenderTestUI;
    private BootstrapContext context;
    private VaadinRequest request;
    private VaadinSession session;
    private VaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration;

    @Before
    public void setup() {
        BootstrapHandler.clientEngineFile = "foobar";
        testUI = new TestUI();
        prerenderTestUI = new UI();

        deploymentConfiguration = new MockDeploymentConfiguration();

        service = Mockito.spy(new MockVaadinServletService(new VaadinServlet(),
                deploymentConfiguration));

        session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(deploymentConfiguration);
        testUI.getInternals().setSession(session);
        prerenderTestUI.getInternals().setSession(session);
        initUI(prerenderTestUI);
    }

    private void initUI(UI ui) {
        initUI(ui, createVaadinRequest(null));
    }

    private void initUI(UI ui, VaadinRequest request) {
        this.request = request;
        try {
            service.init();
        } catch (ServiceException e) {
            throw new RuntimeException("Error initializing the VaadinService",
                    e);
        }
        ui.doInit(request, 0);
        context = new BootstrapContext(request, null, session, ui);
    }

    @Test
    public void testInitialPageTitle_pageSetTitle_noExecuteJavascript() {
        initUI(testUI, createVaadinRequest(null));
        String overriddenPageTitle = "overridden";
        testUI.getPage().setTitle(overriddenPageTitle);

        assertEquals(overriddenPageTitle,
                BootstrapHandler.resolvePageTitle(context).get());

        assertEquals(0, testUI.getInternals().dumpPendingJavaScriptInvocations()
                .size());
    }

    @Test
    public void testInitialPageTitle_nullTitle_noTitle() {
        initUI(testUI, createVaadinRequest(null));
        assertFalse(BootstrapHandler.resolvePageTitle(context).isPresent());
    }

    @Test
    public void prerenderMode() {
        Map<String, PreRenderMode> parameterToMode = new HashMap<>();
        parameterToMode.put("only", PreRenderMode.PRE_ONLY);
        parameterToMode.put("no", PreRenderMode.LIVE_ONLY);

        parameterToMode.put("", PreRenderMode.PRE_AND_LIVE);
        parameterToMode.put(null, PreRenderMode.PRE_AND_LIVE);
        parameterToMode.put("foobar", PreRenderMode.PRE_AND_LIVE);

        for (String parameter : parameterToMode.keySet()) {
            HttpServletRequest request = createRequest(parameter);
            BootstrapContext context = new BootstrapContext(
                    new VaadinServletRequest(request, null), null, null, null);
            assertEquals(parameterToMode.get(parameter),
                    context.getPreRenderMode());
        }
    }

    @Test
    public void prerenderOnlyNoUidlAndDoesNotStartApp() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        assertFalse(page.outerHtml().contains("uidl"));
        assertFalse(page.outerHtml().contains("initApplication"));
    }

    @Test
    public void prerenderOnlyNotification() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        assertTrue(page.outerHtml()
                .contains(BootstrapHandler.PRE_RENDER_INFO_TEXT));
    }

    @Test
    public void prerenderOnlyNotificationNotInProduction() throws Exception {
        deploymentConfiguration.setProductionMode(true);
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        assertFalse(page.outerHtml()
                .contains(BootstrapHandler.PRE_RENDER_INFO_TEXT));
    }

    @Test
    public void prerenderContainsHtml() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_ONLY));

        // Actual test
        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));
        Element body = page.body();

        Element div = (Element) body.childNode(0);
        TextNode textNode = (TextNode) body.childNode(1);
        assertEquals("bar", div.attr("foo"));
        assertEquals("foobar", div.text());
        assertEquals("Hello world", textNode.text());
    }

    @Test
    public void prerenderNoScriptTagsFromTemplate() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_AND_LIVE));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));
        Element body = page.body();

        Node div = body.childNode(2);
        assertEquals(0, div.childNodeSize());
    }

    @Test
    public void prerenderUiOverriddenPreRenderElement() throws IOException {
        TestUI testUI = new TestUI() {

            @Override
            protected Optional<Node> prerender() {
                com.vaadin.flow.dom.Element body = new com.vaadin.flow.dom.Element(
                        "body");
                body.appendChild(createMeterElement());
                return Prerenderer.prerenderElementTree(body);
            }
        };
        testUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest(
                PreRenderMode.PRE_AND_LIVE);
        testUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, testUI);

        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);
        Element body = page.body();

        Element meter = body.child(0);
        verifyMeterElement(meter);
    }

    @Test
    public void prerenderBasicComponent() throws IOException {
        prerenderTestUI.add(new Html("<div foo='bar'>foobar</div>"));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        assertEquals(2, body.childNodeSize());

        Element div = (Element) body.childNode(0);
        assertEquals("bar", div.attr("foo"));
        assertEquals("foobar", div.text());

    }

    @Test
    public void prerenderTextComponent() throws IOException {
        prerenderTestUI.add(new PrerenderText("Hello world"));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        assertEquals(2, body.childNodeSize());
        TextNode textNode = (TextNode) body.childNode(0);
        assertEquals("FOOBAR", textNode.text());
    }

    @Test
    public void prerenderInlineTemplate() throws IOException {
        prerenderTestUI.add(new PrerenderInlineTemplate());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        assertEquals(2, body.childNodeSize());
        Element overriddenTemplate = (Element) body.childNode(0);
        verifyMeterElement(overriddenTemplate);
    }

    @Test
    public void prerenderCompositeWithCustomPrerender() throws IOException {
        prerenderTestUI.add(new CompositeWithCustomPrerender());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        assertEquals(2, body.childNodeSize());
        Element overriddenComposite = (Element) body.childNode(0);
        verifyMeterElement(overriddenComposite);
        assertEquals("baz", overriddenComposite.attr("bar"));
    }

    @Test
    public void prerenderTemplateWithOverride() throws IOException {
        prerenderTestUI.add(new ComponentMappingTemplate());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        assertEquals(2, body.childNodeSize());
        // template with mapped composite that overrides pre-render
        Element templateRoot = (Element) body.childNode(0);
        Element anotherOverriddenComposite = (Element) templateRoot
                .childNode(0);
        verifyMeterElement(anotherOverriddenComposite);
        assertEquals("baz", anotherOverriddenComposite.attr("bar"));

    }

    @Test
    public void prerenderComponentWithNoPrerender() throws IOException {
        prerenderTestUI.add(new NoPrerenderComponent());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        // component not pre-render, only noscript
        assertEquals(1, body.childNodeSize());
        assertEquals("noscript", body.child(0).tagName());
    }

    @Test
    public void prerenderWebComponents() throws IOException {
        prerenderTestUI.add(new WebComponent());
        prerenderTestUI.add(new CustomElementsComponent());
        prerenderTestUI.add(new CustomElementPrerenderOverrideComponent());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();
        // web component excluded from pre-render, only noscript

        assertEquals(3, body.childNodeSize());
        // component that has a custom element inside
        Element customElements = (Element) body.childNode(0);
        assertEquals("div", customElements.tagName());
        assertEquals(0, customElements.childNodeSize());

        // component that has a custom element inside, but is filtered
        Element customElementPrerenderOverride = (Element) body.childNode(1);
        assertEquals("div", customElementPrerenderOverride.tagName());
        assertEquals(1, customElementPrerenderOverride.childNodeSize());
    }

    @Test
    public void prerenderTemplateWithWebComponent() throws IOException {
        prerenderTestUI.add(new TemplateWithWebComponent());

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, prerenderTestUI));
        Element body = page.body();

        Element div = body.child(0);
        assertEquals("div", div.tagName());
        assertEquals(1, div.childNodeSize());
        assertEquals("<span>Grid below</span>", div.html());
    }

    @Test
    public void noPrerenderUI() throws IOException {
        TestUI anotherUI = new TestUI() {
            @Override
            protected Optional<Node> prerender() {
                return Optional.empty();
            }
        };
        anotherUI.getInternals().setSession(session);
        VaadinRequest vaadinRequest = createVaadinRequest(
                PreRenderMode.PRE_AND_LIVE);
        anotherUI.doInit(vaadinRequest, 0);
        BootstrapContext bootstrapContext = new BootstrapContext(vaadinRequest,
                null, session, anotherUI);

        Document page = BootstrapHandler.getBootstrapPage(bootstrapContext);
        Element body = page.body();

        assertEquals(1, body.childNodeSize());
        assertEquals("noscript", body.child(0).tagName());
    }

    @Test
    public void withoutPrerenderDoesNotContainHtml() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.LIVE_ONLY));
        // Actual test
        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));
        Element body = page.body();
        assertEquals(1, body.children().size());
        assertEquals("noscript", body.children().get(0).tagName());
    }

    @Test
    public void checkPrerenderedDependencies() throws Exception {
        initUI(testUI, createVaadinRequest(null));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));
        Element head = page.head();

        assertCssElementPrerendered(head, "blocking.css");
        assertCssElementPrerendered(head, "./blocking-relative.css");
        assertJavaScriptElementPrerendered(head, "blocking.js");

        // For some reason, we don't prerender html now at all
        assertElementNotPrerendered(head, "blocking.html");

        assertElementNotPrerendered(head, "lazy.js");
        assertElementNotPrerendered(head, "lazy.css");
        assertElementNotPrerendered(head, "lazy.html");
    }

    @Test
    public void checkUidlDependencies() {
        initUI(testUI, createVaadinRequest(null));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Optional<String> dataOptional = page.head().getElementsByTag("script")
                .stream().filter(scriptTag -> !scriptTag.hasAttr("src"))
                .map(Element::data).filter(data -> data.contains("var uidl ="))
                .findAny();

        assertTrue("Expected to find uidl tag in the page",
                dataOptional.isPresent());

        String uidlData = dataOptional.get();
        assertFalse(uidlData.contains("blocking.css"));
        assertFalse(uidlData.contains("./blocking-relative.css"));
        assertFalse(uidlData.contains("blocking.js"));

        assertTrue(uidlData.contains("blocking.html"));
        assertTrue(uidlData.contains("lazy.js"));
        assertTrue(uidlData.contains("lazy.css"));
        assertTrue(uidlData.contains("lazy.html"));
    }

    @Test
    public void everyNonBlockingJavaScriptIsIncludedWithDeferAttribute() {
        initUI(testUI, createVaadinRequest(null));
        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Elements jsElements = page.getElementsByTag("script");
        Elements deferElements = page.getElementsByAttribute("defer");

        // Ignore polyfill that should be loaded immediately
        jsElements.removeIf(
                element -> element.attr("src").contains("es6-collections.js"));

        assertEquals(jsElements, deferElements);
    }

    @Test // #1134
    public void testBodyAfterHeadPrerender() throws Exception {
        initUI(testUI, createVaadinRequest(null));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Element body = page.head().nextElementSibling();

        assertEquals("body", body.tagName());
        assertEquals("html", body.parent().tagName());
        assertEquals(2, body.parent().childNodeSize());
    }

    @Test // #1134
    public void testBodyAfterHeadNotPrerender() throws Exception {
        initUI(testUI, createVaadinRequest(PreRenderMode.PRE_ONLY));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Element body = page.head().nextElementSibling();

        assertEquals("body", body.tagName());
        assertEquals("html", body.parent().tagName());
        assertEquals(2, body.parent().childNodeSize());
    }

    @Test
    public void testBootstrapListener() {
        List<BootstrapListener> listeners = new ArrayList<>(3);
        listeners.add(evt -> {
            evt.getDocument().head().getElementsByTag("script").remove();
        });
        listeners.add(evt -> {
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.1");
        });
        listeners.add(evt -> {
            evt.getDocument().head().appendElement("script").attr("src",
                    "testing.2");
        });

        Mockito.when(service.processBootstrapListeners(Mockito.anyList()))
                .thenReturn(listeners);

        initUI(testUI);

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, testUI));

        Elements scripts = page.head().getElementsByTag("script");
        assertEquals(2, scripts.size());
        assertEquals("testing.1", scripts.get(0).attr("src"));
        assertEquals("testing.2", scripts.get(1).attr("src"));

        Mockito.verify(service, Mockito.times(2))
                .processBootstrapListeners(Mockito.anyList());
    }

    private VaadinRequest createVaadinRequest(PreRenderMode mode) {
        HttpServletRequest request;
        if (mode == PreRenderMode.PRE_ONLY) {
            request = createRequest("only");
        } else if (mode == PreRenderMode.LIVE_ONLY) {
            request = createRequest("no");
        } else {
            request = createRequest("");
        }
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest(String preRenderParameter) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> preRenderParameter).when(request)
                .getParameter("prerender");
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
    }

    private static void verifyMeterElement(Element meter) {
        assertEquals("meter", meter.tagName());
        assertEquals("foo", meter.className());
        assertEquals("1000", meter.attr("max"));
        assertEquals("500", meter.attr("value"));
    }

    private static com.vaadin.flow.dom.Element createMeterElement() {
        com.vaadin.flow.dom.Element meter = new com.vaadin.flow.dom.Element(
                "meter");
        meter.getStyle().set("color", "black");
        meter.setAttribute("max", "1000");
        meter.setAttribute("value", "500");
        meter.getClassList().add("foo");
        return meter;
    }

    private void assertCssElementPrerendered(Element head, String url) {
        Elements cssLinks = head.getElementsByAttributeValue("href", url);
        assertEquals(1, cssLinks.size());
        Element linkElement = cssLinks.get(0);
        assertEquals("link", linkElement.tagName());
        assertEquals("text/css", linkElement.attr("type"));
        assertEquals(url, linkElement.attr("href"));
    }

    private void assertJavaScriptElementPrerendered(Element head, String url) {
        Elements jsLinks = head.getElementsByAttributeValue("src", url);
        assertEquals(1, jsLinks.size());
        Element linkElement = jsLinks.get(0);
        assertEquals("script", linkElement.tagName());
        assertEquals("text/javascript", linkElement.attr("type"));
        assertEquals(url, linkElement.attr("src"));
    }

    private void assertElementNotPrerendered(Element head, String url) {
        Stream.of("href", "src").forEach(attribute -> {
            Elements elements = head.getElementsByAttributeValue(attribute,
                    url);
            assertTrue(String.format(
                    "Expected not to have element with url %s for attribute %s",
                    url, attribute), elements.isEmpty());
        });
    }
}

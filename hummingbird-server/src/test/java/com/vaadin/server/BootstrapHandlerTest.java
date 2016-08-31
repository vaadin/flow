package com.vaadin.server;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Title;
import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.nodes.TextNode;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.hummingbird.template.InlineTemplate;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.server.BootstrapHandler.PreRenderMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.Html;
import com.vaadin.ui.Text;
import com.vaadin.ui.UI;

public class BootstrapHandlerTest {

    static final String UI_TITLE = "UI_TITLE";

    @Title(UI_TITLE)
    @StyleSheet("relative.css")
    @StyleSheet("context://context.css")
    @JavaScript("myjavascript.js")
    private class TestUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            super.init(request);
            add(new Html("<div foo=bar>foobar</div>"));
            add(new Text("Hello world"));
            add(new InlineTemplate("<div><script></script></div>"));
        }

    }

    private TestUI ui;
    private BootstrapContext context;
    private VaadinRequest request;
    private VaadinSession session;
    private VaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration;

    @Before
    public void setup() {
        BootstrapHandler.clientEngineFile = "foobar";
        ui = new TestUI();

        deploymentConfiguration = new MockDeploymentConfiguration();
        service = new VaadinServletService(new VaadinServlet(),
                deploymentConfiguration);
        session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(deploymentConfiguration);
        ui.getInternals().setSession(session);

    }

    private void initUI(VaadinRequest request) {
        this.request = request;
        ui.doInit(request, 0);
        context = new BootstrapContext(request, null, session, ui);
    }

    @Test
    public void testInitialPageTitle_pageSetTitle_noExecuteJavascript() {
        initUI(createVaadinRequest(null));
        String overriddenPageTitle = "overridden";
        ui.getPage().setTitle(overriddenPageTitle);

        Assert.assertEquals(overriddenPageTitle,
                BootstrapHandler.resolvePageTitle(context).get());

        Assert.assertEquals(0,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());
    }

    @Test
    public void testInitialPageTitle_nullTitle_noTitle() {
        initUI(createVaadinRequest(null));
        Assert.assertFalse(
                BootstrapHandler.resolvePageTitle(context).isPresent());
    }

    @Test
    public void prerenderMode() {
        Map<String, PreRenderMode> parametertoMode = new HashMap<>();
        parametertoMode.put("only", PreRenderMode.PRE_ONLY);
        parametertoMode.put("no", PreRenderMode.LIVE_ONLY);

        parametertoMode.put("", PreRenderMode.PRE_AND_LIVE);
        parametertoMode.put(null, PreRenderMode.PRE_AND_LIVE);
        parametertoMode.put("foobar", PreRenderMode.PRE_AND_LIVE);

        for (String parameter : parametertoMode.keySet()) {
            HttpServletRequest request = createRequest(parameter);
            BootstrapContext context = new BootstrapContext(
                    new VaadinServletRequest(request, null), null, null, null);
            Assert.assertEquals(parametertoMode.get(parameter),
                    context.getPreRenderMode());
        }
    }

    @Test
    public void prerenderOnlyNoUidlAndDoesNotStartApp() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        Assert.assertFalse(page.outerHtml().contains("uidl"));
        Assert.assertFalse(page.outerHtml().contains("initApplication"));
    }

    @Test
    public void prerenderOnlyNotification() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        Assert.assertTrue(page.outerHtml()
                .contains(BootstrapHandler.PRE_RENDER_INFO_TEXT));
    }

    @Test
    public void prerenderOnlyNotificationNotInProduction() throws Exception {
        deploymentConfiguration.setProductionMode(true);
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));
        Document page = BootstrapHandler.getBootstrapPage(context);
        Assert.assertFalse(page.outerHtml()
                .contains(BootstrapHandler.PRE_RENDER_INFO_TEXT));
    }

    @Test
    public void prerenderContainsHtml() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));

        // Actual test
        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
        Element body = page.body();

        Element div = (Element) body.childNode(0);
        TextNode textNode = (TextNode) body.childNode(1);
        Assert.assertEquals("bar", div.attr("foo"));
        Assert.assertEquals("foobar", div.text());
        Assert.assertEquals("Hello world", textNode.text());
    }

    @Test
    public void prerenderNoScriptTagsFromTemplate() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_AND_LIVE));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
        Element body = page.body();

        Node div = body.childNode(2);
        Assert.assertEquals(0, div.childNodeSize());
    }

    @Test
    public void withoutPrerenderDoesNotContainHtml() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.LIVE_ONLY));
        // Actual test
        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
        Element body = page.body();
        Assert.assertEquals(1, body.children().size());
        Assert.assertEquals("noscript", body.children().get(0).tagName());
    }

    @Test
    public void prerenderContainsStyleSheets() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
        Element head = page.head();

        Elements relativeCssLinks = head.getElementsByAttributeValue("href",
                "relative.css");
        Assert.assertEquals(1, relativeCssLinks.size());
        Element relativeLinkElement = relativeCssLinks.get(0);
        Assert.assertEquals("link", relativeLinkElement.tagName());
        Assert.assertEquals("text/css", relativeLinkElement.attr("type"));

        Elements contextCssLinks = head.getElementsByAttributeValue("href",
                "./context.css");
        Assert.assertEquals(1, contextCssLinks.size());
        Element contextLinkElement = contextCssLinks.get(0);
        Assert.assertEquals("link", contextLinkElement.tagName());
        Assert.assertEquals("text/css", contextLinkElement.attr("type"));
    }

    @Test
    public void styleSheetsNotInUidl() throws Exception {
        initUI(createVaadinRequest(null));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));

        Element uidlScriptTag = null;
        for (Element scriptTag : page.head().getElementsByTag("script")) {
            if (scriptTag.hasAttr("src")) {
                continue;
            }

            uidlScriptTag = scriptTag;

            break;
        }
        Assert.assertNotNull(uidlScriptTag);

        String uidlData = uidlScriptTag.data();
        Assert.assertTrue(uidlData.contains("var uidl ="));
        Assert.assertTrue(uidlData.contains("myjavascript.js"));
        Assert.assertFalse(uidlData.contains("context.css"));
        Assert.assertFalse(uidlData.contains("relative.css"));

    }

    @Test // #1134
    public void testBodyAfterHeadPrerender() throws Exception {
        initUI(createVaadinRequest(null));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));

        Element body = page.head().nextElementSibling();

        Assert.assertEquals("body", body.tagName());
        Assert.assertEquals("html", body.parent().tagName());
        Assert.assertEquals(2, body.parent().childNodeSize());
    }

    @Test // #1134
    public void testBodyAfterHeadNotPrerender() throws Exception {
        initUI(createVaadinRequest(PreRenderMode.PRE_ONLY));

        Document page = BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));

        Element body = page.head().nextElementSibling();

        Assert.assertEquals("body", body.tagName());
        Assert.assertEquals("html", body.parent().tagName());
        Assert.assertEquals(2, body.parent().childNodeSize());
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
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return preRenderParameter;
            }
        }).when(request).getParameter("prerender");
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "";
            }
        }).when(request).getServletPath();
        return request;
    }
}

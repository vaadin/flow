package com.vaadin.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.UI;

public class BootstrapHandlerDependenciesTest {

    @JavaScript(value = "lazy.js", blocking = false)
    @StyleSheet(value = "lazy.css", blocking = false)
    @HtmlImport(value = "lazy.html", blocking = false)
    @JavaScript("blocking.js")
    @StyleSheet("context://blocking-relative.css")
    @StyleSheet("blocking.css")
    @HtmlImport("blocking.html")
    private static class UIAnnotated_BlockingOrderTest extends UI {
    }

    private static class UIWithMethods_BlockingOrderTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("lazy.js", false);
            getPage().addStyleSheet("lazy.css", false);
            getPage().addHtmlImport("lazy.html", false);
            getPage().addJavaScript("blocking.js");
            getPage().addStyleSheet("context://blocking-relative.css");
            getPage().addStyleSheet("blocking.css");
            getPage().addHtmlImport("blocking.html");
        }
    }

    @JavaScript(value = "new.js", blocking = false)
    @JavaScript(value = "new.js")
    private static class UIAnnotated_BothBlockingAndNotTest extends UI {
    }

    private static class UIWithMethods_BothBlockingAndNotTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("new.js", false);
            getPage().addJavaScript("new.js");
        }
    }

    @JavaScript("1.js")
    @JavaScript("2.js")
    @StyleSheet("1.css")
    @StyleSheet("2.css")
    @HtmlImport("1.html")
    @HtmlImport("2.html")
    private static class UIAnnotated_ImportOrderTest_Blocking extends UI {
    }

    @JavaScript(value = "1.js", blocking = false)
    @JavaScript(value = "2.js", blocking = false)
    @StyleSheet(value = "1.css", blocking = false)
    @StyleSheet(value = "2.css", blocking = false)
    @HtmlImport(value = "1.html", blocking = false)
    @HtmlImport(value = "2.html", blocking = false)
    private static class UIAnnotated_ImportOrderTest_NonBlocking extends UI {
    }

    private static class UIWithMethods_ImportOrderTest_Blocking extends UI {
        @Override
        public void init(VaadinRequest request) {
            getPage().addJavaScript("1.js");
            getPage().addJavaScript("2.js");
            getPage().addStyleSheet("1.css");
            getPage().addStyleSheet("2.css");
            getPage().addHtmlImport("1.html");
            getPage().addHtmlImport("2.html");
        }
    }

    private static class UIWithMethods_ImportOrderTest_NonBlocking extends UI {
        @Override
        public void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", false);
            getPage().addJavaScript("2.js", false);
            getPage().addStyleSheet("1.css", false);
            getPage().addStyleSheet("2.css", false);
            getPage().addHtmlImport("1.html", false);
            getPage().addHtmlImport("2.html", false);
        }
    }

    @JavaScript(value = "1.js", blocking = false)
    @JavaScript(value = "2.js", blocking = false)
    @JavaScript(value = "1.js", blocking = false)
    private static class UIAnnotated_DuplicateDependencies_NonBlocking
            extends UI {
    }

    private static class UIWithMethods_DuplicateDependencies_NonBlocking
            extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", false);
            getPage().addJavaScript("2.js", false);
            getPage().addJavaScript("1.js", false);
        }
    }

    @JavaScript(value = "1.js")
    @JavaScript(value = "2.js")
    @JavaScript(value = "1.js")
    private static class UIAnnotated_DuplicateDependencies_Blocking extends UI {
    }

    private static class UIWithMethods_DuplicateDependencies_Blocking
            extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("1.js");
            getPage().addJavaScript("2.js");
            getPage().addJavaScript("1.js");
        }
    }

    private VaadinSession session;
    private VaadinServletService service;

    @Before
    public void setup() {
        BootstrapHandler.clientEngineFile = "foobar";

        DeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();

        service = Mockito.spy(new MockVaadinServletService(new VaadinServlet(),
                deploymentConfiguration));

        session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(deploymentConfiguration);
    }

    @Test
    public void testUiWithSameDependencyInDifferentModes() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String newDependencyUrl = "new.js";

            Elements jsElements = page.getElementsByAttributeValue("src",
                    newDependencyUrl);
            assertEquals(
                    "Should be only one new dependency in the dependencies list",
                    1, jsElements.size());

            String uidlData = extractUidlData(page);
            assertFalse("New dependency should not be loaded via uidl",
                    uidlData.contains(newDependencyUrl));
        };
        testUis(uiPageTestingMethod, new UIAnnotated_BothBlockingAndNotTest(),
                new UIWithMethods_BothBlockingAndNotTest());

    }

    @Test
    public void checkPrerenderedDependencies() throws Exception {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            assertCssElementPrerendered(head, "blocking.css");
            assertCssElementPrerendered(head, "./blocking-relative.css");
            assertJavaScriptElementPrerendered(head, "blocking.js");

            // For some reason, we don't prerender html now at all
            assertElementNotPrerendered(head, "blocking.html");

            assertElementNotPrerendered(head, "lazy.js");
            assertElementNotPrerendered(head, "lazy.css");
            assertElementNotPrerendered(head, "lazy.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_BlockingOrderTest(),
                new UIWithMethods_BlockingOrderTest());
    }

    @Test
    public void checkUidlDependencies() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertFalse(uidlData.contains("blocking.css"));
            assertFalse(uidlData.contains("./blocking-relative.css"));
            assertFalse(uidlData.contains("blocking.js"));

            assertTrue(uidlData.contains("blocking.html"));
            assertTrue(uidlData.contains("lazy.js"));
            assertTrue(uidlData.contains("lazy.css"));
            assertTrue(uidlData.contains("lazy.html"));
        };
        testUis(uiPageTestingMethod, new UIAnnotated_BlockingOrderTest(),
                new UIWithMethods_BlockingOrderTest());
    }

    @Test
    public void everyNonBlockingJavaScriptIsIncludedWithDeferAttribute() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Elements jsElements = page.getElementsByTag("script");
            Elements deferElements = page.getElementsByAttribute("defer");

            // Ignore polyfill that should be loaded immediately
            jsElements.removeIf(element -> element.attr("src")
                    .contains("es6-collections.js"));

            assertEquals(
                    "Expected to have all script elements with defer attribute",
                    jsElements, deferElements);
        };
        testUis(uiPageTestingMethod, new UIAnnotated_BlockingOrderTest(),
                new UIWithMethods_BlockingOrderTest(),
                new UIAnnotated_ImportOrderTest_NonBlocking(),
                new UIWithMethods_ImportOrderTest_NonBlocking());
    }

    @Test
    public void blockingDependenciesAreImportedInConsequentOrder() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportUrls = head.getElementsByTag("script").stream()
                    .map(element -> element.attr("src"))
                    .collect(Collectors.toList());
            assertUrlOrder(jsImportUrls, "1.js", "2.js");

            List<String> cssImportUrls = head.getElementsByTag("link").stream()
                    .map(element -> element.attr("href"))
                    .collect(Collectors.toList());
            assertUrlOrder(cssImportUrls, "1.css", "2.css");

            // We don't include html imports in prerender
            String uidlData = extractUidlData(page);
            assertDependenciesOrderInUidl(uidlData, "1.html", "2.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_ImportOrderTest_Blocking(),
                new UIWithMethods_ImportOrderTest_Blocking());
    }

    @Test
    public void nonBlockingDependenciesAreImportedInConsequentOrder() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertDependenciesOrderInUidl(uidlData, "1.js", "2.js");
            assertDependenciesOrderInUidl(uidlData, "1.css", "2.css");
            assertDependenciesOrderInUidl(uidlData, "1.html", "2.html");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_ImportOrderTest_NonBlocking(),
                new UIWithMethods_ImportOrderTest_NonBlocking());
    }

    @Test
    public void duplicateDependenciesAreDiscarded_Blocking() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportUrls = head.getElementsByTag("script").stream()
                    .map(element -> element.attr("src"))
                    .collect(Collectors.toList());
            assertUrlOrder(jsImportUrls, "1.js", "2.js");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_DuplicateDependencies_Blocking(),
                new UIWithMethods_DuplicateDependencies_Blocking());
    }

    @Test
    public void duplicateDependenciesAreDiscarded_NonBlocking() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertDependenciesOrderInUidl(uidlData, "1.js", "2.js");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_DuplicateDependencies_NonBlocking(),
                new UIWithMethods_DuplicateDependencies_NonBlocking());
    }

    private void testUis(Consumer<Document> uiPageTestingMethod,
            UI... uisToTest) {
        assertTrue("Got no uis to test", uisToTest.length > 0);
        Stream.of(uisToTest).forEach(
                ui -> uiPageTestingMethod.accept(initUIAndGetPage(ui)));
    }

    private Document initUIAndGetPage(UI ui) {
        ui.getInternals().setSession(session);
        VaadinRequest request = new VaadinServletRequest(createRequest(""),
                service);
        try {
            service.init();
        } catch (ServiceException e) {
            throw new RuntimeException("Error initializing the VaadinService",
                    e);
        }
        ui.doInit(request, 0);
        return BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
    }

    private HttpServletRequest createRequest(String preRenderParameter) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> preRenderParameter).when(request)
                .getParameter("prerender");
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
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

    private String extractUidlData(Document page) {
        Optional<String> dataOptional = page.head().getElementsByTag("script")
                .stream().filter(scriptTag -> !scriptTag.hasAttr("src"))
                .map(Element::data).filter(data -> data.contains("var uidl ="))
                .findAny();

        assertTrue("Expected to find uidl tag in the page",
                dataOptional.isPresent());
        return dataOptional.get();
    }

    private void assertUrlOrder(List<String> allUrls, String firstUrl,
            String secondUrl) {
        int firstPosition = -1;
        int secondPosition = -1;
        for (int i = 0; i < allUrls.size(); i++) {
            String url = allUrls.get(i);
            if (firstUrl.equals(url)) {
                firstPosition = i;
            } else if (secondUrl.equals(url)) {
                secondPosition = i;
            }

            if (firstPosition > 0 && secondPosition > 0) {
                break;
            }
        }

        assertCorrectDependencyPositions(firstUrl, secondUrl, firstPosition,
                secondPosition);
    }

    private void assertDependenciesOrderInUidl(String uidlData,
            String firstDependencyUrl, String secondDependencyUrl) {
        int firstPosition = uidlData.indexOf(firstDependencyUrl);
        int secondPosition = uidlData.indexOf(secondDependencyUrl);
        assertCorrectDependencyPositions(firstDependencyUrl,
                secondDependencyUrl, firstPosition, secondPosition);
    }

    private void assertCorrectDependencyPositions(String firstDependencyUrl,
            String secondDependencyUrl, int firstPosition, int secondPosition) {
        assertNotEquals(String.format("Could not find url %s in uidl",
                firstDependencyUrl), -1, firstPosition);
        assertNotEquals(String.format("Could not find url %s in uidl",
                secondDependencyUrl), -1, secondPosition);

        assertTrue(
                String.format(
                        "Expected url %s to be contained before url %s in the uidl, because it is expected to be first to be imported",
                        firstDependencyUrl, secondDependencyUrl),
                firstPosition < secondPosition);
    }
}

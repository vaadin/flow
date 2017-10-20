package com.vaadin.server;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BootstrapHandlerDependenciesTest {
    private static final String BOOTSTRAP_SCRIPT_CONTENTS = "//<![CDATA[\n";

    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "lazy.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "lazy.css", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "lazy.html", loadMode = LoadMode.LAZY)
    @JavaScript(value = "inline.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "inline.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = "inline.html", loadMode = LoadMode.INLINE)
    @JavaScript("eager.js")
    @StyleSheet("context://eager-relative.css")
    @StyleSheet("eager.css")
    @HtmlImport("eager.html")
    private static class UIAnnotated_LoadingOrderTest extends UI {
    }

    private static class UIWithMethods_LoadingOrderTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("lazy.js", LoadMode.LAZY);
            getPage().addStyleSheet("lazy.css", LoadMode.LAZY);
            getPage().addHtmlImport("lazy.html", LoadMode.LAZY);
            getPage().addJavaScript("inline.js", LoadMode.INLINE);
            getPage().addStyleSheet("inline.css", LoadMode.INLINE);
            getPage().addHtmlImport("inline.html", LoadMode.INLINE);
            getPage().addJavaScript("eager.js");
            getPage().addStyleSheet("context://eager-relative.css");
            getPage().addStyleSheet("eager.css");
            getPage().addHtmlImport("eager.html");
        }
    }

    @JavaScript(value = "new.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "new.js")
    private static class UIAnnotated_BothLazyAndEagerTest extends UI {
    }

    private static class UIWithMethods_BothBothLazyAndEagerTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("new.js", LoadMode.LAZY);
            getPage().addJavaScript("new.js");
        }
    }

    @JavaScript(value = "new.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "new.js", loadMode = LoadMode.INLINE)
    private static class UIAnnotated_BothLazyAndInlineTest extends UI {
    }

    private static class UIWithMethods_BothBothLazyAndInlineTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("new.js", LoadMode.LAZY);
            getPage().addJavaScript("new.js", LoadMode.INLINE);
        }
    }

    @JavaScript(value = "new.js", loadMode = LoadMode.INLINE)
    @JavaScript(value = "new.js")
    private static class UIAnnotated_BothInlineAndEagerTest extends UI {
    }

    private static class UIWithMethods_BothBothInlineAndEagerTest extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("new.js", LoadMode.INLINE);
            getPage().addJavaScript("new.js");
        }
    }

    @JavaScript("1.js")
    @JavaScript("2.js")
    @StyleSheet("1.css")
    @StyleSheet("2.css")
    @HtmlImport("1.html")
    @HtmlImport("2.html")
    private static class UIAnnotated_ImportOrderTest_Eager extends UI {
    }

    @JavaScript(value = "1.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "2.js", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "1.css", loadMode = LoadMode.LAZY)
    @StyleSheet(value = "2.css", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "1.html", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "2.html", loadMode = LoadMode.LAZY)
    private static class UIAnnotated_ImportOrderTest_Lazy extends UI {
    }

    @JavaScript(value = "1.js", loadMode = LoadMode.INLINE)
    @JavaScript(value = "2.js", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "1.css", loadMode = LoadMode.INLINE)
    @StyleSheet(value = "2.css", loadMode = LoadMode.INLINE)
    @HtmlImport(value = "1.html", loadMode = LoadMode.INLINE)
    @HtmlImport(value = "2.html", loadMode = LoadMode.INLINE)
    private static class UIAnnotated_ImportOrderTest_Inline extends UI {
    }

    private static class UIWithMethods_ImportOrderTest_Eager extends UI {
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

    private static class UIWithMethods_ImportOrderTest_Lazy extends UI {
        @Override
        public void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", LoadMode.LAZY);
            getPage().addJavaScript("2.js", LoadMode.LAZY);
            getPage().addStyleSheet("1.css", LoadMode.LAZY);
            getPage().addStyleSheet("2.css", LoadMode.LAZY);
            getPage().addHtmlImport("1.html", LoadMode.LAZY);
            getPage().addHtmlImport("2.html", LoadMode.LAZY);
        }
    }

    private static class UIWithMethods_ImportOrderTest_Inline extends UI {
        @Override
        public void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", LoadMode.INLINE);
            getPage().addJavaScript("2.js", LoadMode.INLINE);
            getPage().addStyleSheet("1.css", LoadMode.INLINE);
            getPage().addStyleSheet("2.css", LoadMode.INLINE);
            getPage().addHtmlImport("1.html", LoadMode.INLINE);
            getPage().addHtmlImport("2.html", LoadMode.INLINE);
        }
    }

    @JavaScript(value = "1.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "2.js", loadMode = LoadMode.LAZY)
    @JavaScript(value = "1.js", loadMode = LoadMode.LAZY)
    private static class UIAnnotated_DuplicateDependencies_Lazy extends UI {
    }

    private static class UIWithMethods_DuplicateDependencies_Lazy extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", LoadMode.LAZY);
            getPage().addJavaScript("2.js", LoadMode.LAZY);
            getPage().addJavaScript("1.js", LoadMode.LAZY);
        }
    }

    @JavaScript(value = "1.js", loadMode = LoadMode.INLINE)
    @JavaScript(value = "2.js", loadMode = LoadMode.INLINE)
    @JavaScript(value = "1.js", loadMode = LoadMode.INLINE)
    private static class UIAnnotated_DuplicateDependencies_Inline extends UI {
    }

    private static class UIWithMethods_DuplicateDependencies_Inline extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("1.js", LoadMode.INLINE);
            getPage().addJavaScript("2.js", LoadMode.INLINE);
            getPage().addJavaScript("1.js", LoadMode.INLINE);
        }
    }

    @JavaScript("1.js")
    @JavaScript("2.js")
    @JavaScript("1.js")
    private static class UIAnnotated_DuplicateDependencies_Eager extends UI {
    }

    private static class UIWithMethods_DuplicateDependencies_Eager extends UI {
        @Override
        protected void init(VaadinRequest request) {
            getPage().addJavaScript("1.js");
            getPage().addJavaScript("2.js");
            getPage().addJavaScript("1.js");
        }
    }

    private VaadinSession session;
    private MockVaadinServletService service;

    @Before
    public void setup() {
        BootstrapHandler.clientEngineFile = "foobar";

        DeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();

        service = Mockito
                .spy(new MockVaadinServletService(deploymentConfiguration));

        ServletContext servletContextMock = mock(ServletContext.class);
        when(servletContextMock.getResourceAsStream(anyString()))
                .thenAnswer(invocation -> new ByteArrayInputStream(
                        ((String) invocation.getArguments()[0]).getBytes()));

        HttpServletRequest servletRequestMock = mock(HttpServletRequest.class);
        when(servletRequestMock.getServletContext())
                .thenReturn(servletContextMock);

        VaadinServletRequest vaadinRequestMock = mock(
                VaadinServletRequest.class);
        when(vaadinRequestMock.getHttpServletRequest())
                .thenReturn(servletRequestMock);

        service.setCurrentInstances(vaadinRequestMock,
                mock(VaadinResponse.class));

        session = new MockVaadinSession(service);
        session.lock();
        session.setConfiguration(deploymentConfiguration);

        VaadinUriResolverFactory factory = Mockito
                .mock(VaadinUriResolverFactory.class);
        Mockito.doAnswer(invocation -> invocation.getArguments()[1])
                .when(factory)
                .toServletContextPath(Mockito.any(), Mockito.anyString());

        session.setAttribute(VaadinUriResolverFactory.class, factory);

        VaadinSession.setCurrent(session);
    }

    @After
    public void tearDown() {
        session.unlock();
        VaadinService.setCurrent(null);
    }

    @Test
    public void testUiWithSameDependencyInDifferentModes() {
        Stream.of(new UIAnnotated_BothLazyAndEagerTest(),
                new UIWithMethods_BothBothLazyAndEagerTest(),
                new UIAnnotated_BothInlineAndEagerTest(),
                new UIWithMethods_BothBothInlineAndEagerTest(),
                new UIAnnotated_BothLazyAndInlineTest(),
                new UIWithMethods_BothBothLazyAndInlineTest())
                .forEach(this::checkUiWithException);
    }

    private void checkUiWithException(UI ui) {
        boolean exceptionCaught = false;
        try {
            testUis(doc -> {
            }, ui);
        } catch (IllegalStateException expected) {
            exceptionCaught = true;
        } finally {
            assertThat(
                    "The exception was expected, but not thrown for ui "
                            + ui.getClass().getCanonicalName(),
                    exceptionCaught, is(true));
        }
    }

    @Test
    public void checkDependenciesPresence() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            assertCssElementLoadedEagerly(head, "./frontend/eager.css");
            assertCssElementLoadedEagerly(head, "./eager-relative.css");
            assertJavaScriptElementLoadedEagerly(head, "./frontend/eager.js");
            assertHtmlElementLoadedEagerly(head, "./frontend/eager.html");

            assertCssElementInlined(head, "frontend://inline.css");
            assertJavaScriptElementInlined(head, "frontend://inline.js");
            assertHtmlElementInlined(page.body(), "frontend://inline.html");

            assertElementLazyLoaded(head, "./lazy.js");
            assertElementLazyLoaded(head, "./lazy.css");
            assertElementLazyLoaded(head, "./lazy.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_LoadingOrderTest(),
                new UIWithMethods_LoadingOrderTest());
    }

    @Test
    public void checkUidlDependencies() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertFalse(uidlData.contains("eager.css"));
            assertFalse(uidlData.contains("./eager-relative.css"));
            assertFalse(uidlData.contains("eager.js"));

            assertFalse(uidlData.contains("inline.js"));
            assertFalse(uidlData.contains("inline.css"));
            assertFalse(uidlData.contains("inline.html"));

            assertTrue(uidlData.contains("lazy.js"));
            assertTrue(uidlData.contains("lazy.css"));
            assertTrue(uidlData.contains("lazy.html"));
        };
        testUis(uiPageTestingMethod, new UIAnnotated_LoadingOrderTest(),
                new UIWithMethods_LoadingOrderTest());
    }

    @Test
    public void everyLazyJavaScriptIsIncludedWithDeferAttribute() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Elements jsElements = page.getElementsByTag("script");
            Elements deferElements = page.getElementsByAttribute("defer");

            // Ignore polyfills that should be loaded immediately and scripts
            // without src (separate test)
            jsElements.removeIf(element -> {
                String jsUrl = element.attr("src");
                return jsUrl.isEmpty() || jsUrl.contains("es6-collections.js")
                        || jsUrl.contains("webcomponents-lite.js");
            });

            assertEquals(
                    "Expected to have all script elements with defer attribute",
                    jsElements, deferElements);
        };
        testUis(uiPageTestingMethod, new UIAnnotated_LoadingOrderTest(),
                new UIWithMethods_LoadingOrderTest(),
                new UIAnnotated_ImportOrderTest_Lazy(),
                new UIWithMethods_ImportOrderTest_Lazy());
    }

    @Test
    public void eagerDependenciesAreImportedInConsequentOrder() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportUrls = head.getElementsByTag("script").stream()
                    .map(element -> element.attr("src"))
                    .collect(Collectors.toList());
            assertImportOrder(jsImportUrls, "1.js", "2.js");

            List<String> cssImportUrls = head.getElementsByTag("link").stream()
                    .filter(element -> "stylesheet".equals(element.attr("rel")))
                    .map(element -> element.attr("href"))
                    .collect(Collectors.toList());
            assertImportOrder(cssImportUrls, "1.css", "2.css");

            List<String> htmlImportUrls = head.getElementsByTag("link").stream()
                    .filter(element -> "import".equals(element.attr("rel")))
                    .map(element -> element.attr("href"))
                    .collect(Collectors.toList());
            assertImportOrder(htmlImportUrls, "1.html", "2.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_ImportOrderTest_Eager(),
                new UIWithMethods_ImportOrderTest_Eager());
    }

    @Test
    public void lazyDependenciesAreImportedInConsequentOrder() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertDependenciesOrderInUidl(uidlData, "1.js", "2.js");
            assertDependenciesOrderInUidl(uidlData, "1.css", "2.css");
            assertDependenciesOrderInUidl(uidlData, "1.html", "2.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_ImportOrderTest_Lazy(),
                new UIWithMethods_ImportOrderTest_Lazy());
    }

    @Test
    public void inlineDependenciesAreImportedInConsequentOrder() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportContents = head.getElementsByTag("script")
                    .stream().filter(element -> !element.hasAttr("src"))
                    .filter(element -> !element.toString()
                            .contains(BOOTSTRAP_SCRIPT_CONTENTS))
                    .map(Element::toString).collect(Collectors.toList());
            assertImportOrder(jsImportContents, "1.js", "2.js");

            List<String> cssImportContents = head.getElementsByTag("style")
                    .stream().map(Element::toString)
                    .collect(Collectors.toList());
            assertImportOrder(cssImportContents, "1.css", "2.css");

            List<String> htmlImportContents = page.body()
                    .getElementsByTag("span").stream()
                    .filter(element -> element.hasAttr("hidden"))
                    .map(Element::toString).collect(Collectors.toList());
            assertImportOrder(htmlImportContents, "1.html", "2.html");
        };
        testUis(uiPageTestingMethod, new UIAnnotated_ImportOrderTest_Inline(),
                new UIWithMethods_ImportOrderTest_Inline());
    }

    @Test
    public void duplicateDependenciesAreDiscarded_Eager() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportUrls = head.getElementsByTag("script").stream()
                    .map(element -> element.attr("src"))
                    .collect(Collectors.toList());
            assertImportOrder(jsImportUrls, "1.js", "2.js");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_DuplicateDependencies_Eager(),
                new UIWithMethods_DuplicateDependencies_Eager());
    }

    @Test
    public void duplicateDependenciesAreDiscarded_Lazy() {
        Consumer<Document> uiPageTestingMethod = page -> {
            String uidlData = extractUidlData(page);
            assertDependenciesOrderInUidl(uidlData, "1.js", "2.js");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_DuplicateDependencies_Lazy(),
                new UIWithMethods_DuplicateDependencies_Lazy());
    }

    @Test
    public void duplicateDependenciesAreDiscarded_Inline() {
        Consumer<Document> uiPageTestingMethod = page -> {
            Element head = page.head();

            List<String> jsImportContents = head.getElementsByTag("script")
                    .stream().filter(element -> !element.hasAttr("src"))
                    .filter(element -> !element.toString()
                            .contains(BOOTSTRAP_SCRIPT_CONTENTS))
                    .map(Element::toString).collect(Collectors.toList());
            assertImportOrder(jsImportContents, "1.js", "2.js");
        };
        testUis(uiPageTestingMethod,
                new UIAnnotated_DuplicateDependencies_Inline(),
                new UIWithMethods_DuplicateDependencies_Inline());
    }

    @Test
    public void flowDependenciesShouldBeImportedBeforeUserDependenciesWithCorrectAttributes() {
        Consumer<Document> uiPageTestingMethod = page -> {
            boolean foundClientEngine = false;
            int flowDependencyMaxIndex = Integer.MAX_VALUE;
            int userDependencyMinIndex = Integer.MAX_VALUE;

            Elements children = page.head().children();
            for (int i = 0; i < children.size(); i++) {
                Element element = children.get(i);
                String elementString = element.toString();
                if (foundClientEngine) {
                    if (userDependencyMinIndex > i) {
                        userDependencyMinIndex = i;
                    }
                    assertThat(
                            "Expected to have here dependencies added with Flow public api",
                            elementString,
                            either(containsString("eager"))
                                    .or(containsString("lazy"))
                                    .or(containsString("inline")));
                } else {
                    flowDependencyMaxIndex = i;
                    // skip element with uidl that contains lazy dependencies
                    if (!elementString.contains(BOOTSTRAP_SCRIPT_CONTENTS)) {
                        assertThat(
                                "Flow dependencies should not contain user dependencies",
                                elementString,
                                both(not(containsString("eager")))
                                        .and(not(containsString("lazy")))
                                        .and(not(containsString("inline"))));
                        if (elementString
                                .contains(BootstrapHandler.clientEngineFile)) {
                            foundClientEngine = true;
                        }
                    } else {
                        assertThat(
                                "uidl should not contain eager and inline dependencies",
                                elementString,
                                both(not(containsString("eager")))
                                        .and(not(containsString("inline"))));
                    }
                }

                assertThat(
                        String.format(
                                "All javascript dependencies should be loaded without 'async' attribute. Dependency with url %s has this attribute",
                                element.attr("src")),
                        element.attr("async"), is(""));
            }

            assertThat(
                    "Flow dependencies should be imported before user dependencies",
                    flowDependencyMaxIndex,
                    is(lessThan(userDependencyMinIndex)));

        };

        testUis(uiPageTestingMethod, new UIAnnotated_LoadingOrderTest(),
                new UIWithMethods_LoadingOrderTest());
    }

    @Test
    public void checkThatJsImportsWithoutSrcHaveNoDeferAttribute() {
        Consumer<Document> uiPageTestingMethod = page -> {
            List<Element> scriptsWithNoSrc = page.getElementsByTag("script")
                    .stream().filter(jsElement -> !jsElement.hasAttr("src"))
                    .collect(Collectors.toList());

            for (Element element : scriptsWithNoSrc) {
                String deferAttributeValue = element.attr("defer");
                // https://developer.mozilla.org/en/docs/Web/HTML/Element/script
                assertThat("Scripts without src should not be loaded as defer",
                        deferAttributeValue, either(is("")).or(is("false")));
            }
        };

        testUis(uiPageTestingMethod, new UIAnnotated_LoadingOrderTest(),
                new UIWithMethods_LoadingOrderTest(),
                new UIAnnotated_ImportOrderTest_Lazy(),
                new UIWithMethods_ImportOrderTest_Lazy(),
                new UIAnnotated_ImportOrderTest_Inline(),
                new UIWithMethods_ImportOrderTest_Inline());
    }

    private void testUis(Consumer<Document> uiPageTestingMethod,
            UI... uisToTest) {
        assertTrue("Got no uis to test", uisToTest.length > 0);
        Stream.of(uisToTest).forEach(
                ui -> uiPageTestingMethod.accept(initUIAndGetPage(ui)));
    }

    private Document initUIAndGetPage(UI ui) {
        ui.getInternals().setSession(session);
        VaadinRequest request = new VaadinServletRequest(createRequest(),
                service);
        service.init();
        ui.doInit(request, 0);
        return BootstrapHandler.getBootstrapPage(
                new BootstrapContext(request, null, session, ui));
    }

    private HttpServletRequest createRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
    }

    private void assertCssElementLoadedEagerly(Element head, String url) {
        Elements cssLinks = head.getElementsByAttributeValue("href", url);
        assertEquals(1, cssLinks.size());
        Element linkElement = cssLinks.get(0);
        assertEquals("link", linkElement.tagName());
        assertEquals("text/css", linkElement.attr("type"));
        assertEquals(url, linkElement.attr("href"));
    }

    private void assertJavaScriptElementLoadedEagerly(Element head,
            String url) {
        Elements jsLinks = head.getElementsByAttributeValue("src", url);
        assertEquals(1, jsLinks.size());
        Element linkElement = jsLinks.get(0);
        assertEquals("script", linkElement.tagName());
        assertEquals("text/javascript", linkElement.attr("type"));
        assertEquals(url, linkElement.attr("src"));
    }

    private void assertHtmlElementLoadedEagerly(Element head, String url) {
        Elements cssLinks = head.getElementsByAttributeValue("href", url);
        assertEquals(1, cssLinks.size());
        Element linkElement = cssLinks.get(0);
        assertEquals("link", linkElement.tagName());
        assertEquals("import", linkElement.attr("rel"));
        assertEquals(url, linkElement.attr("href"));
    }

    private void assertElementLazyLoaded(Element head, String url) {
        Stream.of("href", "src").forEach(attribute -> {
            Elements elements = head.getElementsByAttributeValue(attribute,
                    url);
            assertTrue(String.format(
                    "Expected not to have element with url %s for attribute %s",
                    url, attribute), elements.isEmpty());
        });
    }

    private void assertJavaScriptElementInlined(Element head,
            String expectedContents) {
        List<Element> scriptsWithoutExpectedContents = head
                .getElementsByTag("script").stream()
                .filter(element -> element.toString()
                        .contains(expectedContents))
                .collect(Collectors.toList());
        assertThat(
                "Expected to have only one inlined js element with contents = "
                        + expectedContents,
                scriptsWithoutExpectedContents.size(), is(1));
        Element inlinedElement = scriptsWithoutExpectedContents.get(0);
        assertThat("The element should have correct js type attribute",
                inlinedElement.attr("type"), is("text/javascript"));
        assertThat("Inlined js element should not have defer attribute",
                inlinedElement.attr("defer"), is(""));
        assertThat("Inlined js element should not have src attribute",
                inlinedElement.attr("src"), is(""));
    }

    private void assertCssElementInlined(Element head,
            String expectedContents) {
        List<Element> stylesWithExpectedContents = head
                .getElementsByTag("style").stream()
                .filter(element -> element.toString()
                        .contains(expectedContents))
                .collect(Collectors.toList());
        assertThat(
                "Expected to have only one inlined css element with contents = "
                        + expectedContents,
                stylesWithExpectedContents.size(), is(1));
        Element inlinedElement = stylesWithExpectedContents.get(0);
        assertThat("The element should have correct css type attribute",
                inlinedElement.attr("type"), is("text/css"));
    }

    private void assertHtmlElementInlined(Element body,
            String expectedContents) {
        List<Element> inlinedHtmlElements = body.getElementsByTag("span")
                .stream()
                .filter(element -> element.toString()
                        .contains(expectedContents))
                .collect(Collectors.toList());
        assertThat(
                "Expected to have only one inlined html element with contents = "
                        + expectedContents,
                inlinedHtmlElements.size(), is(1));
        Element inlinedElement = inlinedHtmlElements.get(0);
        assertThat("The element should be hidden",
                inlinedElement.hasAttr("hidden"), is(true));
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

    private void assertImportOrder(List<String> allContents,
            String firstContents, String secondContents) {
        int firstPosition = -1;
        int secondPosition = -1;
        for (int i = 0; i < allContents.size(); i++) {
            String currentContents = allContents.get(i);
            if (currentContents.contains(firstContents)) {
                firstPosition = i;
            } else if (currentContents.contains(secondContents)) {
                secondPosition = i;
            }

            if (firstPosition >= 0 && secondPosition >= 0) {
                break;
            }
        }

        assertCorrectDependencyPositions(firstContents, secondContents,
                firstPosition, secondPosition);
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

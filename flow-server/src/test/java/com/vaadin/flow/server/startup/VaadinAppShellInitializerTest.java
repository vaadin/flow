package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinAppShellSettings;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.server.VaadinAppShellRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinAppShellRegistry.VaadinAppShellRegistryWrapper;
import com.vaadin.flow.shared.communication.PushMode;

import static com.vaadin.flow.server.DevModeHandler.getDevModeHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VaadinAppShellInitializerTest {

    public static class MyAppShellWithoutAnnotations implements VaadinAppShell {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @PWA(name = "my-pwa", shortName = "pwa")
    @Inline("inline.html")
    @Inline(position = Position.PREPEND, value = "inline.css")
    @Inline(wrapping = Wrapping.JAVASCRIPT, position = Position.APPEND, target = TargetElement.BODY, value = "inline.js")
    @Viewport("my-viewport")
    @BodySize(height = "my-height", width = "my-width")
    @PageTitle("my-title")
    public static class MyAppShellWithMultipleAnnotations implements VaadinAppShell {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @PWA(name = "my-pwa", shortName = "pwa")
    @Inline("inline.html")
    @Inline(position = Position.PREPEND, value = "inline.css")
    @Inline(wrapping = Wrapping.JAVASCRIPT, position = Position.APPEND, target = TargetElement.BODY, value = "inline.js")
    @Viewport("my-viewport")
    @BodySize(height = "my-height", width = "my-width")
    @PageTitle("my-title")
    public static class OffendingClass {
    }

    public static class MyAppShellWithConfigurator implements VaadinAppShell {
        @Override
        public void configurePage(VaadinAppShellSettings settings) {
            settings.setViewport("my-viewport");
            settings.setPageTitle("my-title");
            settings.addMetaTag("foo", "bar");
            settings.addMetaTag("lorem", "ipsum");
            settings.addInlineFromFile("inline.html", Wrapping.AUTOMATIC);
            settings.addInlineFromFile(Position.PREPEND, "inline.css", Wrapping.AUTOMATIC);
            settings.addInlineFromFile(TargetElement.BODY, Position.APPEND, "inline.js", Wrapping.JAVASCRIPT);
            settings.setBodySize("my-width", "my-height");

            settings.addFavIcon("icon1", "icon1.png", "1x1");
            settings.addFavIcon("icon2", "icon2.png", "2x2");
            settings.addInlineWithContents(Position.PREPEND,
                    "window.messages = window.messages || [];\n"
                            + "window.messages.push(\"content script\");",
                            Wrapping.JAVASCRIPT);
            settings.addInlineFromFile(Position.PREPEND, "inline.js",
                    Wrapping.JAVASCRIPT);

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

            settings.addLink("shortcut icon", "icons/favicon.ico");

            settings.addFavIcon("icon", "icons/icon-192.png", "192x192");
            settings.addFavIcon("icon", "icons/icon-200.png", "2");

            settings.getLoadingIndicatorConfiguration().ifPresent(indicator -> indicator.setApplyDefaultTheme(false));
            settings.getLoadingIndicatorConfiguration().ifPresent(indicator -> indicator.setSecondDelay(700000));
            settings.getPushConfiguration().ifPresent(push -> push.setPushMode(PushMode.MANUAL));
            settings.getReconnectDialogConfiguration().ifPresent(dialog -> dialog.setDialogModal(true));
        }
    }

    public static class OffendingClassWithConfigurator implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }


    @Rule
    public ExpectedException exception = ExpectedException.none();

    private VaadinAppShellInitializer initializer;

    private ServletContext servletContext;
    private VaadinServletContext context;
    private Map<String, String> initParams;
    private Set<Class<?>> classes;
    private Document document;
    private Map<String, Object> attributeMap = new HashMap<>();
    private VaadinResponse response;
    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private VaadinSession session;

    @Before
    public void setup() throws Exception {
        assertNull(getDevModeHandler());

        servletContext = Mockito.mock(ServletContext.class);
        mocks = new MockServletServiceSessionSetup();
        service = mocks.getService();
        session = mocks.getSession();
        response = Mockito.mock(VaadinResponse.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString())).then(invocationOnMock -> attributeMap.get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1]
        )).when(servletContext).setAttribute(Mockito.anyString(), Mockito.any());

        ServletRegistration registration = Mockito
                .mock(ServletRegistration.class);
        context = new VaadinServletContext(servletContext);

        initParams = new HashMap<>();
        Mockito.when(registration.getInitParameters()).thenReturn(initParams);

        classes = new HashSet<>();

        Map<String, ServletRegistration> registry = new HashMap<>();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn((Map) registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        initializer = new VaadinAppShellInitializer();
        document = Document.createShell("");
    }

    @After
    public void teardown() throws Exception {
        VaadinAppShellRegistry.getInstance(context).reset();
    }

    @Test
    public void should_not_modifyDocument_when_noAnnotatedAppShell() throws Exception {
        classes.add(MyAppShellWithoutAnnotations.class);
        initializer.onStartup(classes, servletContext);
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtml(document, session,
                        createVaadinRequest("/"));
        assertEquals(0, document.head().children().size());
        assertEquals(0, document.body().children().size());
    }

    @Test
    public void should_not_throw_when_noClassesFound_empty() throws Exception {
        initializer.onStartup(Collections.emptySet(), servletContext);
    }

    @Test
    public void should_not_throw_when_noClassesFound_null() throws Exception {
        initializer.onStartup(null, servletContext);
    }

    @Test
    public void should_haveMetasAndBodySize_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.onStartup(classes, servletContext);

        VaadinAppShellRegistry.getInstance(context)
        .modifyIndexHtml(document, session,
                createVaadinRequest("/"));

        List<Element> elements = document.head().children();
        assertEquals(7, elements.size());
        assertEquals("text/css", elements.get(5).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}", elements.get(5).childNode(0).toString());
    }

    @Test
    public void should_haveInline_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.onStartup(classes, servletContext);

        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtml(document, session,
                        createVaadinRequest("/"));

        List<Element> headElements = document.head().children();
        assertEquals(7, headElements.size());
        assertEquals("text/css", headElements.get(0).attr("type"));
        assertEquals("style", headElements.get(0).tagName());
        assertTrue(headElements.get(0).outerHtml().contains("#preloadedDiv"));
        assertEquals("foo", headElements.get(1).attr("name"));
        assertEquals("lorem", headElements.get(2).attr("name"));
        assertEquals("viewport", headElements.get(3).attr("name"));
        assertEquals("title", headElements.get(4).tagName());
        assertEquals("my-title", headElements.get(4).childNode(0).toString());

        assertEquals("text/css", headElements.get(5).attr("type"));
        assertEquals("style", headElements.get(5).tagName());
        assertTrue(headElements.get(5).outerHtml().contains("width:my-width"));
        assertTrue(headElements.get(5).outerHtml().contains("height:my-height"));

        assertEquals("text/javascript", headElements.get(6).attr("type"));
        assertEquals("script", headElements.get(6).tagName());
        assertTrue(headElements.get(6).outerHtml().contains("might not yet be accessible"));

        List<Element> bodyElements = document.body().children();
        assertEquals(1, bodyElements.size());
        assertEquals("text/javascript", bodyElements.get(0).attr("type"));
        assertEquals("script", bodyElements.get(0).tagName());
        assertTrue(bodyElements.get(0).outerHtml().contains("window.messages.push"));
    }

    @Test
    public void should_not_haveMetas_when_not_callingInitializer()
            throws Exception {
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtml(document, session,
                        createVaadinRequest("/"));
        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_reuseContextAppShell_when_creatingNewInstance()
            throws Exception {

        // Set class in context and do not call initializer
        VaadinAppShellRegistry registry = new VaadinAppShellRegistry();
        registry.setShell(MyAppShellWithMultipleAnnotations.class);
        context.setAttribute(new VaadinAppShellRegistryWrapper(registry));

        VaadinRequest request = createVaadinRequest("/");
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtml(document, session, request);

        List<Element> elements = document.head().children();

        assertEquals(7, elements.size());
    }

    @Test
    public void should_throw_when_offendingClass() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("Found app shell configuration annotations in non"));
        exception.expectMessage(
                containsString("- @Meta, @PWA, @Inline, @Viewport, @BodySize, @PageTitle from"));
        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(OffendingClass.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_noAppShell_and_offendingClass()
            throws Exception {
        classes.add(OffendingClass.class);
        initializer.onStartup(classes, servletContext);

        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtml(document, session,
                        createVaadinRequest("/"));

        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_throw_when_multipleAppShell() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(containsString("Unable to find a single class"));

        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(MyAppShellWithMultipleAnnotations.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_throw_when_offendingClassWithConfigurator() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("The `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."));
        exception.expectMessage(
                containsString(MyAppShellWithoutAnnotations.class.getName()));
        exception.expectMessage(
                containsString("- " + OffendingClassWithConfigurator.class.getName()));
        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(OffendingClassWithConfigurator.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_noAppShell_and_classWithPageConfigurator() throws Exception {
        classes.add(OffendingClassWithConfigurator.class);
        initializer.onStartup(classes, servletContext);
    }

    private VaadinServletRequest createVaadinRequest(String pathInfo) {
        HttpServletRequest request = createRequest(pathInfo);
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("");
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer(pathInfo));
        return request;
    }
}

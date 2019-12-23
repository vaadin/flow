package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.page.BodySize;

import com.vaadin.flow.component.page.*;
import com.vaadin.flow.server.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.server.startup.VaadinAppShellRegistry.VaadinAppShellRegistryWrapper;

import static com.vaadin.flow.server.DevModeHandler.getDevModeHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VaadinAppShellInitializerTest {

    public static class MyAppShellWithoutAnnotations extends VaadinAppShell {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @PWA(name = "my-pwa", shortName = "pwa")
    @Inline(wrapping = Inline.Wrapping.STYLESHEET, position = Inline.Position.APPEND, target = TargetElement.HEAD, value = "")
    @Inline(wrapping = Inline.Wrapping.JAVASCRIPT, position = Inline.Position.PREPEND, target = TargetElement.BODY, value = "")
    @Viewport(Viewport.DEVICE_DIMENSIONS)
    @BodySize(height = "50vh", width = "50vw")
    public static class MyAppShellWithMultipleAnnotations extends VaadinAppShell {
    }

    @Meta(name = "offending-foo", content = "bar")
    @Meta(name = "offending-lorem", content = "ipsum")
    @PWA(name = "offending-my-pwa", shortName = "pwa")
    @Inline(wrapping = Inline.Wrapping.STYLESHEET, position = Inline.Position.PREPEND, target = TargetElement.HEAD, value = "")
    @Inline(wrapping = Inline.Wrapping.JAVASCRIPT, position = Inline.Position.APPEND, target = TargetElement.BODY, value = "")
    @Viewport(Viewport.DEVICE_DIMENSIONS)
    @BodySize(height = "50vh", width = "50vw")
    public static class OffendingClass {
    }

    public static class MyAppShellWithCofigurator extends VaadinAppShell
            implements AppShellConfigurator {
        @Override
        public void configurePage(AppShellSettings settings) {
        }
    }

    public static class OffendingClassWithConfigurator implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    public static class InvalidClassWithAppShellConfigurator
            implements AppShellConfigurator {
        @Override
        public void configurePage(AppShellSettings settings) {
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
                .modifyIndexHtmlResponse(document);
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
                .modifyIndexHtmlResponse(document);

        List<Element> elements = document.head().children();
        assertEquals(4, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
        assertEquals("viewport", elements.get(2).attr("name"));
        assertEquals(Viewport.DEVICE_DIMENSIONS, elements.get(2).attr("content"));
        assertEquals("text/css", elements.get(3).attr("type"));
        assertEquals("body,#outlet{height:50vh;width:50vw;}", elements.get(3).childNode(0).toString());
    }

    @Test
    public void should_haveInline_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.onStartup(classes, servletContext);
        VaadinRequest request = createVaadinRequest("/");
        VaadinAppShellRegistry.getInstance(context).modifyIndexHtmlResponeWithInline(document, session, request);

        List<Element> headElements = document.head().children();
        assertEquals(1, headElements.size());
        assertEquals("text/css", headElements.get(0).attr("type"));
        assertEquals("style", headElements.get(0).tagName());

        List<Element> bodyElements = document.body().children();
        assertEquals(1, bodyElements.size());
        assertEquals("text/javascript", bodyElements.get(0).attr("type"));
        assertEquals("script", bodyElements.get(0).tagName());
    }

    @Test
    public void should_not_haveMetas_when_not_callingInitializer()
            throws Exception {
        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtmlResponse(document);
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

        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtmlResponse(document);

        List<Element> elements = document.head().children();

        assertEquals(4, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
        assertEquals("viewport", elements.get(2).attr("name"));
        assertEquals(Viewport.DEVICE_DIMENSIONS, elements.get(2).attr("content"));
        assertEquals("text/css", elements.get(3).attr("type"));
        assertEquals("body,#outlet{height:50vh;width:50vw;}", elements.get(3).childNode(0).toString());
    }

    @Test
    public void should_throw_when_offendingClass() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("Found app shell configuration annotations in non"));
        exception.expectMessage(
                containsString("- @Meta, @PWA, @Inline, @Viewport, @BodySize from"));
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
                .modifyIndexHtmlResponse(document);

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
                containsString("Found deprecated `PageConfigurator`"));
        exception.expectMessage(
                containsString("- " + OffendingClassWithConfigurator.class.getName()));
        classes.add(MyAppShellWithCofigurator.class);
        classes.add(OffendingClassWithConfigurator.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_noAppShellConfigurator_and_classWithPageConfigurator() throws Exception {
        classes.add(OffendingClassWithConfigurator.class);
        classes.add(MyAppShellWithoutAnnotations.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_noAppShell_and_classWithPageConfigurator() throws Exception {
        classes.add(OffendingClassWithConfigurator.class);
        classes.add(OffendingClassWithConfigurator.class);
        initializer.onStartup(classes, servletContext);
    }
    
    @Test
    public void should_throw_when_invalidClassWithAppShellConfigurator() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("Found incorrect classes implementing `AppShellConfigurator`"));
        exception.expectMessage(
                containsString("- " + InvalidClassWithAppShellConfigurator.class.getName()));
        classes.add(InvalidClassWithAppShellConfigurator.class);
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

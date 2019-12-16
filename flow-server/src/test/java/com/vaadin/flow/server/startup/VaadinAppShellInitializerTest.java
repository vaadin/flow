package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.page.BodySize;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.VaadinAppShellRegistry.VaadinAppShellRegistryWrapper;

import static com.vaadin.flow.server.DevModeHandler.getDevModeHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VaadinAppShellInitializerTest {

    public static class MyAppShellWithoutMeta extends VaadinAppShell {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @PWA(name = "my-pwa", shortName = "pwa")
    @BodySize(height = "50vh", width = "50vw")
    public static class MyAppShellWithMultipleAnnotations extends VaadinAppShell {
    }

    @Meta(name = "offending-foo", content = "bar")
    @Meta(name = "offending-lorem", content = "ipsum")
    @PWA(name = "offending-my-pwa", shortName = "pwa")
    @BodySize(height = "50vh", width = "50vw")
    public static class OffendingClass {
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

    @Before
    public void setup() throws Exception {
        assertNull(getDevModeHandler());

        servletContext = Mockito.mock(ServletContext.class);
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
    public void should_haveMetasAndBodySize_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.onStartup(classes, servletContext);

        VaadinAppShellRegistry.getInstance(context)
                .modifyIndexHtmlResponse(document);

        List<Element> elements = document.head().children();
        assertEquals(3, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
        assertEquals("text/css", elements.get(2).attr("type"));
        assertEquals("body,#outlet{height:50vh;width:50vw;}", elements.get(2).childNode(0).toString());
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

        assertEquals(3, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
        assertEquals("text/css", elements.get(2).attr("type"));
        assertEquals("body,#outlet{height:50vh;width:50vw;}", elements.get(2).childNode(0).toString());
    }

    @Test
    public void should_throw_when_offendingClass() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("Found app shell configuration annotations in non"));
        exception.expectMessage(
                containsString("- @Meta, @PWA, @BodySize from"));

        classes.add(MyAppShellWithoutMeta.class);
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

        classes.add(MyAppShellWithoutMeta.class);
        classes.add(MyAppShellWithMultipleAnnotations.class);
        initializer.onStartup(classes, servletContext);
    }

}

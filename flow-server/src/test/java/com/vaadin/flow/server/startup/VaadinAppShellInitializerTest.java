package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

import static com.vaadin.flow.server.DevModeHandler.getDevModeHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VaadinAppShellInitializerTest {

    public static class MyAppShellWithoutMeta extends VaadinAppShell {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    public static class MyAppShellWithMultipleMeta extends VaadinAppShell {
    }

    @Meta(name = "", content = "")
    public static class OfendingClass {
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private VaadinAppShellInitializer initializer;

    private ServletContext servletContext;
    private Map<String, String> initParams;
    private Set<Class<?>> classes;
    private Document document;

    @Before
    public void setup() throws Exception {
        assertNull(getDevModeHandler());

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration registration = Mockito
                .mock(ServletRegistration.class);

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
        Field instance = VaadinAppShellRegistry.class
                .getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void should_haveMetas_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleMeta.class);

        initializer.onStartup(classes, servletContext);
        VaadinAppShellRegistry.getInstance(servletContext)
                .applyModifications(document);

        List<Element> elements = document.head().children();
        assertEquals(2, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
    }

    @Test
    public void should_not_haveMetas_when_not_callingInitializer()
            throws Exception {
        VaadinAppShellRegistry.getInstance(servletContext)
                .applyModifications(document);
        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_reuseContextAppShell_when_creatingNewInstance()
            throws Exception {
        // Set class in context and do not call initializer
        Mockito.when(
                servletContext.getAttribute(VaadinAppShell.class.getName()))
                .thenReturn(MyAppShellWithMultipleMeta.class.getName());

        VaadinAppShellRegistry.getInstance(servletContext)
                .applyModifications(document);

        List<Element> elements = document.head().children();

        assertEquals(2, elements.size());
        assertEquals("foo", elements.get(0).attr("name"));
        assertEquals("bar", elements.get(0).attr("content"));
        assertEquals("lorem", elements.get(1).attr("name"));
        assertEquals("ipsum", elements.get(1).attr("content"));
    }

    @Test
    public void should_throw_when_ofendingClass() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(
                containsString("Found configuration annotations"));

        classes.add(MyAppShellWithoutMeta.class);
        classes.add(OfendingClass.class);
        initializer.onStartup(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_noAppShell_and_ofendingClass()
            throws Exception {
        classes.add(OfendingClass.class);
        initializer.onStartup(classes, servletContext);

        VaadinAppShellRegistry.getInstance(servletContext)
                .applyModifications(document);

        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_throw_when_multipleAppShell() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(containsString("Unable to find a single class"));

        classes.add(MyAppShellWithoutMeta.class);
        classes.add(MyAppShellWithMultipleMeta.class);
        initializer.onStartup(classes, servletContext);
    }

}

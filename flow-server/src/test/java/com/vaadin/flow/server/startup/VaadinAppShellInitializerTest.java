package com.vaadin.flow.server.startup;

import javax.annotation.concurrent.NotThreadSafe;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.AppShellRegistry.AppShellRegistryWrapper;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.DevModeHandler.getDevModeHandler;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class VaadinAppShellInitializerTest {

    public static class MyAppShellWithoutAnnotations
            implements AppShellConfigurator {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @Inline("inline.html")
    @Inline(position = Position.PREPEND, value = "inline.css")
    @Inline(wrapping = Wrapping.JAVASCRIPT, position = Position.APPEND, target = TargetElement.BODY, value = "inline.js")
    @Viewport("my-viewport")
    @BodySize(height = "my-height", width = "my-width")
    @PageTitle("my-title")
    @Push(value = PushMode.MANUAL, transport = Transport.WEBSOCKET)
    @Theme(themeClass = AbstractTheme.class)
    public static class MyAppShellWithMultipleAnnotations
            implements AppShellConfigurator {
    }

    @Meta(name = "foo", content = "bar")
    @Meta(name = "lorem", content = "ipsum")
    @Inline("inline.html")
    @Inline(position = Position.PREPEND, value = "inline.css")
    @Inline(wrapping = Wrapping.JAVASCRIPT, position = Position.APPEND, target = TargetElement.BODY, value = "inline.js")
    @Viewport("my-viewport")
    @BodySize(height = "my-height", width = "my-width")
    @PageTitle("my-title")
    @Push(value = PushMode.MANUAL, transport = Transport.WEBSOCKET)
    @Theme(themeClass = AbstractTheme.class)
    public static class OffendingClass {
    }

    public static class WebHolder extends Component {
    }

    @Theme(themeClass = AbstractTheme.class)
    public static class NonOffendingExporter
        extends WebComponentExporter<WebHolder> {
        public NonOffendingExporter() {
            super("web-component");
        }

        @Override
        public void configureInstance(WebComponent<WebHolder> webComponent,
            WebHolder component) {
        }
    }

    public static class MyAppShellWithConfigurator
            implements AppShellConfigurator {
        @Override
        public void configurePage(AppShellSettings settings) {
            settings.setViewport("my-viewport");
            settings.setPageTitle("my-title");
            settings.addMetaTag("foo", "bar");
            settings.addMetaTag("lorem", "ipsum");
            settings.addInlineFromFile("inline.html", Wrapping.AUTOMATIC);
            settings.addInlineFromFile(Position.PREPEND, "inline.css",
                    Wrapping.AUTOMATIC);
            settings.addInlineFromFile(TargetElement.BODY, Position.APPEND,
                    "inline.js", Wrapping.JAVASCRIPT);
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

            settings.getLoadingIndicatorConfiguration().ifPresent(
                    indicator -> indicator.setApplyDefaultTheme(false));
            settings.getLoadingIndicatorConfiguration()
                    .ifPresent(indicator -> indicator.setSecondDelay(700000));
            settings.getPushConfiguration()
                    .ifPresent(push -> push.setPushMode(PushMode.AUTOMATIC));
            settings.getReconnectDialogConfiguration()
                    .ifPresent(dialog -> dialog.setDialogModal(true));
        }
    }

    public static class OffendingClassWithConfigurator
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @PWA(name = "name", shortName = "n")
    @Viewport("my-viewport")
    public static class OffendingPwaClass {
    }

    @Viewport("my-viewport")
    public static class OffendingNonPwaClass {
    }

    @PWA(name = "name", shortName = "n")
    @Viewport("my-viewport")
    @BodySize(height = "my-height", width = "my-width")
    public static class AppShellWithPWA implements AppShellConfigurator {
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private VaadinAppShellInitializer initializer;

    private ServletContext servletContext;
    private VaadinServletContext context;
    private Set<Class<?>> classes;
    private Document document;
    private Map<String, Object> attributeMap = new HashMap<>();
    private MockServletServiceSessionSetup mocks;
    private MockServletServiceSessionSetup.TestVaadinServletService service;
    private PushConfiguration pushConfiguration;
    private Logger logger;
    private ApplicationConfiguration appConfig;

    @Before
    public void setup() throws Exception {
        logger = mockLog(VaadinAppShellInitializer.class);
        assertNull(getDevModeHandler());

        mocks = new MockServletServiceSessionSetup();

        servletContext = mocks.getServletContext();

        appConfig = mockApplicationConfiguration();

        attributeMap.put(Lookup.class.getName(),
                servletContext.getAttribute(Lookup.class.getName()));
        attributeMap.put(ApplicationConfiguration.class.getName(), appConfig);

        service = mocks.getService();
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .then(invocationOnMock -> attributeMap
                        .get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1])).when(servletContext)
                .setAttribute(Mockito.anyString(), Mockito.any());

        ServletRegistration registration = Mockito
                .mock(ServletRegistration.class);
        context = new VaadinServletContext(servletContext);

        classes = new HashSet<>();

        Map<String, ServletRegistration> registry = new HashMap<>();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn((Map) registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        initializer = new VaadinAppShellInitializer();
        document = Document.createShell("");

        pushConfiguration = Mockito.mock(PushConfiguration.class);
    }

    @After
    public void teardown() throws Exception {
        AppShellRegistry.getInstance(context).reset();
        clearIlogger();
    }

    @Test
    public void should_not_modifyDocument_when_noAnnotatedAppShell()
            throws Exception {
        classes.add(MyAppShellWithoutAnnotations.class);
        initializer.process(classes, servletContext);
        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
                createVaadinRequest("/"));
        assertEquals(0, document.head().children().size());
        assertEquals(0, document.body().children().size());
    }

    @Test
    public void should_not_modifyPushConfiguration_when_noAnnotatedAppShell()
            throws Exception {
        classes.add(MyAppShellWithoutAnnotations.class);
        initializer.process(classes, servletContext);

        AppShellRegistry.getInstance(context)
                .modifyPushConfiguration(pushConfiguration);

        initializer.process(Collections.emptySet(), servletContext);
    }

    @Test
    public void should_not_throw_when_noClassesFound_null() throws Exception {
        initializer.process(null, servletContext);
    }

    @Test
    public void should_haveMetasAndBodySize_when_annotatedAppShell()
            throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.process(classes, servletContext);

        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
                createVaadinRequest("/"));

        List<Element> elements = document.head().children();
        assertEquals(7, elements.size());
        assertEquals("text/css", elements.get(5).attr("type"));
        assertEquals("body,#outlet{width:my-width;height:my-height;}",
                elements.get(5).childNode(0).toString());
    }

    @Test
    public void should_haveInline_when_annotatedAppShell() throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);

        initializer.process(classes, servletContext);

        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
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
        assertTrue(
                headElements.get(5).outerHtml().contains("height:my-height"));

        assertEquals("text/javascript", headElements.get(6).attr("type"));
        assertEquals("script", headElements.get(6).tagName());
        assertTrue(headElements.get(6).outerHtml()
                .contains("might not yet be accessible"));

        List<Element> bodyElements = document.body().children();
        assertEquals(1, bodyElements.size());
        assertEquals("text/javascript", bodyElements.get(0).attr("type"));
        assertEquals("script", bodyElements.get(0).tagName());
        assertTrue(bodyElements.get(0).outerHtml()
                .contains("window.messages.push"));
    }

    @Test
    public void should_modifyPushConfiguration_when_annotatedAppShell()
            throws Exception {
        classes.add(MyAppShellWithMultipleAnnotations.class);
        initializer.process(classes, servletContext);

        AppShellRegistry.getInstance(context)
                .modifyPushConfiguration(pushConfiguration);

        Mockito.verify(pushConfiguration).setPushMode(PushMode.MANUAL);
        Mockito.verify(pushConfiguration).setTransport(Transport.WEBSOCKET);
    }

    @Test
    public void should_not_haveMetas_when_not_callingInitializer()
            throws Exception {
        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
                createVaadinRequest("/"));
        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_not_modifyPushConfiguration_when_not_callingInitializer()
            throws Exception {
        AppShellRegistry.getInstance(context)
                .modifyPushConfiguration(pushConfiguration);

        Mockito.verify(pushConfiguration, Mockito.never())
                .setPushMode(Mockito.any(PushMode.class));
    }

    @Test
    public void should_reuseContextAppShell_when_creatingNewInstance()
            throws Exception {

        // Set class in context and do not call initializer
        AppShellRegistry registry = new AppShellRegistry();
        registry.setShell(MyAppShellWithMultipleAnnotations.class);
        context.setAttribute(new AppShellRegistryWrapper(registry));

        VaadinRequest request = createVaadinRequest("/");
        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
                request);

        List<Element> elements = document.head().children();

        assertEquals(7, elements.size());
    }

    @Test
    public void should_throw_when_offendingClass() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(containsString(
                "Found app shell configuration annotations in non"));
        exception.expectMessage(containsString(
                "- @Meta, @Inline, @Viewport, @BodySize, @Push, @Theme"
                        + " from"));
        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(OffendingClass.class);
        initializer.process(classes, servletContext);
    }

    @Test
    public void offendingEmbeddedThemeClass_shouldNotThrow() throws Exception {
        classes.add(NonOffendingExporter.class);
        initializer.process(classes, servletContext);
    }

    @Test
    public void should_throw_when_multipleAppShell() throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception
                .expectMessage(containsString("Unable to find a single class"));

        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(MyAppShellWithMultipleAnnotations.class);
        initializer.process(classes, servletContext);
    }

    @Test
    public void should_throw_when_offendingClassWithConfigurator()
            throws Exception {
        exception.expect(InvalidApplicationConfigurationException.class);
        exception.expectMessage(containsString(
                "The `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."));
        exception.expectMessage(
                containsString(MyAppShellWithoutAnnotations.class.getName()));
        exception.expectMessage(containsString(
                "- " + OffendingClassWithConfigurator.class.getName()));
        classes.add(MyAppShellWithoutAnnotations.class);
        classes.add(OffendingClassWithConfigurator.class);
        initializer.process(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_classWithPageConfigurator()
            throws Exception {
        classes.add(OffendingClassWithConfigurator.class);
        initializer.process(classes, servletContext);
    }

    @Test
    public void should_not_throw_when_appShellAnnotationsAreAllowed_and_offendingClass()
            throws Exception {
        Mockito.when(appConfig.getBooleanProperty(
                Constants.ALLOW_APPSHELL_ANNOTATIONS, false)).thenReturn(true);
        classes.add(OffendingClass.class);
        initializer.process(classes, servletContext);

        AppShellRegistry.getInstance(context).modifyIndexHtml(document,
                createVaadinRequest("/"));

        List<Element> elements = document.head().children();
        assertEquals(0, elements.size());
    }

    @Test
    public void should_link_to_PWA_article() throws Exception {
        Mockito.when(appConfig.getBooleanProperty(
                Constants.ALLOW_APPSHELL_ANNOTATIONS, false)).thenReturn(true);
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
        classes.add(OffendingPwaClass.class);
        initializer.process(classes, servletContext);
        Mockito.verify(logger, Mockito.times(1)).error(arg.capture());
        assertTrue(arg.getValue()
                .contains("We changed the way you configure PWAs"));
    }

    @Test
    public void should_not_link_to_PWA_article() throws Exception {
        Mockito.when(appConfig.getBooleanProperty(
                Constants.ALLOW_APPSHELL_ANNOTATIONS, false)).thenReturn(true);
        ArgumentCaptor<String> arg = ArgumentCaptor.forClass(String.class);
        classes.add(OffendingNonPwaClass.class);
        initializer.process(classes, servletContext);
        Mockito.verify(logger, Mockito.times(1)).error(arg.capture());
        assertFalse(arg.getValue()
                .contains("We changed the way you configure PWAs"));
        assertTrue(arg.getValue().contains("@Viewport"));
    }

    private VaadinServletRequest createVaadinRequest(String pathInfo) {
        HttpServletRequest request = createRequest(pathInfo);
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest(String pathInfo) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletPath()).thenReturn("");
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        Mockito.when(request.getRequestURL())
                .thenReturn(new StringBuffer(pathInfo));
        return request;
    }

    private Logger mockLog(Class clz) throws Exception {
        // wrap logger for clz in an spy
        Logger spy = Mockito.spy(LoggerFactory.getLogger(clz.getName()));
        ConcurrentMap<String, Logger> ilogger = clearIlogger();
        // replace original logger with the spy
        ilogger.put(clz.getName(), spy);
        return spy;
    }

    private ConcurrentMap<String, Logger> clearIlogger() throws Exception {
        ILoggerFactory ilogger = LoggerFactory.getILoggerFactory();
        Field field = SimpleLoggerFactory.class.getDeclaredField("loggerMap");
        field.setAccessible(true);
        ConcurrentMap<String, Logger> map = (ConcurrentMap<String, Logger>) field
                .get(ilogger);
        map.clear();
        return map;
    }

    private ApplicationConfiguration mockApplicationConfiguration() {
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.isProductionMode()).thenReturn(false);
        Mockito.when(config.enableDevServer()).thenReturn(true);

        Mockito.when(config.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgumentAt(1,
                        String.class));
        Mockito.when(config.getBooleanProperty(Mockito.anyString(),
                Mockito.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgumentAt(1,
                        Boolean.class));
        return config;
    }
}

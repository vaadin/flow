package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static java.util.Collections.enumeration;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BootstrapHandlerPushConfigurationTest {

    private BootstrapHandlerTest.TestUI testUI;
    private VaadinSession session;
    private MockVaadinServletService service;
    private MockDeploymentConfiguration deploymentConfiguration;
    private WebBrowser browser;

    @Before
    public void setup() {
        TestRouteRegistry routeRegistry = new TestRouteRegistry();

        BootstrapHandler.clientEngineFile = "foobar";
        testUI = new BootstrapHandlerTest.TestUI();

        deploymentConfiguration = new MockDeploymentConfiguration("test/");

        service = spy(new MockVaadinServletService(deploymentConfiguration));
        when(service.getRouteRegistry()).thenReturn(routeRegistry);
        when(service.getRouter()).thenReturn(new Router(routeRegistry) {
            @Override
            public void initializeUI(UI ui, VaadinRequest initRequest) {
                // Skip initial navigation during UI.init if no routes have been
                // injected
                if (routeRegistry.hasNavigationTargets()) {
                    super.initializeUI(ui, initRequest);
                }
            }
        });

        session = spy(new MockVaadinSession(service));
        session.lock();
        session.setConfiguration(deploymentConfiguration);
        testUI.getInternals().setSession(session);

        browser = mock(WebBrowser.class);

        when(browser.isEs6Supported()).thenReturn(false);
        when(session.getBrowser()).thenReturn(browser);
    }

    @After
    public void tearDown() {
        session.unlock();
    }

    @Test
    public void uiInitialization_pushNotConfiguredWhenAnnotationIsNotPresent() {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = mock(VaadinResponse.class);
        final BootstrapHandler.BootstrapContext context = bootstrapHandler.createAndInitUI(
            BootstrapHandlerTest.TestUI.class, createVaadinRequest(), response, session);

        PushConfiguration pushConfiguration = context.getUI().getPushConfiguration();
        Assert.assertEquals("Push mode should be disabled without @Push annotation.",
            PushMode.DISABLED, pushConfiguration.getPushMode());
        Assert.assertEquals("Push transport should be WEBSOCKET_XHR",
            Transport.WEBSOCKET_XHR, pushConfiguration.getTransport());
        Assert.assertEquals("Push fallback transport should be LONG_POLLING.",
            Transport.LONG_POLLING, pushConfiguration.getFallbackTransport());
    }

    @Test
    public void uiInitialization_defaultPushConfigurationIsApplied() throws InvalidRouteConfigurationException {
        assertPushConfigurationForComponent(PushDefaultTest.class, AtmospherePushConnection.class);
    }

    @Test
    public void uiInitialization_pushConfigurationIsApplied() throws InvalidRouteConfigurationException {
        assertPushConfigurationForComponent(PushConfiguredTest.class, AtmospherePushConnection.class);
    }

    @Test
    public void uiInitialization_customPushConnectionFactoryIsApplied() throws Exception {
        ClassLoader classLoader = service.getClassLoader();
        ClassLoader mockClassLoader = mockClassloaderForServiceLoader(classLoader, "PushConnectionFactory_serviceLoader_single.txt");
        service.setClassLoader(mockClassLoader);
        try {
            assertPushConfigurationForComponent(PushDefaultTest.class, TestPushConnection.class);
        } finally {
            service.setClassLoader(classLoader);
        }
    }

    @Test
    public void uiInitialization_shouldFailIfMultiplePushConnectionFactoryAreAvailable() throws Exception {
        ClassLoader classLoader = service.getClassLoader();
        ClassLoader mockClassLoader = mockClassloaderForServiceLoader(classLoader, "PushConnectionFactory_serviceLoader_multiple.txt");

        service.setClassLoader(mockClassLoader);
        try {
            assertPushConfigurationForComponent(PushDefaultTest.class, TestPushConnection.class);
            fail("Should fail due to Multiple PushConnectionFactory providers");
        } catch (BootstrapException ex) {
            assertThat(
                "Not multiple PushConnectionFactory error",
                ex.getMessage(), containsString(PushConnectionFactory.class.getName())
            );
        } finally {
            service.setClassLoader(classLoader);
        }
    }

    private ClassLoader mockClassloaderForServiceLoader(ClassLoader classLoader, String s) throws IOException {
        Answer<?> delegateToReal = i -> i.getMethod().invoke(classLoader, i.getArguments());
        ClassLoader mockClassLoader = mock(ClassLoader.class, delegateToReal);
        when(mockClassLoader.getResources("META-INF/services/com.vaadin.flow.server.communication.PushConnectionFactory"))
            .thenReturn(enumeration(singletonList(getClass().getResource(s))));
        return mockClassLoader;
    }


    private void assertPushConfigurationForComponent(Class<? extends Component> annotatedClazz,
                                                     Class<? extends PushConnection> pushConnectionType)
        throws InvalidRouteConfigurationException {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = mock(VaadinResponse.class);
        service.getRouteRegistry().setNavigationTargets(Collections.singleton(annotatedClazz));
        service.init();

        final BootstrapHandler.BootstrapContext context = bootstrapHandler.createAndInitUI(
            UI.class, createVaadinRequest(), response, session);
        Push pushAnnotation = annotatedClazz.getAnnotation(Push.class);
        Assert.assertNotNull("Should have @Push annotated component", pushAnnotation);
        PushConfiguration pushConfiguration = context.getUI().getPushConfiguration();
        assertPushConfiguration(pushConfiguration, pushAnnotation);
        assertThat(context.getUI().getInternals().getPushConnection(), instanceOf(pushConnectionType));
    }

    private VaadinRequest createVaadinRequest() {
        HttpServletRequest request = createRequest();
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
    }

    private static void assertPushConfiguration(PushConfiguration pushConfiguration, Push pushAnnotation) {
        assertPushConfiguration(pushConfiguration, pushAnnotation.value(), pushAnnotation.transport(), Transport.LONG_POLLING);
    }

    private static void assertPushConfiguration(PushConfiguration pushConfiguration, PushMode pushMode,
                                                Transport transport, Transport fallbackTransport) {
        Assert.assertEquals("Push mode should be the same as in @Push annotation.",
            pushMode, pushConfiguration.getPushMode());
        Assert.assertEquals("Push transport should be the same as in @Push annotation.",
            transport, pushConfiguration.getTransport());
        Assert.assertEquals("Push fallback transport should be LONG_POLLING.",
            fallbackTransport, Transport.LONG_POLLING);
    }

    @Route("")
    @Tag(Tag.DIV)
    @Push
    public static class PushDefaultTest extends Component {}

    @Route("")
    @Tag(Tag.DIV)
    @Push(value = PushMode.MANUAL, transport = Transport.LONG_POLLING)
    public static class PushConfiguredTest extends Component {}

    public static class TestPushConnection implements PushConnection {

        @Override
        public void push() {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean isConnected() {
            return false;
        }
    }

    public static class TestPushConnectionFactory1 implements PushConnectionFactory {

        @Override
        public PushConnection apply(UI ui) {
            return new TestPushConnection();
        }
    }

}

package com.vaadin.flow.server;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.function.BiConsumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.PushConfigurator;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class BootstrapHandlerPushConfigurationTest {

    @Route("")
    @Tag(Tag.DIV)
    @Push
    public static class PushDefaultTest extends Component {}

    @Route("")
    @Tag(Tag.DIV)
    @Push(value = PushMode.MANUAL, transport = Transport.LONG_POLLING)
    public static class PushConfiguredTest extends Component {}

    @Route("")
    @Tag(Tag.DIV)
    @Push(configurator = TestPushConfigurator.class)
    public static class CustomPushTest extends Component {}

    public static class TestPushConnection implements PushConnection {
        
        @Override
        public void push() { }

        @Override
        public void disconnect() { }

        @Override
        public boolean isConnected() { return false; }
    }

    public static class TestPushConfigurator implements PushConfigurator {
        @Override
        public void configurePush(PushConfiguration configuration, Push pushSetting) {
            configuration.applyConnectionFactoryIfPossible(ui -> new TestPushConnection());
            configuration.setTransport(Transport.LONG_POLLING);
            configuration.setPushMode(PushMode.MANUAL);
        }

        public static void assertPushConfiguration(PushConfiguration pushConfiguration, Push push) {
            BootstrapHandlerPushConfigurationTest.assertPushConfiguration(
                pushConfiguration, PushMode.MANUAL, Transport.LONG_POLLING, Transport.LONG_POLLING
            );
        }
    }

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

        service = Mockito
            .spy(new MockVaadinServletService(deploymentConfiguration));
        Mockito.when(service.getRouteRegistry()).thenReturn(routeRegistry);
        Mockito.when(service.getRouter()).thenReturn(new Router(routeRegistry) {
            @Override
            public void initializeUI(UI ui, VaadinRequest initRequest) {
                // Skip initial navigation during UI.init if no routes have been
                // injected
                if (routeRegistry.hasNavigationTargets()) {
                    super.initializeUI(ui, initRequest);
                }
            }
        });

        session = Mockito.spy(new MockVaadinSession(service));
        session.lock();
        session.setConfiguration(deploymentConfiguration);
        testUI.getInternals().setSession(session);

        browser = Mockito.mock(WebBrowser.class);

        Mockito.when(browser.isEs6Supported()).thenReturn(false);
        Mockito.when(session.getBrowser()).thenReturn(browser);
    }

    @After
    public void tearDown() {
        session.unlock();
    }

    @Test
    public void uiInitialization_pushNotConfiguredWhenAnnotationIsNotPresent() {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
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
        assertPushConfigurationForComponent(
            PushDefaultTest.class, AtmospherePushConnection.class, BootstrapHandlerPushConfigurationTest::assertPushConfiguration
        );
    }

    @Test
    public void uiInitialization_pushConfigurationIsApplied() throws InvalidRouteConfigurationException {
        assertPushConfigurationForComponent(
            PushConfiguredTest.class, AtmospherePushConnection.class, BootstrapHandlerPushConfigurationTest::assertPushConfiguration
        );
    }

    @Test
    public void uiInitialization_customPushConfigurationIsApplied() throws InvalidRouteConfigurationException {
        assertPushConfigurationForComponent(
            CustomPushTest.class, TestPushConnection.class, TestPushConfigurator::assertPushConfiguration
        );
    }

    private void assertPushConfigurationForComponent(Class<? extends Component> annotatedClazz,
                                                     Class<? extends PushConnection> pushConnectionType,
                                                     BiConsumer<PushConfiguration, Push> pushConfigurationAsserter)
        throws InvalidRouteConfigurationException {
        BootstrapHandler bootstrapHandler = new BootstrapHandler();
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        service.getRouteRegistry().setNavigationTargets(Collections.singleton(annotatedClazz));
        service.init();

        final BootstrapHandler.BootstrapContext context = bootstrapHandler.createAndInitUI(
            UI.class, createVaadinRequest(), response, session);
        Push pushAnnotation = annotatedClazz.getAnnotation(Push.class);
        Assert.assertNotNull("Should have @Push annotated component", pushAnnotation);
        PushConfiguration pushConfiguration = context.getUI().getPushConfiguration();
        pushConfigurationAsserter.accept(pushConfiguration, pushAnnotation);
        assertThat(context.getUI().getInternals().getPushConnection(), instanceOf(pushConnectionType));
    }

    private static void assertPushConfiguration(PushConfiguration pushConfiguration, Push pushAnnotation) {
        assertPushConfiguration(pushConfiguration, pushAnnotation.value(), pushAnnotation.transport(), Transport.LONG_POLLING);
    }
    private static void assertPushConfiguration(PushConfiguration pushConfiguration, PushMode pushMode, Transport transport, Transport fallbackTransport) {
        Assert.assertEquals("Push mode should be the same as in @Push annotation.",
            pushMode, pushConfiguration.getPushMode());
        Assert.assertEquals("Push transport should be the same as in @Push annotation.",
            transport, pushConfiguration.getTransport());
        Assert.assertEquals("Push fallback transport should be LONG_POLLING.",
            fallbackTransport, Transport.LONG_POLLING);
    }

    private VaadinRequest createVaadinRequest() {
        HttpServletRequest request = createRequest();
        return new VaadinServletRequest(request, service);
    }

    private HttpServletRequest createRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocation -> "").when(request).getServletPath();
        return request;
    }

}

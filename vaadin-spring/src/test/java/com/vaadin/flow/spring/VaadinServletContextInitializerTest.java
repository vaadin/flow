package com.vaadin.flow.spring;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.DevModeInitializer;
import com.vaadin.flow.server.startup.ServletDeployer;
import com.vaadin.flow.spring.router.SpringRouteNotFoundError;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DevModeInitializer.class,
        VaadinServletContextInitializer.SpringStubServletConfig.class,
        VaadinServletContextInitializer.class, ServletDeployer.class,
        ServletDeployer.StubServletConfig.class,
        AutoConfigurationPackages.class })
public class VaadinServletContextInitializerTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Environment environment;

    @Mock
    private ServletContext servletContext;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    @Before
    public void init() {
        PowerMockito.mockStatic(
                VaadinServletContextInitializer.SpringStubServletConfig.class);
        PowerMockito.mockStatic(ServletDeployer.class);
        PowerMockito.mockStatic(ServletDeployer.StubServletConfig.class);
        PowerMockito.mockStatic(DevModeInitializer.class);
        PowerMockito.mockStatic(AutoConfigurationPackages.class);
    }

    @Test
    public void onStartup_devModeNotInitialized_devModeInitialized()
            throws Exception {
        initDefaultMocks();

        VaadinServletContextInitializer vaadinServletContextInitializer = getStubbedVaadinServletContextInitializer();

        // Simulate Spring context start only
        vaadinServletContextInitializer.onStartup(servletContext);

        // This is how PowerMockito works, call PowerMockito.verifyStatic()
        // first
        // to start verifying behavior of DevModeInitializer static methods
        PowerMockito.verifyStatic(DevModeInitializer.class);
        // IMPORTANT: Call the static method we want to verify.
        // In our case, we want to check if Dev Mode has been started within
        // onStartup() call,
        // that means DevModeInitializer.initDevModeHandler() should has been
        // called exactly one time
        DevModeInitializer.initDevModeHandler(Mockito.any(), Mockito.any(),
                Mockito.any());
    }

    @Test
    public void onStartup_devModeAlreadyInitialized_devModeInitializationSkipped()
            throws Exception {
        initDefaultMocks();

        VaadinServletContextInitializer vaadinServletContextInitializer = getStubbedVaadinServletContextInitializer();

        DevModeInitializer devModeInitializer = getStubbedDevModeInitializer();

        // Simulate Servlet container start -> Spring context start
        devModeInitializer.process(Collections.emptySet(), servletContext);
        vaadinServletContextInitializer.onStartup(servletContext);

        // This is how PowerMockito works, call PowerMockito.verifyStatic()
        // first
        // to start verifying behavior of DevModeInitializer static methods
        PowerMockito.verifyStatic(DevModeInitializer.class);
        // IMPORTANT: Call the static method we want to verify.
        // In our case, we want to check if Dev Mode has been started within
        // devModeInitializer.process() call (i.e. from Servlet Container), and
        // not started again
        // within DevModeInitializer.initDevModeHandler() (Spring context),
        // so, we expect this method has been called exactly one time:
        DevModeInitializer.initDevModeHandler(Mockito.any(), Mockito.any(),
                Mockito.any());
    }

    @Test
    public void errorParameterServletContextListenerEvent_defaultRouteNotFoundView_defaultRouteNotFoundViewIsRegistered()
            throws Exception {
        // given
        initDefaultMocks();
        Runnable when = initRouteNotFoundMocksAndGetContextInitializedMockCall(
                getStubbedVaadinServletContextInitializer());

        // when
        when.run();

        // then
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(new VaadinServletContext(servletContext));
        final Class<? extends Component> navigationTarget = registry
                .getErrorNavigationTarget(new NotFoundException()).get()
                .getNavigationTarget();
        Assert.assertEquals(SpringRouteNotFoundError.class, navigationTarget);
    }

    @Test
    public void errorParameterServletContextListenerEvent_hasCustomRouteNotFoundViewExtendingRouteNotFoundError_customRouteNotFoundViewIsRegistered()
            throws Exception {
        // given
        initDefaultMocks();
        VaadinServletContextInitializer initializer = getStubbedVaadinServletContextInitializer();
        Runnable when = initRouteNotFoundMocksAndGetContextInitializedMockCall(
                initializer);

        class TestErrorView extends RouteNotFoundError {
        }

        PowerMockito.doAnswer(invocation -> Stream.of(TestErrorView.class))
                .when(initializer, "findBySuperType", Mockito.anyCollection(),
                        Mockito.eq(HasErrorParameter.class));

        // when
        when.run();

        // then
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(new VaadinServletContext(servletContext));
        final Class<? extends Component> navigationTarget = registry
                .getErrorNavigationTarget(new NotFoundException()).get()
                .getNavigationTarget();
        Assert.assertEquals(TestErrorView.class, navigationTarget);
    }

    @Test
    public void errorParameterServletContextListenerEvent_hasCustomRouteNotFoundViewImplementingHasErrorParameter_customRouteNotFoundViewIsRegistered()
            throws Exception {
        // given
        initDefaultMocks();
        VaadinServletContextInitializer initializer = getStubbedVaadinServletContextInitializer();
        Runnable when = initRouteNotFoundMocksAndGetContextInitializedMockCall(
                initializer);

        class TestErrorView extends Component
                implements HasErrorParameter<NotFoundException> {
            @Override
            public int setErrorParameter(BeforeEnterEvent event,
                    ErrorParameter<NotFoundException> parameter) {
                return 0;
            }
        }

        PowerMockito.doAnswer(invocation -> Stream.of(TestErrorView.class))
                .when(initializer, "findBySuperType", Mockito.anyCollection(),
                        Mockito.eq(HasErrorParameter.class));

        // when
        when.run();

        // then
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(new VaadinServletContext(servletContext));
        final Class<? extends Component> navigationTarget = registry
                .getErrorNavigationTarget(new NotFoundException()).get()
                .getNavigationTarget();
        Assert.assertEquals(TestErrorView.class, navigationTarget);
    }

    private Runnable initRouteNotFoundMocksAndGetContextInitializedMockCall(
            VaadinServletContextInitializer vaadinServletContextInitializer)
            throws Exception {
        initDefaultMocks();

        AtomicReference<ServletContextListener> theListener = new AtomicReference<>();
        Mockito.doAnswer(answer -> {
            ServletContextListener listener = answer.getArgument(0);
            theListener.set(listener);
            return null;
        }).when(servletContext)
                .addListener(Mockito.any(ServletContextListener.class));

        vaadinServletContextInitializer.onStartup(servletContext);

        Mockito.when(applicationContext.getBeanNamesForType(
                VaadinScanPackagesRegistrar.VaadinScanPackages.class))
                .thenReturn(new String[] {});
        PowerMockito.when(AutoConfigurationPackages.class, "has",
                applicationContext).thenReturn(false);

        ServletContextEvent initEventMock = Mockito
                .mock(ServletContextEvent.class);
        Mockito.when(initEventMock.getServletContext())
                .thenReturn(servletContext);

        return () -> {
            theListener.get().contextInitialized(initEventMock);
        };
    }

    private DevModeInitializer getStubbedDevModeInitializer() throws Exception {
        PowerMockito.when(ServletDeployer.StubServletConfig.class,
                "createDeploymentConfiguration", Mockito.any(), Mockito.any())
                .thenReturn(deploymentConfiguration);

        PowerMockito.when(DevModeInitializer.class, "isDevModeAlreadyStarted",
                servletContext).thenCallRealMethod();

        return new DevModeInitializer();
    }

    private VaadinServletContextInitializer getStubbedVaadinServletContextInitializer()
            throws Exception {
        VaadinServletContextInitializer vaadinServletContextInitializerMock = PowerMockito
                .spy(new VaadinServletContextInitializer(applicationContext));

        PowerMockito.when(
                VaadinServletContextInitializer.SpringStubServletConfig.class,
                "createDeploymentConfiguration", Mockito.any(), Mockito.any(),
                Mockito.any()).thenReturn(deploymentConfiguration);

        PowerMockito.doAnswer(invocation -> Stream.empty()).when(
                vaadinServletContextInitializerMock,
                "findByAnnotationOrSuperType", Mockito.anyCollection(),
                Mockito.any(), Mockito.anyCollection(),
                Mockito.anyCollection());

        PowerMockito.doReturn(Collections.emptyList()).when(
                vaadinServletContextInitializerMock, "getDefaultPackages");

        Mockito.doAnswer(invocation -> {
            ServletContextListener devModeListener = invocation.getArgument(0);
            devModeListener.contextInitialized(
                    new ServletContextEvent(servletContext));
            return null;
        }).when(servletContext)
                .addListener(Mockito.any(ServletContextListener.class));

        PowerMockito.doNothing().when(ServletDeployer.class);
        ServletDeployer.logAppStartupToConsole(Mockito.any(),
                Mockito.anyBoolean());

        return vaadinServletContextInitializerMock;
    }

    private void initDefaultMocks() {
        mockDeploymentConfiguration();
        mockApplicationContext();
        mockEnvironment();
        mockServletContext();
    }

    private void mockServletContext() {
        final Map<String, Object> servletContextAttributesMap = Maps
                .newHashMap();
        Mockito.doAnswer(answer -> {
            String key = answer.getArgument(0, String.class);
            Object value = answer.getArgument(1, Object.class);
            servletContextAttributesMap.putIfAbsent(key, value);
            return null;
        }).when(servletContext).setAttribute(Mockito.anyString(),
                Mockito.any());
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .thenAnswer(answer -> servletContextAttributesMap
                        .get(answer.getArgument(0, String.class)));
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(Maps.newHashMap());
    }

    private void mockEnvironment() {
        Mockito.when(environment.resolveRequiredPlaceholders(StringUtils.EMPTY))
                .thenReturn(StringUtils.EMPTY);
    }

    private void mockDeploymentConfiguration() {
        Mockito.when(deploymentConfiguration.isProductionMode())
                .thenReturn(false);
        Mockito.when(deploymentConfiguration.enableDevServer())
                .thenReturn(true);
    }

    private void mockApplicationContext() {
        Mockito.when(applicationContext.getEnvironment())
                .thenReturn(environment);
    }
}

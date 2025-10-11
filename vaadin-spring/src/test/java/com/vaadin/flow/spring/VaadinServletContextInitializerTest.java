/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.ServletDeployer;

public class VaadinServletContextInitializerTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Environment environment;

    @Mock
    private ServletContext servletContext;

    @Mock
    private DeploymentConfiguration deploymentConfiguration;

    @Mock
    private Executor executor;

    @Mock
    private ApplicationConfiguration appConfig;

    @Mock
    private Lookup lookup;

    @Mock
    private DevModeHandlerManager devModeHandlerManager;

    private MockedStatic<AutoConfigurationPackages> autoConfigurationPackagesMock;

    private MockedStatic<ServletDeployer> servletDeployerMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        Mockito.when(applicationContext.getBeansOfType(Executor.class))
                .thenReturn(Collections.singletonMap("foo", executor));

        autoConfigurationPackagesMock = Mockito
                .mockStatic(AutoConfigurationPackages.class);
        servletDeployerMock = Mockito.mockStatic(ServletDeployer.class);
    }

    @After
    public void teardown() {
        autoConfigurationPackagesMock.close();
        autoConfigurationPackagesMock = null;
        servletDeployerMock.close();
        servletDeployerMock = null;
    }

    @Test
    public void onStartup_devModeNotInitialized_devModeInitialized()
            throws Exception {
        initDefaultMocks();

        Mockito.when(devModeHandlerManager.getDevModeHandler())
                .thenReturn(null);
        Mockito.when(devModeHandlerManager.getHandlesTypes())
                .thenReturn(new Class<?>[0]);

        VaadinServletContextInitializer vaadinServletContextInitializer = getStubbedVaadinServletContextInitializer();

        // Simulate Spring context start only
        vaadinServletContextInitializer.onStartup(servletContext);

        // In our case, we want to check if Dev Mode has been started within
        // onStartup() call, that means DevModeInitializer.initDevModeHandler()
        // should have been called exactly one time.
        Mockito.verify(devModeHandlerManager).initDevModeHandler(Mockito.any(),
                Mockito.any(VaadinContext.class));
    }

    @Test
    public void onStartup_devModeAlreadyInitialized_devModeInitializationSkipped()
            throws Exception {
        initDefaultMocks();

        DevModeHandler devModeHandler = Mockito.mock(DevModeHandler.class);
        Mockito.when(devModeHandlerManager.getDevModeHandler())
                .thenReturn(devModeHandler);
        Mockito.when(devModeHandlerManager.getHandlesTypes())
                .thenReturn(new Class<?>[0]);

        VaadinServletContextInitializer vaadinServletContextInitializer = getStubbedVaadinServletContextInitializer();

        // Simulate Spring context start only
        vaadinServletContextInitializer.onStartup(servletContext);

        // In our case, we want to check if Dev Mode has been started within
        // onStartup() call, that means DevModeInitializer.initDevModeHandler()
        // should not have been called.
        Mockito.verify(devModeHandlerManager, Mockito.never())
                .initDevModeHandler(Mockito.any(),
                        Mockito.any(VaadinContext.class));
    }

    @Test
    public void errorParameterServletContextListenerEvent_hasCustomRouteNotFoundViewExtendingRouteNotFoundError_customRouteNotFoundViewIsRegistered()
            throws Exception {
        // given
        initDefaultMocks();
        VaadinServletContextInitializer initializer = getStubbedVaadinServletContextInitializer();
        Runnable when = initRouteNotFoundMocksAndGetContextInitializedMockCall(
                initializer);
        Mockito.when(devModeHandlerManager.getHandlesTypes())
                .thenReturn(new Class<?>[0]);

        class TestErrorView extends RouteNotFoundError {
        }

        Mockito.doAnswer(invocation -> Stream.of(TestErrorView.class))
                .when(initializer).findBySuperType(Mockito.anyCollection(),
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
        Mockito.when(devModeHandlerManager.getHandlesTypes())
                .thenReturn(new Class<?>[0]);

        class TestErrorView extends Component
                implements HasErrorParameter<NotFoundException> {
            @Override
            public int setErrorParameter(BeforeEnterEvent event,
                    ErrorParameter<NotFoundException> parameter) {
                return 0;
            }
        }

        Mockito.doAnswer(invocation -> Stream.of(TestErrorView.class))
                .when(initializer).findBySuperType(Mockito.anyCollection(),
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

        autoConfigurationPackagesMock
                .when(() -> AutoConfigurationPackages.has(applicationContext))
                .thenReturn(false);
        ServletContextEvent initEventMock = Mockito
                .mock(ServletContextEvent.class);
        Mockito.when(initEventMock.getServletContext())
                .thenReturn(servletContext);

        return () -> {
            theListener.get().contextInitialized(initEventMock);
        };
    }

    private VaadinServletContextInitializer getStubbedVaadinServletContextInitializer()
            throws Exception {
        VaadinServletContextInitializer vaadinServletContextInitializerMock = Mockito
                .spy(new VaadinServletContextInitializer(applicationContext));

        Mockito.doAnswer(invocation -> Stream.empty())
                .when(vaadinServletContextInitializerMock)
                .findByAnnotationOrSuperType(Mockito.anyCollection(),
                        Mockito.any(), Mockito.anyCollection(),
                        Mockito.anyCollection());
        Mockito.doReturn(Collections.emptyList())
                .when(vaadinServletContextInitializerMock).getDefaultPackages();

        Mockito.doAnswer(invocation -> {
            ServletContextListener devModeListener = invocation.getArgument(0);
            devModeListener.contextInitialized(
                    new ServletContextEvent(servletContext));
            return null;
        }).when(servletContext)
                .addListener(Mockito.any(ServletContextListener.class));

        return vaadinServletContextInitializerMock;
    }

    private void initDefaultMocks() {
        mockDeploymentConfiguration();
        mockApplicationContext();
        mockEnvironment();
        mockServletContext();
        mockDevModeHandlerManager();
    }

    private void mockServletContext() {
        final Map<String, Object> servletContextAttributesMap = new HashMap<>();
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
                .thenReturn(new HashMap<>());

        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);
    }

    private void mockEnvironment() {
        Mockito.when(environment.resolveRequiredPlaceholders(""))
                .thenReturn("");
    }

    private void mockDeploymentConfiguration() {
        Mockito.when(deploymentConfiguration.isProductionMode())
                .thenReturn(false);
    }

    private void mockApplicationContext() {
        Mockito.when(applicationContext.getEnvironment())
                .thenReturn(environment);
    }

    private void mockDevModeHandlerManager() {
        Mockito.when(lookup.lookup(DevModeHandlerManager.class))
                .thenReturn(devModeHandlerManager);
    }
}

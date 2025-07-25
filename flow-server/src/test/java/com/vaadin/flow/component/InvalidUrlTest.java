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
package com.vaadin.flow.component;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.Assert.assertEquals;

public class InvalidUrlTest {

    @Test
    public void invalidUrlAtInitialization_uiInitialiazesWith404ReturnCode()
            throws InvalidRouteConfigurationException, ServiceException {
        UI ui = new UI();

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);

        initUI(ui, "%3faaa", statusCodeCaptor);

        assertEquals("Return message should have been 404 not found.",
                Integer.valueOf(404), statusCodeCaptor.getValue());
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    private static void initUI(UI ui, String initialLocation,
            ArgumentCaptor<Integer> statusCodeCaptor)
            throws InvalidRouteConfigurationException, ServiceException {
        VaadinServletRequest request = Mockito.mock(VaadinServletRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        String pathInfo;
        if (initialLocation.isEmpty()) {
            pathInfo = null;
        } else {
            Assert.assertFalse(initialLocation.startsWith("/"));
            pathInfo = "/" + initialLocation;
        }
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

        VaadinService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }

            @Override
            public DeploymentConfiguration getDeploymentConfiguration() {
                if (super.getDeploymentConfiguration() != null) {
                    return super.getDeploymentConfiguration();
                } else {
                    DeploymentConfiguration config = Mockito
                            .mock(DeploymentConfiguration.class);
                    Mockito.when(config.isProductionMode()).thenReturn(false);
                    setConfiguration(config);
                    return config;
                }
            }
        };
        service.setCurrentInstances(request, response);

        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.isProductionMode()).thenReturn(false);
        Mockito.when(config.getFrontendFolder()).thenReturn(new File("front"));
        Mockito.when(config.getProjectFolder()).thenReturn(new File("./"));
        Mockito.when(config.getBuildFolder()).thenReturn("build");

        ((MockVaadinServletService) service).setConfiguration(config);
        CurrentInstance.set(VaadinSession.class, session);

        ui.getInternals().setSession(session);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(ui.getInternals().getRouter().getRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(UITest.RootNavigationTarget.class,
                    UITest.FooBarNavigationTarget.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });

        ui.doInit(request, 0, "foo");
        ui.getInternals().getRouter().initializeUI(ui,
                UITest.requestToLocation(request));

        if (statusCodeCaptor != null) {
            Mockito.verify(response).setStatus(statusCodeCaptor.capture());
        }
    }
}

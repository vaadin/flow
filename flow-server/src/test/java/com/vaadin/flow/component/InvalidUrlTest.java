/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.BootstrapHandlerTest;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
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
        };
        service.setCurrentInstances(request, response);

        MockVaadinSession session = new AlwaysLockedVaadinSession(service);

        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.isProductionMode()).thenReturn(false);

        session.lock();
        session.setConfiguration(config);

        ui.getInternals().setSession(session);

        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(ui.getRouter().getRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(UITest.RootNavigationTarget.class,
                    UITest.FooBarNavigationTarget.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });

        ui.doInit(request, 0);
        ui.getRouter().initializeUI(ui,
                BootstrapHandlerTest.requestToLocation(request));

        session.unlock();

        if (statusCodeCaptor != null) {
            Mockito.verify(response).setStatus(statusCodeCaptor.capture());
        }
    }
}

/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.HashSet;

import com.vaadin.flow.internal.CurrentInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.Assert.assertEquals;

public class InvalidUrlTest {

    @Test
    public void invalidUrlAtInitialization_uiInitialiazesAsExpected()
            throws InvalidRouteConfigurationException {
        UI ui = new UI();

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);

        initUI(ui, "?aaa", statusCodeCaptor);

        assertEquals("Return message should have been 404 not found.",
                Integer.valueOf(404), statusCodeCaptor.getValue());
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    private static void initUI(UI ui, String initialLocation,
                               ArgumentCaptor<Integer> statusCodeCaptor)
            throws InvalidRouteConfigurationException {
        try {
            VaadinServletRequest request = Mockito
                    .mock(VaadinServletRequest.class);
            VaadinResponse response = Mockito.mock(VaadinResponse.class);

            String pathInfo;
            if (initialLocation.isEmpty()) {
                pathInfo = null;
            } else {
                Assert.assertFalse(initialLocation.startsWith("/"));
                pathInfo = "/" + initialLocation;
            }
            Mockito.when(request.getPathInfo()).thenReturn(pathInfo);

            ServletConfig servletConfig = new MockServletConfig();
            VaadinServlet servlet = new VaadinServlet();
            servlet.init(servletConfig);
            VaadinService service = servlet.getService();
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
                        UITest.FooBarNavigationTarget.class).forEach(routeConfiguration::setAnnotatedRoute);
            });

            ui.doInit(request, 0);
            ui.getRouter().initializeUI(ui, request);

            session.unlock();

            if (statusCodeCaptor != null) {
                Mockito.verify(response).setStatus(statusCodeCaptor.capture());
            }
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
}

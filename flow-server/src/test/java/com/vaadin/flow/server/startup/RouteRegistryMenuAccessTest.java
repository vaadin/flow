/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.ServletContext;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.auth.NavigationAccessControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteRegistryMenuAccessTest {

    private ApplicationRouteRegistry registry;
    private VaadinRequest vaadinRequest;

    @BeforeEach
    public void init() {
        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(mock(ServletContext.class)));
        this.vaadinRequest = mock(VaadinRequest.class);
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withoutRequest_returnEmpty() {
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(null, null).size(),
                "No accessible menu routes should be available without VaadinService.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withoutVaadinService_returnEmpty() {
        when(vaadinRequest.getService()).thenReturn(null);
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest, null)
                        .size(),
                "No accessible menu routes should be available without VaadinService.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withoutNavAccessControl_noMenuRoutes() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.ALWAYS);
        registry.clean();
        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest, null)
                        .size(),
                "No accessible menu routes should be available.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_populateClientSideMenuIsFalse_noMenuRoute() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.NEVER);
        registry.clean();
        registry.setRoute("home", MyMenuRoute.class, Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(0, registry
                .getRegisteredAccessibleMenuRoutes(vaadinRequest, null).size(),
                "No routes should be registered.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_populateClientSideMenuIsAutomatic_oneMenuRoute() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.AUTOMATIC);
        registry.clean();
        registry.setRoute("home", MyMenuRoute.class, Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");

        try (MockedStatic<ApplicationConfiguration> config = Mockito.mockStatic(
                ApplicationConfiguration.class, Mockito.CALLS_REAL_METHODS)) {
            config.when(() -> ApplicationConfiguration.get(any()))
                    .thenReturn(mock(ApplicationConfiguration.class));
            assertEquals(1, registry
                    .getRegisteredAccessibleMenuRoutes(vaadinRequest, null)
                    .size(), "One route should be registered.");
        }
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withoutNavAccessControl_oneMenuRoute() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.ALWAYS);
        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoute.class,
                Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(1,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        List.of()).size(),
                "One accessible menu routes should be available.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withNavAccessControlWithoutRequest_noAccessibleMenuRoute() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.ALWAYS);
        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoute.class,
                Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        List.of(new NavigationAccessControl())).size(),
                "No accessible menu routes should be available without an active request.");
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withNavAccessControl_anonymous() {
        testAsAnonymous(new NavigationAccessControl());
    }

    @Test
    public void getRegisteredAccessibleMenuRoutes_withNavAccessControl_admin() {
        testAsAdmin(new NavigationAccessControl());
    }

    private void setupForAnonymous() {
        mockInstantiator(MenuAccessControl.PopulateClientMenu.ALWAYS);
    }

    private void testAsAnonymous(BeforeEnterListener... withAccessControls) {
        setupForAnonymous();

        List<BeforeEnterListener> accessControls = Stream.of(withAccessControls)
                .toList();

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoute.class,
                Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "No accessible menu routes should be available due to lack of security annotation.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRouteAnonymousAllowed.class,
                Collections.emptyList());
        assertEquals(1,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "One accessible menu routes should be available.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoutePermitAll.class,
                Collections.emptyList());
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "no accessible menu routes should be available for anonymous user.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRouteRolesAllowedAdmin.class,
                Collections.emptyList());
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "No accessible menu routes should be available without admin role.");
    }

    private void setupForAdmin(VaadinRequest vaadinRequest) {
        when(vaadinRequest.getUserPrincipal()).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "vaadin_user";
            }
        });
        when(vaadinRequest.isUserInRole("admin")).thenReturn(true);
        mockInstantiator(MenuAccessControl.PopulateClientMenu.ALWAYS);
    }

    private void testAsAdmin(BeforeEnterListener... withAccessControls) {
        setupForAdmin(vaadinRequest);

        List<BeforeEnterListener> accessControls = Stream.of(withAccessControls)
                .toList();

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoute.class,
                Collections.emptyList());
        assertEquals(1, registry.getRegisteredRoutes().size(),
                "One route should be registered.");
        assertEquals(0,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "No accessible menu routes should be available due to lack of security annotation.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRouteAnonymousAllowed.class,
                Collections.emptyList());
        assertEquals(1,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "One accessible menu routes should be available.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRoutePermitAll.class,
                Collections.emptyList());
        assertEquals(1,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "One accessible menu route should be available.");

        registry.clean();
        registry.setRoute("hasmenu", MyMenuRouteRolesAllowedAdmin.class,
                Collections.emptyList());
        assertEquals(1,
                registry.getRegisteredAccessibleMenuRoutes(vaadinRequest,
                        accessControls).size(),
                "One accessible menu route should be available.");
    }

    private void mockInstantiator(
            MenuAccessControl.PopulateClientMenu populateClientSideMenu) {
        var vaadinService = mock(VaadinService.class);
        when(vaadinRequest.getService()).thenReturn(vaadinService);
        var instantiator = mock(Instantiator.class);
        when(vaadinService.getInstantiator()).thenReturn(instantiator);
        Router router = mock(Router.class);
        when(vaadinService.getRouter()).thenReturn(router);
        when(router.getRegistry()).thenReturn(registry);
        when(instantiator.getMenuAccessControl())
                .thenReturn(new MenuAccessControl() {
                    @Override
                    public void setPopulateClientSideMenu(
                            PopulateClientMenu populateClientSideMenu) {
                    }

                    @Override
                    public PopulateClientMenu getPopulateClientSideMenu() {
                        return populateClientSideMenu;
                    }
                });
    }

    @Tag("div")
    @Route("home")
    protected static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("hasmenu")
    @Menu
    protected static class MyMenuRoute extends Component {
    }

    @AnonymousAllowed
    protected static class MyMenuRouteAnonymousAllowed extends MyMenuRoute {
    }

    @PermitAll
    protected static class MyMenuRoutePermitAll extends MyMenuRoute {
    }

    @RolesAllowed("admin")
    protected static class MyMenuRouteRolesAllowedAdmin extends MyMenuRoute {
    }
}

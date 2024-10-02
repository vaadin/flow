/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteResolver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutingTestBase;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.menu.RouteParamType;

public class DefaultRouteResolverTest extends RoutingTestBase {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private RouteResolver resolver;

    @Override
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        resolver = new DefaultRouteResolver();
        CurrentInstance.clearAll();
    }

    @Test
    public void basic_route_navigation_target_resolved_correctly()
            throws InvalidRouteConfigurationException {

        setRoutes(router.getRegistry(),
                Stream.of(RootNavigationTarget.class, FooNavigationTarget.class,
                        FooBarNavigationTarget.class,
                        GreetingNavigationTarget.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(RootNavigationTarget.class,
                resolveNavigationTarget(""));
        Assert.assertEquals(FooNavigationTarget.class,
                resolveNavigationTarget("foo"));
        Assert.assertEquals(FooBarNavigationTarget.class,
                resolveNavigationTarget("foo/bar"));
    }

    private void setRoutes(RouteRegistry registry,
            Set<Class<? extends Component>> routes) {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            routes.forEach(routeConfiguration::setAnnotatedRoute);
        });
    }

    @Test
    public void no_route_found_resolves_to_null() {
        Assert.assertNull(
                "Attempting to resolve an invalid location should return null",
                resolver.resolve(new ResolveRequest(router,
                        new Location("Not a configured location"))));
    }

    @Test
    public void string_url_parameter_correctly_set_to_state()
            throws InvalidRouteConfigurationException {
        setRoutes(router.getRegistry(),
                Collections.singleton(GreetingNavigationTarget.class));

        Assert.assertEquals(Collections.singletonList("World"),
                resolveNavigationState("greeting/World").getUrlParameters()
                        .get());
    }

    @Test
    public void route_precedence_with_parameters()
            throws InvalidRouteConfigurationException {
        setRoutes(router.getRegistry(),
                Stream.of(GreetingNavigationTarget.class,
                        OtherGreetingNavigationTarget.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(GreetingNavigationTarget.class,
                resolveNavigationTarget("greeting/World"));
        Assert.assertEquals(OtherGreetingNavigationTarget.class,
                resolveNavigationTarget("greeting/other/World"));
    }

    @Test
    public void wrong_number_of_parameters_does_not_match()
            throws InvalidRouteConfigurationException {
        setRoutes(router.getRegistry(),
                Collections.singleton(GreetingNavigationTarget.class));

        Assert.assertEquals(null,
                resolveNavigationState("greeting/World/something"));
        Assert.assertEquals(null, resolveNavigationState("greeting"));
    }

    @Test
    public void clientRouteRequest_getDefinedLayout() {
        String path = "route";

        router.getRegistry().setLayout(DefaultLayout.class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class);
                MockedStatic<DefaultRouteResolver> util = Mockito.mockStatic(
                        DefaultRouteResolver.class,
                        Mockito.CALLS_REAL_METHODS)) {

            util.when(() -> DefaultRouteResolver.isLayoutEnabled())
                    .thenReturn(true);
            menuRegistry.when(() -> MenuRegistry.hasClientRoute(path))
                    .thenReturn(true);
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
                    .thenReturn(Collections.singletonMap("/route",
                            new AvailableViewInfo("", null, false, "/route",
                                    false, false, null, null, null, true)));
            NavigationState greeting = resolveNavigationState(path);
            Assert.assertEquals(
                    "Layout should be returned for a non server route when matching @Layout exists",
                    DefaultLayout.class, greeting.getRouteTarget().getTarget());
        }
    }

    @Test
    public void clientRouteRequest_noLayoutForPath_Throws() {
        expectedEx.expect(NotFoundException.class);
        expectedEx.expectMessage("No layout for client path 'route'");

        String path = "route";

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class);
                MockedStatic<DefaultRouteResolver> util = Mockito.mockStatic(
                        DefaultRouteResolver.class,
                        Mockito.CALLS_REAL_METHODS)) {

            util.when(() -> DefaultRouteResolver.isLayoutEnabled())
                    .thenReturn(true);
            menuRegistry.when(() -> MenuRegistry.hasClientRoute(path))
                    .thenReturn(true);
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
                    .thenReturn(Collections.singletonMap("/route",
                            new AvailableViewInfo("", null, false, "/route",
                                    false, false, null, null, null, true)));
            NavigationState greeting = resolveNavigationState(path);
        }
    }

    @Tag("div")
    @Layout
    private static class DefaultLayout extends Component
            implements RouterLayout {
    }

    private Class<? extends Component> resolveNavigationTarget(String path) {
        return resolveNavigationState(path).getNavigationTarget();
    }

    private NavigationState resolveNavigationState(String path) {
        return resolver.resolve(new ResolveRequest(router, new Location(path)));
    }
}

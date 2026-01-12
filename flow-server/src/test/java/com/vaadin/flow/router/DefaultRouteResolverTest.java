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
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.internal.DefaultRouteResolver;
import com.vaadin.flow.router.internal.ResolveRequest;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.menu.AvailableViewInfo;

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
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
                    .thenReturn(Collections.singletonMap("/route",
                            new AvailableViewInfo("", null, false, "/route",
                                    false, false, null, null, null, true,
                                    null)));
            NavigationState greeting = resolveNavigationState(path);
            Assert.assertEquals(
                    "Layout should be returned for a non server route when matching @Layout exists",
                    DefaultLayout.class, greeting.getRouteTarget().getTarget());
        }
    }

    @Test
    public void clientRouteRequest_getDefinedLayoutAndParentLayouts() {
        String path = "route";

        router.getRegistry().setLayout(DefaultWithParentLayout.class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
                    .thenReturn(Collections.singletonMap("/route",
                            new AvailableViewInfo("", null, false, "/route",
                                    false, false, null, null, null, true,
                                    null)));
            NavigationState greeting = resolveNavigationState(path);
            Assert.assertEquals(
                    "Layout should be returned for a non server route when matching @Layout exists",
                    DefaultWithParentLayout.class,
                    greeting.getRouteTarget().getTarget());
            Assert.assertEquals(
                    "@ParentLayout annotation should be followed. @Layout class should not be in parent layout list.",
                    1, greeting.getRouteTarget().getParentLayouts().size());
            Assert.assertEquals(
                    "@ParentLayout annotation should be followed. @Layout class should not be in parent layout list.",
                    DefaultParentLayout.class,
                    greeting.getRouteTarget().getParentLayouts().get(0));

        }
    }

    @Test
    public void clientRouteRequest_withRouteParameters_getDefinedLayout() {
        router.getRegistry().setLayout(DefaultLayout.class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
            // @formatter:off
                    .thenReturn(buildClientRoutes(
                            "/route/:param",
                            "/opt_route/:param?",
                            "/wildcard_route/:param*",
                            "/one/:param/two/:param2",
                            "/foo/:param?/bar/:param2?"));
            Stream.of(
                    "route/1",
                            "/route/abc",
                            "/opt_route",
                            "/opt_route/",
                            "/opt_route/1",
                            "/wildcard_route",
                            "wildcard_route",
                            "wildcard_route/",
                            "wildcard_route/1",
                            "/wildcard_route/1/2",
                            "one/1/two/2",
                            "foo/bar",
                            "foo/a/bar",
                            "foo/bar/b",
                            "foo/a/bar/b")
                    // @formatter:on
                    .forEach(path -> {
                        String msg = String.format(
                                "Layout should be returned for path '%s' a non server route when matching @Layout exists",
                                path);
                        var state = resolveNavigationState(path);
                        Assert.assertNotNull(msg, state);
                        Assert.assertEquals(msg, DefaultLayout.class,
                                resolveNavigationState(path).getRouteTarget()
                                        .getTarget());
                    });
        }
    }

    @Test
    public void clientRouteRequest_withRouteParameters_noLayout() {
        router.getRegistry().setLayout(DefaultLayout.class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
            // @formatter:off
                    .thenReturn(buildClientRoutes(
                            "/route/:param",
                            "/opt_route/:param?",
                            "/wildcard_route/:param*",
                            "/one/:param/two/:param2",
                            "/foo/:param?/bar/:param2?"));
            // @formatter:off
            Stream.of(
                            "route",
                            "/route/abc/def",
                            "/unknown_route",
                            "/opt_route/1/2",
                            "/",
                            "",
                            "one/1/two/2/3",
                            "one/two/2",
                            "one/two/",
                            "foo",
                            "foo/foo/foo/bar/bar/")
                    // @formatter:on
                    .forEach(path -> {
                        Assert.assertNull(String.format(
                                "Layout should not be returned for a non server route '%s' when matching @Layout doesn't exist",
                                path), resolveNavigationState(path));
                    });
        }
    }

    /**
     * Ambiguous routes should be resolved by the order they were added to the
     * RouteModel. First added wins. Ambiguous route is detected by
     * AmbiguousRouteConfigurationException.
     */
    @Test
    public void clientRouteRequest_withRouteParameters_ambiguousRoutesFirstAddedWins() {
        router.getRegistry().setLayout(DefaultLayout.class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
            // @formatter:off
                    .thenReturn(buildClientRoutes(
                            "/route",
                            "/route/:param?",
                            "/foo",
                            "/foo/:param*"));
            // @formatter:off
            Stream.of(
                    "route",
                            "foo",
                            "foo/1"
                            )
                    // @formatter:on
                    .forEach(path -> {
                        String msg = String.format(
                                "Layout should be returned for path '%s' a non server route when matching @Layout exists",
                                path);
                        var state = resolveNavigationState(path);
                        Assert.assertNotNull(msg, state);
                        Assert.assertEquals(msg, DefaultLayout.class,
                                resolveNavigationState(path).getRouteTarget()
                                        .getTarget());
                    });
            Stream.of("route/1").forEach(path -> {
                Assert.assertNull(String.format(
                        "Layout should not be returned for a non server route '%s' when matching @Layout doesn't exist",
                        path), resolveNavigationState(path));
            });
        }
    }

    private Map<String, AvailableViewInfo> buildClientRoutes(String... routes) {
        Map<String, AvailableViewInfo> clientRoutes = new LinkedHashMap<>();
        for (String route : routes) {
            clientRoutes.put(route, createAvailableViewInfo(route));
        }
        return clientRoutes;
    }

    private AvailableViewInfo createAvailableViewInfo(String route) {
        return new AvailableViewInfo("", null, false, route, false, false, null,
                null, null, true, null);
    }

    @Test
    public void clientRouteRequest_noLayoutForPath_Throws() {
        expectedEx.expect(NotFoundException.class);
        expectedEx.expectMessage("No layout for client path 'route'");

        String path = "route";

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry.when(() -> MenuRegistry.getClientRoutes(false))
                    .thenReturn(Collections.singletonMap("/route",
                            new AvailableViewInfo("", null, false, "/route",
                                    false, false, null, null, null, true,
                                    null)));
            NavigationState greeting = resolveNavigationState(path);
        }
    }

    @Tag("div")
    @Layout
    private static class DefaultLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    @Layout
    @ParentLayout(DefaultParentLayout.class)
    private static class DefaultWithParentLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    private static class DefaultParentLayout extends Component
            implements RouterLayout {
    }

    private Class<? extends Component> resolveNavigationTarget(String path) {
        return resolveNavigationState(path).getNavigationTarget();
    }

    private NavigationState resolveNavigationState(String path) {
        return resolver.resolve(new ResolveRequest(router, new Location(path)));
    }
}

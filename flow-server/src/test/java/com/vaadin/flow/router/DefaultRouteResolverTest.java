/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.internal.DefaultRouteResolver;
import com.vaadin.flow.router.internal.ResolveRequest;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;

public class DefaultRouteResolverTest extends RoutingTestBase {

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

    private Class<? extends Component> resolveNavigationTarget(String path) {
        return resolveNavigationState(path).getNavigationTarget();
    }

    private NavigationState resolveNavigationState(String path) {
        return resolver.resolve(new ResolveRequest(router, new Location(path)));
    }
}

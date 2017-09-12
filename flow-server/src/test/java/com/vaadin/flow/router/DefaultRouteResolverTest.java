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
package com.vaadin.flow.router;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;

public class DefaultRouteResolverTest extends NewRoutingTestBase {

    private RouteResolver resolver;

    @Override
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        resolver = new DefaultRouteResolver();
    }

    @Test
    public void basic_route_navigation_target_resolved_correctly()
            throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(RootNavigationTarget.class,
                resolveNavigationTarget(""));
        Assert.assertEquals(FooNavigationTarget.class,
                resolveNavigationTarget("foo"));
        Assert.assertEquals(FooBarNavigationTarget.class,
                resolveNavigationTarget("foo/bar"));
    }

    @Test
    public void no_route_found_resolves_to_null() {
        Assert.assertNull(
                "Attempting to resolve an invalid location should return null",
                resolver.resolve(new ResolveRequest(router,
                        new Location("Not a configured location"))));
    }

    private Class<? extends Component> resolveNavigationTarget(String path) {
        return resolver.resolve(new ResolveRequest(router, new Location(path)))
                .getNavigationTarget();
    }
}

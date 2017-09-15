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
package com.vaadin.server.startup;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.ParentLayout;
import com.vaadin.annotations.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.ui.Component;

/**
 * Unit tests for RouteRegistryInitializer and RouteRegistry.
 */
public class RouteRegistryInitializerTest {

    private RouteRegistryInitializer routeRegistryInitializer;

    @Before
    public void init() {
        routeRegistryInitializer = new RouteRegistryInitializer();
        RouteRegistry.getInstance().initialized = false;
    }

    @Test
    public void onStartUp() throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTarget.class, NavigationTargetFoo.class,
                        NavigationTargetBar.class).collect(Collectors.toSet()),
                null);

        Assert.assertEquals("Route '' registered to NavigationTarget.class",
                NavigationTarget.class, RouteRegistry.getInstance()
                        .getNavigationTarget("").get());
        Assert.assertEquals(
                "Route 'foo' registered to NavigationTargetFoo.class",
                NavigationTargetFoo.class, RouteRegistry.getInstance()
                        .getNavigationTarget("foo").get());
        Assert.assertEquals(
                "Route 'bar' registered to NavigationTargetBar.class",
                NavigationTargetBar.class, RouteRegistry.getInstance()
                        .getNavigationTarget("bar").get());
    }

    @Test
    public void onStartUp_no_exception_with_null_arguments() {
        try {
            routeRegistryInitializer.onStartup(null, null);
        } catch (Exception e) {
            Assert.fail(
                    "RouteRegistryInitializer.onStartup should not throw with null arguments");
        }
    }

    @Test(expected = ServletException.class)
    public void onStartUp_duplicate_routes_throws() throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTargetFoo.class, NavigationTargetFoo2.class)
                        .collect(Collectors.toSet()),
                null);
    }

    @Test(expected = InvalidRouteConfigurationException.class)
    public void routeRegistry_routes_can_only_be_set_once()
            throws InvalidRouteConfigurationException {
        Assert.assertFalse("RouteRegistry should not be initialized",
                RouteRegistry.getInstance().isInitialized());
        RouteRegistry.getInstance().setNavigationTargets(new HashSet<>());
        Assert.assertTrue("RouteRegistry should be initialized",
                RouteRegistry.getInstance().isInitialized());
        RouteRegistry.getInstance().setNavigationTargets(new HashSet<>());
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeRegistry_fails_on_faulty_configuration() throws ServletException  {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTarget.class, NavigationTargetFoo.class,
                        FaultyConfiguration.class).collect(Collectors.toSet()),
                null);
    }

    @Route("")
    private static class NavigationTarget extends Component {
    }

    @Route("foo")
    private static class NavigationTargetFoo extends Component {
    }

    @Route("foo")
    private static class NavigationTargetFoo2 extends Component {
    }

    @Route("bar")
    private static class NavigationTargetBar extends Component {
    }

    private static class RouteParentLayout extends Component
            implements RouterLayout {
    }

    @ParentLayout(RouteParentLayout.class)
    @Route(value = "wrong")
    private static class FaultyConfiguration extends Component {
    }
}

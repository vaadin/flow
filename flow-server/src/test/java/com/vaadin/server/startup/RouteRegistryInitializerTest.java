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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.PageTitle;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RoutePrefix;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.TestRouteRegistry;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.ui.Component;

/**
 * Unit tests for RouteRegistryInitializer and RouteRegistry.
 */
public class RouteRegistryInitializerTest {

    private RouteRegistryInitializer routeRegistryInitializer;
    private RouteRegistry registry;
    private ServletContext servletContext;

    @Before
    public void init() {
        routeRegistryInitializer = new RouteRegistryInitializer();
        registry = new TestRouteRegistry();
        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(RouteRegistry.class.getName()))
                .thenReturn(registry);
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void onStartUp() throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTarget.class, NavigationTargetFoo.class,
                        NavigationTargetBar.class).collect(Collectors.toSet()),
                servletContext);

        Assert.assertEquals("Route '' registered to NavigationTarget.class",
                NavigationTarget.class, registry.getNavigationTarget("").get());
        Assert.assertEquals(
                "Route 'foo' registered to NavigationTargetFoo.class",
                NavigationTargetFoo.class,
                registry.getNavigationTarget("foo").get());
        Assert.assertEquals(
                "Route 'bar' registered to NavigationTargetBar.class",
                NavigationTargetBar.class,
                registry.getNavigationTarget("bar").get());
    }

    @Test
    public void onStartUp_no_exception_with_null_arguments() {
        try {
            routeRegistryInitializer.onStartup(null, servletContext);
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
                servletContext);
    }

    @Test(expected = InvalidRouteConfigurationException.class)
    public void routeRegistry_routes_can_only_be_set_once()
            throws InvalidRouteConfigurationException {
        Assert.assertFalse("RouteRegistry should not be initialized",
                registry.navigationTargetsInitialized());
        registry.setNavigationTargets(new HashSet<>());
        Assert.assertTrue("RouteRegistry should be initialized",
                registry.navigationTargetsInitialized());
        registry.setNavigationTargets(new HashSet<>());
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeRegistry_fails_on_faulty_configuration()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTarget.class, NavigationTargetFoo.class,
                        FaultyConfiguration.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void routeRegistry_stores_whole_path_with_parent_route_prefix()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(ExtendingPrefix.class).collect(Collectors.toSet()),
                servletContext);

        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget("parent/prefix");

        Assert.assertTrue("Couldn't find navigation target for `parent/prefix`",
                navigationTarget.isPresent());
        Assert.assertEquals(
                "Route 'parent/prefix' was not registered to ExtendingPrefix.class",
                ExtendingPrefix.class, navigationTarget.get());
    }

    @Test
    public void routeRegistry_route_with_absolute_ignores_parent_route_prefix()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(AbosulteRoute.class).collect(Collectors.toSet()),
                servletContext);

        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget("absolute");

        Assert.assertTrue("Could not find navigation target for `absolute`",
                navigationTarget.isPresent());
        Assert.assertEquals("Route 'absolute' was not registered correctly",
                AbosulteRoute.class, navigationTarget.get());
    }

    @Test
    public void routeRegistry_route_with_absolute_parent_prefix_ignores_remaining_parent_route_prefixes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(MultiLevelRoute.class).collect(Collectors.toSet()),
                servletContext);

        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget("absolute/levels");

        Assert.assertTrue(
                "Could not find navigation target for `absolute/levels`",
                navigationTarget.isPresent());
        Assert.assertEquals("Route 'absolute' was not registered correctly",
                MultiLevelRoute.class, navigationTarget.get());
    }

    @Test
    public void routeRegistry_route_returns_registered_string_for_get_url()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream
                .of(NavigationTarget.class, NavigationTargetFoo.class,
                        AbosulteRoute.class, ExtendingPrefix.class)
                .collect(Collectors.toSet()), servletContext);

        Assert.assertEquals("",
                registry.getTargetUrl(NavigationTarget.class).get());
        Assert.assertEquals("foo",
                registry.getTargetUrl(NavigationTargetFoo.class).get());
        Assert.assertEquals("absolute",
                registry.getTargetUrl(AbosulteRoute.class).get());
        Assert.assertEquals("parent/prefix",
                registry.getTargetUrl(ExtendingPrefix.class).get());
    }

    @Test
    public void routeRegistry_routes_with_parameters_return_parameter_type_for_target_url()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(ParameterRoute.class, StringParameterRoute.class)
                        .collect(Collectors.toSet()),
                servletContext);

        Assert.assertEquals("parameter/{Boolean}",
                registry.getTargetUrl(ParameterRoute.class).get());
        Assert.assertEquals("string/{String}",
                registry.getTargetUrl(StringParameterRoute.class).get());
    }

    @Test
    public void routeRegistry_route_returns_string_not_ending_in_dash()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream
                .of(NavigationRootWithParent.class).collect(Collectors.toSet()),
                servletContext);

        Assert.assertEquals(
                "The root target for a parent layout should not end with '/'",
                "parent",
                registry.getTargetUrl(NavigationRootWithParent.class).get());
    }

    @Test
    public void registration_fails_for_navigation_target_with_duplicate_title()
            throws ServletException {
        expectedEx.expect(DuplicateNavigationTitleException.class);
        expectedEx.expectMessage(String.format(
                "'%s' has a PageTitle annotation, but also implements HasDynamicTitle.",
                FaultyNavigationTargetWithTitle.class.getName()));

        routeRegistryInitializer.onStartup(
                Collections.singleton(FaultyNavigationTargetWithTitle.class),
                servletContext);
    }

    @Test
    public void registration_fails_for_navigation_target_with_inherited_dynamic_title()
            throws ServletException {
        expectedEx.expect(DuplicateNavigationTitleException.class);
        expectedEx.expectMessage(String.format(
                "'%s' has a PageTitle annotation, but also implements HasDynamicTitle.",
                FaultyChildWithDuplicateTitle.class.getName()));

        routeRegistryInitializer.onStartup(
                Collections.singleton(FaultyChildWithDuplicateTitle.class),
                servletContext);
    }

    @Test
    public void registration_succeeds_for_navigation_target_with_inherited_title_annotation()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Collections.singleton(ChildWithDynamicTitle.class),
                servletContext);

        Assert.assertEquals("bar",
                registry.getTargetUrl(ChildWithDynamicTitle.class).get());
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

    @Route(value = "", layout = ParentWithRoutePrefix.class)
    private static class NavigationRootWithParent extends Component {
    }

    @ParentLayout(RouteParentLayout.class)
    @Route(value = "wrong")
    private static class FaultyConfiguration extends Component {
    }

    @RoutePrefix("parent")
    private static class ParentWithRoutePrefix extends Component
            implements RouterLayout {
    }

    @Route(value = "prefix", layout = ParentWithRoutePrefix.class)
    private static class ExtendingPrefix extends Component {
    }

    @Route(value = "absolute", layout = ParentWithRoutePrefix.class, absolute = true)
    private static class AbosulteRoute extends Component {
    }

    @RoutePrefix(value = "absolute", absolute = true)
    @ParentLayout(ParentWithRoutePrefix.class)
    private static class AbsoluteMiddleParent extends Component
            implements RouterLayout {
    }

    @Route(value = "levels", layout = AbsoluteMiddleParent.class)
    private static class MultiLevelRoute extends Component {
    }

    @Route("parameter")
    private static class ParameterRoute extends Component
            implements HasUrlParameter<Boolean> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                Boolean parameter) {

        }
    }

    @Route("string")
    private static class StringParameterRoute extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {

        }
    }

    @Route("foo")
    @PageTitle("Custom Title")
    private static class FaultyNavigationTargetWithTitle extends Component
            implements HasDynamicTitle {

        @Override
        public String getPageTitle() {
            return "";
        }
    }

    @Route("foo")
    private static class ParentWithDynamicTitle extends Component
            implements HasDynamicTitle {

        @Override
        public String getPageTitle() {
            return "Parent View";
        }
    }

    @Route("bar")
    @PageTitle("Child View")
    private static class FaultyChildWithDuplicateTitle
            extends ParentWithDynamicTitle {
    }

    @Route("foo")
    @PageTitle("Parent View")
    private static class ParentWithTitleAnnotation extends Component
            implements HasDynamicTitle {

        @Override
        public String getPageTitle() {
            return "";
        }
    }

    @Route("bar")
    private static class ChildWithDynamicTitle
            extends ParentWithTitleAnnotation
            implements HasDynamicTitle {

        @Override
        public String getPageTitle() {
            return "Child View";
        }
    }
}

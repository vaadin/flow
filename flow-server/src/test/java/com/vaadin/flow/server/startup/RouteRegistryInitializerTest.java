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
package com.vaadin.flow.server.startup;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteAliasData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterTest.FileNotFound;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Unit tests for RouteRegistryInitializer and RouteRegistry.
 */
public class RouteRegistryInitializerTest {

    private RouteRegistryInitializer routeRegistryInitializer;
    private ApplicationRouteRegistry registry;
    private ServletContext servletContext;
    private VaadinServletContext vaadinContext;

    @Before
    public void init() {
        routeRegistryInitializer = new RouteRegistryInitializer();
        registry = new TestRouteRegistry();
        servletContext = Mockito.mock(ServletContext.class);
        vaadinContext = new VaadinServletContext(servletContext);
        registry = ApplicationRouteRegistry
                .getInstance(vaadinContext);

        Mockito.when(vaadinContext.getAttribute(
                ApplicationRouteRegistry.ApplicationRouteRegistryWrapper.class))
                .thenReturn(new ApplicationRouteRegistry.ApplicationRouteRegistryWrapper(registry));
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

    @Test(expected = ServletException.class)
    public void onStartUp_duplicate_routesViaAlias_throws()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTargetBar.class, NavigationTargetBar2.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void routeRegistry_fails_for_multiple_registration_of_same_route() {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(
                "Navigation targets must have unique routes, found navigation targets "
                        + "'com.vaadin.flow.server.startup.RouteRegistryInitializerTest$NavigationTargetFoo' and "
                        + "'com.vaadin.flow.server.startup.RouteRegistryInitializerTest$NavigationTargetFoo2' with the same route.");

        RouteConfiguration.forRegistry(registry)
                .setAnnotatedRoute(NavigationTargetFoo.class);

        Assert.assertTrue("RouteRegistry should be initialized",
                registry.hasNavigationTargets());

        // Test should fail on this as there already exists a route for this
        // route
        RouteConfiguration.forRegistry(registry)
                .setAnnotatedRoute(NavigationTargetFoo2.class);
    }

    @Test
    public void routeRegistry_registers_correctly_route_with_parentLayout()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream
                .of(NavigationTarget.class, NavigationTargetFoo.class,
                        MiddleParentWithRoute.class)
                .collect(Collectors.toSet()), servletContext);

        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget("middle_parent");

        Assert.assertTrue("Couldn't find navigation target for `middle_parent`",
                navigationTarget.isPresent());
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void routeRegistry_fails_on_aloneRouteAlias()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTarget.class, NavigationTargetFoo.class,
                        RouteAliasAlone.class).collect(Collectors.toSet()),
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
                Stream.of(AbsoluteRoute.class).collect(Collectors.toSet()),
                servletContext);

        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget("absolute");

        Assert.assertTrue("Could not find navigation target for `absolute`",
                navigationTarget.isPresent());
        Assert.assertEquals("Route 'absolute' was not registered correctly",
                AbsoluteRoute.class, navigationTarget.get());
    }

    @Test
    public void routeRegistry_route_with_absolute_parent_prefix_ignores_remaining_parent_route_prefixes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(MultiLevelRoute.class).collect(Collectors.toSet()),
                servletContext);

        assertRouteTarget(MultiLevelRoute.class, "absolute/levels",
                "Route 'absolute' was not registered correctly");
    }

    @Test
    public void routeRegistry_routeWithAlias_parentRoutePrefix()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        assertRouteTarget(MultiLevelRouteAlias.class, "absolute/levels",
                "Route 'absolute' was not registered correctly");

        Optional<String> url = registry
                .getTargetUrl(MultiLevelRouteAlias.class);

        Assert.assertTrue(url.isPresent());

        Assert.assertEquals("absolute/levels", url.get());

        assertRouteTarget(MultiLevelRouteAlias.class, "parent/alias1",
                "RouteAlias 'alias1' was not registered correctly");
    }

    @Test
    public void routeRegistry_routeWithAlias_parent_prefix_ignores_remaining_parent_route_prefixes()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);
        assertRouteTarget(MultiLevelRouteAlias.class, "absolute/alias2",
                "RouteAlias 'alias2' was not registered correctly");
    }

    @Test
    public void routeRegistry_routeWithAlias_absoluteRoute()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        assertRouteTarget(MultiLevelRouteAlias.class, "alias3",
                "RouteAlias 'alias3' was not registered correctly");
    }

    @Test
    public void routeRegistry_routeWithAlias_noParent()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        assertRouteTarget(MultiLevelRouteAlias.class, "alias4",
                "RouteAlias 'alias4' was not registered correctly");
    }

    @Test
    public void routeRegistry_routeWithAlias_twoParentLevels()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        assertRouteTarget(MultiLevelRouteAlias.class, "parent/middle/alias5",
                "RouteAlias 'alias5' was not registered correctly");
    }

    @Test
    public void routeRegistry_route_returns_registered_string_for_get_url()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream
                .of(NavigationTarget.class, NavigationTargetFoo.class,
                        AbsoluteRoute.class, ExtendingPrefix.class)
                .collect(Collectors.toSet()), servletContext);

        Assert.assertEquals("",
                registry.getTargetUrl(NavigationTarget.class).get());
        Assert.assertEquals("foo",
                registry.getTargetUrl(NavigationTargetFoo.class).get());
        Assert.assertEquals("absolute",
                registry.getTargetUrl(AbsoluteRoute.class).get());
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

    @Route("bar2")
    @RouteAlias("bar")
    private static class NavigationTargetBar2 extends Component {
    }

    private static class RouteParentLayout extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = ParentWithRoutePrefix.class)
    private static class NavigationRootWithParent extends Component {
    }

    @ParentLayout(RouteParentLayout.class)
    @Route(value = "middle_parent")
    private static class MiddleParentWithRoute extends Component {
    }

    @RouteAlias(value = "wrong")
    private static class RouteAliasAlone extends Component {
    }

    @RoutePrefix("parent")
    private static class ParentWithRoutePrefix extends Component
            implements RouterLayout {
    }

    @Route(value = "prefix", layout = ParentWithRoutePrefix.class)
    private static class ExtendingPrefix extends Component {
    }

    @Route(value = "absolute", layout = ParentWithRoutePrefix.class, absolute = true)
    private static class AbsoluteRoute extends Component {
    }

    @RoutePrefix(value = "absolute", absolute = true)
    @ParentLayout(ParentWithRoutePrefix.class)
    private static class AbsoluteMiddleParent extends Component
            implements RouterLayout {
    }

    @RoutePrefix(value = "middle")
    @ParentLayout(ParentWithRoutePrefix.class)
    private static class MiddleParent extends Component
            implements RouterLayout {
    }

    @Route(value = "levels", layout = AbsoluteMiddleParent.class)
    private static class MultiLevelRoute extends Component {
    }

    @Route(value = "levels", layout = AbsoluteMiddleParent.class)
    @RouteAlias(value = "alias1", layout = ParentWithRoutePrefix.class)
    @RouteAlias(value = "alias2", layout = AbsoluteMiddleParent.class)
    @RouteAlias(value = "alias3", absolute = true, layout = ParentWithRoutePrefix.class)
    @RouteAlias(value = "alias4")
    @RouteAlias(value = "alias5", layout = MiddleParent.class)
    private static class MultiLevelRouteAlias extends Component {
    }

    @Route("parameter")
    private static class ParameterRoute extends Component
            implements HasUrlParameter<Boolean> {

        @Override
        public void setParameter(BeforeEvent event, Boolean parameter) {

        }
    }

    @Route("string")
    private static class StringParameterRoute extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {

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
            extends ParentWithTitleAnnotation {

        @Override
        public String getPageTitle() {
            return "Child View";
        }
    }

    private void assertRouteTarget(Class<?> routeClass, String path,
            String errorMessage) {
        Optional<Class<? extends Component>> navigationTarget = registry
                .getNavigationTarget(path);

        Assert.assertTrue("Could not find navigation target for `" + path + "`",
                navigationTarget.isPresent());
        Assert.assertEquals(errorMessage, routeClass, navigationTarget.get());
    }

    /* @Viewport tests */

    @Route("single")
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class SingleNavigationTarget extends Component {
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    @Viewport("width=device-width")
    public static class MiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(MiddleParentLayout.class)
    @Viewport("width=device-width")
    public static class MultiMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class RootWithParent extends Component {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class RootViewportWithParent extends Component {
    }

    @Route(value = "", layout = MiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class RootWithParents extends Component {
    }

    @Route(value = "", layout = MultiMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class MultiViewport extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = Parent.class)
    @Tag(Tag.DIV)
    @Viewport("width=device-width")
    public static class FailingAliasView extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class AliasView extends Component {
    }

    @Test
    public void onStartUp_wrong_position_view_layout_throws()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Viewport annotation should be on the top most route layout '%s'. Offending class: '%s'",
                Parent.class.getName(), MiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(RootWithParents.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_only_one_viewport_in_route_chain()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(
                "Only one Viewport annotation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                        + MultiMiddleParentLayout.class.getName() + ", "
                        + MiddleParentLayout.class.getName());

        routeRegistryInitializer.onStartup(
                Stream.of(MultiViewport.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_viewport_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Viewport annotation needs to be on the top parent layout '%s' not on '%s'",
                Parent.class.getName(),
                RootViewportWithParent.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                .of(RootViewportWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_one_viewport_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(SingleNavigationTarget.class, RootWithParent.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Viewport annotation needs to be on the top parent layout '%s' not on '%s'",
                Parent.class.getName(), FailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(FailingAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_valid_alias_does_not_throw() throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(AliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    /* BodySize tests */

    @Route("single")
    @Tag(Tag.DIV)
    @BodySize(width = "100vw", height = "100vh")
    public static class BodySingleNavigationTarget extends Component {
    }

    @Tag(Tag.DIV)
    public static class BodyParent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(BodyParent.class)
    @BodySize(width = "100vw", height = "100vh")
    public static class BodyMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(BodyMiddleParentLayout.class)
    @BodySize(width = "100vw", height = "100vh")
    public static class BodyMultiMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = BodyParent.class)
    @Tag(Tag.DIV)
    public static class BodyRootWithParent extends Component {
    }

    @Route(value = "", layout = BodyParent.class)
    @Tag(Tag.DIV)
    @BodySize(width = "100vw", height = "100vh")
    public static class BodyRootViewportWithParent extends Component {
    }

    @Route(value = "", layout = BodyMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class BodyRootWithParents extends Component {
    }

    @Route(value = "", layout = BodyMultiMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class BodyMultiViewport extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = BodyParent.class)
    @Tag(Tag.DIV)
    @BodySize(width = "100vw", height = "100vh")
    public static class BodyFailingAliasView extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = BodyParent.class)
    @Tag(Tag.DIV)
    public static class BodyAliasView extends Component {
    }

    @Test
    public void onStartUp_wrong_position_body_size_view_layout_throws()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "BodySize annotation should be on the top most route layout '%s'. Offending class: '%s'",
                BodyParent.class.getName(),
                BodyMiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(Stream.of(BodyRootWithParents.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_check_only_one_body_size_in_route_chain()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(
                "Only one BodySize annotation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                        + BodyMultiMiddleParentLayout.class.getName() + ", "
                        + BodyMiddleParentLayout.class.getName());

        routeRegistryInitializer.onStartup(
                Stream.of(BodyMultiViewport.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_body_size_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "BodySize annotation needs to be on the top parent layout '%s' not on '%s'",
                BodyParent.class.getName(),
                BodyRootViewportWithParent.class.getName()));

        routeRegistryInitializer
                .onStartup(Stream.of(BodyRootViewportWithParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_one_body_size_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(BodySingleNavigationTarget.class,
                        BodyRootWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_body_size_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "BodySize annotation needs to be on the top parent layout '%s' not on '%s'",
                BodyParent.class.getName(),
                BodyFailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(Stream.of(BodyFailingAliasView.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_valid_body_size_alias_does_not_throw()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(BodyAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    /* PageConfigurator tests */

    @Route("single")
    @Tag(Tag.DIV)
    public static class SingleConfigurator extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Tag(Tag.DIV)
    public static class ParentConfigurator extends Component
            implements RouterLayout, PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    public static class MiddleParentConfigurator extends Component
            implements RouterLayout, PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Route(value = "", layout = MiddleParentConfigurator.class)
    @Tag(Tag.DIV)
    public static class RootWithMultipleParentConfigurator extends Component {
    }

    @Tag(Tag.DIV)
    @ParentLayout(MiddleParentConfigurator.class)
    public static class MultiMiddleParentConfigurator extends Component
            implements RouterLayout, PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Route(value = "", layout = MultiMiddleParentConfigurator.class)
    @Tag(Tag.DIV)
    public static class MultiConfigurator extends Component {
    }

    @Route(value = "", layout = ParentConfigurator.class)
    @Tag(Tag.DIV)
    public static class RootWithParentConfigurator extends Component {
    }

    @Route(value = "", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class RootConfiguratorWithParent extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Route("")
    @RouteAlias(value = "alias", layout = Parent.class)
    @Tag(Tag.DIV)
    public static class FailingAliasConfigurator extends Component
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
        }
    }

    @Test
    public void onStartUp_valid_page_configurator_does_not_throw()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(SingleConfigurator.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_wrong_position_page_configurator_throws()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "PageConfigurator implementation should be the top most route layout '%s'. Offending class: '%s'",
                Parent.class.getName(),
                MiddleParentConfigurator.class.getName()));

        routeRegistryInitializer
                .onStartup(Stream.of(RootWithMultipleParentConfigurator.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_check_only_one_page_configurator_in_route_chain()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(
                "Only one PageConfigurator implementation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                        + MultiMiddleParentConfigurator.class.getName() + ", "
                        + MiddleParentConfigurator.class.getName());

        routeRegistryInitializer.onStartup(
                Stream.of(MultiConfigurator.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_page_configurator_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "PageConfigurator needs to be the top parent layout '%s' not '%s'",
                Parent.class.getName(),
                RootConfiguratorWithParent.class.getName()));

        routeRegistryInitializer
                .onStartup(Stream.of(RootConfiguratorWithParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_one_page_configurator_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream
                .of(SingleConfigurator.class, RootWithParentConfigurator.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_check_page_configurator_for_faulty_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "PageConfigurator needs to be the top parent layout '%s' not '%s'",
                Parent.class.getName(),
                FailingAliasConfigurator.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                .of(FailingAliasConfigurator.class).collect(Collectors.toSet()),
                servletContext);
    }

    /* Inline tests */

    @Route("single")
    @Tag(Tag.DIV)
    @Inline(value = "inline.js", position = Inline.Position.PREPEND)
    @Inline("inline.css")
    public static class InlineSingleNavigationTarget extends Component {
    }

    @Tag(Tag.DIV)
    public static class InlineParent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(InlineParent.class)
    @Inline("inline.js")
    public static class InlineMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(InlineMiddleParentLayout.class)
    @Inline("inline.js")
    public static class InlineMultiMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = InlineParent.class)
    @Tag(Tag.DIV)
    public static class InlineRootWithParent extends Component {
    }

    @Route(value = "", layout = InlineParent.class)
    @Tag(Tag.DIV)
    @Inline("inline.js")
    public static class InlineRootViewportWithParent extends Component {
    }

    @Route(value = "", layout = InlineMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class InlineRootWithParents extends Component {
    }

    @Route(value = "", layout = InlineMultiMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class InlineMultiViewport extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = InlineParent.class)
    @Tag(Tag.DIV)
    @Inline("inline.js")
    public static class InlineFailingAliasView extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = InlineParent.class)
    @Tag(Tag.DIV)
    public static class InlineAliasView extends Component {
    }

    @Test
    public void onStartUp_wrong_position_inline_view_layout_throws()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Inline annotation should be on the top most route layout '%s'. Offending class: '%s'",
                InlineParent.class.getName(),
                InlineMiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                .of(InlineRootWithParents.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_only_one_inline_in_route_chain()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(
                "Only one Inline annotation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                        + InlineMultiMiddleParentLayout.class.getName() + ", "
                        + InlineMiddleParentLayout.class.getName());

        routeRegistryInitializer.onStartup(Stream.of(InlineMultiViewport.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_inline_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Inline annotation needs to be on the top parent layout '%s' not on '%s'",
                InlineParent.class.getName(),
                InlineRootViewportWithParent.class.getName()));

        routeRegistryInitializer
                .onStartup(Stream.of(InlineRootViewportWithParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_one_inline_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(InlineSingleNavigationTarget.class,
                        InlineRootWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_inline_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Inline annotation needs to be on the top parent layout '%s' not on '%s'",
                InlineParent.class.getName(),
                InlineFailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                .of(InlineFailingAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_valid_inline_alias_does_not_throw()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(InlineAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    /* RouteData tests */

    @Test
    public void routeData_returns_all_registered_routes()
            throws ServletException {
        Set<Class<?>> routes = Stream.of(NavigationTarget.class,
                NavigationTargetFoo.class, NavigationTargetBar.class)
                .collect(Collectors.toSet());
        routeRegistryInitializer.onStartup(routes, servletContext);
        List<RouteData> registeredRoutes = registry.getRegisteredRoutes();

        Assert.assertEquals("Not all registered routes were returned", 3,
                registeredRoutes.size());

        Set<Class<?>> collectedRoutes = registeredRoutes.stream()
                .map(RouteData::getNavigationTarget)
                .collect(Collectors.toSet());

        Assert.assertTrue("Not all targets were correct",
                routes.containsAll(collectedRoutes));
    }

    @Test
    public void routeData_gets_correct_urls_for_targets()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(NavigationTarget.class,
                NavigationRootWithParent.class, AbsoluteRoute.class,
                ExtendingPrefix.class, StringParameterRoute.class,
                ParameterRoute.class, MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        List<RouteData> registeredRoutes = registry.getRegisteredRoutes();

        Assert.assertEquals("Not all registered routes were returned", 7,
                registeredRoutes.size());

        // RouteData should be sorted by url
        Assert.assertEquals("Sort order was not the one expected", "",
                registeredRoutes.get(0).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "absolute",
                registeredRoutes.get(1).getUrl());
        Assert.assertEquals("Sort order was not the one expected",
                "absolute/levels", registeredRoutes.get(2).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "parameter",
                registeredRoutes.get(3).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "parent",
                registeredRoutes.get(4).getUrl());
        Assert.assertEquals("Sort order was not the one expected",
                "parent/prefix", registeredRoutes.get(5).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "string",
                registeredRoutes.get(6).getUrl());
    }

    @Test
    public void routeData_gets_correct_parents_for_targets()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(NavigationTarget.class,
                NavigationRootWithParent.class, AbsoluteRoute.class,
                ExtendingPrefix.class, StringParameterRoute.class,
                ParameterRoute.class, MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        List<RouteData> registeredRoutes = registry.getRegisteredRoutes();

        Assert.assertEquals("Not all registered routes were returned", 7,
                registeredRoutes.size());

        Assert.assertEquals("Parent is wrongly set to data",
                Collections.emptyList(),
                registeredRoutes.get(0).getParentLayouts());
        Assert.assertEquals("Parent is wrongly set to data",
                ParentWithRoutePrefix.class,
                registeredRoutes.get(1).getParentLayout());
        Assert.assertEquals("Parent is wrongly set to data",
                AbsoluteMiddleParent.class,
                registeredRoutes.get(2).getParentLayout());
        Assert.assertEquals("Parent is wrongly set to data",
                Collections.emptyList(),
                registeredRoutes.get(3).getParentLayouts());
        Assert.assertEquals("Parent is wrongly set to data",
                ParentWithRoutePrefix.class,
                registeredRoutes.get(4).getParentLayout());
        Assert.assertEquals("Parent is wrongly set to data",
                ParentWithRoutePrefix.class,
                registeredRoutes.get(5).getParentLayout());
        Assert.assertEquals("Parent is wrongly set to data",
                Collections.emptyList(),
                registeredRoutes.get(6).getParentLayouts());

    }

    @Test
    public void routeData_gets_correct_parameters_for_targets()
            throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(NavigationTarget.class,
                NavigationRootWithParent.class, AbsoluteRoute.class,
                ExtendingPrefix.class, StringParameterRoute.class,
                ParameterRoute.class, MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        List<RouteData> registeredRoutes = registry.getRegisteredRoutes();

        Assert.assertEquals("Not all registered routes were returned", 7,
                registeredRoutes.size());

        Assert.assertEquals("Unexpected parameters encountered", 0,
                registeredRoutes.get(0).getParameters().size());
        Assert.assertEquals("Unexpected parameters encountered", 0,
                registeredRoutes.get(1).getParameters().size());
        Assert.assertEquals("Unexpected parameters encountered", 0,
                registeredRoutes.get(2).getParameters().size());
        Assert.assertEquals("Missing parameters", 1,
                registeredRoutes.get(3).getParameters().size());

        Assert.assertEquals("Unexpected parameters encountered", 0,
                registeredRoutes.get(4).getParameters().size());
        Assert.assertEquals("Unexpected parameters encountered", 0,
                registeredRoutes.get(5).getParameters().size());
        Assert.assertEquals("Missing parameters", 1,
                registeredRoutes.get(6).getParameters().size());

        Assert.assertEquals("Unexpected parameter type encountered",
                Boolean.class, registeredRoutes.get(3).getParameters().get(0));
        Assert.assertEquals("Unexpected parameter type encountered",
                String.class, registeredRoutes.get(6).getParameters().get(0));
    }

    @Test
    public void routeData_for_alias_data_is_correct() throws ServletException {
        routeRegistryInitializer.onStartup(Stream.of(MultiLevelRouteAlias.class)
                .collect(Collectors.toSet()), servletContext);

        List<RouteData> registeredRoutes = registry.getRegisteredRoutes();

        Assert.assertEquals("Not all registered routes were returned", 1,
                registeredRoutes.size());

        RouteData routeData = registeredRoutes.get(0);
        Assert.assertEquals("Not all registered routes were returned", 5,
                routeData.getRouteAliases().size());

        List<RouteAliasData> routeAliases = routeData.getRouteAliases();

        Assert.assertEquals("Sort order was not the one expected",
                "absolute/alias2", routeAliases.get(0).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "alias3",
                routeAliases.get(1).getUrl());
        Assert.assertEquals("Sort order was not the one expected", "alias4",
                routeAliases.get(2).getUrl());
        Assert.assertEquals("Sort order was not the one expected",
                "parent/alias1", routeAliases.get(3).getUrl());
        Assert.assertEquals("Sort order was not the one expected",
                "parent/middle/alias5", routeAliases.get(4).getUrl());

        Assert.assertEquals("Sort order was not the one expected",
                AbsoluteMiddleParent.class,
                routeAliases.get(0).getParentLayout());
        Assert.assertEquals("Sort order was not the one expected",
                ParentWithRoutePrefix.class,
                routeAliases.get(1).getParentLayout());
        Assert.assertEquals("Sort order was not the one expected",
                Collections.emptyList(),
                routeAliases.get(2).getParentLayouts());
        Assert.assertEquals("Sort order was not the one expected",
                ParentWithRoutePrefix.class,
                routeAliases.get(3).getParentLayout());
        Assert.assertEquals("Sort order was not the one expected",
                MiddleParent.class, routeAliases.get(4).getParentLayout());
    }

    /* Theme tests */
    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "src/";
        }

        @Override
        public String getThemeUrl() {
            return "theme/myTheme/";
        }
    }

    @Route("single")
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeSingleNavigationTarget extends Component {
    }

    @Tag(Tag.DIV)
    public static class ThemeParent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(ThemeParent.class)
    @Theme(MyTheme.class)
    public static class ThemeMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @ParentLayout(ThemeMiddleParentLayout.class)
    @Theme(MyTheme.class)
    public static class ThemeMultiMiddleParentLayout extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = ThemeParent.class)
    @Tag(Tag.DIV)
    public static class ThemeRootWithParent extends Component {
    }

    @Route(value = "", layout = ThemeParent.class)
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeRootViewportWithParent extends Component {
    }

    @Route(value = "", layout = ThemeMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class ThemeRootWithParents extends Component {
    }

    @Route(value = "", layout = ThemeMultiMiddleParentLayout.class)
    @Tag(Tag.DIV)
    public static class ThemeMultiViewport extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = ThemeParent.class)
    @Tag(Tag.DIV)
    @Theme(MyTheme.class)
    public static class ThemeFailingAliasView extends Component {
    }

    @Route("")
    @RouteAlias(value = "alias", layout = ThemeParent.class)
    @Tag(Tag.DIV)
    public static class ThemeAliasView extends Component {
    }

    @Test
    public void onStartUp_wrong_position_theme_view_layout_throws()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Theme annotation should be on the top most route layout '%s'. Offending class: '%s'",
                ThemeParent.class.getName(),
                ThemeMiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(Stream.of(ThemeRootWithParents.class)
                .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_check_only_one_theme_in_route_chain()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(
                "Only one Theme annotation is supported for navigation chain and should be on the top most level. Offending classes in chain: "
                        + ThemeMultiMiddleParentLayout.class.getName() + ", "
                        + ThemeMiddleParentLayout.class.getName());

        routeRegistryInitializer.onStartup(
                Stream.of(ThemeMultiViewport.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_theme_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Theme annotation needs to be on the top parent layout '%s' not on '%s'",
                ThemeParent.class.getName(),
                ThemeRootViewportWithParent.class.getName()));

        routeRegistryInitializer
                .onStartup(Stream.of(ThemeRootViewportWithParent.class)
                        .collect(Collectors.toSet()), servletContext);
    }

    @Test
    public void onStartUp_one_theme_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(ThemeSingleNavigationTarget.class,
                        ThemeRootWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_theme_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Theme annotation needs to be on the top parent layout '%s' not on '%s'",
                ThemeParent.class.getName(),
                ThemeFailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                .of(ThemeFailingAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_valid_theme_alias_does_not_throw()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(ThemeAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Route("ignored")
    public static class IgnoredView extends Component {
    }

    public static class TestRouteFilter implements NavigationTargetFilter {
        @Override
        public boolean testNavigationTarget(
                Class<? extends Component> navigationTarget) {
            return !navigationTarget.getSimpleName().startsWith("Ignored");
        }

        @Override
        public boolean testErrorNavigationTarget(
                Class<?> errorNavigationTarget) {
            return !errorNavigationTarget.getSimpleName().startsWith("Ignored");
        }
    }

    // An additional filter to test that it's not enough to have only one
    // passing filter
    public static class AlwaysTrueRouterFilter
            implements NavigationTargetFilter {
        @Override
        public boolean testNavigationTarget(
                Class<? extends Component> navigationTarget) {
            return true;
        }

        @Override
        public boolean testErrorNavigationTarget(
                Class<?> errorNavigationTarget) {
            return true;
        }
    }

    @Test
    public void routeFilter_ignoresRoutes()
            throws InvalidRouteConfigurationException, ServletException {
        routeRegistryInitializer
                .onStartup(Stream.of(IgnoredView.class, NavigationTarget.class)
                        .collect(Collectors.toSet()), servletContext);

        List<?> registeredTargets = registry.getRegisteredRoutes().stream()
                .map(RouteData::getNavigationTarget)
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(NavigationTarget.class),
                registeredTargets);
    }

    public static class IgnoredErrorView extends Component
            implements HasErrorParameter<Exception> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<Exception> parameter) {
            return 0;
        }
    }

    @Test
    public void routeFilter_ignoresErrorTargets()
            throws InvalidRouteConfigurationException {
        registry.setErrorNavigationTargets(
                Stream.of(IgnoredErrorView.class, FileNotFound.class)
                        .collect(Collectors.toSet()));

        Assert.assertTrue(registry
                .getErrorNavigationTarget(new NotFoundException()).isPresent());

        ErrorTargetEntry errorTargetEntry = registry
                .getErrorNavigationTarget(new Exception()).get();

        Assert.assertNotEquals(IgnoredErrorView.class,
                errorTargetEntry.getNavigationTarget());
    }

    @Test
    public void registerClassesWithSameRoute_abstractClass_subclass_subclassIsRegistered()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(AbstractRouteTarget.class);
        classes.add(BaseRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);

        Assert.assertEquals(BaseRouteTarget.class,
                registry.getNavigationTarget("foo").get());
    }

    @Test
    public void registerClassesWithSameRoute_class_abstractSuperClass_subclassIsRegistered()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(BaseRouteTarget.class);
        classes.add(AbstractRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);

        Assert.assertEquals(BaseRouteTarget.class,
                registry.getNavigationTarget("foo").get());
    }

    @Test
    public void registerClassesWithSameRoute_class_subclass_subclassIsRegistered()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(BaseRouteTarget.class);
        classes.add(SuperRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);

        Assert.assertEquals(SuperRouteTarget.class,
                registry.getNavigationTarget("foo").get());
    }

    @Test
    public void registerClassesWithSameRoute_class_superClass_subclassIsRegistered()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(SuperRouteTarget.class);
        classes.add(BaseRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);

        Assert.assertEquals(SuperRouteTarget.class,
                registry.getNavigationTarget("foo").get());
    }

    @Test(expected = ServletException.class)
    public void registerClassesWithSameRoute_absatrctClass_unrelatedClass_throws()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(AbstractRouteTarget.class);
        classes.add(OtherRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);
    }

    @Test(expected = ServletException.class)
    public void registerClassesWithSameRoute_unrelatedClass_abstractClass_throws()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(OtherRouteTarget.class);
        classes.add(AbstractRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);
    }

    @Test(expected = ServletException.class)
    public void registerClassesWithSameRoute_class_unrelatedClass_throws()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(BaseRouteTarget.class);
        classes.add(OtherRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);
    }

    @Test(expected = ServletException.class)
    public void registerClassesWithSameRoute_unrelatedClass_class_throws()
            throws ServletException {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(OtherRouteTarget.class);
        classes.add(BaseRouteTarget.class);
        routeRegistryInitializer.onStartup(classes, servletContext);
    }

    @Tag(Tag.DIV)
    @Route("foo")
    private abstract static class AbstractRouteTarget extends Component {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    private static class BaseRouteTarget extends AbstractRouteTarget {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    private static class SuperRouteTarget extends BaseRouteTarget {

    }

    @Tag(Tag.DIV)
    @Route("foo")
    private static class OtherRouteTarget extends Component {

    }
}

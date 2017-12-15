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

import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.PageTitle;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouteAlias;
import com.vaadin.router.RoutePrefix;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.TestRouteRegistry;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.BodySize;
import com.vaadin.ui.Component;
import com.vaadin.ui.Inline;
import com.vaadin.ui.Tag;
import com.vaadin.ui.Viewport;

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

    @Test(expected = ServletException.class)
    public void onStartUp_duplicate_routesViaAlias_throws()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(NavigationTargetBar.class, NavigationTargetBar2.class)
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
    @Route(value = "wrong")
    private static class FaultyConfiguration extends Component {
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
    private static class AbosulteRoute extends Component {
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
                BodyParent.class.getName(), BodyMiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(BodyRootWithParents.class).collect(Collectors.toSet()),
                servletContext);
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

        routeRegistryInitializer.onStartup(Stream
                .of(BodyRootViewportWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_one_body_size_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(BodySingleNavigationTarget.class, BodyRootWithParent.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_body_size_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "BodySize annotation needs to be on the top parent layout '%s' not on '%s'",
                BodyParent.class.getName(), BodyFailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(BodyFailingAliasView.class).collect(Collectors.toSet()),
                servletContext);
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
                InlineParent.class.getName(), InlineMiddleParentLayout.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(InlineRootWithParents.class).collect(Collectors.toSet()),
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

        routeRegistryInitializer.onStartup(
                Stream.of(InlineMultiViewport.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_route_can_not_contain_inline_if_has_parent()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Inline annotation needs to be on the top parent layout '%s' not on '%s'",
                InlineParent.class.getName(),
                InlineRootViewportWithParent.class.getName()));

        routeRegistryInitializer.onStartup(Stream
                        .of(InlineRootViewportWithParent.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_one_inline_in_chain_and_one_for_route_passes()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(InlineSingleNavigationTarget.class, InlineRootWithParent.class)
                        .collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_check_also_faulty_inline_alias_route()
            throws ServletException {
        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Inline annotation needs to be on the top parent layout '%s' not on '%s'",
                InlineParent.class.getName(), InlineFailingAliasView.class.getName()));

        routeRegistryInitializer.onStartup(
                Stream.of(InlineFailingAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

    @Test
    public void onStartUp_valid_inline_alias_does_not_throw()
            throws ServletException {
        routeRegistryInitializer.onStartup(
                Stream.of(InlineAliasView.class).collect(Collectors.toSet()),
                servletContext);
    }

}

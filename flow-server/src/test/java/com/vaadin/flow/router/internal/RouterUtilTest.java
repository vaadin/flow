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
package com.vaadin.flow.router.internal;

import java.util.List;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * Test that {@link RouterUtil} route resolving works as intended for both
 * simple and complex cases.
 */
public class RouterUtilTest {

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("parent")
    public static class RoutePrefixParent extends Component
            implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("aliasparent")
    public static class RouteAliasPrefixParent extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    public static class BaseRouteWithParentPrefixAndRouteAlias
            extends Component {
    }

    @Route(value = "flow", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    public static class RouteWithParentPrefixAndRouteAlias extends Component {
    }

    @Route(value = "flow", layout = RoutePrefixParent.class)
    @RouteAlias(value = "alias", layout = RouteAliasPrefixParent.class)
    @Tag(Tag.DIV)
    public static class RouteAliasWithParentPrefix extends Component {
    }

    @Route(value = "single", layout = RoutePrefixParent.class, absolute = true)
    @RouteAlias(value = "alias", layout = RoutePrefixParent.class, absolute = true)
    public static class AbsoluteRoute extends Component {
    }

    @Tag(Tag.DIV)
    @ParentLayout(RoutePrefixParent.class)
    @RoutePrefix(value = "absolute", absolute = true)
    public static class AbsoluteCenterParent extends Component
            implements RouterLayout {
    }

    @Route(value = "child", layout = AbsoluteCenterParent.class)
    @RouteAlias(value = "alias", layout = AbsoluteCenterParent.class)
    public static class AbsoluteCenterRoute extends Component {
    }

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    public static class MiddleParent extends Component implements RouterLayout {
    }

    @Route(value = "", layout = MiddleParent.class)
    @Tag(Tag.DIV)
    public static class RootWithParents extends Component {
    }

    @Test
    public void route_path_should_contain_parent_prefix() {
        String routePath = RouterUtil.getRoutePath(
                BaseRouteWithParentPrefixAndRouteAlias.class,
                BaseRouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(Route.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent", routePath);
    }

    @Test
    public void absolute_route_should_not_contain_parent_prefix() {
        String routePath = RouterUtil.getRoutePath(AbsoluteRoute.class,
                AbsoluteRoute.class.getAnnotation(Route.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "single", routePath);
    }

    @Test
    public void absolute_middle_parent_route_should_not_contain_parent_prefix() {
        String routePath = RouterUtil.getRoutePath(AbsoluteRoute.class,
                AbsoluteCenterRoute.class.getAnnotation(Route.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/child", routePath);
    }

    @Test
    public void absolute_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouterUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteRoute.class.getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.", "alias",
                routePath);
    }

    @Test
    public void absolute_middle_parent_for_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouterUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteCenterRoute.class.getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/alias", routePath);
    }

    @Test
    public void route_path_should_contain_route_and_parent_prefix() {
        String routePath = RouterUtil.getRoutePath(
                RouteWithParentPrefixAndRouteAlias.class,
                RouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(Route.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent/flow", routePath);
    }

    @Test
    public void route_alias_path_should_not_contain_parent_prefix() {
        String routePath = RouterUtil.getRouteAliasPath(
                BaseRouteWithParentPrefixAndRouteAlias.class,
                BaseRouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
        routePath = RouterUtil.getRouteAliasPath(
                RouteWithParentPrefixAndRouteAlias.class,
                RouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
    }

    @Test
    public void route_alias_should_contain_parent_prefix() {
        String routePath = RouterUtil.getRouteAliasPath(
                RouteAliasWithParentPrefix.class,
                RouteAliasWithParentPrefix.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "aliasparent/alias", routePath);
    }

    @Test
    public void top_parent_layout_should_be_found_for_base_route() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_should_be_found_for_non_base_route() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                RouteWithParentPrefixAndRouteAlias.class, "parent/flow");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void no_top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                BaseRouteWithParentPrefixAndRouteAlias.class, "alias");

        Assert.assertNull("Found parent for RouteAlias without parent.",
                parent);
    }

    @Test
    public void top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                RouteAliasWithParentPrefix.class, "aliasparent/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RouteAliasPrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route() {
        Class<? extends RouterLayout> parent = RouterUtil
                .getTopParentLayout(AbsoluteRoute.class, "single");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_parent() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                AbsoluteCenterRoute.class, "absolute/child");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias() {
        Class<? extends RouterLayout> parent = RouterUtil
                .getTopParentLayout(AbsoluteRoute.class, "alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias_parent() {
        Class<? extends RouterLayout> parent = RouterUtil.getTopParentLayout(
                AbsoluteCenterRoute.class, "absolute/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void expected_parent_layouts_are_found_for_route() {
        List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                .getParentLayouts(BaseRouteWithParentPrefixAndRouteAlias.class);

        Assert.assertThat(
                "Shorthand for @Route parent layouts gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouterUtil.getParentLayouts(
                BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouterUtil.getParentLayouts(RootWithParents.class, "");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { MiddleParent.class, Parent.class }));
    }

    @Test
    public void expected_parent_layouts_are_found_for_route_alias() {
        List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                .getParentLayouts(RouteAliasWithParentPrefix.class,
                        "aliasparent/alias");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { RouteAliasPrefixParent.class }));
    }

    @Test
    public void abolute_route_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                .getParentLayouts(AbsoluteRoute.class);

        Assert.assertThat(
                "Shorthand for @Route parent layouts gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouterUtil.getParentLayouts(AbsoluteRoute.class,
                "single");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouterUtil.getParentLayouts(AbsoluteCenterRoute.class,
                "absolute/child");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));
    }

    @Test
    public void abolute_route_alias_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouterUtil
                .getParentLayouts(AbsoluteRoute.class, "alias");

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouterUtil.getParentLayouts(AbsoluteCenterRoute.class,
                "absolute/alias");

        Assert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));

    }

}

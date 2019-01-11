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
package com.vaadin.flow.router.internal;

import java.util.List;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

/**
 * Test that {@link RouteUtil} route resolving works as intended for both simple
 * and complex cases.
 */
public class RouteUtilTest {

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

    @Tag(Tag.DIV)
    @ParentLayout(Parent.class)
    public static class NonRouteTargetWithParents extends Component {
    }

    @Route(value = "", layout = Parent.class)
    @RouteAlias(value = "alias", layout = MiddleParent.class)
    @Tag(Tag.DIV)
    @ParentLayout(RoutePrefixParent.class)
    public static class MultiTarget extends Component implements RouterLayout {
    }

    @Route(value = "sub", layout = MultiTarget.class)
    @Tag(Tag.DIV)
    public static class SubLayout extends Component {
    }

    @Test
    public void top_parent_layout_should_be_found_for_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_should_be_found_for_non_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                RouteWithParentPrefixAndRouteAlias.class, "parent/flow");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void no_top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                BaseRouteWithParentPrefixAndRouteAlias.class, "alias");

        Assert.assertNull("Found parent for RouteAlias without parent.",
                parent);
    }

    @Test
    public void top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                RouteAliasWithParentPrefix.class, "aliasparent/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RouteAliasPrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route() {
        Class<? extends RouterLayout> parent = RouteUtil
                .getTopParentLayout(AbsoluteRoute.class, "single");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                AbsoluteCenterRoute.class, "absolute/child");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil
                .getTopParentLayout(AbsoluteRoute.class, "alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                AbsoluteCenterRoute.class, "absolute/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void expected_to_get_parent_layout() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayoutsForNonRouteTarget(
                        NonRouteTargetWithParents.class);

        Assert.assertEquals("Expected one parent layout", 1,
                parentLayouts.size());

        Assert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { Parent.class }));
    }

    @Test
    public void also_non_routes_can_be_used_to_get_top_parent_layout() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(MiddleParent.class, null);
        Assert.assertEquals(
                "Middle parent should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);
    }

    @Test // 3424
    public void top_layout_resolves_correctly_for_route_parent() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(MultiTarget.class, "");
        Assert.assertEquals(
                "@Route path should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);

        topParentLayout = RouteUtil.getTopParentLayout(MultiTarget.class,
                "alias");
        Assert.assertEquals(
                "@RouteAlias path should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);

        topParentLayout = RouteUtil.getTopParentLayout(SubLayout.class,
                "parent/sub");
        Assert.assertEquals(
                "SubLayout using MultiTarget as parent should have gotten RoutePrefixParent as top parent layout",
                RoutePrefixParent.class, topParentLayout);

    }

}

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
package com.vaadin.flow.router.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.BundleUtils;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

/**
 * Test that {@link RouteUtil} route resolving works as intended for both simple
 * and complex cases.
 */
public class RouteUtilTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

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

    @Tag(Tag.DIV)
    @Layout
    private static class AutoLayout extends Component implements RouterLayout {
    }

    @Route(value = "auto")
    @RouteAlias(value = "alias", autoLayout = false)
    @RouteAlias(value = "mainLayout", layout = AutoLayout.class)
    @RouteAlias(value = "autoAlias")
    @Tag(Tag.DIV)
    public static class AutoLayoutView extends Component {
    }

    @Test
    public void route_path_should_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class);
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent", routePath);
    }

    @Test
    public void absolute_route_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                AbsoluteRoute.class);
        Assert.assertEquals("No parent prefix should have been added.",
                "single", routePath);
    }

    @Test
    public void absolute_middle_parent_route_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                AbsoluteCenterRoute.class);
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/child", routePath);
    }

    @Test
    public void absolute_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteRoute.class.getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.", "alias",
                routePath);
    }

    @Test
    public void absolute_middle_parent_for_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteCenterRoute.class.getAnnotation(RouteAlias.class));
        Assert.assertEquals("No parent prefix should have been added.",
                "absolute/alias", routePath);
    }

    @Test
    public void route_path_should_contain_route_and_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                RouteWithParentPrefixAndRouteAlias.class);
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "parent/flow", routePath);
    }

    @Test
    public void route_alias_path_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(
                BaseRouteWithParentPrefixAndRouteAlias.class,
                BaseRouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
        routePath = RouteUtil.getRouteAliasPath(
                RouteWithParentPrefixAndRouteAlias.class,
                RouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "alias", routePath);
    }

    @Test
    public void route_alias_should_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(
                RouteAliasWithParentPrefix.class,
                RouteAliasWithParentPrefix.class
                        .getAnnotation(RouteAlias.class));
        Assert.assertEquals(
                "Expected path should only have been parent RoutePrefix",
                "aliasparent/alias", routePath);
    }

    @Test
    public void top_parent_layout_should_be_found_for_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_should_be_found_for_non_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                RouteWithParentPrefixAndRouteAlias.class, "parent/flow");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void no_top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class, "alias");

        Assert.assertNull("Found parent for RouteAlias without parent.",
                parent);
    }

    @Test
    public void top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), RouteAliasWithParentPrefix.class,
                "aliasparent/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RouteAliasPrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteRoute.class, "single");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteCenterRoute.class,
                "absolute/child");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteRoute.class, "alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void top_parent_layout_for_absolute_route_alias_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteCenterRoute.class,
                "absolute/alias");

        Assert.assertNotNull("Didn't find any parent for route", parent);
        Assert.assertEquals("Received wrong parent class.",
                RoutePrefixParent.class, parent);
    }

    @Test
    public void automaticLayoutShouldBeAvailableForDefaultRoute() {

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setLayout(AutoLayout.class);

        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(registry, AutoLayoutView.class, "auto");

        Assert.assertEquals(
                "Route with no layout should not get automatic layout", 0,
                parentLayouts.size());
        Assert.assertTrue(
                RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "auto"));
    }

    @Test
    public void routeAliasForAutoLayoutRoute_correctAliasIsSelectedForRoute() {

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setLayout(AutoLayout.class);

        Assert.assertTrue(
                RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "auto"));
        Assert.assertFalse("'alias' route has autolayout false",
                RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "alias"));
        Assert.assertFalse("'mainLayout' has a defined layout", RouteUtil
                .isAutolayoutEnabled(AutoLayoutView.class, "mainLayout"));
        Assert.assertTrue(RouteUtil.isAutolayoutEnabled(AutoLayoutView.class,
                "autoAlias"));
    }

    @Test
    public void expected_parent_layouts_are_found_for_route() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(),
                        BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteUtil.getParentLayouts(new MockVaadinContext(),
                RootWithParents.class, "");

        MatcherAssert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { MiddleParent.class, Parent.class }));
    }

    @Test
    public void expected_to_get_parent_layout() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayoutsForNonRouteTarget(
                        NonRouteTargetWithParents.class);

        Assert.assertEquals("Expected one parent layout", 1,
                parentLayouts.size());

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { Parent.class }));
    }

    @Test
    public void expected_parent_layouts_are_found_for_route_alias() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(),
                        RouteAliasWithParentPrefix.class, "aliasparent/alias");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { RouteAliasPrefixParent.class }));
    }

    @Test
    public void absolute_route_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(), AbsoluteRoute.class,
                        "single");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteUtil.getParentLayouts(new MockVaadinContext(),
                AbsoluteCenterRoute.class, "absolute/child");

        MatcherAssert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));
    }

    @Test
    public void abolute_route_alias_gets_expected_parent_layouts() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(), AbsoluteRoute.class,
                        "alias");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { RoutePrefixParent.class }));

        parentLayouts = RouteUtil.getParentLayouts(new MockVaadinContext(),
                AbsoluteCenterRoute.class, "absolute/alias");

        MatcherAssert.assertThat(
                "Expected to receive MiddleParent and Parent classes as parents.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        AbsoluteCenterParent.class, RoutePrefixParent.class }));

    }

    @Test
    public void also_non_routes_can_be_used_to_get_top_parent_layout() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(new MockVaadinContext(), MiddleParent.class,
                        null);
        Assert.assertEquals(
                "Middle parent should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);
    }

    @Test // 3424
    public void top_layout_resolves_correctly_for_route_parent() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(new MockVaadinContext(), MultiTarget.class,
                        "");
        Assert.assertEquals(
                "@Route path should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);

        topParentLayout = RouteUtil.getTopParentLayout(new MockVaadinContext(),
                MultiTarget.class, "alias");
        Assert.assertEquals(
                "@RouteAlias path should have gotten Parent as top parent layout",
                Parent.class, topParentLayout);

        topParentLayout = RouteUtil.getTopParentLayout(new MockVaadinContext(),
                SubLayout.class, "parent/sub");
        Assert.assertEquals(
                "SubLayout using MultiTarget as parent should have gotten RoutePrefixParent as top parent layout",
                RoutePrefixParent.class, topParentLayout);

    }

    @Test // 3424
    public void parent_layouts_resolve_correctly_for_route_parent() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(), MultiTarget.class,
                        "");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { Parent.class }));

        parentLayouts = RouteUtil.getParentLayouts(new MockVaadinContext(),
                MultiTarget.class, "alias");

        MatcherAssert.assertThat(
                "Get parent layouts for routeAlias \"alias\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { MiddleParent.class, Parent.class }));

        parentLayouts = RouteUtil.getParentLayouts(new MockVaadinContext(),
                SubLayout.class, "parent/sub");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"parent/sub\" with parent Route + ParentLayout gave wrong result.",
                parentLayouts,
                IsIterableContainingInOrder.contains(new Class[] {
                        MultiTarget.class, RoutePrefixParent.class }));
    }

    @Test
    public void newRouteAnnotatedClass_updateRouteRegistry_routeIsAddedToRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newComponentClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
        // given
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newLazyRouteAnnotatedClass_updateRouteRegistry_routeIsNotAddedToRegistry() {
        // given
        @Route(value = "a", registerAtStartup = false)
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newLazyRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
        // given
        @Route(value = "a", registerAtStartup = false)
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newRouteComponentWithoutRouteAnnotation_updateRouteRegistry_routeIsNotAddedToRegistry() {
        // given
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void deletedRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void deletedNotAnnotatedRouteClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void deletedRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void deletedNotAnnotatedRouteClass_sessionRegistry_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void renamedRouteAnnotatedClass_updateRouteRegistry_routeIsUpdatedInRegistry() {
        // given
        @Route("aa")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
        Assert.assertTrue(registry.getConfiguration().hasRoute("aa"));
    }

    @Test
    public void changedAliasesRouteAnnotatedClass_updateRouteRegistry_routeIsUpdatedInRegistry() {
        // given
        @Route("a")
        @RouteAlias("alias-new")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
        registry.setRoute("alias", A.class, Collections.emptyList());
        Assert.assertTrue(registry.getConfiguration().hasRoute("alias"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
        Assert.assertTrue(registry.getConfiguration().hasRoute("alias-new"));
        Assert.assertFalse(registry.getConfiguration().hasRoute("alias"));
    }

    @Test
    public void changedToLazyRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedInRegistry() {
        // given
        @Route(value = "a", registerAtStartup = false)
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        // simulate a automatic registration with registerAtStartup=true
        mutableRoutesMap(registry);
        registry.getConfiguration().getRoutesMap().computeIfPresent("a",
                (path, routeTarget) -> new MockRouteTarget(routeTarget, true));
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void changedFromLazyRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedInRegistry() {
        // given
        @Route(value = "a")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        // simulate a manual registration with registerAtStartup=false
        mutableRoutesMap(registry);
        registry.getConfiguration().getRoutesMap().computeIfPresent("a",
                (path, routeTarget) -> new MockRouteTarget(routeTarget, false));
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void modifiedLazyRouteAnnotatedClass_updateRouteRegistry_existingRoutesArePreserved() {
        // given
        @Route(value = "a", registerAtStartup = false)
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        // simulate a manual registration with registerAtStartup=false
        mutableRoutesMap(registry);
        registry.getConfiguration().getRoutesMap().computeIfPresent("a",
                (path, routeTarget) -> new MockRouteTarget(routeTarget, false));

        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void deannotatedRouteClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        // simulate an automatic registration with registerAtStartup=true
        mutableRoutesMap(registry);
        registry.getConfiguration().getRoutesMap().computeIfPresent("a",
                (path, routeTarget) -> new MockRouteTarget(routeTarget, true));
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        Assert.assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    // Hotswap agent may fire CREATE, MODIFY and REMOVE events for a class
    // change. MODIFY wins over CREATE and REMOVE

    @Test
    public void routeAnnotatedClassAddedModifiedAndRemoved_updateRouteRegistry_routeIsAddedToRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.singleton(A.class), Collections.singleton(A.class));

        // then
        Assert.assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    public void newLayoutAnnotatedComponent_updateRouteRegistry_routeIsUpdated() {
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.update(() -> {
            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(registry);
            routeConfiguration.setAnnotatedRoute(AutoLayoutView.class);
        });
        Assert.assertFalse("AutoLayout should not be available",
                registry.hasLayout("auto"));

        RouteUtil.updateRouteRegistry(registry,
                Collections.singleton(AutoLayout.class), Collections.emptySet(),
                Collections.emptySet());

        Assert.assertTrue("AutoLayout should be available",
                registry.hasLayout("auto"));
    }

    @Test
    public void removeAnnotationsFromLayoutAnnotatedComponent_updateRouteRegistry_routeIsUpdated() {

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        class A extends Component implements RouterLayout {
        }
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        tamperLayouts(registry, layouts -> {
            layouts.put("/", A.class);
        });
        registry.update(() -> {
            RouteConfiguration.forRegistry(registry)
                    .setAnnotatedRoute(AutoLayoutView.class);
        });
        Assert.assertTrue("AutoLayout should be available",
                registry.hasLayout("auto"));

        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        Assert.assertFalse("AutoLayout should not be available anymore",
                registry.hasLayout("auto"));
    }

    @Test
    public void layoutAnnotatedComponent_modifiedValue_updateRouteRegistry_routeIsUpdated() {

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };

        @Route("hey/view")
        class View extends Component {

        }
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        tamperLayouts(registry, layouts -> {
            layouts.put("/hey", AutoLayout.class);
        });
        registry.update(() -> {
            RouteConfiguration routeConfiguration = RouteConfiguration
                    .forRegistry(registry);
            routeConfiguration.setAnnotatedRoute(AutoLayoutView.class);
            routeConfiguration.setAnnotatedRoute(View.class);
        });

        Assert.assertTrue("AutoLayout should be available for /hey/view path",
                registry.hasLayout("hey/view"));
        Assert.assertFalse("AutoLayout should not be available for /auto path",
                registry.hasLayout("auto"));

        RouteUtil.updateRouteRegistry(registry,
                Collections.singleton(AutoLayout.class), Collections.emptySet(),
                Collections.emptySet());

        Assert.assertTrue(
                "AutoLayout should still be available anymore for /hey/view path because path matches",
                registry.hasLayout("hey/view"));
        Assert.assertTrue("AutoLayout should now be available for /auto path",
                registry.hasLayout("auto"));
    }

    @Test
    public void clientHasMappedLayout_validateNoClientRouteCollisions() {
        Map<String, AvailableViewInfo> clientRoutes = new HashMap<>();

        clientRoutes.put("", new AvailableViewInfo("public", null, false, "",
                false, false, null, null, null, false, null));
        clientRoutes.put("/flow", new AvailableViewInfo("public", null, false,
                "", false, false, null,
                Arrays.asList(new AvailableViewInfo("child", null, false, "",
                        false, false, null, null, null, false, null)),
                null, false, null));
        clientRoutes.put("/hilla/components", new AvailableViewInfo("public",
                null, false, "", false, false, null, null, null, false, null));
        clientRoutes.put("/hilla", new AvailableViewInfo("public", null, false,
                "", false, false, null, null, null, false, null));

        try (MockedStatic<MenuRegistry> registry = Mockito
                .mockStatic(MenuRegistry.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<FrontendUtils> frontendUtils = Mockito.mockStatic(
                        FrontendUtils.class, Mockito.CALLS_REAL_METHODS);) {
            VaadinService service = Mockito.mock(VaadinService.class);
            DeploymentConfiguration conf = Mockito
                    .mock(DeploymentConfiguration.class);
            Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
            Mockito.when(conf.isProductionMode()).thenReturn(false);
            Mockito.when(conf.getFrontendFolder())
                    .thenReturn(Mockito.mock(File.class));

            registry.when(
                    () -> MenuRegistry.collectClientMenuItems(false, conf))
                    .thenReturn(clientRoutes);
            frontendUtils.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                    .thenReturn(true);

            RouteUtil.checkForClientRouteCollisions(service, "flow",
                    "flow/hello-world", "hilla/flow");
        }
    }

    @Test
    public void clientHasOverlappingTarget_validateClientRouteCollision() {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(
                "Invalid route configuration. The following Hilla route(s) conflict with configured Flow routes: flow");
        Map<String, AvailableViewInfo> clientRoutes = new HashMap<>();

        clientRoutes.put("", new AvailableViewInfo("public", null, false, "",
                false, false, null, null, null, false, null));
        clientRoutes.put("/flow", new AvailableViewInfo("public", null, false,
                "", false, false, null, null, null, false, null));
        clientRoutes.put("/hilla/components", new AvailableViewInfo("public",
                null, false, "", false, false, null, null, null, false, null));
        clientRoutes.put("/hilla", new AvailableViewInfo("public", null, false,
                "", false, false, null, null, null, false, null));

        try (MockedStatic<MenuRegistry> registry = Mockito
                .mockStatic(MenuRegistry.class, Mockito.CALLS_REAL_METHODS);
                MockedStatic<FrontendUtils> frontendUtils = Mockito.mockStatic(
                        FrontendUtils.class, Mockito.CALLS_REAL_METHODS);) {
            VaadinService service = Mockito.mock(VaadinService.class);
            DeploymentConfiguration conf = Mockito
                    .mock(DeploymentConfiguration.class);
            Mockito.when(service.getDeploymentConfiguration()).thenReturn(conf);
            Mockito.when(conf.isProductionMode()).thenReturn(false);
            Mockito.when(conf.getFrontendFolder())
                    .thenReturn(Mockito.mock(File.class));

            registry.when(
                    () -> MenuRegistry.collectClientMenuItems(false, conf))
                    .thenReturn(clientRoutes);
            frontendUtils.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                    .thenReturn(true);

            RouteUtil.checkForClientRouteCollisions(service, "flow",
                    "flow/hello-world", "hilla/flow");
        }
    }

    @SuppressWarnings("unchecked")
    private void tamperLayouts(ApplicationRouteRegistry registry,
            Consumer<Map<String, Class<? extends RouterLayout>>> consumer) {
        try {
            Field layoutsField = AbstractRouteRegistry.class
                    .getDeclaredField("layouts");
            Map<String, Class<? extends RouterLayout>> layouts = (Map<String, Class<? extends RouterLayout>>) ReflectTools
                    .getJavaFieldValue(registry, layoutsField);
            consumer.accept(layouts);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    private static void mutableRoutesMap(AbstractRouteRegistry registry) {
        ConfiguredRoutes configuration = registry.getConfiguration();
        try {
            Field routeMapField = ConfiguredRoutes.class
                    .getDeclaredField("routeMap");
            routeMapField.setAccessible(true);
            Map<String, RouteTarget> routeMap = (Map<String, RouteTarget>) routeMapField
                    .get(configuration);
            routeMapField.set(configuration, new HashMap<>(routeMap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class MockRouteTarget extends RouteTarget {
        private final Boolean registerAtStartup;

        // registerAtStartup = null means not annotated route target
        private MockRouteTarget(RouteTarget target, Boolean registerAtStartup) {
            super(target.getTarget(), target.getParentLayouts());
            this.registerAtStartup = registerAtStartup;
        }

        @Override
        boolean isAnnotatedRoute() {
            return registerAtStartup != null;
        }

        @Override
        boolean isRegisteredAtStartup() {
            return registerAtStartup != null && registerAtStartup;
        }
    }
}

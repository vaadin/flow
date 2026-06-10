/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Optional;
import java.util.function.Consumer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteParent;
import com.vaadin.flow.router.RouteParentContext;
import com.vaadin.flow.router.RouteParentReference;
import com.vaadin.flow.router.RouteParentResolver;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test that {@link RouteUtil} route resolving works as intended for both simple
 * and complex cases.
 */
class RouteUtilTest {

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
    void route_path_should_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class);
        assertEquals("parent", routePath,
                "Expected path should only have been parent RoutePrefix");
    }

    @Test
    void absolute_route_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                AbsoluteRoute.class);
        assertEquals("single", routePath,
                "No parent prefix should have been added.");
    }

    @Test
    void absolute_middle_parent_route_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                AbsoluteCenterRoute.class);
        assertEquals("absolute/child", routePath,
                "No parent prefix should have been added.");
    }

    @Test
    void absolute_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteRoute.class.getAnnotation(RouteAlias.class));
        assertEquals("alias", routePath,
                "No parent prefix should have been added.");
    }

    @Test
    void absolute_middle_parent_for_route_alias_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(AbsoluteRoute.class,
                AbsoluteCenterRoute.class.getAnnotation(RouteAlias.class));
        assertEquals("absolute/alias", routePath,
                "No parent prefix should have been added.");
    }

    @Test
    void route_path_should_contain_route_and_parent_prefix() {
        String routePath = RouteUtil.getRoutePath(new MockVaadinContext(),
                RouteWithParentPrefixAndRouteAlias.class);
        assertEquals("parent/flow", routePath,
                "Expected path should only have been parent RoutePrefix");
    }

    @Test
    void route_alias_path_should_not_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(
                BaseRouteWithParentPrefixAndRouteAlias.class,
                BaseRouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        assertEquals("alias", routePath,
                "Expected path should only have been parent RoutePrefix");
        routePath = RouteUtil.getRouteAliasPath(
                RouteWithParentPrefixAndRouteAlias.class,
                RouteWithParentPrefixAndRouteAlias.class
                        .getAnnotation(RouteAlias.class));
        assertEquals("alias", routePath,
                "Expected path should only have been parent RoutePrefix");
    }

    @Test
    void route_alias_should_contain_parent_prefix() {
        String routePath = RouteUtil.getRouteAliasPath(
                RouteAliasWithParentPrefix.class,
                RouteAliasWithParentPrefix.class
                        .getAnnotation(RouteAlias.class));
        assertEquals("aliasparent/alias", routePath,
                "Expected path should only have been parent RoutePrefix");
    }

    @Test
    void top_parent_layout_should_be_found_for_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class, "parent");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void top_parent_layout_should_be_found_for_non_base_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                RouteWithParentPrefixAndRouteAlias.class, "parent/flow");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void no_top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(),
                BaseRouteWithParentPrefixAndRouteAlias.class, "alias");

        assertNull(parent, "Found parent for RouteAlias without parent.");
    }

    @Test
    void top_parent_layout_for_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), RouteAliasWithParentPrefix.class,
                "aliasparent/alias");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RouteAliasPrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void top_parent_layout_for_absolute_route() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteRoute.class, "single");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void top_parent_layout_for_absolute_route_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteCenterRoute.class,
                "absolute/child");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void top_parent_layout_for_absolute_route_alias() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteRoute.class, "alias");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void top_parent_layout_for_absolute_route_alias_parent() {
        Class<? extends RouterLayout> parent = RouteUtil.getTopParentLayout(
                new MockVaadinContext(), AbsoluteCenterRoute.class,
                "absolute/alias");

        assertNotNull(parent, "Didn't find any parent for route");
        assertEquals(RoutePrefixParent.class, parent,
                "Received wrong parent class.");
    }

    @Test
    void automaticLayoutShouldBeAvailableForDefaultRoute() {

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

        assertEquals(0, parentLayouts.size(),
                "Route with no layout should not get automatic layout");
        assertTrue(RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "auto"));
    }

    @Test
    void routeAliasForAutoLayoutRoute_correctAliasIsSelectedForRoute() {

        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                return new MockVaadinContext();
            }
        };
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setLayout(AutoLayout.class);

        assertTrue(RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "auto"));
        assertFalse(
                RouteUtil.isAutolayoutEnabled(AutoLayoutView.class, "alias"),
                "'alias' route has autolayout false");
        assertFalse(RouteUtil.isAutolayoutEnabled(AutoLayoutView.class,
                "mainLayout"), "'mainLayout' has a defined layout");
        assertTrue(RouteUtil.isAutolayoutEnabled(AutoLayoutView.class,
                "autoAlias"));
    }

    @Test
    void expected_parent_layouts_are_found_for_route() {
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
    void expected_to_get_parent_layout() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayoutsForNonRouteTarget(
                        NonRouteTargetWithParents.class);

        assertEquals(1, parentLayouts.size(), "Expected one parent layout");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder
                        .contains(new Class[] { Parent.class }));
    }

    @Test
    void expected_parent_layouts_are_found_for_route_alias() {
        List<Class<? extends RouterLayout>> parentLayouts = RouteUtil
                .getParentLayouts(new MockVaadinContext(),
                        RouteAliasWithParentPrefix.class, "aliasparent/alias");

        MatcherAssert.assertThat(
                "Get parent layouts for route \"\" with parent prefix \"parent\" gave wrong result.",
                parentLayouts, IsIterableContainingInOrder.contains(
                        new Class[] { RouteAliasPrefixParent.class }));
    }

    @Test
    void absolute_route_gets_expected_parent_layouts() {
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
    void abolute_route_alias_gets_expected_parent_layouts() {
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
    void also_non_routes_can_be_used_to_get_top_parent_layout() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(new MockVaadinContext(), MiddleParent.class,
                        null);
        assertEquals(Parent.class, topParentLayout,
                "Middle parent should have gotten Parent as top parent layout");
    }

    @Test // 3424
    void top_layout_resolves_correctly_for_route_parent() {
        Class<? extends RouterLayout> topParentLayout = RouteUtil
                .getTopParentLayout(new MockVaadinContext(), MultiTarget.class,
                        "");
        assertEquals(Parent.class, topParentLayout,
                "@Route path should have gotten Parent as top parent layout");

        topParentLayout = RouteUtil.getTopParentLayout(new MockVaadinContext(),
                MultiTarget.class, "alias");
        assertEquals(Parent.class, topParentLayout,
                "@RouteAlias path should have gotten Parent as top parent layout");

        topParentLayout = RouteUtil.getTopParentLayout(new MockVaadinContext(),
                SubLayout.class, "parent/sub");
        assertEquals(RoutePrefixParent.class, topParentLayout,
                "SubLayout using MultiTarget as parent should have gotten RoutePrefixParent as top parent layout");

    }

    @Test // 3424
    void parent_layouts_resolve_correctly_for_route_parent() {
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
    void newRouteAnnotatedClass_updateRouteRegistry_routeIsAddedToRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
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
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newComponentClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
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
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newLazyRouteAnnotatedClass_updateRouteRegistry_routeIsNotAddedToRegistry() {
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
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newLazyRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsNotAddedToRegistry() {
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
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newRouteComponentWithoutRouteAnnotation_updateRouteRegistry_routeIsNotAddedToRegistry() {
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
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void deletedRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void deletedNotAnnotatedRouteClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute("a", A.class, Collections.emptyList());
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void deletedRouteAnnotatedClass_sessionRegistry_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        @Route("a")
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));
        registry.setRoute("a", A.class, Collections.emptyList());
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void deletedNotAnnotatedRouteClass_sessionRegistry_updateRouteRegistry_routeIsRemovedFromRegistry() {
        // given
        class A extends Component {
        }

        MockVaadinServletService service = new MockVaadinServletService();
        SessionRouteRegistry registry = (SessionRouteRegistry) SessionRouteRegistry
                .getSessionRegistry(new AlwaysLockedVaadinSession(service));
        registry.setRoute("a", A.class, Collections.emptyList());
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.emptySet(), Collections.singleton(A.class));

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void renamedRouteAnnotatedClass_updateRouteRegistry_routeIsUpdatedInRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
        assertTrue(registry.getConfiguration().hasRoute("aa"));
    }

    @Test
    void changedAliasesRouteAnnotatedClass_updateRouteRegistry_routeIsUpdatedInRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));
        registry.setRoute("alias", A.class, Collections.emptyList());
        assertTrue(registry.getConfiguration().hasRoute("alias"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertTrue(registry.getConfiguration().hasRoute("a"));
        assertTrue(registry.getConfiguration().hasRoute("alias-new"));
        assertFalse(registry.getConfiguration().hasRoute("alias"));
    }

    @Test
    void changedToLazyRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedInRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void changedFromLazyRouteAnnotatedClass_updateRouteRegistry_routeIsRemovedInRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void modifiedLazyRouteAnnotatedClass_updateRouteRegistry_existingRoutesArePreserved() {
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

        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void deannotatedRouteClass_updateRouteRegistry_routeIsRemovedFromRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));

        // when
        RouteUtil.updateRouteRegistry(registry, Collections.emptySet(),
                Collections.singleton(A.class), Collections.emptySet());

        // then
        assertFalse(registry.getConfiguration().hasRoute("a"));
    }

    // Hotswap agent may fire CREATE, MODIFY and REMOVE events for a class
    // change. MODIFY wins over CREATE and REMOVE

    @Test
    void routeAnnotatedClassAddedModifiedAndRemoved_updateRouteRegistry_routeIsAddedToRegistry() {
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
        assertTrue(registry.getConfiguration().hasRoute("a"));
    }

    @Test
    void newLayoutAnnotatedComponent_updateRouteRegistry_routeIsUpdated() {
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
        assertFalse(registry.hasLayout("auto"),
                "AutoLayout should not be available");

        RouteUtil.updateRouteRegistry(registry,
                Collections.singleton(AutoLayout.class), Collections.emptySet(),
                Collections.emptySet());

        assertTrue(registry.hasLayout("auto"),
                "AutoLayout should be available");
    }

    @Test
    void removeAnnotationsFromLayoutAnnotatedComponent_updateRouteRegistry_routeIsUpdated() {

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
        assertTrue(registry.hasLayout("auto"),
                "AutoLayout should be available");

        RouteUtil.updateRouteRegistry(registry, Collections.singleton(A.class),
                Collections.emptySet(), Collections.emptySet());

        assertFalse(registry.hasLayout("auto"),
                "AutoLayout should not be available anymore");
    }

    @Test
    void layoutAnnotatedComponent_modifiedValue_updateRouteRegistry_routeIsUpdated() {

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

        assertTrue(registry.hasLayout("hey/view"),
                "AutoLayout should be available for /hey/view path");
        assertFalse(registry.hasLayout("auto"),
                "AutoLayout should not be available for /auto path");

        RouteUtil.updateRouteRegistry(registry,
                Collections.singleton(AutoLayout.class), Collections.emptySet(),
                Collections.emptySet());

        assertTrue(registry.hasLayout("hey/view"),
                "AutoLayout should still be available anymore for /hey/view path because path matches");
        assertTrue(registry.hasLayout("auto"),
                "AutoLayout should now be available for /auto path");
    }

    @Test
    void clientHasMappedLayout_validateNoClientRouteCollisions() {
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
    void clientHasOverlappingTarget_validateClientRouteCollision() {
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
                    .thenReturn(new File("/tmp/folder"));

            registry.when(
                    () -> MenuRegistry.collectClientMenuItems(false, conf))
                    .thenReturn(clientRoutes);
            frontendUtils.when(() -> FrontendUtils.isHillaUsed(Mockito.any()))
                    .thenReturn(true);

            InvalidRouteConfigurationException ex = assertThrows(
                    InvalidRouteConfigurationException.class,
                    () -> RouteUtil.checkForClientRouteCollisions(service,
                            "flow", "flow/hello-world", "hilla/flow"));
            assertTrue(ex.getMessage().contains(
                    "Invalid route configuration. The following Hilla route(s) conflict with configured Flow routes: 'flow'"));
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
            fail(ex.getMessage());
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

    @Test
    void routeHierarchy_resolvesParentsAndTitlesWithoutInstance() {
        OrgView.instantiated = false;
        ProjectView.instantiated = false;

        RouteParameters parameters = new RouteParameters(
                Map.of("orgId", "acme", "projectId", "42"));

        List<RouteParentReference> hierarchy = RouteUtil
                .getRouteHierarchy(ProjectView.class, parameters);

        // ordered from root to current target
        assertEquals(List.of(OrgView.class, ProjectView.class), hierarchy
                .stream().map(RouteParentReference::navigationTarget).toList());

        // the org parameter is carried over to the parent reference
        assertEquals("acme",
                hierarchy.get(0).routeParameters().get("orgId").orElseThrow());

        // titles compose with PageTitleGenerator, also without an instance
        List<String> titles = hierarchy.stream()
                .map(reference -> MenuRegistry.getTitle(
                        reference.navigationTarget(),
                        reference.routeParameters()))
                .toList();
        assertEquals(List.of("Org acme", "Project 42"), titles);

        assertFalse(OrgView.instantiated,
                "Hierarchy resolution must not instantiate the route");
        assertFalse(ProjectView.instantiated,
                "Hierarchy resolution must not instantiate the route");
    }

    @Test
    void getRouteParent_staticValue_carriesParameters() {
        RouteParameters parameters = new RouteParameters("orgId", "acme");

        RouteParentReference parent = RouteUtil
                .getRouteParent(SettingsView.class, parameters).orElseThrow();

        assertEquals(OrgView.class, parent.navigationTarget());
        assertEquals("acme",
                parent.routeParameters().get("orgId").orElseThrow());
    }

    @Test
    void getRouteParent_noAnnotationNoRegistry_isEmpty() {
        assertFalse(
                RouteUtil.getRouteParent(OrgView.class, RouteParameters.empty())
                        .isPresent());
    }

    @Test
    void getRouteParent_staticParentWithFewerParameters_narrowsParameters() {
        withRegistry(registry -> {
            RouteParentReference parent = RouteUtil
                    .getRouteParent(registry, OrderDetailView.class,
                            new RouteParameters("orderId", "1001"))
                    .orElseThrow();

            assertEquals(OrdersView.class, parent.navigationTarget());
            // the parent route declares no parameters, so orderId is dropped
            assertTrue(parent.routeParameters().getParameterNames().isEmpty());
            // and a link to the parent can actually be built
            assertEquals("uc2", RouteConfiguration.forRegistry(registry).getUrl(
                    parent.navigationTarget(), parent.routeParameters()));
        }, OrdersView.class, OrderDetailView.class);
    }

    @Test
    void getRouteParent_noAnnotation_derivedFromRouteUrl() {
        withRegistry(registry -> {
            // orgs/:orgId/members has no @RouteParent, so the parent is the
            // route serving the nearest ancestor path orgs/:orgId
            RouteParentReference parent = RouteUtil.getRouteParent(registry,
                    MembersView.class, new RouteParameters("orgId", "acme"))
                    .orElseThrow();

            assertEquals(OrgView.class, parent.navigationTarget());
            assertEquals("acme",
                    parent.routeParameters().get("orgId").orElseThrow());
        }, OrgView.class, MembersView.class);
    }

    @Test
    void getRouteHierarchy_classWithoutRoute_returnsSingletonOfItself() {
        withRegistry(registry -> assertEquals(List.of(PlainView.class),
                targets(RouteUtil.getRouteHierarchy(registry, PlainView.class,
                        RouteParameters.empty()))));
    }

    @Test
    void getRouteHierarchy_singleRouteNoParent_returnsSingletonChain() {
        withRegistry(
                registry -> assertEquals(List.of(AView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                AView.class, RouteParameters.empty()))),
                AView.class);
    }

    @Test
    void getRouteHierarchy_urlPrefixHappyPath_returnsTwoElementChain() {
        withRegistry(
                registry -> assertEquals(List.of(AView.class, AbView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                AbView.class, RouteParameters.empty()))),
                AView.class, AbView.class);
    }

    @Test
    void getRouteHierarchy_deepChain_walksMultipleSteps() {
        withRegistry(
                registry -> assertEquals(
                        List.of(AView.class, AbView.class, AbcView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                AbcView.class, RouteParameters.empty()))),
                AView.class, AbView.class, AbcView.class);
    }

    @Test
    void getRouteHierarchy_missingIntermediate_terminatesAtLeaf() {
        // only a/b/c is registered (no a/b, no a), so no ancestor is found
        withRegistry(
                registry -> assertEquals(List.of(AbcView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                AbcView.class, RouteParameters.empty()))),
                AbcView.class);
    }

    @Test
    void getRouteHierarchy_emptyTemplateRoot_isFoundAsParent() {
        withRegistry(
                registry -> assertEquals(
                        List.of(HomeView.class, DocsView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                DocsView.class, RouteParameters.empty()))),
                HomeView.class, DocsView.class);
    }

    @Test
    void getRouteHierarchy_routeParent_takesPrecedenceOverUrlPrefix() {
        // OrderDetailView is order-detail/:orderId; its URL parent would be
        // order-detail, but @RouteParent(OrdersView) wins
        withRegistry(
                registry -> assertEquals(
                        List.of(OrdersView.class, OrderDetailView.class),
                        targets(RouteUtil.getRouteHierarchy(registry,
                                OrderDetailView.class,
                                new RouteParameters("orderId", "1001")))),
                OrdersView.class, OrderDetailView.class);
    }

    @Test
    void getRouteHierarchy_cycleViaRouteParent_truncatesWithoutDuplicates() {
        withRegistry(registry -> {
            List<Class<? extends Component>> chain = targets(
                    RouteUtil.getRouteHierarchy(registry, CycleAView.class,
                            RouteParameters.empty()));
            assertTrue(chain.size() <= 2,
                    "Cycle must be truncated, was " + chain.size());
            assertTrue(chain.contains(CycleAView.class));
            assertEquals(chain.size(), chain.stream().distinct().count(),
                    "Chain must not contain duplicates");
        }, CycleAView.class, CycleBView.class);
    }

    @Test
    void getRouteParent_resolverReturningEmpty_marksRoot() {
        // RootResolver returns empty -> OrderDetailView is its own root
        assertFalse(RouteUtil
                .getRouteParent(RootResolvedView.class, RouteParameters.empty())
                .isPresent());
    }

    @SafeVarargs
    private static void withRegistry(Consumer<RouteRegistry> test,
            Class<? extends Component>... routes) {
        MockVaadinServletService service = new MockVaadinServletService();
        VaadinService.setCurrent(service);
        try {
            ApplicationRouteRegistry registry = ApplicationRouteRegistry
                    .getInstance(service.getContext());
            RouteConfiguration configuration = RouteConfiguration
                    .forRegistry(registry);
            for (Class<? extends Component> route : routes) {
                configuration.setAnnotatedRoute(route);
            }
            test.accept(registry);
        } finally {
            VaadinService.setCurrent(null);
        }
    }

    private static List<Class<? extends Component>> targets(
            List<RouteParentReference> hierarchy) {
        return hierarchy.stream().map(RouteParentReference::navigationTarget)
                .toList();
    }

    public static class OrgTitleGenerator implements PageTitleGenerator {
        @Override
        public String generatePageTitle(PageTitleContext context) {
            return "Org " + context.routeParameters().get("orgId").orElse("");
        }
    }

    public static class ProjectTitleGenerator implements PageTitleGenerator {
        @Override
        public String generatePageTitle(PageTitleContext context) {
            return "Project "
                    + context.routeParameters().get("projectId").orElse("");
        }
    }

    public static class OrgParentResolver implements RouteParentResolver {
        @Override
        public Optional<RouteParentReference> resolveParent(
                RouteParentContext context) {
            RouteParameters parentParameters = new RouteParameters("orgId",
                    context.routeParameters().get("orgId").orElseThrow());
            return Optional.of(
                    new RouteParentReference(OrgView.class, parentParameters));
        }
    }

    @Tag(Tag.DIV)
    @Route("orgs/:orgId")
    @DynamicPageTitle(OrgTitleGenerator.class)
    public static class OrgView extends Component {
        static boolean instantiated = false;

        public OrgView() {
            instantiated = true;
        }
    }

    @Tag(Tag.DIV)
    @Route("orgs/:orgId/projects/:projectId")
    @DynamicPageTitle(ProjectTitleGenerator.class)
    @RouteParent(resolver = OrgParentResolver.class)
    public static class ProjectView extends Component {
        static boolean instantiated = false;

        public ProjectView() {
            instantiated = true;
        }
    }

    @Tag(Tag.DIV)
    @Route("orgs/:orgId/settings")
    @PageTitle("Settings")
    @RouteParent(OrgView.class)
    public static class SettingsView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("orgs/:orgId/members")
    public static class MembersView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("uc2")
    public static class OrdersView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("order-detail/:orderId")
    @RouteParent(OrdersView.class)
    public static class OrderDetailView extends Component {
    }

    @Tag(Tag.DIV)
    public static class PlainView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("a")
    public static class AView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("a/b")
    public static class AbView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("a/b/c")
    public static class AbcView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("")
    public static class HomeView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("docs")
    public static class DocsView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("cycle-a")
    @RouteParent(CycleBView.class)
    public static class CycleAView extends Component {
    }

    @Tag(Tag.DIV)
    @Route("cycle-b")
    @RouteParent(CycleAView.class)
    public static class CycleBView extends Component {
    }

    public static class RootResolver implements RouteParentResolver {
        @Override
        public Optional<RouteParentReference> resolveParent(
                RouteParentContext context) {
            return Optional.empty();
        }
    }

    @Tag(Tag.DIV)
    @Route("root-resolved")
    @RouteParent(resolver = RootResolver.class)
    public static class RootResolvedView extends Component {
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

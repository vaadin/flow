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
package com.vaadin.flow.server.startup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RouteRegistry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NotThreadSafe
abstract class RouteRegistryTestBase {

    @Test
    public void initializedRoutes_routesCanBeAdded() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("home", MyRoute.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("info", MyInfo.class,
                Collections.emptyList());

        assertEquals(2, getTestedRegistry().getRegisteredRoutes().size(),
                "Initial registration of routes should have succeeded.");

        getTestedRegistry().setRoute("palace", MyPalace.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("modular", MyModular.class,
                Collections.emptyList());

        assertEquals(4, getTestedRegistry().getRegisteredRoutes().size(),
                "All new routes should have been registered");

        getTestedRegistry().setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        assertEquals(5, getTestedRegistry().getRegisteredRoutes().size(),
                "The new route should have registered");
    }

    @Test
    public void registeringRouteWithAlias_RouteDataIsPopulatedCorrectly() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("home", MyRoute.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("info", MyInfo.class,
                Collections.emptyList());

        getInitializationRegistry().setRoute("withAliases",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("version",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Optional<RouteData> first = getTestedRegistry()
                .getRegisteredRoutes().stream().filter(route -> route
                        .getNavigationTarget().equals(MyRouteWithAliases.class))
                .findFirst();
        assertTrue(first.isPresent(),
                "Didn't get RouteData for MyRouteWithAliases.");

        assertEquals(2, first.get().getRouteAliases().size(),
                "Expected two route aliases to be registered");
    }

    @Test
    public void registeredRouteWithAlias_removingClassRemovesAliases() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("withAliases",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("version",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        assertTrue(!getTestedRegistry().getRegisteredRoutes().isEmpty(),
                "Registry didn't contain routes even though 3 should have been registered");

        assertTrue(
                getTestedRegistry().getNavigationTarget("withAliases")
                        .isPresent(),
                "Path for main route 'withAliases' returned empty");
        assertTrue(
                getTestedRegistry().getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' returned empty.");
        assertTrue(
                getTestedRegistry().getNavigationTarget("person").isPresent(),
                "RouteAlias 'person' returned empty.");

        getTestedRegistry().removeRoute(MyRouteWithAliases.class);

        assertFalse(!getTestedRegistry().getRegisteredRoutes().isEmpty(),
                "Registry should be empty after removing the only registered Class.");
    }

    @Test
    public void registeredRouteWithAlias_removingPathLeavesAliases() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("withAliases",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("version",
                MyRouteWithAliases.class, Collections.emptyList());
        getInitializationRegistry().setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        assertTrue(!getTestedRegistry().getRegisteredRoutes().isEmpty(),
                "Registry didn't contain routes even though 3 should have been registered");

        assertTrue(
                getTestedRegistry().getNavigationTarget("withAliases")
                        .isPresent(),
                "Path for main route 'withAliases' returned empty");
        assertTrue(
                getTestedRegistry().getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' returned empty.");
        assertTrue(
                getTestedRegistry().getNavigationTarget("person").isPresent(),
                "RouteAlias 'person' returned empty.");

        getTestedRegistry().removeRoute("withAliases");

        assertTrue(!getTestedRegistry().getRegisteredRoutes().isEmpty(),
                "Registry should contain alias routes");

        assertEquals(1,
                getTestedRegistry().getRegisteredRoutes().get(0)
                        .getRouteAliases().size(),
                "One RouteAlias should be the main url so only 1 route alias should be marked as an alias");
    }

    @Test
    public void routesWithParentLayouts_parentLayoutReturnsAsExpected() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("MyRoute",
                MyRouteWithAliases.class,
                Collections.singletonList(MainLayout.class));
        getInitializationRegistry().setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("version",
                MyRouteWithAliases.class,
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        assertFalse(
                getTestedRegistry().getNavigationRouteTarget("MyRoute")
                        .getRouteTarget().getParentLayouts().isEmpty(),
                "'MyRoute' should have a single parent");
        assertTrue(
                getTestedRegistry().getNavigationRouteTarget("info")
                        .getRouteTarget().getParentLayouts().isEmpty(),
                "'info' should have no parents.");
        assertEquals(2,
                getTestedRegistry().getNavigationRouteTarget("version")
                        .getRouteTarget().getParentLayouts().size(),
                "'version' should return two parents");
    }

    @Test
    public void registeredParentLayouts_changingListDoesntChangeRegistration() {
        getInitializationRegistry().clean();

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        getInitializationRegistry().setRoute("version", MyRoute.class,
                parentChain);

        parentChain.remove(MainLayout.class);

        assertEquals(2,
                getTestedRegistry().getNavigationRouteTarget("version")
                        .getRouteTarget().getParentLayouts().size(),
                "'version' should return two parents even when original list is changed");
    }

    @Test
    public void registeredParentLayouts_returnedListInSameOrder() {
        getInitializationRegistry().clean();

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        getInitializationRegistry().setRoute("version", MyRoute.class,
                parentChain);

        assertArrayEquals(parentChain.toArray(),
                getTestedRegistry().getNavigationRouteTarget("version")
                        .getRouteTarget().getParentLayouts().toArray(),
                "Registry should return parent layouts in the same order as set.");
    }

    /**
     * Returns registry which is used to initialize the registry with data.
     *
     * @return initialization registry
     */
    protected abstract RouteRegistry getInitializationRegistry();

    /**
     * Returns registry which is tested.
     * <p>
     * This may be the same registry which is returned by
     * {@link #getInitializationRegistry()} but doesn't have to be.
     *
     * @return the tested registry
     */
    protected abstract RouteRegistry getTestedRegistry();

    @Tag("div")
    @Route("home")
    protected static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("info")
    protected static class MyInfo extends Component {
    }

    @Tag("div")
    @Route("palace")
    protected static class MyPalace extends Component {
    }

    @Tag("div")
    protected static class Secondary extends Component {
    }

    @Tag("div")
    @Route("modular")
    protected static class MyModular extends Component {
    }

    @Tag("div")
    @Route("withAliases")
    @RouteAlias("version")
    @RouteAlias("person")
    protected static class MyRouteWithAliases extends Component {
    }

    @Tag("div")
    protected static class MainLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    protected static class MiddleLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    public static abstract class AbstractErrorView<EXCEPTION_TYPE extends Exception>
            extends Component implements HasErrorParameter<EXCEPTION_TYPE> {
    }

    @Tag("div")
    public static class ErrorView
            extends AbstractErrorView<NullPointerException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NullPointerException> parameter) {
            return 0;
        }
    }
}

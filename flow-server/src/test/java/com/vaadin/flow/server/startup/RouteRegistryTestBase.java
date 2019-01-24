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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.RouteRegistry;

@NotThreadSafe
public abstract class RouteRegistryTestBase {

    @Test
    public void initializedRoutes_routesCanBeAdded() {
        getInitializationRegistry().clean();

        getInitializationRegistry().setRoute("home", MyRoute.class,
                Collections.emptyList());
        getInitializationRegistry().setRoute("info", MyInfo.class,
                Collections.emptyList());

        Assert.assertEquals(
                "Initial registration of routes should have succeeded.", 2,
                getTestedRegistry().getRegisteredRoutes().size());

        getTestedRegistry().setRoute("palace", MyPalace.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("modular", MyModular.class,
                Collections.emptyList());

        Assert.assertEquals("All new routes should have been registered", 4,
                getTestedRegistry().getRegisteredRoutes().size());

        getTestedRegistry().setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Assert.assertEquals("The new route should have registered", 5,
                getTestedRegistry().getRegisteredRoutes().size());
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
        Assert.assertTrue("Didn't get RouteData for MyRouteWithAliases.",
                first.isPresent());

        Assert.assertEquals("Expected two route aliases to be registered", 2,
                first.get().getRouteAliases().size());
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

        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !getTestedRegistry().getRegisteredRoutes().isEmpty());

        Assert.assertTrue("Path for main route 'withAliases' returned empty",
                getTestedRegistry().getNavigationTarget("withAliases")
                        .isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                getTestedRegistry().getNavigationTarget("version").isPresent());
        Assert.assertTrue("RouteAlias 'person' returned empty.",
                getTestedRegistry().getNavigationTarget("person").isPresent());

        getTestedRegistry().removeRoute(MyRouteWithAliases.class);

        Assert.assertFalse(
                "Registry should be empty after removing the only registered Class.",
                !getTestedRegistry().getRegisteredRoutes().isEmpty());
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

        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !getTestedRegistry().getRegisteredRoutes().isEmpty());

        Assert.assertTrue("Path for main route 'withAliases' returned empty",
                getTestedRegistry().getNavigationTarget("withAliases")
                        .isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                getTestedRegistry().getNavigationTarget("version").isPresent());
        Assert.assertTrue("RouteAlias 'person' returned empty.",
                getTestedRegistry().getNavigationTarget("person").isPresent());

        getTestedRegistry().removeRoute("withAliases");

        Assert.assertTrue("Registry should contain alias routes",
                !getTestedRegistry().getRegisteredRoutes().isEmpty());

        Assert.assertEquals(
                "One RouteAlias should be the main url so only 1 route alias should be marked as an alias",
                1, getTestedRegistry().getRegisteredRoutes().get(0)
                        .getRouteAliases().size());
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

        Assert.assertFalse("'MyRoute' should have a single parent",
                getTestedRegistry()
                        .getRouteLayouts("MyRoute", MyRouteWithAliases.class)
                        .isEmpty());
        Assert.assertTrue("'info' should have no parents.", getTestedRegistry()
                .getRouteLayouts("info", MyRouteWithAliases.class).isEmpty());
        Assert.assertEquals("'version' should return two parents", 2,
                getTestedRegistry()
                        .getRouteLayouts("version", MyRouteWithAliases.class)
                        .size());
    }

    @Test
    public void registeredParentLayouts_changingListDoesntChangeRegistration() {
        getInitializationRegistry().clean();

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        getInitializationRegistry().setRoute("version", MyRoute.class,
                parentChain);

        parentChain.remove(MainLayout.class);

        Assert.assertEquals(
                "'version' should return two parents even when original list is changed",
                2, getTestedRegistry().getRouteLayouts("version", MyRoute.class)
                        .size());
    }

    @Test
    public void registeredParentLayouts_returnedListInSameOrder() {
        getInitializationRegistry().clean();

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        getInitializationRegistry().setRoute("version", MyRoute.class,
                parentChain);

        Assert.assertArrayEquals(
                "Registry should return parent layouts in the same order as set.",
                parentChain.toArray(), getTestedRegistry()
                        .getRouteLayouts("version", MyRoute.class).toArray());
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
}

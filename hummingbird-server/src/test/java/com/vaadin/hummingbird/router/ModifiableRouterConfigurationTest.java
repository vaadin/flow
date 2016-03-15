/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.router.ViewRendererTest.AnotherParentView;
import com.vaadin.hummingbird.router.ViewRendererTest.ParentView;
import com.vaadin.hummingbird.router.ViewRendererTest.TestView;

public class ModifiableRouterConfigurationTest {
    @Test
    public void testExactRoute() {
        assertMatches("", "");
        assertNotMatches("a", "");

        assertMatches("foo", "foo");
        assertMatches("foo/", "foo/");

        assertNotMatches("foo/", "foo");
        assertNotMatches("foo", "foo/");

        assertNotMatches("foo/bar", "foo/");
        assertNotMatches("foo/", "foo/bar");

        assertNotMatches("foo/bar", "foo/baz");
        assertNotMatches("foo/bar", "baz/bar");

        assertMatches("foo/bar/baz", "foo/bar/baz");
    }

    @Test
    public void testPlaceholderRoute() {
        assertMatches("foo", "{name}");
        assertMatches("foo/", "{name}/");

        assertNotMatches("foo", "{name}/");
        assertNotMatches("foo/", "{name}");

        assertMatches("foo/bar/baz", "{name}/bar/baz");
        assertMatches("foo/bar/baz", "foo/{name}/baz");
        assertMatches("foo/bar/baz", "foo/bar/{name}");

        assertNotMatches("foo/bar/baz", "{name}/asdf/baz");
        assertNotMatches("foo/bar/baz", "{name}/bar/asdf");
        assertNotMatches("foo/bar/baz", "asdf/{name}/baz");
        assertNotMatches("foo/bar/baz", "foo/{name}/asdf");
        assertNotMatches("foo/bar/baz", "asdf/bar/{name}");
        assertNotMatches("foo/bar/baz", "foo/asdf/{name}");

        assertMatches("foo/bar/baz", "{name1}/{name2}/{name3}");
    }

    @Test
    public void testWildcardRoute() {
        assertMatches("", "*");
        assertMatches("foo", "*");
        assertMatches("foo/", "*");
        assertMatches("foo/bar", "*");

        assertMatches("foo/", "foo/*");
        assertMatches("foo/bar", "foo/*");
        assertMatches("foo/bar/", "foo/*");

        assertNotMatches("foo", "foo/*");
        assertNotMatches("bar/foo", "foo/*");
    }

    @Test
    public void testRoutePriorityOrder() {
        assertRoutePriorityOrder("foo", "{name}", "*");

        assertRoutePriorityOrder("foo/bar", "foo/{name}", "{name}/bar",
                "{name}/{name2}");

        assertRoutePriorityOrder("foo/bar", "foo/*", "*");
    }

    @Test
    public void testRoutesCopied() {
        ModifiableRouterConfiguration original = createConfiguration();
        original.setRoute("foo/bar", createNoopHandler());

        ModifiableRouterConfiguration copy = new ModifiableRouterConfiguration(
                original, false);

        original.removeRoute("foo/bar");

        Assert.assertNotNull("Updating the original should not affect the copy",
                copy.resolveRoute(createEvent("foo/bar")));
    }

    @Test
    public void testRemoveRoutes() {
        ModifiableRouterConfiguration configuration = createConfiguration();

        NavigationHandler navigationHandler = createNoopHandler();

        configuration.setRoute("foo", navigationHandler);
        configuration.setRoute("{name}", navigationHandler);
        configuration.setRoute("*", navigationHandler);

        configuration.removeRoute("foo");
        Assert.assertNotNull(configuration.resolveRoute(createEvent("foo")));
        configuration.removeRoute("{otherName}");
        Assert.assertNotNull(configuration.resolveRoute(createEvent("foo")));
        configuration.removeRoute("*");

        // Should resolve to null only after removing all the routes
        Assert.assertNull(configuration.resolveRoute(createEvent("foo")));
    }

    private static NavigationHandler createNoopHandler() {
        return e -> {
        };
    }

    @Test(expected = IllegalStateException.class)
    public void testSetExistingRouteThrows() {
        ModifiableRouterConfiguration configuration = createConfiguration();
        configuration.setRoute("foo", createNoopHandler());
        configuration.setRoute("foo", createNoopHandler());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetExistingPlaceholderThrows() {
        ModifiableRouterConfiguration configuration = createConfiguration();
        configuration.setRoute("{name}", createNoopHandler());
        configuration.setRoute("{anotherName}", createNoopHandler());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetExistingWildcardThrows() {
        ModifiableRouterConfiguration configuration = createConfiguration();
        configuration.setRoute("foo/*", createNoopHandler());
        configuration.setRoute("foo/*", createNoopHandler());
    }

    @Test
    public void testParentViewsWithoutParent() {
        RouterUI ui = new RouterUI();

        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route", TestView.class);
            conf.setParentView(TestView.class, ParentView.class);
            conf.setParentView(ParentView.class, AnotherParentView.class);
        });

        router.navigate(ui, new Location("route"));

        Assert.assertEquals(Arrays.asList(TestView.class, ParentView.class,
                AnotherParentView.class), getViewChainTypes(ui));
    }

    @Test
    public void testParentViewsWithParent() {
        RouterUI ui = new RouterUI();

        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route", TestView.class, ParentView.class);
            conf.setParentView(ParentView.class, AnotherParentView.class);
        });

        router.navigate(ui, new Location("route"));

        Assert.assertEquals(Arrays.asList(TestView.class, ParentView.class,
                AnotherParentView.class), getViewChainTypes(ui));
    }

    private List<Class<? extends View>> getViewChainTypes(RouterUI ui) {
        return ui.getActiveViewChain().stream().map(v -> v.getClass())
                .collect(Collectors.toList());
    }

    @Test(expected = IllegalStateException.class)
    public void testReplaceParentViewThrows() {
        ModifiableRouterConfiguration configuration = createConfiguration();

        configuration.setParentView(TestView.class, ParentView.class);
        configuration.setParentView(TestView.class, ParentView.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testParentViewLoopDetection() {
        ModifiableRouterConfiguration configuration = createConfiguration();

        configuration.setParentView(AnotherParentView.class, ParentView.class);
        configuration.setParentView(ParentView.class, AnotherParentView.class);
    }

    private static void assertRoutePriorityOrder(String... routes) {
        // First param also serves as the base location
        String location = routes[0];
        for (int i = 0; i < routes.length - 1; i++) {
            String stronger = routes[i];
            String weaker = routes[i + 1];

            assertRoutePriority(location, stronger, weaker);
        }
    }

    private static void assertRoutePriority(String location,
            String strongerRoute, String weakerRoute) {
        // First verify that the test makes sense
        assertMatches(location, strongerRoute);
        assertMatches(location, weakerRoute);

        NavigationHandler weakerHandler = createNoopHandler();
        NavigationHandler strongerHandler = createNoopHandler();

        ModifiableRouterConfiguration configuration = createConfiguration();

        configuration.setRoute(strongerRoute, strongerHandler);
        configuration.setRoute(weakerRoute, weakerHandler);

        NavigationHandler handler = configuration
                .resolveRoute(createEvent(location));
        Assert.assertSame(handler, strongerHandler);

        // Do the same again with setRoute run in the opposite order
        configuration = createConfiguration();

        configuration.setRoute(weakerRoute, weakerHandler);
        configuration.setRoute(strongerRoute, strongerHandler);

        handler = configuration.resolveRoute(createEvent(location));
        Assert.assertSame(handler, strongerHandler);
    }

    private static void assertMatches(String location, String route) {
        Assert.assertTrue(
                "The route " + route + " should match the location " + location,
                routeMatches(location, route));
    }

    private static void assertNotMatches(String location, String route) {
        Assert.assertFalse("The route " + route
                + " shouldn't match the location " + location,
                routeMatches(location, route));
    }

    private static boolean routeMatches(String location, String route) {
        ModifiableRouterConfiguration configuration = createConfiguration();
        configuration.setRoute(route, createNoopHandler());

        NavigationHandler resolveRoute = configuration
                .resolveRoute(createEvent(location));
        boolean condition = resolveRoute != null;
        return condition;
    }

    private static ModifiableRouterConfiguration createConfiguration() {
        // Create a modifiable copy of a an empty configuration
        return new ModifiableRouterConfiguration(
                new ModifiableRouterConfiguration(), true);
    }

    private static NavigationEvent createEvent(String location) {
        Assert.assertFalse(location.contains(".."));
        Assert.assertFalse(location.contains("*"));
        Assert.assertFalse(location.contains("{"));
        Assert.assertFalse(location.contains("}"));

        return new NavigationEvent(new Router(), new Location(location),
                new RouterUI());
    }

}

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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.router.ViewRendererTest.AnotherParentView;
import com.vaadin.hummingbird.router.ViewRendererTest.AnotherTestView;
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

        assertMatches("foo/", "foo/{name}");
        assertMatches("foo//bar", "foo/{name}/bar");
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

        assertRoutePriorityOrder("foo/", "foo/{name}", "foo/*");
    }

    @Test
    public void testRoutesCopied() throws Exception {
        ModifiableRouterConfiguration original = createConfiguration();
        original.setRoute("foo/bar", createNoopHandler());

        ModifiableRouterConfiguration copy = new ModifiableRouterConfiguration(
                original, false);

        original.removeRoute("foo/bar");

        Assert.assertNotNull("Updating the original should not affect the copy",
                copy.resolveRoute(new Location("foo/bar")));
    }

    @Test
    public void testEverythingIsCopied() throws Exception {
        ModifiableRouterConfiguration original = createConfiguration();
        original.setRoute("foo/bar", TestView.class);

        ModifiableRouterConfiguration copy = new ModifiableRouterConfiguration(
                original, false);

        validateNoSameInstances(original, copy);
    }

    @SuppressWarnings("rawtypes")
    private void validateNoSameInstances(Object original, Object copy)
            throws Exception {
        Assert.assertNotNull(original);
        Assert.assertNotSame(original, copy);
        for (Field f : original.getClass().getDeclaredFields()) {
            Class<?> fieldType = f.getType();
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            } else if (fieldType.isPrimitive() || fieldType == String.class) {
                continue;
            } else if (isIgnoredType(fieldType)) {
                continue;
            }

            f.setAccessible(true);
            Object originalValue = f.get(original);
            Object copyValue = f.get(copy);
            if (Map.class.isAssignableFrom(fieldType)) {
                validateNoSameInstances((Map) originalValue, (Map) copyValue);
            } else {
                validateNoSameInstances(originalValue, copyValue);
            }
        }
    }

    private boolean isIgnoredType(Class<?> c) {
        return NavigationHandler.class.isAssignableFrom(c)
                || Resolver.class.isAssignableFrom(c)
                || PageTitleGenerator.class.isAssignableFrom(c);
    }

    @SuppressWarnings("rawtypes")
    private void validateNoSameInstances(Map original, Map copy)
            throws Exception {
        for (Object key : original.keySet()) {
            Object originalValue = original.get(key);
            Object copyValue = copy.get(key);
            if (originalValue != null
                    && !isIgnoredType(originalValue.getClass())) {
                validateNoSameInstances(originalValue, copyValue);
            }
        }
    }

    @Test
    public void testRemoveRoutes() {
        ModifiableRouterConfiguration configuration = createConfiguration();

        NavigationHandler navigationHandler = createNoopHandler();

        configuration.setRoute("foo", navigationHandler);
        configuration.setRoute("{name}", navigationHandler);
        configuration.setRoute("*", navigationHandler);

        configuration.removeRoute("foo");
        Assert.assertNotNull(configuration.resolveRoute(new Location("foo")));
        configuration.removeRoute("{otherName}");
        Assert.assertNotNull(configuration.resolveRoute(new Location("foo")));
        configuration.removeRoute("*");

        // Should resolve to null only after removing all the routes
        Assert.assertNull(configuration.resolveRoute(new Location("foo")));
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

        Assert.assertEquals(ParentView.class,
                router.getConfiguration().getParentView(TestView.class));
        Assert.assertEquals(AnotherParentView.class,
                router.getConfiguration().getParentView(ParentView.class));

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
        Assert.assertEquals(ParentView.class,
                router.getConfiguration().getParentView(TestView.class));
        Assert.assertEquals(AnotherParentView.class,
                router.getConfiguration().getParentView(ParentView.class));
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
                .resolveRoute(new Location(location));
        Assert.assertSame(handler, strongerHandler);

        // Do the same again with setRoute run in the opposite order
        configuration = createConfiguration();

        configuration.setRoute(weakerRoute, weakerHandler);
        configuration.setRoute(strongerRoute, strongerHandler);

        handler = configuration.resolveRoute(new Location(location));
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
                .resolveRoute(new Location(location));
        boolean condition = resolveRoute != null;
        return condition;
    }

    private static ModifiableRouterConfiguration createConfiguration() {
        // Create a modifiable copy of a an empty configuration
        return new ModifiableRouterConfiguration(
                new ModifiableRouterConfiguration(), true);
    }

    @Test
    public void getRoute() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route1", TestView.class, ParentView.class);
            conf.setRoute("route2", AnotherTestView.class);
        });

        assertRoute(router, TestView.class, "route1");
        assertRoute(router, AnotherTestView.class, "route2");
        assertRoute(router, ParentView.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getRouteMultipleMappings() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route1", TestView.class);
            conf.setRoute("route2", TestView.class);
        });

        assertRoute(router, TestView.class, "Should throw exception");
    }

    @Test
    public void getRoutesSingleMapping() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route1", TestView.class);
        });

        assertRoutes(router, TestView.class, "route1");
    }

    @Test
    public void getRoutesMultipleMappings() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route2", TestView.class);
            conf.setRoute("route1", TestView.class);
        });

        assertRoutes(router, TestView.class, "route2", "route1");
    }

    @Test
    public void getRoutesNoMapping() {
        Router router = new Router();

        assertRoutes(router, TestView.class);
    }

    @Test
    public void getRoutesAfterRemoved() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route1", TestView.class);
            conf.setRoute("route2", AnotherTestView.class);
        });
        router.reconfigure(conf -> {
            conf.removeRoute("route1");
        });
        assertRoutes(router, TestView.class);
        assertRoutes(router, AnotherTestView.class, "route2");
    }

    private void assertRoutes(Router router, Class<? extends View> viewType,
            String... expected) {
        List<String> routes = router.getConfiguration().getRoutes(viewType)
                .collect(Collectors.toList());
        Assert.assertArrayEquals(expected, routes.toArray());
    }

    private void assertRoute(Router router, Class<? extends View> class1,
            String expected) {
        Assert.assertEquals(expected,
                router.getConfiguration().getRoute(class1).orElse(null));
    }

    @Test
    public void getParentViewsWithoutParent() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route", TestView.class);
        });

        assertParentViews(router, TestView.class);
    }

    @SafeVarargs
    private final void assertParentViews(Router router,
            Class<TestView> viewType,
            Class<? extends HasChildView>... expected) {
        Assert.assertArrayEquals(expected,
                router.getConfiguration().getParentViews(viewType)
                        .collect(Collectors.toList()).toArray());

    }

    @Test
    public void getParentViewsOneLevel() {

        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route", TestView.class);
            conf.setParentView(TestView.class, ParentView.class);
        });

        assertParentViews(router, TestView.class, ParentView.class);
    }

    @Test
    public void getParentViewsManyLevels() {
        Router router = new Router();

        router.reconfigure(conf -> {
            conf.setRoute("route", TestView.class);
            conf.setParentView(TestView.class, ParentView.class);
            conf.setParentView(ParentView.class, AnotherParentView.class);
        });

        assertParentViews(router, TestView.class, ParentView.class,
                AnotherParentView.class);
    }

    @Test
    public void testDefaultPageGeneratorUsed() {
        Router router = new Router();
        PageTitleGenerator pageTitleGenerator = router.getConfiguration()
                .getPageTitleGenerator();
        Assert.assertSame(DefaultPageTitleGenerator.getInstance(),
                pageTitleGenerator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullPageTitleGenerator() {
        Router router = new Router();
        router.reconfigure(conf -> {
            conf.setPageTitleGenerator(null);
        });
    }

}

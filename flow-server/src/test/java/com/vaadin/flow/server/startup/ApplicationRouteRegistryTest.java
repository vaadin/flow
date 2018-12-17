package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;

public class ApplicationRouteRegistryTest {

    private ApplicationRouteRegistry registry;

    @Before
    public void init() {
        registry = ApplicationRouteRegistry
                .getInstance(Mockito.mock(ServletContext.class));
    }

    @Test
    public void initalizedRoutes_routesCanBeAdded() {
        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("info", MyInfo.class, Collections.emptyList());

        Assert.assertEquals(
                "Initial registration of routes should have succeeded.", 2,
                registry.getRegisteredRoutes().size());

        registry.setRoute("palace", MyPalace.class, Collections.emptyList());
        registry.setRoute("modular", MyModular.class, Collections.emptyList());

        Assert.assertEquals("All new routes should have been registered", 4,
                registry.getRegisteredRoutes().size());

        registry.setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Assert.assertEquals("The new route should have registered", 5,
                registry.getRegisteredRoutes().size());
    }

    @Test
    public void registeringRouteWithAlias_RouteDataIsPopulatedCorrectly() {

        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("info", MyInfo.class, Collections.emptyList());

        registry.setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Optional<RouteData> first = registry.getRegisteredRoutes().stream()
                .filter(route -> route.getNavigationTarget()
                        .equals(MyRouteWithAliases.class)).findFirst();
        Assert.assertTrue("Didn't get RouteData for MyRouteWithAliases.",
                first.isPresent());

        Assert.assertEquals("Expected two route aliases to be registered", 2,
                first.get().getRouteAliases().size());
    }

    @Test
    public void registeredRouteWithAlias_removingClassRemovesAliases() {

        registry.setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !registry.getRegisteredRoutes().isEmpty());

        Assert.assertTrue("Path for main route 'withAliases' returned empty",
                registry.getNavigationTarget("withAliases").isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                registry.getNavigationTarget("version").isPresent());
        Assert.assertTrue("RouteAlias 'person' returned empty.",
                registry.getNavigationTarget("person").isPresent());

        registry.removeRoute(MyRouteWithAliases.class);

        Assert.assertFalse(
                "Registry should be empty after removing the only registered Class.",
                !registry.getRegisteredRoutes().isEmpty());
    }

    @Test
    public void registeredRouteWithAlias_removingPathLeavesAliases() {

        registry.setRoute("withAliases", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("person", MyRouteWithAliases.class,
                Collections.emptyList());

        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !registry.getRegisteredRoutes().isEmpty());

        Assert.assertTrue("Path for main route 'withAliases' returned empty",
                registry.getNavigationTarget("withAliases").isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                registry.getNavigationTarget("version").isPresent());
        Assert.assertTrue("RouteAlias 'person' returned empty.",
                registry.getNavigationTarget("person").isPresent());

        registry.removeRoute("withAliases");

        Assert.assertTrue("Registry should contain alias routes",
                !registry.getRegisteredRoutes().isEmpty());

        Assert.assertEquals(
                "One RouteAlias should be the main url so only 1 route alias should be marked as an alias",
                1,
                registry.getRegisteredRoutes().get(0).getRouteAliases().size());
    }

    @Test
    public void routesWithParentLayouts_parentLayoutReturnsAsExpected() {
        registry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.singletonList(MainLayout.class));
        registry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        Assert.assertFalse("'MyRoute' should have a single parent",
                registry.getRouteLayouts("MyRoute", MyRouteWithAliases.class)
                        .isEmpty());
        Assert.assertTrue("'info' should have no parents.",
                registry.getRouteLayouts("info", MyRouteWithAliases.class)
                        .isEmpty());
        Assert.assertEquals("'version' should return two parents", 2,
                registry.getRouteLayouts("version", MyRouteWithAliases.class)
                        .size());
    }

    @Test
    public void registeredParentLayouts_changingListDoesntChangeRegistration() {
        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        registry.setRoute("version", MyRoute.class, parentChain);

        parentChain.remove(MainLayout.class);

        Assert.assertEquals(
                "'version' should return two parents even when original list is changed",
                2, registry.getRouteLayouts("version", MyRoute.class).size());
    }

    @Test
    public void registeredParentLayouts_returnedListInSameOrder() {
        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        registry.setRoute("version", MyRoute.class, parentChain);

        Assert.assertArrayEquals(
                "Registry should return parent layouts in the same order as set.",
                parentChain.toArray(),
                registry.getRouteLayouts("version", MyRoute.class).toArray());
    }

    @Test
    public void updateRoutesFromMultipleThreads_allRoutesAreRegistered()
            throws InterruptedException, ExecutionException {

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                registry.setRoute("home", MyRoute.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute("info", MyInfo.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute("palace", MyPalace.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future<Result>> futures = executorService.invokeAll(callables);

        executorService.shutdown();

        List<String> exceptions = new ArrayList<>();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.value != null) {
                exceptions.add(result.value);
            }
        }

        Assert.assertEquals(
                "No exceptions should have been thrown for threaded updates.",
                0, exceptions.size());

        Assert.assertTrue("Route 'home' was not registered into the scope.",
                registry.getNavigationTarget("home").isPresent());
        Assert.assertTrue("Route 'info' was not registered into the scope.",
                registry.getNavigationTarget("info").isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                registry.getNavigationTarget("palace").isPresent());
    }

    @Test
    public void updateAndRemoveFromMultipleThreads_endResultAsExpected()
            throws InterruptedException, ExecutionException {

        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("info", MyInfo.class, Collections.emptyList());

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                registry.removeRoute("info");
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute("modular", MyModular.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute("palace", MyPalace.class,
                        Collections.emptyList());
                registry.removeRoute("home");
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<Future<Result>> futures = executorService.invokeAll(callables);

        executorService.shutdown();

        List<String> exceptions = new ArrayList<>();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.value != null) {
                exceptions.add(result.value);
            }
        }

        Assert.assertEquals(
                "No exceptions should have been thrown for threaded updates.",
                0, exceptions.size());

        Assert.assertFalse(
                "Route 'home' was still registered even though it should have been removed.",
                registry.getNavigationTarget("home").isPresent());

        Assert.assertFalse(
                "Route 'info' was still registered even though it should have been removed.",
                registry.getNavigationTarget("info").isPresent());

        Assert.assertTrue("Route 'modular' was not registered into the scope.",
                registry.getNavigationTarget("modular").isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                registry.getNavigationTarget("palace").isPresent());
    }

    private static class Result {
        final String value;

        Result(String value) {
            this.value = value;
        }
    }

    @Test
    public void lockingConfiguration_newConfigurationIsGottenOnlyAfterUnlock() {
        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());

            Assert.assertTrue("Registry should still remain empty",
                    registry.getRegisteredRoutes().isEmpty());

            registry.setRoute("path", Secondary.class, Collections.emptyList());

            Assert.assertTrue("Registry should still remain empty",
                    registry.getRegisteredRoutes().isEmpty());
        });

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                2, registry.getRegisteredRoutes().size());
    }

    @Test
    public void routeChangeListener_correctChangesAreReturned() {
        List<RouteData> added = new ArrayList<>();
        List<RouteData> removed = new ArrayList<>();

        registry.addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        registry.setRoute("", MyRoute.class, Collections.emptyList());

        Assert.assertFalse("Added should contain data for one entry",
                added.isEmpty());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        Assert.assertEquals(MyRoute.class, added.get(0).getNavigationTarget());
        Assert.assertEquals("", added.get(0).getUrl());

        registry.setRoute("home", Secondary.class, Collections.emptyList());

        Assert.assertFalse("Added should contain data for one entry",
                added.isEmpty());
        Assert.assertEquals("Only latest change should be available", 1,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        Assert.assertEquals(Secondary.class,
                added.get(0).getNavigationTarget());
        Assert.assertEquals("home", added.get(0).getUrl());

        registry.removeRoute("home");

        Assert.assertTrue("No routes should have been added", added.isEmpty());
        Assert.assertFalse("One route should have gotten removed",
                removed.isEmpty());

        Assert.assertEquals(Secondary.class,
                removed.get(0).getNavigationTarget());
        Assert.assertEquals("The 'home' route should have been removed", "home",
                removed.get(0).getUrl());
    }

    @Test
    public void routeChangeListener_blockChangesAreGivenCorrectlyInEvent() {
        registry.setRoute("", MyRoute.class, Collections.emptyList());

        List<RouteData> added = new ArrayList<>();
        List<RouteData> removed = new ArrayList<>();

        registry.addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        registry.update(() -> {
            registry.removeRoute("");
            registry.setRoute("path", Secondary.class, Collections.emptyList());
            registry.setRoute("", MyRoute.class,
                    Collections.singletonList(MainLayout.class));
        });

        Assert.assertFalse("", added.isEmpty());
        Assert.assertEquals("", 2, added.size());
        Assert.assertFalse("", removed.isEmpty());

        for (RouteData data : added) {
            if (data.getUrl().equals("")) {
                Assert.assertEquals("MyRoute should have been added",
                        MyRoute.class, data.getNavigationTarget());
                Assert.assertEquals(
                        "MyRoute should have been seen as a update as the parent layouts changed.",
                        MainLayout.class, data.getParentLayout());
            } else {
                Assert.assertEquals("", Secondary.class,
                        data.getNavigationTarget());
            }
        }

        Assert.assertEquals("MyRoute should have been both removed and added",
                MyRoute.class, removed.get(0).getNavigationTarget());
        Assert.assertEquals("Removed version should not have a parent layout",
                UI.class, removed.get(0).getParentLayout());
    }

    @Tag("div")
    @Route("home")
    private static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("info")
    private static class MyInfo extends Component {
    }

    @Tag("div")
    @Route("palace")
    private static class MyPalace extends Component {
    }

    @Tag("div")
    private static class Secondary extends Component {
    }

    @Tag("div")
    @Route("modular")
    private static class MyModular extends Component {
    }

    @Tag("div")
    @Route("withAliases")
    @RouteAlias("version")
    @RouteAlias("person")
    private static class MyRouteWithAliases extends Component {
    }

    @Tag("div")
    private static class MainLayout extends Component implements RouterLayout {
    }

    @Tag("div")
    private static class MiddleLayout extends Component
            implements RouterLayout {
    }
}

package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.RouteRegistry;

public class GlobalRouteRegistryTest {

    private RouteRegistry registry;

    @Before
    public void init() {
        registry = GlobalRouteRegistry
                .getInstance(Mockito.mock(ServletContext.class));
    }

    @Test
    public void initalizedRoutes_routesCanBeAdded() {
        registry.setNavigationTargets(Stream.of(MyRoute.class, MyInfo.class)
                .collect(Collectors.toSet()));
        Assert.assertEquals(
                "Initial registration of routes should have succeeded.", 2,
                registry.getRegisteredRoutes().size());

        registry.setNavigationTargets(Stream.of(MyPalace.class, MyModular.class)
                .collect(Collectors.toSet()));

        Assert.assertEquals("All new routes should have been registered", 4,
                registry.getRegisteredRoutes().size());

        registry.setRoute(MyRouteWithAliases.class);

        Assert.assertEquals("The new route should have registered", 5,
                registry.getRegisteredRoutes().size());
    }

    @Test
    public void registeringRouteWithAlias_RouteDataIsPopulatedCorrectly() {

        registry.setNavigationTargets(Stream.of(MyRoute.class, MyInfo.class)
                .collect(Collectors.toSet()));
        registry.setRoute(MyRouteWithAliases.class);

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

        registry.setRoute(MyRouteWithAliases.class);

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

        registry.setRoute(MyRouteWithAliases.class);

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
    public void updateRoutesFromMultipleThreads_allRoutesAreRegistered()
            throws InterruptedException, ExecutionException {

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                registry.setRoute(MyRoute.class);
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute(MyInfo.class);
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute(MyPalace.class);
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

        registry.setRoute(MyRoute.class);
        registry.setRoute(MyInfo.class);

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
                registry.setRoute(MyModular.class);
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                registry.setRoute(MyPalace.class);
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
    private static class Secondary extends Component {
    }

    @Tag("div")
    private static class Parameter extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Tag("div")
    private static class ErrorView extends Component
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 404;
        }
    }
}

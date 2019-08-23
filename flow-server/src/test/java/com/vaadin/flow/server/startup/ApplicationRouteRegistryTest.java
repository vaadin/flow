package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Tests for {@link ApplicationRouteRegistry} instance inside OSGi container.
 */
public class ApplicationRouteRegistryTest extends RouteRegistryTestBase {

    private ApplicationRouteRegistry registry;

    @Before
    public void init() {
        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(Mockito.mock(ServletContext.class)));
    }

    @Test
    public void assertApplicationRegistry() {
        Assert.assertEquals(ApplicationRouteRegistry.class,
                getTestedRegistry().getClass());
    }

    @Test
    public void updateRoutesFromMultipleThreads_allRoutesAreRegistered()
            throws InterruptedException, ExecutionException {

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                getTestedRegistry().setRoute("home", MyRoute.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getTestedRegistry().setRoute("info", MyInfo.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getTestedRegistry().setRoute("palace", MyPalace.class,
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
                getTestedRegistry().getNavigationTarget("home").isPresent());
        Assert.assertTrue("Route 'info' was not registered into the scope.",
                getTestedRegistry().getNavigationTarget("info").isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                getTestedRegistry().getNavigationTarget("palace").isPresent());
    }

    @Test
    public void updateAndRemoveFromMultipleThreads_endResultAsExpected()
            throws InterruptedException, ExecutionException {

        getTestedRegistry().setRoute("home", MyRoute.class,
                Collections.emptyList());
        getTestedRegistry().setRoute("info", MyInfo.class,
                Collections.emptyList());

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                getTestedRegistry().removeRoute("info");
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getTestedRegistry().setRoute("modular", MyModular.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getTestedRegistry().setRoute("palace", MyPalace.class,
                        Collections.emptyList());
                getTestedRegistry().removeRoute("home");
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
                getTestedRegistry().getNavigationTarget("home").isPresent());

        Assert.assertFalse(
                "Route 'info' was still registered even though it should have been removed.",
                getTestedRegistry().getNavigationTarget("info").isPresent());

        Assert.assertTrue("Route 'modular' was not registered into the scope.",
                getTestedRegistry().getNavigationTarget("modular").isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                getTestedRegistry().getNavigationTarget("palace").isPresent());
    }

    private static class Result {
        final String value;

        Result(String value) {
            this.value = value;
        }
    }

    @Test
    public void lockingConfiguration_newConfigurationIsGottenOnlyAfterUnlock() {
        CountDownLatch waitReaderThread = new CountDownLatch(1);
        CountDownLatch waitUpdaterThread = new CountDownLatch(2);

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        getTestedRegistry().getRegisteredRoutes().isEmpty());

                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        getTestedRegistry().getRegisteredRoutes().isEmpty());

                waitReaderThread.countDown();
            }
        };

        readerThread.start();

        getTestedRegistry().update(() -> {
            getTestedRegistry().setRoute("", MyRoute.class,
                    Collections.emptyList());

            waitUpdaterThread.countDown();

            getTestedRegistry().setRoute("path", Secondary.class,
                    Collections.emptyList());

            waitUpdaterThread.countDown();
            awaitCountDown(waitReaderThread);

        });

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                2, getTestedRegistry().getRegisteredRoutes().size());
    }

    @Test
    public void routeChangeListener_correctChangesAreReturned() {
        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        getTestedRegistry().addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        getTestedRegistry().setRoute("", MyRoute.class,
                Collections.emptyList());

        Assert.assertFalse("Added should contain data for one entry",
                added.isEmpty());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        Assert.assertEquals(MyRoute.class, added.get(0).getNavigationTarget());
        Assert.assertEquals("", added.get(0).getUrl());

        getTestedRegistry().setRoute("home", Secondary.class,
                Collections.emptyList());

        Assert.assertFalse("Added should contain data for one entry",
                added.isEmpty());
        Assert.assertEquals("Only latest change should be available", 1,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        Assert.assertEquals(Secondary.class,
                added.get(0).getNavigationTarget());
        Assert.assertEquals("home", added.get(0).getUrl());

        getTestedRegistry().removeRoute("home");

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
        getTestedRegistry().setRoute("", MyRoute.class,
                Collections.emptyList());

        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        getTestedRegistry().addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        getTestedRegistry().update(() -> {
            getTestedRegistry().removeRoute("");
            getTestedRegistry().setRoute("path", Secondary.class,
                    Collections.emptyList());
            getTestedRegistry().setRoute("", MyRoute.class,
                    Collections.singletonList(MainLayout.class));
        });

        Assert.assertFalse("", added.isEmpty());
        Assert.assertEquals("", 2, added.size());
        Assert.assertFalse("", removed.isEmpty());

        for (RouteBaseData data : added) {
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
                Collections.emptyList(), removed.get(0).getParentLayouts());
    }

    @Test
    public void routeWithAliases_eventShowsCorrectlyAsRemoved() {
        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        getTestedRegistry().addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        getTestedRegistry().update(() -> {
            getTestedRegistry().setRoute("main", Secondary.class,
                    Collections.emptyList());
            getTestedRegistry().setRoute("Alias1", Secondary.class,
                    Collections.emptyList());
            getTestedRegistry().setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        Assert.assertEquals(
                "Main route and aliases should all be seen as added.", 3,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        getTestedRegistry().removeRoute("Alias2");

        Assert.assertTrue("No routes should have been added", added.isEmpty());
        Assert.assertEquals(
                "Removing the alias route should be seen in the event", 1,
                removed.size());
    }

    @Override
    protected RouteRegistry getInitializationRegistry() {
        return registry;
    }

    @Override
    protected RouteRegistry getTestedRegistry() {
        return registry;
    }

    private void awaitCountDown(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

}

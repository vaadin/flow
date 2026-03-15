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

import jakarta.servlet.ServletContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServletContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link ApplicationRouteRegistry} instance .
 */
class ApplicationRouteRegistryTest extends RouteRegistryTestBase {

    private ApplicationRouteRegistry registry;

    @BeforeEach
    public void init() {
        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(Mockito.mock(ServletContext.class)));
    }

    @Test
    public void assertApplicationRegistry() {
        assertEquals(ApplicationRouteRegistry.class,
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

        assertEquals(0, exceptions.size(),
                "No exceptions should have been thrown for threaded updates.");

        assertTrue(getTestedRegistry().getNavigationTarget("home").isPresent(),
                "Route 'home' was not registered into the scope.");
        assertTrue(getTestedRegistry().getNavigationTarget("info").isPresent(),
                "Route 'info' was not registered into the scope.");
        assertTrue(
                getTestedRegistry().getNavigationTarget("palace").isPresent(),
                "Route 'palace' was not registered into the scope.");
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

        assertEquals(0, exceptions.size(),
                "No exceptions should have been thrown for threaded updates.");

        assertFalse(getTestedRegistry().getNavigationTarget("home").isPresent(),
                "Route 'home' was still registered even though it should have been removed.");

        assertFalse(getTestedRegistry().getNavigationTarget("info").isPresent(),
                "Route 'info' was still registered even though it should have been removed.");

        assertTrue(
                getTestedRegistry().getNavigationTarget("modular").isPresent(),
                "Route 'modular' was not registered into the scope.");
        assertTrue(
                getTestedRegistry().getNavigationTarget("palace").isPresent(),
                "Route 'palace' was not registered into the scope.");
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

                assertTrue(getTestedRegistry().getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

                awaitCountDown(waitUpdaterThread);

                assertTrue(getTestedRegistry().getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

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

        assertEquals(2, getTestedRegistry().getRegisteredRoutes().size(),
                "After unlock registry should be updated for others to configure with new data");
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

        assertFalse(added.isEmpty(), "Added should contain data for one entry");
        assertTrue(removed.isEmpty(), "No routes should have been removed");

        assertEquals(MyRoute.class, added.get(0).getNavigationTarget());
        assertEquals("", added.get(0).getTemplate());

        getTestedRegistry().setRoute("home", Secondary.class,
                Collections.emptyList());

        assertFalse(added.isEmpty(), "Added should contain data for one entry");
        assertEquals(1, added.size(), "Only latest change should be available");
        assertTrue(removed.isEmpty(), "No routes should have been removed");

        assertEquals(Secondary.class, added.get(0).getNavigationTarget());
        assertEquals("home", added.get(0).getTemplate());

        getTestedRegistry().removeRoute("home");

        assertTrue(added.isEmpty(), "No routes should have been added");
        assertFalse(removed.isEmpty(), "One route should have gotten removed");

        assertEquals(Secondary.class, removed.get(0).getNavigationTarget());
        assertEquals("home", removed.get(0).getTemplate(),
                "The 'home' route should have been removed");
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

        assertFalse(added.isEmpty(), "");
        assertEquals(2, added.size(), "");
        assertFalse(removed.isEmpty(), "");

        for (RouteBaseData data : added) {
            if (data.getTemplate().equals("")) {
                assertEquals(MyRoute.class, data.getNavigationTarget(),
                        "MyRoute should have been added");
                assertEquals(MainLayout.class, data.getParentLayout(),
                        "MyRoute should have been seen as a update as the parent layouts changed.");
            } else {
                assertEquals(Secondary.class, data.getNavigationTarget(), "");
            }
        }

        assertEquals(MyRoute.class, removed.get(0).getNavigationTarget(),
                "MyRoute should have been both removed and added");
        assertEquals(Collections.emptyList(), removed.get(0).getParentLayouts(),
                "Removed version should not have a parent layout");
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

        assertEquals(3, added.size(),
                "Main route and aliases should all be seen as added.");
        assertTrue(removed.isEmpty(), "No routes should have been removed");

        getTestedRegistry().removeRoute("Alias2");

        assertTrue(added.isEmpty(), "No routes should have been added");
        assertEquals(1, removed.size(),
                "Removing the alias route should be seen in the event");
    }

    @Test
    public void setErrorNavigationTargets_abstractClassesAreIgnored() {
        registry.setErrorNavigationTargets(new HashSet<>(
                Arrays.asList(ErrorView.class, AbstractErrorView.class)));

        Optional<ErrorTargetEntry> errorNavigationTarget = registry
                .getErrorNavigationTarget(new NullPointerException());

        assertTrue(errorNavigationTarget.isPresent(),
                "Error navigation target was not registered");
        assertEquals(ErrorView.class,
                errorNavigationTarget.get().getNavigationTarget(),
                "Wrong errorNavigationTarget was registered");
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
            fail();
        }
    }

}

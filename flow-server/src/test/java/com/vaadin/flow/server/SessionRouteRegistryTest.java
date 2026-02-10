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
package com.vaadin.flow.server;

import jakarta.servlet.ServletContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionRouteRegistryTest {

    private ApplicationRouteRegistry registry;
    private MockService vaadinService;
    private VaadinSession session;

    @BeforeEach
    public void init() {
        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(Mockito.mock(ServletContext.class)));

        vaadinService = Mockito.mock(MockService.class);
        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getFrontendFolder())
                .thenReturn(new File("/frontend"));
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinServletContext context = new MockVaadinContext();

        ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);

        context.setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);

        Mockito.when(vaadinService.getContext()).thenReturn(context);
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(true);

        VaadinService.setCurrent(vaadinService);

        session = new MockVaadinSession(vaadinService) {
            @Override
            public VaadinService getService() {
                return vaadinService;
            }
        };
    }

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    /**
     * Get registry by handing the session lock correctly.
     *
     * @param session
     *            target vaadin session
     * @return session route registry for session if exists or new.
     */
    private SessionRouteRegistry getRegistry(VaadinSession session) {
        try {
            session.lock();
            return (SessionRouteRegistry) SessionRouteRegistry
                    .getSessionRegistry(session);
        } finally {
            session.unlock();
        }
    }

    @Test
    public void addSameClassForMultipleRoutes_removalOfRouteClassClearsRegisttry() {
        SessionRouteRegistry registry = getRegistry(session);

        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("info", MyRoute.class, Collections.emptyList());
        registry.setRoute("path", MyRoute.class, Collections.emptyList());
        registry.setRoute("palace", MyRoute.class, Collections.emptyList());

        Assertions.assertTrue(!registry.getRegisteredRoutes().isEmpty(),
                "Registry didn't contain navigation targets even though some were registered");

        registry.removeRoute(MyRoute.class);

        Assertions.assertFalse(!registry.getRegisteredRoutes().isEmpty(),
                "Registry should be empty as only one class was registered");
    }

    @Test
    public void addMultipleClassesToSameRoute_removeClassLeavesRoute() {
        SessionRouteRegistry registry = getRegistry(session);

        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("home", Parameter.class, Collections.emptyList());

        Assertions.assertTrue(!registry.getRegisteredRoutes().isEmpty(),
                "Registry didn't contain navigation targets even though some were registered");

        Assertions.assertEquals(MyRoute.class,
                registry.getNavigationTarget("home").get(),
                "No parameters route class was expected for only path String.");
        Assertions.assertEquals(MyRoute.class,
                registry.getNavigationTarget("home", Collections.emptyList())
                        .get(),
                "No parameters route class was expected for empty segments.");
        Assertions.assertEquals(Parameter.class,
                registry.getNavigationTarget("home", Arrays.asList("param"))
                        .get(),
                "Expected HasRouteParameters class for request with segments.");

        registry.removeRoute(MyRoute.class);

        Assertions.assertTrue(!registry.getRegisteredRoutes().isEmpty(),
                "Registry is empty even though we should have one route available");
        Assertions.assertFalse(registry.getTargetUrl(MyRoute.class).isPresent(),
                "MyRoute should have been removed from the registry.");
        Assertions.assertTrue(
                registry.getTargetUrl(Parameter.class,
                        HasUrlParameterFormat.getParameters("foo")).isPresent(),
                "Parameter class should have been available from the registry");
        Assertions.assertTrue(registry.getTemplate(Parameter.class).isPresent(),
                "Parameter class should have been available from the registry");
        Assertions.assertEquals(Parameter.class, registry
                .getNavigationTarget("home", Arrays.asList("param")).get(),
                "Parameter route should have been available.");
    }

    @Test
    public void sessionRegistryOverridesParentRegistryForGetTargetUrl_globalRouteStillAccessible() {
        registry.setRoute("MyRoute", MyRoute.class, Collections.emptyList());
        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("alternate", MyRoute.class,
                Collections.emptyList());

        Assertions.assertEquals("alternate",
                sessionRegistry.getTargetUrl(MyRoute.class).get(),
                "Expected session registry route to be returned");

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("alternate").isPresent(),
                "Route 'alternate' should be available.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Route 'MyRoute' should be available.");
    }

    @Test
    public void sessionRegistryOverridesParentRegistryWithOwnClass_globalRouteReturnedAfterClassRemoval() {
        registry.setRoute("MyRoute", MyRoute.class, Collections.emptyList());
        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("MyRoute", Secondary.class,
                Collections.emptyList());

        Assertions.assertEquals(Secondary.class,
                sessionRegistry.getNavigationTarget("MyRoute").get(),
                "Route 'MyRoute' should return Secondary as registered to SessionRegistry.");

        sessionRegistry.removeRoute(Secondary.class);

        Assertions.assertEquals(MyRoute.class,
                sessionRegistry.getNavigationTarget("MyRoute").get(),
                "Route 'MyRoute' should return MyRoute as registered to GlobalRegistry.");
    }

    @Test
    public void registerRouteWithAliases_routeAliasesRegisteredAsExpected() {

        SessionRouteRegistry sessionRegistry = getRegistry(session);

        // register route and have default path "MyRoute"
        // also should also register "info" and "version"
        sessionRegistry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.emptyList());
        sessionRegistry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        sessionRegistry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Main route was not registered for given Class.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("info").isPresent(),
                "RouteAlias 'info' was not registered for given Class.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' was not registered for given Class.");
    }

    @Test
    public void routesWithParentLayouts_parentLayoutReturnsAsExpected() {
        SessionRouteRegistry sessionRegistry = getRegistry(session);

        sessionRegistry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.singletonList(MainLayout.class));
        sessionRegistry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        sessionRegistry.setRoute("version", MyRouteWithAliases.class,
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        Assertions.assertFalse(
                sessionRegistry.getNavigationRouteTarget("MyRoute")
                        .getRouteTarget().getParentLayouts().isEmpty(),
                "'MyRoute' should have a single parent");
        Assertions.assertTrue(
                sessionRegistry.getNavigationRouteTarget("info")
                        .getRouteTarget().getParentLayouts().isEmpty(),
                "'info' should have no parents.");
        Assertions.assertEquals(2,
                sessionRegistry.getNavigationRouteTarget("version")
                        .getRouteTarget().getParentLayouts().size(),
                "'version' should return two parents");
    }

    @Test
    public void registeredParentLayouts_changingListDoesntChangeRegistration() {
        SessionRouteRegistry registry = getRegistry(session);

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        registry.setRoute("version", MyRoute.class, parentChain);

        parentChain.remove(MainLayout.class);

        Assertions.assertEquals(2,
                registry.getNavigationRouteTarget("version").getRouteTarget()
                        .getParentLayouts().size(),
                "'version' should return two parents even when original list is changed");
    }

    @Test
    public void registeredParentLayouts_returnedListInSameOrder() {
        SessionRouteRegistry registry = getRegistry(session);

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        registry.setRoute("version", MyRoute.class, parentChain);

        Assertions.assertArrayEquals(parentChain.toArray(),
                registry.getNavigationRouteTarget("version").getRouteTarget()
                        .getParentLayouts().toArray(),
                "Registry should return parent layouts in the same order as set.");
    }

    @Test
    public void routeRegisteredOnMultiplePaths_removalOfDefaultPathUpdatesDefaultPath() {
        SessionRouteRegistry sessionRegistry = getRegistry(session);

        // register route and have default path "MyRoute"
        // also should also register "info" and "version"
        sessionRegistry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.emptyList());
        sessionRegistry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        sessionRegistry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Main route was not registered for given Class.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("info").isPresent(),
                "RouteAlias 'info' was not registered for given Class.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' was not registered for given Class.");

        Assertions.assertEquals("MyRoute",
                sessionRegistry.getTargetUrl(MyRouteWithAliases.class).get());

        sessionRegistry.removeRoute("MyRoute");

        Assertions.assertFalse(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Route 'MyRoute' was still available even though it should have been removed.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("info").isPresent(),
                "Route 'info' has been removed eve though it should still be available");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("version").isPresent(),
                "Route 'version' has been removed eve though it should still be available");

        Assertions.assertTrue(
                sessionRegistry.getTargetUrl(MyRouteWithAliases.class)
                        .isPresent(),
                "Route was not found from the registry anymore even though it should be available.");

        // Either or is expected as the new default as first match is picked
        // from the map
        Assertions.assertTrue(
                Arrays.asList("info", "version")
                        .contains(sessionRegistry
                                .getTargetUrl(MyRouteWithAliases.class).get()),
                "Route didn't return a url matching either of the expected aliases.");
    }

    @Test
    public void manuallyRegisteredAliases_RouteDataIsReturnedCorrectly() {

        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("main", Secondary.class,
                Collections.emptyList());
        sessionRegistry.setRoute("Alias1", Secondary.class,
                Collections.emptyList());
        sessionRegistry.setRoute("Alias2", Secondary.class,
                Collections.emptyList());

        List<RouteData> registeredRoutes = sessionRegistry
                .getRegisteredRoutes();
        Assertions.assertTrue(!registeredRoutes.isEmpty(),
                "Registry didn't contain routes even though 3 should have been registered");

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("main").isPresent(),
                "Path for main route 'main' returned empty");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("Alias1").isPresent(),
                "RouteAlias 'Alias1' returned empty.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("Alias2").isPresent(),
                "RouteAlias 'Alias2' returned empty.");

        Assertions.assertEquals(2,
                registeredRoutes.get(0).getRouteAliases().size(),
                "Two 'RouteAlias'es should be registered in the collected route data.");

        sessionRegistry.removeRoute("main");

        registeredRoutes = sessionRegistry.getRegisteredRoutes();

        Assertions.assertTrue(!registeredRoutes.isEmpty(),
                "Registry should still contain the alias routes");

        Assertions.assertEquals(1,
                registeredRoutes.get(0).getRouteAliases().size(),
                "One RouteAlias should be the main url so only 1 route alias should be marked as an alias");
    }

    @Test
    public void registeredRouteWithAliasGlobally_sessionRegistryReturnsFromGlobal() {
        registry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());

        SessionRouteRegistry sessionRegistry = getRegistry(session);

        List<RouteData> registeredRoutes = sessionRegistry
                .getRegisteredRoutes();
        Assertions.assertTrue(!registeredRoutes.isEmpty(),
                "Registry didn't contain routes even though 3 should have been registered");

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Path for main route 'MyRoute' returned empty");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("info").isPresent(),
                "RouteAlias 'info' returned empty.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' returned empty.");

        Assertions.assertEquals(2,
                registeredRoutes.get(0).getRouteAliases().size(),
                "Both route aliases should be found for Route");
    }

    @Test
    public void registeredRouteWithAliasGlobally_sessionRegistryOverridesMainUrl() {
        registry.setRoute("MyRoute", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("info", MyRouteWithAliases.class,
                Collections.emptyList());
        registry.setRoute("version", MyRouteWithAliases.class,
                Collections.emptyList());

        SessionRouteRegistry sessionRegistry = getRegistry(session);

        sessionRegistry.setRoute("MyRoute", Secondary.class,
                Collections.emptyList());

        Assertions.assertTrue(!sessionRegistry.getRegisteredRoutes().isEmpty(),
                "Registry didn't contain routes.");

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("MyRoute").isPresent(),
                "Path for main route 'MyRoute' returned empty");
        Assertions.assertEquals(Secondary.class,
                sessionRegistry.getNavigationTarget("MyRoute").get(),
                "Navigation target for route 'MyRoute' was not the expected one.");

        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("info").isPresent(),
                "RouteAlias 'info' returned empty.");
        Assertions.assertTrue(
                sessionRegistry.getNavigationTarget("version").isPresent(),
                "RouteAlias 'version' returned empty.");

        Assertions.assertTrue(
                sessionRegistry.getRegisteredRoutes().get(0).getRouteAliases()
                        .isEmpty(),
                "Both route aliases should be found for Route");
    }

    @Test
    public void setSameRouteValueFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<Result>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<Result> callable = () -> {
                        try {
                            SessionRouteRegistry sessionRegistry = getRegistry(
                                    session);
                            sessionRegistry.setRoute("MyRoute", MyRoute.class,
                                    Collections.emptyList());
                        } catch (Exception e) {
                            return new Result(e.getMessage());
                        }
                        return new Result(null);
                    };
                    return callable;
                }).collect(Collectors.toList());

        List<Future<Result>> futures = executorService.invokeAll(callables);
        List<String> exceptions = new ArrayList<>();

        executorService.shutdown();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.value != null) {
                exceptions.add(result.value);
            }
        }

        Assertions.assertEquals(THREADS - 1, exceptions.size(),
                "Expected 4 route already exists exceptions due to route target validation");
        String expected = String.format(RouteUtil.ROUTE_CONFLICT,
                MyRoute.class.getName(), MyRoute.class.getName());
        for (String exception : exceptions) {
            Assertions.assertEquals(expected, exception);
        }
        Optional<Class<? extends Component>> myRoute = getRegistry(session)
                .getNavigationTarget("MyRoute");
        Assertions.assertTrue(myRoute.isPresent(),
                "MyRoute was missing from the session scope registry.");

    }

    @Test
    public void useRouteResolutionFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<Result>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<Result> callable = () -> {
                        try {
                            SessionRouteRegistry sessionRegistry = getRegistry(
                                    session);
                            sessionRegistry.setRoute("MyRoute", MyRoute.class,
                                    Collections.emptyList());
                        } catch (Exception e) {
                            return new Result(e.getMessage());
                        }
                        return new Result(null);
                    };
                    return callable;
                }).collect(Collectors.toList());

        List<Future<Result>> futures = executorService.invokeAll(callables);
        List<String> exceptions = new ArrayList<>();

        executorService.shutdown();

        for (Future<Result> resultFuture : futures) {
            Result result = resultFuture.get();
            if (result.value != null) {
                exceptions.add(result.value);
            }
        }

        Assertions.assertEquals(THREADS - 1, exceptions.size(),
                "Expected 4 route already exists exceptions due to route target validation");
        String expected = String.format(RouteUtil.ROUTE_CONFLICT,
                MyRoute.class.getName(), MyRoute.class.getName());
        for (String exception : exceptions) {
            Assertions.assertEquals(expected, exception);
        }
        Optional<Class<? extends Component>> myRoute = getRegistry(session)
                .getNavigationTarget("MyRoute");
        Assertions.assertTrue(myRoute.isPresent(),
                "MyRoute was missing from the session scope registry.");

    }

    @Test
    public void updateRoutesFromMultipleThreads_allRoutesAreRegistered()
            throws InterruptedException, ExecutionException {

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                getRegistry(session).setRoute("home", MyRoute.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getRegistry(session).setRoute("info", MyRoute.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getRegistry(session).setRoute("palace", MyRoute.class,
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

        Assertions.assertEquals(0, exceptions.size(),
                "No exceptions should have been thrown for threaded updates.");

        Assertions.assertTrue(
                getRegistry(session).getNavigationTarget("home").isPresent(),
                "Route 'home' was not registered into the scope.");
        Assertions.assertTrue(
                getRegistry(session).getNavigationTarget("info").isPresent(),
                "Route 'info' was not registered into the scope.");
        Assertions.assertTrue(
                getRegistry(session).getNavigationTarget("palace").isPresent(),
                "Route 'palace' was not registered into the scope.");
    }

    @Test
    public void updateAndRemoveFromMultipleThreads_endResultAsExpected()
            throws InterruptedException, ExecutionException {

        getRegistry(session).setRoute("home", MyRoute.class,
                Collections.emptyList());
        getRegistry(session).setRoute("info", MyRoute.class,
                Collections.emptyList());

        List<Callable<Result>> callables = new ArrayList<>();
        callables.add(() -> {
            try {
                getRegistry(session).removeRoute("info");
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getRegistry(session).setRoute("modular", MyRoute.class,
                        Collections.emptyList());
            } catch (Exception e) {
                return new Result(e.getMessage());
            }
            return new Result(null);
        });

        callables.add(() -> {
            try {
                getRegistry(session).setRoute("palace", MyRoute.class,
                        Collections.emptyList());
                getRegistry(session).removeRoute("home");
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

        Assertions.assertEquals(0, exceptions.size(),
                "No exceptions should have been thrown for threaded updates.");

        Assertions.assertFalse(
                getRegistry(session).getNavigationTarget("home").isPresent(),
                "Route 'home' was still registered even though it should have been removed.");

        Assertions.assertFalse(
                getRegistry(session).getNavigationTarget("info").isPresent(),
                "Route 'info' was still registered even though it should have been removed.");

        Assertions.assertTrue(
                getRegistry(session).getNavigationTarget("modular").isPresent(),
                "Route 'modular' was not registered into the scope.");
        Assertions.assertTrue(
                getRegistry(session).getNavigationTarget("palace").isPresent(),
                "Route 'palace' was not registered into the scope.");
    }

    @Test
    public void settingSessionRouteRegistryOfAnotherSession_getRegistryFails() {
        assertThrows(IllegalStateException.class, () -> {
            SessionRouteRegistry registry = getRegistry(session);

            VaadinSession anotherSession = new MockVaadinSession(
                    vaadinService) {
                @Override
                public VaadinService getService() {
                    return vaadinService;
                }
            };

            SessionRouteRegistry anotherRegistry = getRegistry(anotherSession);
            Assertions.assertNotEquals(registry, anotherRegistry,
                    "Another session should receive another session");

            session.lock();
            try {
                session.setAttribute(SessionRouteRegistry.class,
                        anotherRegistry);
            } finally {
                session.unlock();
            }

            getRegistry(session);

            Assertions.fail(
                    "Setting anotherRegistry to session should fail when getting the registry!");
        });
    }

    private static class Result {
        final String value;

        Result(String value) {
            this.value = value;
        }
    }

    @Test
    public void lockingConfiguration_configurationIsUpdatedOnlyAfterUnlockk() {
        CountDownLatch waitReaderThread = new CountDownLatch(1);
        CountDownLatch waitUpdaterThread = new CountDownLatch(2);

        SessionRouteRegistry registry = getRegistry(session);

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                awaitCountDown(waitUpdaterThread);

                Assertions.assertTrue(
                        getRegistry(session).getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

                awaitCountDown(waitUpdaterThread);

                Assertions.assertTrue(
                        getRegistry(session).getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

                waitReaderThread.countDown();
            }
        };

        readerThread.start();

        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());

            waitUpdaterThread.countDown();

            registry.setRoute("path", Secondary.class, Collections.emptyList());

            waitUpdaterThread.countDown();
            awaitCountDown(waitReaderThread);
        });

        Assertions.assertEquals(2,
                getRegistry(session).getRegisteredRoutes().size(),
                "After unlock registry should be updated for others to configure with new data");
    }

    @Test
    public void routeChangeListener_correctChangesAreReturned() {
        SessionRouteRegistry registry = getRegistry(session);

        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        registry.addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        registry.setRoute("", MyRoute.class, Collections.emptyList());

        Assertions.assertFalse(added.isEmpty(),
                "Added should contain data for one entry");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        Assertions.assertEquals(MyRoute.class,
                added.get(0).getNavigationTarget());
        Assertions.assertEquals("", added.get(0).getTemplate());

        registry.setRoute("home", Secondary.class, Collections.emptyList());

        Assertions.assertFalse(added.isEmpty(),
                "Added should contain data for one entry");
        Assertions.assertEquals(1, added.size(),
                "Only latest change should be available");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        Assertions.assertEquals(Secondary.class,
                added.get(0).getNavigationTarget());
        Assertions.assertEquals("home", added.get(0).getTemplate());

        registry.removeRoute("home");

        Assertions.assertTrue(added.isEmpty(),
                "No routes should have been added");
        Assertions.assertEquals(1, removed.size(),
                "One route should have gotten removed");

        Assertions.assertEquals(Secondary.class,
                removed.get(0).getNavigationTarget());
        Assertions.assertEquals("home", removed.get(0).getTemplate(),
                "The 'home' route should have been removed");
    }

    @Test
    public void routeChangeListener_blockChangesAreGivenCorrectlyInEvent() {
        SessionRouteRegistry registry = getRegistry(session);

        registry.setRoute("", MyRoute.class, Collections.emptyList());

        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

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

        Assertions.assertEquals(2, added.size(),
                "Two ne paths should have been added");
        Assertions.assertEquals(1, removed.size(),
                "One path should have been removed");

        for (RouteBaseData data : added) {
            if (data.getTemplate().equals("")) {
                Assertions.assertEquals(MyRoute.class,
                        data.getNavigationTarget(),
                        "MyRoute should have been added");
                Assertions.assertEquals(MainLayout.class,
                        data.getParentLayout(),
                        "MyRoute should have been seen as a update as the parent layouts changed.");
            } else {
                Assertions.assertEquals(Secondary.class,
                        data.getNavigationTarget(), "");
            }
        }

        Assertions.assertEquals(MyRoute.class,
                removed.get(0).getNavigationTarget(),
                "One MyRoute should have been removed");
        Assertions.assertEquals(Collections.emptyList(),
                removed.get(0).getParentLayouts(),
                "Removed version should not have a parent layout");
    }

    @Test
    public void routeWithAliases_eventShowsCorrectlyAsRemoved() {
        SessionRouteRegistry sessionRegistry = getRegistry(session);

        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        sessionRegistry.addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        sessionRegistry.update(() -> {
            sessionRegistry.setRoute("main", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias1", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        Assertions.assertEquals(3, added.size(),
                "Main route and aliases should all be seen as added.");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        sessionRegistry.removeRoute("Alias2");

        Assertions.assertTrue(added.isEmpty(),
                "No routes should have been added");
        Assertions.assertEquals(1, removed.size(),
                "Removing the alias route should be seen in the event");
    }

    @Test
    public void maskedPathsInParent_eventContainsOnlyChangesVisibleForSession() {
        registry.setRoute("main", MyRoute.class, Collections.emptyList());

        SessionRouteRegistry sessionRegistry = getRegistry(session);

        List<RoutesChangedEvent> events = new ArrayList<>();

        sessionRegistry.update(() -> {
            sessionRegistry.setRoute("main", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias1", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        sessionRegistry.addRoutesChangeListener(events::add);

        registry.removeRoute(MyRoute.class);

        Assertions.assertTrue(events.isEmpty(),
                "No event for masked path should have been received.");

        registry.setRoute("main", MyRoute.class, Collections.emptyList());

        Assertions.assertTrue(events.isEmpty(),
                "No event for masked path should have been received.");

        registry.setRoute("home", Secondary.class, Collections.emptyList());

        Assertions.assertEquals(1, events.size(),
                "Addition of non masked path should have fired an event.");
        Assertions.assertEquals(registry, events.get(0).getSource(),
                "Source should have been ApplicationRouteRegistry");
        Assertions.assertEquals(1, events.get(0).getAddedRoutes().size(),
                "One route should have been added");
        Assertions.assertEquals(0, events.get(0).getRemovedRoutes().size(),
                "No routes should have been removed");
    }

    @Test
    public void removeListener_noEventsAreGottenForAnyRegistry() {

        SessionRouteRegistry sessionRegistry = getRegistry(session);

        List<RoutesChangedEvent> events = new ArrayList<>();

        Registration registration = sessionRegistry
                .addRoutesChangeListener(events::add);

        registry.setRoute("main", MyRoute.class, Collections.emptyList());
        sessionRegistry.update(() -> {
            sessionRegistry.setRoute("main", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias1", Secondary.class,
                    Collections.emptyList());
            sessionRegistry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        Assertions.assertEquals(2, events.size(),
                "One event for both registries should have been fired.");

        registration.remove();

        sessionRegistry.removeRoute("main");

        Assertions.assertEquals(2, events.size(),
                "No new event should have been received for session scope");

        registry.removeRoute("main");

        Assertions.assertEquals(2, events.size(),
                "No new event should have been received for application scope");

    }

    @Test
    public void serialize_deserialize_parentRegistryIsANewOne()
            throws Throwable {
        session = new MockVaadinSession(vaadinService);

        TestSessionRouteRegistry registry = new TestSessionRouteRegistry(
                session);

        TestSessionRouteRegistry deserialized = serializeAndDeserialize(
                registry);

        VaadinService service = new TestService();
        RouteRegistry newAppRegistry = service.getRouteRegistry();

        Mockito.when(newAppRegistry.getNavigationTarget("foo",
                Collections.emptyList()))
                .thenReturn(Optional.of(HtmlContainer.class));

        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        deserialized.session.refreshTransients(wrappedSession, service);

        // The original registry doesn't contain "foo" navigation target
        Assertions.assertEquals(Optional.empty(),
                registry.getNavigationTarget("foo", Collections.emptyList()));
        // The deserialized one (after refreshing transients) contains "foo"
        // navigation target
        Assertions.assertEquals(Optional.of(HtmlContainer.class), deserialized
                .getNavigationTarget("foo", Collections.emptyList()));

    }

    @Test
    public void getTargetUrl_annotatedRoute_rootIsAlias_mainRouteIsNotRoot_mainRouteIsReturned() {
        SessionRouteRegistry registry = getRegistry(session);
        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(registry);

        configuration.setAnnotatedRoute(RouteWithRootAlias.class);

        Optional<String> url = registry.getTargetUrl(RouteWithRootAlias.class,
                RouteParameters.empty());

        Assertions.assertTrue(url.isPresent());
        Assertions.assertEquals("foo", url.get());
    }

    @Test
    public void getTargetUrl_annotatedRoute_rootIsAlias_mainRouteIsParamerterized_routeAliasIsReturned() {
        SessionRouteRegistry registry = getRegistry(session);
        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(registry);

        configuration.setAnnotatedRoute(ParameterizedRouteWithRootAlias.class);

        Optional<String> url = registry.getTargetUrl(
                ParameterizedRouteWithRootAlias.class, RouteParameters.empty());

        Assertions.assertTrue(url.isPresent());
        Assertions.assertEquals("", url.get());
    }

    @Test
    public void sessionScopeContainsTemplateRoute_applicationRegistryExactMatchIsReturned() {
        registry.setRoute(":first/:second", Templated.class,
                Collections.emptyList());
        registry.setRoute("other/view", NonTemplated.class,
                Collections.emptyList());

        SessionRouteRegistry sessionRegistry = getRegistry(session);
        Assertions.assertEquals(Templated.class,
                sessionRegistry.getNavigationTarget("oh/my").get(),
                "ApplicationRegisty Templated should be found.");
        Assertions.assertEquals(NonTemplated.class,
                sessionRegistry.getNavigationTarget("other/view").get(),
                "ApplicationRegistry NonTemplated should be found");

        sessionRegistry.setRoute(":one/:two", Secondary.class,
                Collections.emptyList());

        Assertions.assertEquals(Secondary.class,
                sessionRegistry.getNavigationTarget("oh/my").get(),
                "SessionRegistry should override ApplicationRegistry Templated");

        Assertions.assertEquals(NonTemplated.class,
                sessionRegistry.getNavigationTarget("other/view").get(),
                "ApplicationRegistry exact match should be returned instead of SessionRegistry wildcard match");

        sessionRegistry.setRoute("other/:one", MyRoute.class,
                Collections.emptyList());

        Assertions.assertEquals(NonTemplated.class,
                sessionRegistry.getNavigationTarget("other/view").get(),
                "ApplicationRegistry exact match should be returned instead of any SessionRegistry wildcard match");
        Assertions.assertEquals(MyRoute.class,
                sessionRegistry.getNavigationTarget("other/plank").get(),
                "SessionRegistry best match with least wildcards should be returned");

    }

    @Test
    public void sessionScopeContainsTemplateRoute_applicationRegistryBetterMatchIsReturned() {
        registry.setRoute("other/view/parent", NonTemplated.class,
                Collections.emptyList());
        registry.setRoute("other/alias/:extra?", Templated.class,
                Collections.emptyList());

        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("other/view/:session", MyRoute.class,
                Collections.emptyList());
        sessionRegistry.setRoute("other/:match/:session?", Secondary.class,
                Collections.emptyList());

        Assertions.assertEquals(MyRoute.class,
                sessionRegistry.getNavigationTarget("other/view/offset").get(),
                "MyRoute should be selected as the matching parts are equal");
        Assertions.assertEquals(NonTemplated.class,
                sessionRegistry.getNavigationTarget("other/view/parent").get(),
                "Exact macth in ApplicationRegistry should be selected");
        Assertions.assertEquals(Templated.class,
                sessionRegistry.getNavigationTarget("other/alias").get(),
                "Closer macth in ApplicationRegistry should be selected");
        Assertions.assertEquals(Templated.class,
                sessionRegistry.getNavigationTarget("other/alias/extra").get(),
                "Closer macth in ApplicationRegistry should be selected");

    }

    private <T> T serializeAndDeserialize(T instance) throws Throwable {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(instance);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));

        @SuppressWarnings("unchecked")
        T readObject = (T) in.readObject();

        return readObject;
    }

    private void awaitCountDown(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Assertions.fail();
        }
    }

    @Tag("div")
    @Route("MyRoute")
    private static class MyRoute extends Component {
    }

    @Tag("div")
    @Route("MyRoute")
    @RouteAlias("info")
    @RouteAlias("version")
    private static class MyRouteWithAliases extends Component {
    }

    @Tag("div")
    private static class Secondary extends Component {
    }

    @Tag("div")
    private static class MainLayout extends Component implements RouterLayout {
    }

    @Tag("div")
    private static class MiddleLayout extends Component
            implements RouterLayout {
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

    @Tag("div")
    @Route("foo")
    @RouteAlias("")
    private static class RouteWithRootAlias extends Component {

    }

    @Tag("div")
    @Route(":foo")
    @RouteAlias("")
    private static class ParameterizedRouteWithRootAlias extends Component {

    }

    @Route(":first/:second")
    @Tag("div")
    public static class Templated extends Component {
    }

    @Route("other/view")
    @Tag("div")
    public static class NonTemplated extends Component {
    }

    /**
     * Extending class to let us mock the getRouteRegistry method for testing.
     */
    private static class MockService extends VaadinServletService {

        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }

    private static class TestSessionRouteRegistry extends SessionRouteRegistry {

        private final VaadinSession session;

        TestSessionRouteRegistry(VaadinSession session) {
            super(session);
            this.session = session;
        }

    }

    private static class TestService extends VaadinServletService {

        private ReentrantLock lock = Mockito.mock(ReentrantLock.class);

        private RouteRegistry appRegistry = Mockito
                .mock(ApplicationRouteRegistry.class);

        {
            Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        }

        @Override
        protected Lock getSessionLock(WrappedSession wrappedSession) {
            return lock;
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            return appRegistry;
        }
    }
}

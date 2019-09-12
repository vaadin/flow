package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
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
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.Registration;

public class SessionRouteRegistryTest {

    private ApplicationRouteRegistry registry;
    private MockService vaadinService;
    private VaadinSession session;

    @Before
    public void init() {
        registry = ApplicationRouteRegistry.getInstance(
                new VaadinServletContext(Mockito.mock(ServletContext.class)));

        vaadinService = Mockito.mock(MockService.class);
        Mockito.when(vaadinService.getRouteRegistry()).thenReturn(registry);

        VaadinService.setCurrent(vaadinService);

        session = new MockVaadinSession(vaadinService) {
            @Override
            public VaadinService getService() {
                return vaadinService;
            }
        };
    }

    @After
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

        Assert.assertTrue(
                "Registry didn't contain navigation targets even though some were registered",
                !registry.getRegisteredRoutes().isEmpty());

        registry.removeRoute(MyRoute.class);

        Assert.assertFalse(
                "Registry should be empty as only one class was registered",
                !registry.getRegisteredRoutes().isEmpty());
    }

    @Test
    public void addMultipleClassesToSameRoute_removeClassLeavesRoute() {
        SessionRouteRegistry registry = getRegistry(session);

        registry.setRoute("home", MyRoute.class, Collections.emptyList());
        registry.setRoute("home", Parameter.class, Collections.emptyList());

        Assert.assertTrue(
                "Registry didn't contain navigation targets even though some were registered",
                !registry.getRegisteredRoutes().isEmpty());

        Assert.assertEquals(
                "No parameters route class was expected for only path String.",
                MyRoute.class, registry.getNavigationTarget("home").get());
        Assert.assertEquals(
                "No parameters route class was expected for empty segments.",
                MyRoute.class,
                registry.getNavigationTarget("home", Collections.emptyList())
                        .get());
        Assert.assertEquals(
                "Expected HasUrlParameters class for request with segments.",
                Parameter.class,
                registry.getNavigationTarget("home", Arrays.asList("param"))
                        .get());

        registry.removeRoute(MyRoute.class);

        Assert.assertTrue(
                "Registry is empty even though we should have one route available",
                !registry.getRegisteredRoutes().isEmpty());
        Assert.assertFalse(
                "MyRoute should have been removed from the registry.",
                registry.getTargetUrl(MyRoute.class).isPresent());
        Assert.assertTrue(
                "Parameter class should have been available from the registry",
                registry.getTargetUrl(Parameter.class).isPresent());
        Assert.assertEquals("Parameter route should have been available.",
                Parameter.class,
                registry.getNavigationTarget("home", Arrays.asList("param"))
                        .get());

    }

    @Test
    public void sessionRegistryOverridesParentRegistryForGetTargetUrl_globalRouteStillAccessible() {
        registry.setRoute("MyRoute", MyRoute.class, Collections.emptyList());
        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("alternate", MyRoute.class,
                Collections.emptyList());

        Assert.assertEquals("Expected session registry route to be returned",
                "alternate", sessionRegistry.getTargetUrl(MyRoute.class).get());

        Assert.assertTrue("Route 'alternate' should be available.",
                sessionRegistry.getNavigationTarget("alternate").isPresent());
        Assert.assertTrue("Route 'MyRoute' should be available.",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
    }

    @Test
    public void sessionRegistryOverridesParentRegistryWithOwnClass_globalRouteReturnedAfterClassRemoval() {
        registry.setRoute("MyRoute", MyRoute.class, Collections.emptyList());
        SessionRouteRegistry sessionRegistry = getRegistry(session);
        sessionRegistry.setRoute("MyRoute", Secondary.class,
                Collections.emptyList());

        Assert.assertEquals(
                "Route 'MyRoute' should return Secondary as registered to SessionRegistry.",
                Secondary.class,
                sessionRegistry.getNavigationTarget("MyRoute").get());

        sessionRegistry.removeRoute(Secondary.class);

        Assert.assertEquals(
                "Route 'MyRoute' should return MyRoute as registered to GlobalRegistry.",
                MyRoute.class,
                sessionRegistry.getNavigationTarget("MyRoute").get());
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

        Assert.assertTrue("Main route was not registered for given Class.",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
        Assert.assertTrue(
                "RouteAlias 'info' was not registered for given Class.",
                sessionRegistry.getNavigationTarget("info").isPresent());
        Assert.assertTrue(
                "RouteAlias 'version' was not registered for given Class.",
                sessionRegistry.getNavigationTarget("version").isPresent());
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

        Assert.assertFalse("'MyRoute' should have a single parent",
                sessionRegistry
                        .getRouteLayouts("MyRoute", MyRouteWithAliases.class)
                        .isEmpty());
        Assert.assertTrue("'info' should have no parents.", sessionRegistry
                .getRouteLayouts("info", MyRouteWithAliases.class).isEmpty());
        Assert.assertEquals("'version' should return two parents", 2,
                sessionRegistry
                        .getRouteLayouts("version", MyRouteWithAliases.class)
                        .size());
    }

    @Test
    public void registeredParentLayouts_changingListDoesntChangeRegistration() {
        SessionRouteRegistry registry = getRegistry(session);

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
        SessionRouteRegistry registry = getRegistry(session);

        List<Class<? extends RouterLayout>> parentChain = new ArrayList<>(
                Arrays.asList(MiddleLayout.class, MainLayout.class));

        registry.setRoute("version", MyRoute.class, parentChain);

        Assert.assertArrayEquals(
                "Registry should return parent layouts in the same order as set.",
                parentChain.toArray(),
                registry.getRouteLayouts("version", MyRoute.class).toArray());
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

        Assert.assertTrue("Main route was not registered for given Class.",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
        Assert.assertTrue(
                "RouteAlias 'info' was not registered for given Class.",
                sessionRegistry.getNavigationTarget("info").isPresent());
        Assert.assertTrue(
                "RouteAlias 'version' was not registered for given Class.",
                sessionRegistry.getNavigationTarget("version").isPresent());

        Assert.assertEquals("MyRoute",
                sessionRegistry.getTargetUrl(MyRouteWithAliases.class).get());

        sessionRegistry.removeRoute("MyRoute");

        Assert.assertFalse(
                "Route 'MyRoute' was still available even though it should have been removed.",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
        Assert.assertTrue(
                "Route 'info' has been removed eve though it should still be available",
                sessionRegistry.getNavigationTarget("info").isPresent());
        Assert.assertTrue(
                "Route 'version' has been removed eve though it should still be available",
                sessionRegistry.getNavigationTarget("version").isPresent());

        Assert.assertTrue(
                "Route was not found from the registry anymore even though it should be available.",
                sessionRegistry.getTargetUrl(MyRouteWithAliases.class)
                        .isPresent());

        // Either or is expected as the new default as first match is picked
        // from the map
        Assert.assertTrue(
                "Route didn't return a url matching either of the expected aliases.",
                Arrays.asList("info", "version").contains(sessionRegistry
                        .getTargetUrl(MyRouteWithAliases.class).get()));
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
        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !registeredRoutes.isEmpty());

        Assert.assertTrue("Path for main route 'main' returned empty",
                sessionRegistry.getNavigationTarget("main").isPresent());
        Assert.assertTrue("RouteAlias 'Alias1' returned empty.",
                sessionRegistry.getNavigationTarget("Alias1").isPresent());
        Assert.assertTrue("RouteAlias 'Alias2' returned empty.",
                sessionRegistry.getNavigationTarget("Alias2").isPresent());

        Assert.assertEquals(
                "Two 'RouteAlias'es should be registered in the collected route data.",
                2, registeredRoutes.get(0).getRouteAliases().size());

        sessionRegistry.removeRoute("main");

        registeredRoutes = sessionRegistry.getRegisteredRoutes();

        Assert.assertTrue("Registry should still contain the alias routes",
                !registeredRoutes.isEmpty());

        Assert.assertEquals(
                "One RouteAlias should be the main url so only 1 route alias should be marked as an alias",
                1, registeredRoutes.get(0).getRouteAliases().size());
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
        Assert.assertTrue(
                "Registry didn't contain routes even though 3 should have been registered",
                !registeredRoutes.isEmpty());

        Assert.assertTrue("Path for main route 'MyRoute' returned empty",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
        Assert.assertTrue("RouteAlias 'info' returned empty.",
                sessionRegistry.getNavigationTarget("info").isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                sessionRegistry.getNavigationTarget("version").isPresent());

        Assert.assertEquals("Both route aliases should be found for Route", 2,
                registeredRoutes.get(0).getRouteAliases().size());
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

        Assert.assertTrue("Registry didn't contain routes.",
                !sessionRegistry.getRegisteredRoutes().isEmpty());

        Assert.assertTrue("Path for main route 'MyRoute' returned empty",
                sessionRegistry.getNavigationTarget("MyRoute").isPresent());
        Assert.assertEquals(
                "Navigation target for route 'MyRoute' was not the expected one.",
                Secondary.class,
                sessionRegistry.getNavigationTarget("MyRoute").get());

        Assert.assertTrue("RouteAlias 'info' returned empty.",
                sessionRegistry.getNavigationTarget("info").isPresent());
        Assert.assertTrue("RouteAlias 'version' returned empty.",
                sessionRegistry.getNavigationTarget("version").isPresent());

        Assert.assertTrue("Both route aliases should be found for Route",
                sessionRegistry.getRegisteredRoutes().get(0).getRouteAliases()
                        .isEmpty());
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

        Assert.assertEquals(
                "Expected 4 route already exists exceptions due to route target validation",
                THREADS - 1, exceptions.size());
        String expected = String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.",
                MyRoute.class.getName(), MyRoute.class.getName());
        for (String exception : exceptions) {
            Assert.assertEquals(expected, exception);
        }
        Optional<Class<? extends Component>> myRoute = getRegistry(session)
                .getNavigationTarget("MyRoute");
        Assert.assertTrue(
                "MyRoute was missing from the session scope registry.",
                myRoute.isPresent());

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

        Assert.assertEquals(
                "Expected 4 route already exists exceptions due to route target validation",
                THREADS - 1, exceptions.size());
        String expected = String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.",
                MyRoute.class.getName(), MyRoute.class.getName());
        for (String exception : exceptions) {
            Assert.assertEquals(expected, exception);
        }
        Optional<Class<? extends Component>> myRoute = getRegistry(session)
                .getNavigationTarget("MyRoute");
        Assert.assertTrue(
                "MyRoute was missing from the session scope registry.",
                myRoute.isPresent());

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

        Assert.assertEquals(
                "No exceptions should have been thrown for threaded updates.",
                0, exceptions.size());

        Assert.assertTrue("Route 'home' was not registered into the scope.",
                getRegistry(session).getNavigationTarget("home").isPresent());
        Assert.assertTrue("Route 'info' was not registered into the scope.",
                getRegistry(session).getNavigationTarget("info").isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                getRegistry(session).getNavigationTarget("palace").isPresent());
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

        Assert.assertEquals(
                "No exceptions should have been thrown for threaded updates.",
                0, exceptions.size());

        Assert.assertFalse(
                "Route 'home' was still registered even though it should have been removed.",
                getRegistry(session).getNavigationTarget("home").isPresent());

        Assert.assertFalse(
                "Route 'info' was still registered even though it should have been removed.",
                getRegistry(session).getNavigationTarget("info").isPresent());

        Assert.assertTrue("Route 'modular' was not registered into the scope.",
                getRegistry(session).getNavigationTarget("modular")
                        .isPresent());
        Assert.assertTrue("Route 'palace' was not registered into the scope.",
                getRegistry(session).getNavigationTarget("palace").isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void settingSessionRouteRegistryOfAnotherSession_getRegistryFails() {
        SessionRouteRegistry registry = getRegistry(session);

        VaadinSession anotherSession = new MockVaadinSession(vaadinService) {
            @Override
            public VaadinService getService() {
                return vaadinService;
            }
        };

        SessionRouteRegistry anotherRegistry = getRegistry(anotherSession);
        Assert.assertNotEquals("Another session should receive another session",
                registry, anotherRegistry);

        session.lock();
        try {
            session.setAttribute(SessionRouteRegistry.class, anotherRegistry);
        } finally {
            session.unlock();
        }

        getRegistry(session);

        Assert.fail(
                "Setting anotherRegistry to session should fail when getting the registry!");
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

                Assert.assertTrue("Registry should still remain empty",
                        getRegistry(session).getRegisteredRoutes().isEmpty());

                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        getRegistry(session).getRegisteredRoutes().isEmpty());

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

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                2, getRegistry(session).getRegisteredRoutes().size());
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
        Assert.assertEquals("One route should have gotten removed", 1,
                removed.size());

        Assert.assertEquals(Secondary.class,
                removed.get(0).getNavigationTarget());
        Assert.assertEquals("The 'home' route should have been removed", "home",
                removed.get(0).getUrl());
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

        Assert.assertEquals("Two ne paths should have been added", 2,
                added.size());
        Assert.assertEquals("One path should have been removed", 1,
                removed.size());

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

        Assert.assertEquals("One MyRoute should have been removed",
                MyRoute.class, removed.get(0).getNavigationTarget());
        Assert.assertEquals("Removed version should not have a parent layout",
                Collections.emptyList(), removed.get(0).getParentLayouts());
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

        Assert.assertEquals(
                "Main route and aliases should all be seen as added.", 3,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        sessionRegistry.removeRoute("Alias2");

        Assert.assertTrue("No routes should have been added", added.isEmpty());
        Assert.assertEquals(
                "Removing the alias route should be seen in the event", 1,
                removed.size());
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

        Assert.assertTrue("No event for masked path should have been received.",
                events.isEmpty());

        registry.setRoute("main", MyRoute.class, Collections.emptyList());

        Assert.assertTrue("No event for masked path should have been received.",
                events.isEmpty());

        registry.setRoute("home", Secondary.class, Collections.emptyList());

        Assert.assertEquals(
                "Addition of non masked path should have fired an event.", 1,
                events.size());
        Assert.assertEquals("Source should have been ApplicationRouteRegistry",
                registry, events.get(0).getSource());
        Assert.assertEquals("One route should have been added", 1,
                events.get(0).getAddedRoutes().size());
        Assert.assertEquals("No routes should have been removed", 0,
                events.get(0).getRemovedRoutes().size());
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

        Assert.assertEquals(
                "One event for both registries should have been fired.", 2,
                events.size());

        registration.remove();

        sessionRegistry.removeRoute("main");

        Assert.assertEquals(
                "No new event should have been received for session scope", 2,
                events.size());

        registry.removeRoute("main");

        Assert.assertEquals(
                "No new event should have been received for application scope",
                2, events.size());

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
        Assert.assertEquals(Optional.empty(),
                registry.getNavigationTarget("foo", Collections.emptyList()));
        // The deserialized one (after refreshing transients) contains "foo"
        // navigation target
        Assert.assertEquals(Optional.of(HtmlContainer.class), deserialized
                .getNavigationTarget("foo", Collections.emptyList()));

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
            Assert.fail();
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

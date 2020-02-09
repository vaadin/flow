package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.shared.Registration;

public class AbstractRouteRegistryTest {

    private AbstractRouteRegistry registry;

    @Before
    public void init() {
        registry = new AbstractRouteRegistry() {

            @Override
            public RouteSearchResult getNavigationTargetResult(String path) {
                return getConfiguration().getRouteSearchResult(path);
            }

            @Override
            public Optional<Class<? extends Component>> getNavigationTarget(
                    String pathString) {
                Objects.requireNonNull(pathString,
                        "pathString must not be null.");
                if (getConfiguration().hasRoute(pathString)) {
                    return getConfiguration().getRoute(pathString);
                }
                return Optional.empty();
            }

            @Override
            public Optional<Class<? extends Component>> getNavigationTarget(
                    String pathString, List<String> segments) {

                return getNavigationTarget(
                        PathUtil.getPath(pathString, segments));
            }
        };
    }

    @Test
    public void lockingConfiguration_configurationIsUpdatedOnlyAfterUnlock() {
        CountDownLatch waitReaderThread = new CountDownLatch(1);
        CountDownLatch waitUpdaterThread = new CountDownLatch(2);

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        registry.getRegisteredRoutes().isEmpty());

                awaitCountDown(waitUpdaterThread);

                Assert.assertTrue("Registry should still remain empty",
                        registry.getRegisteredRoutes().isEmpty());

                waitReaderThread.countDown();
            }
        };

        readerThread.start();

        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());
            registry.setRoute("path", Secondary.class, Collections.emptyList());
        });

        Assert.assertEquals(
                "After unlock registry should be updated for others to configure with new data",
                2, registry.getRegisteredRoutes().size());
    }

    @Test
    public void routeChangeListener_correctChangesAreReturned() {
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
        Assert.assertEquals(Collections.emptyList(),
                added.get(0).getParentLayouts());

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

        registry.addRoutesChangeListener(event -> {
            added.clear();
            removed.clear();
            added.addAll(event.getAddedRoutes());
            removed.addAll(event.getRemovedRoutes());
        });

        registry.update(() -> {
            registry.setRoute("main", Secondary.class, Collections.emptyList());
            registry.setRoute("Alias1", Secondary.class,
                    Collections.emptyList());
            registry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        Assert.assertEquals(
                "Main route and aliases should all be seen as added.", 3,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        registry.removeRoute("Alias2");

        Assert.assertTrue("No routes should have been added", added.isEmpty());
        Assert.assertEquals(
                "Removing the alias route should be seen in the event", 1,
                removed.size());
    }

    @Test
    public void changeListenerAddedDuringUpdate_eventIsFiredForListener() {
        List<RouteBaseData> added = new ArrayList<>();
        List<RouteBaseData> removed = new ArrayList<>();

        registry.update(() -> {
            registry.setRoute("main", Secondary.class, Collections.emptyList());
            registry.setRoute("Alias1", Secondary.class,
                    Collections.emptyList());

            // Long running task was done here and another thread added a
            // listener
            registry.addRoutesChangeListener(event -> {
                added.clear();
                removed.clear();
                added.addAll(event.getAddedRoutes());
                removed.addAll(event.getRemovedRoutes());
            });

            registry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
        });

        Assert.assertEquals(
                "Main route and aliases should all be seen as added.", 3,
                added.size());
        Assert.assertTrue("No routes should have been removed",
                removed.isEmpty());

        registry.removeRoute("Alias2");

        Assert.assertTrue("No routes should have been added", added.isEmpty());
        Assert.assertEquals(
                "Removing the alias route should be seen in the event", 1,
                removed.size());
    }

    @Test
    public void removeChangeListener_noEventsAreFired() {
        List<RoutesChangedEvent> events = new ArrayList<>();

        Registration registration = registry
                .addRoutesChangeListener(events::add);

        registry.setRoute("home", MyRoute.class, Collections.emptyList());

        Assert.assertEquals("Event should have been fired for listener", 1,
                events.size());

        registration.remove();

        registry.setRoute("away", MyRoute.class, Collections.emptyList());

        Assert.assertEquals("No new event should have fired", 1, events.size());
    }

    @Test
    public void routeChangedEvent_testRouteAddedAndRemoved() {
        registry.setRoute("MyRoute1", MyRoute.class, Collections.emptyList());

        registry.addRoutesChangeListener(event -> {
            Assert.assertEquals("MyRoute2 and Alias2 must be added", 2, event.getAddedRoutes().size());
            Assert.assertEquals("MyRoute1 must be deleted", 1, event.getRemovedRoutes().size());

            Assert.assertTrue("MyRoute2 must be added", event.isRouteAdded(MyRoute.class));
            Assert.assertTrue("Alias2 must be added", event.isRouteAdded(Secondary.class));
            Assert.assertTrue("MyRoute1 must be deleted", event.isRouteRemoved(MyRoute.class));
            Assert.assertTrue("MyRoute2 must be added", event.getAddedNavigationTargets().contains(MyRoute.class));
            Assert.assertTrue("Alias2 must be added", event.getAddedNavigationTargets().contains(Secondary.class));
            Assert.assertTrue("MyRoute1 must be deleted", event.getRemovedNavigationTargets().contains(MyRoute.class));
        });

        registry.update(() -> {
            registry.setRoute("MyRoute2", MyRoute.class, Collections.emptyList());
            registry.setRoute("Alias2", Secondary.class, Collections.emptyList());
            registry.removeRoute("MyRoute1");
        });
    }

    @Test
    public void routeChangedEvent_testPathAddedAndRemoved() {
        registry.setRoute("MyRoute1", MyRoute.class, Collections.emptyList());

        registry.addRoutesChangeListener(event -> {
            Assert.assertEquals("MyRoute2 and Alias2 must be added", 2, event.getAddedRoutes().size());
            Assert.assertEquals("MyRoute1 must be deleted", 1, event.getRemovedRoutes().size());

            Assert.assertTrue("MyRoute2 must be added", event.isPathAdded("MyRoute2"));
            Assert.assertTrue("Alias2 must be added", event.isPathAdded("Alias2"));
            Assert.assertTrue("MyRoute1 must be deleted", event.isPathRemoved("MyRoute1"));
            Assert.assertTrue("MyRoute2 must be added", event.getAddedURLs().contains("MyRoute2"));
            Assert.assertTrue("Alias2 must be added", event.getAddedURLs().contains("Alias2"));
            Assert.assertTrue("MyRoute1 must be deleted", event.getRemovedURLs().contains("MyRoute1"));
        });

        registry.update(() -> {
            registry.setRoute("MyRoute2", MyRoute.class, Collections.emptyList());
            registry.setRoute("Alias2", Secondary.class, Collections.emptyList());
            registry.removeRoute("MyRoute1");
        });
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
    private static class Secondary extends Component {
    }

    @Tag("div")
    private static class MainLayout extends Component implements RouterLayout {
    }
}

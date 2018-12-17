package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.SessionRouteRegistryTest;
import com.vaadin.flow.server.SessionRouteRegistry;

public class AbstractRouteRegistryTest {

    AbstractRouteRegistry registry;

    @Before
    public void init() {
        registry = new AbstractRouteRegistry() {
            @Override
            public Optional<Class<? extends Component>> getNavigationTarget(
                    String pathString) {
                Objects.requireNonNull(pathString,
                        "pathString must not be null.");
                return getNavigationTarget(pathString, Collections.emptyList());
            }

            @Override
            public Optional<Class<? extends Component>> getNavigationTarget(
                    String pathString, List<String> segments) {
                if (getConfiguration().hasRoute(pathString, segments)) {
                    return getConfiguration().getRoute(pathString, segments);
                }
                return Optional.empty();
            }
        };
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

        Assert.assertEquals(
                MyRoute.class, added.get(0).getNavigationTarget());
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
                    Collections.singletonList(
                            MainLayout.class));
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

        Assert.assertEquals("MyRoute should have been both removed and added", MyRoute.class,
                removed.get(0).getNavigationTarget());
        Assert.assertEquals("Removed version should not have a parent layout", UI.class, removed.get(0).getParentLayout());
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
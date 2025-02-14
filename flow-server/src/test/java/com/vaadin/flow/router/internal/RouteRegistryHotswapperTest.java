/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.router.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.RoutesChangedListener;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionRouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;

public class RouteRegistryHotswapperTest {

    VaadinService vaadinService;
    RouteRegistryHotswapper updater = new RouteRegistryHotswapper();
    RegistryChangeTracking appRouteRegistryTracker;

    @Before
    public void setup() {
        vaadinService = new MockVaadinServletService();

        ApplicationRouteRegistry applicationRouteRegistry = ApplicationRouteRegistry
                .getInstance(vaadinService.getContext());
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(applicationRouteRegistry);
        routeConfiguration.setAnnotatedRoute(MyRouteA.class);
        // simulate a route class whose annotation value will change
        routeConfiguration.setRoute("ORIGINAL_B", MyRouteB.class);
        routeConfiguration.setRoute("M1", NotAnnotatedRouteA.class);

        appRouteRegistryTracker = new RegistryChangeTracking();
        applicationRouteRegistry
                .addRoutesChangeListener(appRouteRegistryTracker);
    }

    private static class RegistryChangeTracking
            implements RoutesChangedListener {

        private final List<RouteBaseData<?>> added = new ArrayList<>();
        private final List<RouteBaseData<?>> removed = new ArrayList<>();
        private final RouteRegistry listenerOwner;

        RegistryChangeTracking() {
            this.listenerOwner = null;
        }

        RegistryChangeTracking(RouteRegistry listenerOwner) {
            this.listenerOwner = listenerOwner;
        }

        @Override
        public void routesChanged(RoutesChangedEvent event) {
            if (listenerOwner == null || event.getSource() == listenerOwner) {
                added.addAll(event.getAddedRoutes());
                removed.addAll(event.getRemovedRoutes());
            }
        }
    }

    @Test
    public void onClassLoadEvent_applicationRegistry_changesApplied() {
        updater.onClassLoadEvent(vaadinService, Set.of(MyRouteC.class), false);
        updater.onClassLoadEvent(vaadinService,
                Set.of(MyRouteB.class, MyRouteA.class), true);

        Assert.assertEquals(
                "Expected only changed route to be removed from the registry",
                1, appRouteRegistryTracker.removed.size());

        Assert.assertEquals(Set.of(MyRouteB.class),
                appRouteRegistryTracker.removed.stream()
                        .map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(
                "Expected added and modified routes to be added to the registry",
                2, appRouteRegistryTracker.added.size());

        Assert.assertEquals(Set.of(MyRouteC.class, MyRouteB.class),
                appRouteRegistryTracker.added.stream()
                        .map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void onClassLoadEvent_applicationRegistry_lazyRouteAdded_noChangesApplied() {
        updater.onClassLoadEvent(vaadinService, Set.of(SessionRouteA.class,
                SessionRouteB.class, SessionRouteC.class), false);

        Assert.assertEquals(
                "Expected routes with registerAtStartup=false not to be added to the registry",
                0, appRouteRegistryTracker.removed.size());

    }

    @Ignore("Double-check use case. RouteUtil states that session registry should not be updated on class modification")
    @Test
    public void onClassLoadEvent_sessionRegistries_updatesOnlyOnModifiedAndRemovedClasses() {
        VaadinSession session = new AlwaysLockedVaadinSession(vaadinService);
        RouteRegistry sessionRegistry = SessionRouteRegistry
                .getSessionRegistry(session);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(sessionRegistry);
        routeConfiguration.setAnnotatedRoute(SessionRouteA.class);
        // simulate a route class whose annotation value will change
        routeConfiguration.setRoute("ORIGINAL_S-B", SessionRouteB.class);
        routeConfiguration.setRoute("S-M1", NotAnnotatedRouteA.class);

        RegistryChangeTracking tracker = new RegistryChangeTracking(
                sessionRegistry);
        sessionRegistry.addRoutesChangeListener(tracker);

        updater.onClassLoadEvent(session,
                Set.of(SessionRouteC.class, NotAnnotatedRouteA.class), false);
        updater.onClassLoadEvent(session,
                Set.of(SessionRouteB.class, SessionRouteA.class), true);

        Assert.assertEquals(
                "Expected only changed routes to be removed from the registry",
                2, tracker.removed.size());

        Assert.assertEquals(Set.of(SessionRouteB.class),
                tracker.removed.stream().map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(
                "Expected only modified route to be added to the registry", 1,
                tracker.added.size());

        Assert.assertEquals(Set.of(SessionRouteB.class),
                tracker.added.stream().map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void onClassLoadEvent_sessionRegistries_modifiedClass_routePathShouldBePreserved() {
        VaadinSession session = new AlwaysLockedVaadinSession(vaadinService);

        RouteRegistry sessionRegistry = SessionRouteRegistry
                .getSessionRegistry(session);
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(sessionRegistry);
        routeConfiguration.setRoute("R1", SessionRouteA.class);
        routeConfiguration.setRoute("R2", SessionRouteB.class);

        Set<RouteData> before = new HashSet<>(
                sessionRegistry.getRegisteredRoutes());

        RegistryChangeTracking tracker = new RegistryChangeTracking(
                sessionRegistry);
        sessionRegistry.addRoutesChangeListener(tracker);

        updater.onClassLoadEvent(session, Set.of(SessionRouteA.class), true);

        Set<RouteData> after = new HashSet<>(
                sessionRegistry.getRegisteredRoutes());

        Assert.assertTrue(
                "No changes to session registry should be applied after session destroy",
                tracker.removed.isEmpty() && tracker.added.isEmpty());
        Assert.assertEquals(before, after);

    }

    @Test
    public void onClassLoadEvent_reactEnabled_layoutChanges_layoutJsonUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        Assert.assertFalse(
                "Expected layouts.json file not to be present before hotswap",
                Files.exists(layoutFile));

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassLoadEvent(vaadinService, Set.of(MyLayout.class), true);
        Assert.assertTrue("Expected layouts.json file to be written",
                Files.exists(layoutFile));
    }

    @Test
    public void onClassLoadEvent_reactEnabled_layoutNotChanged_layoutJsonNotUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        Assert.assertFalse(
                "Expected layouts.json file not to be present before hotswap",
                Files.exists(layoutFile));

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassLoadEvent(vaadinService, Set.of(MyLayout.class), true);
        Assert.assertTrue("Expected layouts.json file to be written",
                Files.exists(layoutFile));
        long lastModified = layoutFile.toFile().lastModified();

        updater.onClassLoadEvent(vaadinService, Set.of(MyLayout.class), true);
        Assert.assertTrue("Expected layouts.json file to be written",
                Files.exists(layoutFile));
        Assert.assertEquals(
                "Layout not changed, json file should not be written",
                lastModified, layoutFile.toFile().lastModified());

    }

    @Test
    public void onClassLoadEvent_reactEnabled_notLayoutChanges_layoutJsonNotUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        Assert.assertFalse(
                "Expected layouts.json file not to be present before hotswap",
                Files.exists(layoutFile));

        updater.onClassLoadEvent(vaadinService, Set.of(MyRouteA.class), true);
        Assert.assertFalse(
                "Expected layouts.json file not to be present after hotswap",
                Files.exists(layoutFile));
    }

    @Test
    public void onClassLoadEvent_reactDisabled_layoutChanges_layoutJsonNotWritten()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(false);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        Assert.assertFalse(
                "Expected layouts.json file not to be present before hotswap",
                Files.exists(layoutFile));

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassLoadEvent(vaadinService, Set.of(MyLayout.class), true);
        Assert.assertFalse(
                "Expected layouts.json file not to be present after hotswap",
                Files.exists(layoutFile));
    }

    @Test
    public void updateRegistries_notComponentClasses_registriesNotUpdated() {
        updater.onClassLoadEvent(vaadinService, Set.of(String.class), false);
        updater.onClassLoadEvent(vaadinService, Set.of(Long.class), true);

        Assert.assertTrue("Expected registry not to be updated",
                appRouteRegistryTracker.removed.isEmpty()
                        && appRouteRegistryTracker.added.isEmpty());
    }

    @Test
    public void updateRegistries_emptyClassSet_registriesNotUpdated() {
        updater.onClassLoadEvent(vaadinService, Set.of(), false);
        updater.onClassLoadEvent(vaadinService, Set.of(), true);

        Assert.assertTrue("Expected registry not to be updated",
                appRouteRegistryTracker.removed.isEmpty()
                        && appRouteRegistryTracker.added.isEmpty());
    }

    @Test
    public void updateRegistries_nullClassSet_registriesNotUpdated() {
        updater.onClassLoadEvent(vaadinService, null, false);
        updater.onClassLoadEvent(vaadinService, null, true);

        Assert.assertTrue("Expected registry not to be updated",
                appRouteRegistryTracker.removed.isEmpty()
                        && appRouteRegistryTracker.added.isEmpty());
    }

    @Tag("div")
    @Route("A")
    private static class MyRouteA extends Component {
    }

    @Tag("div")
    @Route("B")
    private static class MyRouteB extends Component {
    }

    @Tag("div")
    @Route("C")
    private static class MyRouteC extends Component {
    }

    @Tag("div")
    @Route(value = "S-A", registerAtStartup = false)
    private static class SessionRouteA extends Component {
    }

    @Tag("div")
    @Route(value = "S-B", registerAtStartup = false)
    private static class SessionRouteB extends Component {
    }

    @Tag("div")
    @Route(value = "S-C", registerAtStartup = false)
    private static class SessionRouteC extends Component {
    }

    @Tag("div")
    private static class NotAnnotatedRouteA extends Component {
    }
}

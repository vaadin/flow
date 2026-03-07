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
package com.vaadin.base.devserver.hotswap.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteRegistryHotswapperTest {

    VaadinService vaadinService;
    RouteRegistryHotswapper updater = new RouteRegistryHotswapper();
    RegistryChangeTracking appRouteRegistryTracker;

    @BeforeEach
    void setup() {
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
    void onClassLoadEvent_applicationRegistry_changesApplied() {
        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyRouteC.class), false));
        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyRouteB.class, MyRouteA.class), true));

        assertEquals(1, appRouteRegistryTracker.removed.size(),
                "Expected only changed route to be removed from the registry");

        assertEquals(Set.of(MyRouteB.class),
                appRouteRegistryTracker.removed.stream()
                        .map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));

        assertEquals(2, appRouteRegistryTracker.added.size(),
                "Expected added and modified routes to be added to the registry");

        assertEquals(Set.of(MyRouteC.class, MyRouteB.class),
                appRouteRegistryTracker.added.stream()
                        .map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));
    }

    @Test
    void onClassLoadEvent_applicationRegistry_lazyRouteAdded_noChangesApplied() {
        updater.onClassesChange(
                new HotswapClassEvent(
                        vaadinService, Set.of(SessionRouteA.class,
                                SessionRouteB.class, SessionRouteC.class),
                        false));

        assertEquals(0, appRouteRegistryTracker.removed.size(),
                "Expected routes with registerAtStartup=false not to be added to the registry");

    }

    @Disabled("Double-check use case. RouteUtil states that session registry should not be updated on class modification")
    @Test
    void onClassLoadEvent_sessionRegistries_updatesOnlyOnModifiedAndRemovedClasses() {
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

        updater.onClassesChange(new HotswapClassSessionEvent(vaadinService,
                session, Set.of(SessionRouteC.class, NotAnnotatedRouteA.class),
                false));
        updater.onClassesChange(new HotswapClassSessionEvent(vaadinService,
                session, Set.of(SessionRouteB.class, SessionRouteA.class),
                true));

        assertEquals(2, tracker.removed.size(),
                "Expected only changed routes to be removed from the registry");

        assertEquals(Set.of(SessionRouteB.class),
                tracker.removed.stream().map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));

        assertEquals(1, tracker.added.size(),
                "Expected only modified route to be added to the registry");

        assertEquals(Set.of(SessionRouteB.class),
                tracker.added.stream().map(RouteBaseData::getNavigationTarget)
                        .collect(Collectors.toSet()));
    }

    @Test
    void onClassLoadEvent_sessionRegistries_modifiedClass_routePathShouldBePreserved() {
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

        updater.onClassesChange(new HotswapClassSessionEvent(vaadinService,
                session, Set.of(SessionRouteA.class), true));

        Set<RouteData> after = new HashSet<>(
                sessionRegistry.getRegisteredRoutes());

        assertTrue(tracker.removed.isEmpty() && tracker.added.isEmpty(),
                "No changes to session registry should be applied after session destroy");
        assertEquals(before, after);

    }

    @Test
    void onClassLoadEvent_reactEnabled_layoutChanges_layoutJsonUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present before hotswap");

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyLayout.class), true));
        assertTrue(Files.exists(layoutFile),
                "Expected layouts.json file to be written");
    }

    @Test
    void onClassLoadEvent_reactEnabled_layoutNotChanged_layoutJsonNotUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present before hotswap");

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyLayout.class), true));
        assertTrue(Files.exists(layoutFile),
                "Expected layouts.json file to be written");
        long lastModified = layoutFile.toFile().lastModified();

        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyLayout.class), true));
        assertTrue(Files.exists(layoutFile),
                "Expected layouts.json file to be written");
        assertEquals(lastModified, layoutFile.toFile().lastModified(),
                "Layout not changed, json file should not be written");

    }

    @Test
    void onClassLoadEvent_reactEnabled_notLayoutChanges_layoutJsonNotUpdated()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(true);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present before hotswap");

        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyRouteA.class), true));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present after hotswap");
    }

    @Test
    void onClassLoadEvent_reactDisabled_layoutChanges_layoutJsonNotWritten()
            throws IOException {
        MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) vaadinService
                .getDeploymentConfiguration();
        configuration.setReactEnabled(false);
        configuration.setProjectFolder(
                Files.createTempDirectory("temp-project").toFile());
        Path layoutFile = configuration.getFrontendFolder().toPath()
                .resolve(Path.of("generated", "layouts.json"));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present before hotswap");

        @Layout
        class MyLayout extends Component implements RouterLayout {
        }

        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(MyLayout.class), true));
        assertFalse(Files.exists(layoutFile),
                "Expected layouts.json file not to be present after hotswap");
    }

    @Test
    void updateRegistries_notComponentClasses_registriesNotUpdated() {
        updater.onClassesChange(new HotswapClassEvent(vaadinService,
                Set.of(String.class), false));
        updater.onClassesChange(
                new HotswapClassEvent(vaadinService, Set.of(Long.class), true));

        assertTrue(
                appRouteRegistryTracker.removed.isEmpty()
                        && appRouteRegistryTracker.added.isEmpty(),
                "Expected registry not to be updated");
    }

    @Test
    void updateRegistries_emptyClassSet_registriesNotUpdated() {
        updater.onClassesChange(
                new HotswapClassEvent(vaadinService, Set.of(), false));
        updater.onClassesChange(
                new HotswapClassEvent(vaadinService, Set.of(), true));

        assertTrue(
                appRouteRegistryTracker.removed.isEmpty()
                        && appRouteRegistryTracker.added.isEmpty(),
                "Expected registry not to be updated");
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

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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouteParameterRegex;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.shared.Registration;

class AbstractRouteRegistryTest {

    private AbstractRouteRegistry registry;

    @BeforeEach
    public void init() {
        registry = new TestAbstractRouteRegistry();
    }

    @Test
    public void lockingConfiguration_configurationIsUpdatedOnlyAfterUnlock() {
        CountDownLatch waitReaderThread = new CountDownLatch(1);
        CountDownLatch waitUpdaterThread = new CountDownLatch(2);

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                awaitCountDown(waitUpdaterThread);

                Assertions.assertTrue(registry.getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

                awaitCountDown(waitUpdaterThread);

                Assertions.assertTrue(registry.getRegisteredRoutes().isEmpty(),
                        "Registry should still remain empty");

                waitReaderThread.countDown();
            }
        };

        readerThread.start();

        registry.update(() -> {
            registry.setRoute("", MyRoute.class, Collections.emptyList());
            registry.setRoute("path", Secondary.class, Collections.emptyList());
        });

        Assertions.assertEquals(2, registry.getRegisteredRoutes().size(),
                "After unlock registry should be updated for others to configure with new data");
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

        Assertions.assertFalse(added.isEmpty(),
                "Added should contain data for one entry");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        Assertions.assertEquals(MyRoute.class,
                added.get(0).getNavigationTarget());
        Assertions.assertEquals("", added.get(0).getTemplate());
        Assertions.assertEquals(Collections.emptyList(),
                added.get(0).getParentLayouts());

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
        Assertions.assertFalse(removed.isEmpty(),
                "One route should have gotten removed");

        Assertions.assertEquals(Secondary.class,
                removed.get(0).getNavigationTarget());
        Assertions.assertEquals("home", removed.get(0).getTemplate(),
                "The 'home' route should have been removed");
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

        Assertions.assertFalse(added.isEmpty(), "");
        Assertions.assertEquals(2, added.size(), "");
        Assertions.assertFalse(removed.isEmpty(), "");

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
                "MyRoute should have been both removed and added");
        Assertions.assertEquals(Collections.emptyList(),
                removed.get(0).getParentLayouts(),
                "Removed version should not have a parent layout");
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

        Assertions.assertEquals(3, added.size(),
                "Main route and aliases should all be seen as added.");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        registry.removeRoute("Alias2");

        Assertions.assertTrue(added.isEmpty(),
                "No routes should have been added");
        Assertions.assertEquals(1, removed.size(),
                "Removing the alias route should be seen in the event");
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

        Assertions.assertEquals(3, added.size(),
                "Main route and aliases should all be seen as added.");
        Assertions.assertTrue(removed.isEmpty(),
                "No routes should have been removed");

        registry.removeRoute("Alias2");

        Assertions.assertTrue(added.isEmpty(),
                "No routes should have been added");
        Assertions.assertEquals(1, removed.size(),
                "Removing the alias route should be seen in the event");
    }

    @Test
    public void removeChangeListener_noEventsAreFired() {
        List<RoutesChangedEvent> events = new ArrayList<>();

        Registration registration = registry
                .addRoutesChangeListener(events::add);

        registry.setRoute("home", MyRoute.class, Collections.emptyList());

        Assertions.assertEquals(1, events.size(),
                "Event should have been fired for listener");

        registration.remove();

        registry.setRoute("away", MyRoute.class, Collections.emptyList());

        Assertions.assertEquals(1, events.size(),
                "No new event should have fired");
    }

    @Test
    public void routeChangedEvent_testRouteAddedAndRemoved() {
        registry.setRoute("MyRoute1", MyRoute.class, Collections.emptyList());

        registry.addRoutesChangeListener(event -> {
            Assertions.assertEquals(2, event.getAddedRoutes().size(),
                    "MyRoute2 and Alias2 must be added");
            Assertions.assertEquals(1, event.getRemovedRoutes().size(),
                    "MyRoute1 must be deleted");

            Assertions.assertTrue(event.isRouteAdded(MyRoute.class),
                    "MyRoute2 must be added");
            Assertions.assertTrue(event.isRouteAdded(Secondary.class),
                    "Alias2 must be added");
            Assertions.assertTrue(event.isRouteRemoved(MyRoute.class),
                    "MyRoute1 must be deleted");
            Assertions.assertTrue(
                    event.getAddedNavigationTargets().contains(MyRoute.class),
                    "MyRoute2 must be added");
            Assertions.assertTrue(
                    event.getAddedNavigationTargets().contains(Secondary.class),
                    "Alias2 must be added");
            Assertions.assertTrue(
                    event.getRemovedNavigationTargets().contains(MyRoute.class),
                    "MyRoute1 must be deleted");
        });

        registry.update(() -> {
            registry.setRoute("MyRoute2", MyRoute.class,
                    Collections.emptyList());
            registry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
            registry.removeRoute("MyRoute1");
        });
    }

    @Test
    public void routeChangedEvent_testPathAddedAndRemoved() {
        registry.setRoute("MyRoute1", MyRoute.class, Collections.emptyList());

        registry.addRoutesChangeListener(event -> {
            Assertions.assertEquals(2, event.getAddedRoutes().size(),
                    "MyRoute2 and Alias2 must be added");
            Assertions.assertEquals(1, event.getRemovedRoutes().size(),
                    "MyRoute1 must be deleted");

            Assertions.assertTrue(event.isPathAdded("MyRoute2"),
                    "MyRoute2 must be added");
            Assertions.assertTrue(event.isPathAdded("Alias2"),
                    "Alias2 must be added");
            Assertions.assertTrue(event.isPathRemoved("MyRoute1"),
                    "MyRoute1 must be deleted");
            Assertions.assertTrue(event.getAddedURLs().contains("MyRoute2"),
                    "MyRoute2 must be added");
            Assertions.assertTrue(event.getAddedURLs().contains("Alias2"),
                    "Alias2 must be added");
            Assertions.assertTrue(event.getRemovedURLs().contains("MyRoute1"),
                    "MyRoute1 must be deleted");
        });

        registry.update(() -> {
            registry.setRoute("MyRoute2", MyRoute.class,
                    Collections.emptyList());
            registry.setRoute("Alias2", Secondary.class,
                    Collections.emptyList());
            registry.removeRoute("MyRoute1");
        });
    }

    /* Parameters tests */

    @Test
    public void only_normal_target_works_as_expected() {
        addTarget(NormalRoute.class);

        Assertions.assertEquals(NormalRoute.class, getTarget(),
                "NormalRoute should have been returned");
    }

    @Test
    public void only_has_url_target_works_as_expected() {
        addTarget(HasUrlRoute.class);

        Assertions.assertNull(getTarget(new ArrayList<>()),
                "No has url should have been returned without parameter");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");
    }

    @Test
    public void only_optional_target_works_as_expected() {
        addTarget(OptionalRoute.class);

        Assertions.assertEquals(OptionalRoute.class,
                getTarget(new ArrayList<>()),
                "OptionalRoute should have been returned with no parameter");

        Assertions.assertEquals(OptionalRoute.class,
                getTarget(Arrays.asList("optional")),
                "OptionalRoute should have been returned");
    }

    @Test
    public void only_wildcard_target_works_as_expected() {
        addTarget(WildcardRoute.class);

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(new ArrayList<>()),
                "WildcardRoute should have been returned with no parameter");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild")),
                "WildcardRoute should have been returned for one parameter");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    @Test
    public void normal_and_has_url_work_together() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);

        Assertions.assertEquals(NormalRoute.class, getTarget(new ArrayList<>()),
                "NormalRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");
    }

    @Test
    public void has_url_and_normal_work_together() {
        addTarget(HasUrlRoute.class);
        addTarget(NormalRoute.class);

        Assertions.assertEquals(NormalRoute.class, getTarget(new ArrayList<>()),
                "NormalRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");
    }

    @Test
    public void normal_and_wildcard_work_together() {
        addTarget(NormalRoute.class);
        addTarget(WildcardRoute.class);

        Assertions.assertEquals(NormalRoute.class, getTarget(new ArrayList<>()),
                "NormalRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("parameter")),
                "WildcardRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    @Test
    public void wildcard_and_normal_work_together() {
        addTarget(WildcardRoute.class);
        addTarget(NormalRoute.class);

        Assertions.assertEquals(NormalRoute.class, getTarget(new ArrayList<>()),
                "NormalRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("parameter")),
                "WildcardRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    @Test
    public void normal_and_has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        assertNormalHasUrlAndWildcard();
    }

    @Test
    public void normal_and_wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(NormalRoute.class);
        addTarget(WildcardRoute.class);
        addTarget(HasUrlRoute.class);

        assertNormalHasUrlAndWildcard();
    }

    @Test
    public void wildcard_and_normal_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(WildcardRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(NormalRoute.class);

        assertNormalHasUrlAndWildcard();
    }

    @Test
    public void has_url_and_wildcard_and_normal_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);
        addTarget(NormalRoute.class);

        assertNormalHasUrlAndWildcard();
    }

    @Test
    public void has_url_and_normal_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(NormalRoute.class);
        addTarget(WildcardRoute.class);

        assertNormalHasUrlAndWildcard();
    }

    private void assertNormalHasUrlAndWildcard() {
        Assertions.assertEquals(NormalRoute.class, getTarget(new ArrayList<>()),
                "NormalRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    @Test
    public void has_url_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(OptionalRoute.class);

        Assertions.assertEquals(OptionalRoute.class,
                getTarget(new ArrayList<>()),
                "OptionalRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");
    }

    @Test
    public void has_url_and_wildcard_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);
        addTarget(OptionalRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    @Test
    public void optional_parameter_and_has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(OptionalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    @Test
    public void optional_parameter_and_wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(OptionalRoute.class);
        addTarget(WildcardRoute.class);
        addTarget(HasUrlRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    @Test
    public void wildcard_and_has_url_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(WildcardRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(OptionalRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    @Test
    public void wildcard_and_optional_parameter_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(WildcardRoute.class);
        addTarget(OptionalRoute.class);
        addTarget(HasUrlRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    @Test
    public void has_url_and_optional_parameter_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(OptionalRoute.class);
        addTarget(WildcardRoute.class);

        assertHasUrlOptionalAndWildcard();
    }

    private void assertHasUrlOptionalAndWildcard() {
        Assertions.assertEquals(OptionalRoute.class,
                getTarget(new ArrayList<>()),
                "OptionalRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    @Test
    public void has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        assertHasUrlAndWildcard();
    }

    @Test
    public void wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(WildcardRoute.class);
        addTarget(HasUrlRoute.class);

        assertHasUrlAndWildcard();
    }

    private void assertHasUrlAndWildcard() {
        Assertions.assertEquals(WildcardRoute.class,
                getTarget(new ArrayList<>()),
                "WildcardRoute should have been returned");

        Assertions.assertEquals(HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")),
                "HasUrlRoute should have been returned");

        Assertions.assertEquals(WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")),
                "WildcardRoute should have been returned for multiple parameters");
    }

    /* Test exception cases */

    /* "normal" target registered first */
    @Test
    public void multiple_normal_routes_throw_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(NormalRoute.class);
                    addTarget(SecondNormalRoute.class);
                });
        Assertions.assertTrue(ex.getMessage()
                .contains(String.format(RouteUtil.ROUTE_CONFLICT,
                        NormalRoute.class.getName(),
                        SecondNormalRoute.class.getName())));
    }

    @Test
    public void normal_and_optional_throws_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(NormalRoute.class);
                    addTarget(OptionalRoute.class);
                });
        Assertions.assertTrue(ex.getMessage().contains(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName())));
    }

    /* Optional target registered first */

    @Test
    public void two_optionals_throw_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(OptionalRoute.class);
                    addTarget(SecondOptionalRoute.class);
                });
    }

    @Test
    public void optional_and_normal_throws_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(OptionalRoute.class);
                    addTarget(NormalRoute.class);
                });
        Assertions.assertTrue(ex.getMessage().contains(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName())));
    }

    /* HasUrl parameter */
    @Test
    public void two_has_route_parameters_throw_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(HasUrlRoute.class);
                    addTarget(SecondHasUrlRoute.class);
                });
    }

    /* Wildcard parameters */
    @Test
    public void two_wildcard_parameters_throw_exception()
            throws InvalidRouteConfigurationException {
        InvalidRouteConfigurationException ex = Assertions
                .assertThrows(InvalidRouteConfigurationException.class, () -> {

                    addTarget(WildcardRoute.class);
                    addTarget(SecondWildcardRoute.class);
                });
    }

    @Test
    public void removing_target_leaves_others() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        Assertions.assertEquals(3, config().getTargetRoutes().size(),
                "Expected three routes to be registered");

        registry.removeRoute(HasUrlRoute.class);

        Assertions.assertEquals(2, config().getTargetRoutes().size(),
                "Only 2 routes should remain after removing one.");

        Assertions.assertTrue(config().hasRouteTarget(NormalRoute.class),
                "NormalRoute should still be available");
        Assertions.assertTrue(config().hasRouteTarget(WildcardRoute.class),
                "WildcardRoute should still be available");
    }

    @Test
    public void removing_all_targets_is_possible_and_returns_empty() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        Assertions.assertEquals(3, config().getTargetRoutes().size(),
                "Expected three routes to be registered");

        registry.removeRoute(HasUrlRoute.class);
        registry.removeRoute(NormalRoute.class);
        registry.removeRoute(WildcardRoute.class);

        Assertions.assertTrue(config().getTargetRoutes().isEmpty(),
                "All routes should have been removed from the target.");
    }

    @Test
    public void check_has_parameters_returns_correctly() {
        registry.setRoute("", NormalRoute.class, null);
        registry.setRoute("url", HasUrlRoute.class, null);
        registry.setRoute("optional", OptionalRoute.class, null);
        registry.setRoute("wild", WildcardRoute.class, null);
        registry.setRoute(
                ParameterView.class.getAnnotation(Route.class).value(),
                ParameterView.class, null);

        Assertions.assertEquals(5, config().getTargetRoutes().size(),
                "All routes should be registered.");

        Assertions.assertFalse(
                registry.hasMandatoryParameter(NormalRoute.class),
                "Normal route should not mark as requiring parameter");
        Assertions.assertFalse(
                registry.hasMandatoryParameter(OptionalRoute.class),
                "Optional parameter should not mark as requiring parameter");
        Assertions.assertFalse(
                registry.hasMandatoryParameter(WildcardRoute.class),
                "Wildcard parameter should not mark as requiring parameter");

        Assertions.assertTrue(registry.hasMandatoryParameter(HasUrlRoute.class),
                "HasUrl should require parameter");
        Assertions.assertTrue(
                registry.hasMandatoryParameter(ParameterView.class),
                "Template parameter should require parameter");

        Assertions.assertThrows(NotFoundException.class,
                () -> registry.hasMandatoryParameter(Secondary.class),
                "Checking unregistered route should throw exception");
    }

    @Test
    public void multipleLayouts_stricterLayoutMatches_correctLayoutsReturned() {
        registry.setLayout(DefaultLayout.class);
        registry.setLayout(ViewLayout.class);

        Assertions.assertEquals(ViewLayout.class, registry.getLayout("/view"),
                "Path match returned wrong layout");
        Assertions.assertEquals(ViewLayout.class,
                registry.getLayout("/view/home"),
                "Beginning path match returned wrong layout");

        Assertions.assertEquals(DefaultLayout.class,
                registry.getLayout("/path"),
                "Any route match returned wrong layout");
    }

    @Test
    public void singleLayout_nonMatchingPathsReturnFalseOnHasLayout() {
        registry.setLayout(ViewLayout.class);

        Assertions.assertTrue(registry.hasLayout("/view"),
                "Existing layout should have returned true");
        Assertions.assertFalse(registry.hasLayout("/path"),
                "Path outside layout should return false");
    }
    /* Private stuff */

    private void awaitCountDown(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Assertions.fail();
        }
    }

    @Tag("div")
    @Layout
    private static class DefaultLayout extends Component
            implements RouterLayout {
    }

    @Tag("div")
    @Layout("/view")
    private static class ViewLayout extends Component implements RouterLayout {
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

    @Route("")
    @Tag(Tag.DIV)
    private static class NormalRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class SecondNormalRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class HasUrlRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class SecondHasUrlRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class OptionalRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class SecondOptionalRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class WildcardRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    private static class SecondWildcardRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
        }
    }

    @Route(value = "item/:long(" + RouteParameterRegex.LONG + ")")
    @Tag("div")
    private static class ParameterView extends Component {
    }

    @Tag(Tag.DIV)
    private static class Parent extends Component implements RouterLayout {
    }

    private ConfiguredRoutes config() {
        return registry.getConfiguration();
    }

    private void addTarget(Class<? extends Component> navigationTarget) {
        registry.setRoute("", navigationTarget, Collections.emptyList());
    }

    private void addTarget(Class<? extends Component> navigationTarget,
            List<Class<? extends RouterLayout>> parentChain) {
        registry.setRoute("", navigationTarget, parentChain);
    }

    private Class<? extends Component> getTarget() {
        return registry.getNavigationTarget("").orElse(null);
    }

    private Class<? extends Component> getTarget(String segment) {
        return registry.getNavigationTarget(segment).orElse(null);
    }

    private Class<? extends Component> getTarget(List<String> segments) {
        return registry.getNavigationTarget(PathUtil.getPath(segments))
                .orElse(null);
    }

}

package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.startup.RouteTargetTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteBaseData;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RoutesChangedEvent;
import com.vaadin.flow.shared.Registration;
import org.junit.rules.ExpectedException;

public class AbstractRouteRegistryTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AbstractRouteRegistry registry;

    @Before
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

    /* Parameters tests */

    @Test
    public void only_normal_target_works_as_expected() {
        addTarget(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget());
    }

    @Test
    public void only_has_url_target_works_as_expected() {
        addTarget(HasUrlRoute.class);

        Assert.assertNull(
                "No has url should have been returned without parameter",
                getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void only_optional_target_works_as_expected() {
        addTarget(OptionalRoute.class);

        Assert.assertEquals(
                "OptionalRoute should have been returned with no parameter",
                OptionalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class, getTarget(Arrays.asList("optional")));
    }

    @Test
    public void only_wildcard_target_works_as_expected() {
        addTarget(WildcardRoute.class);

        Assert.assertEquals(
                "WildcardRoute should have been returned with no parameter",
                WildcardRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals(
                "WildcardRoute should have been returned for one parameter",
                WildcardRoute.class, getTarget(Arrays.asList("wild")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void normal_and_has_url_work_together() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void has_url_and_normal_work_together() {
        addTarget(HasUrlRoute.class);
        addTarget(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void normal_and_wildcard_work_together() {
        addTarget(NormalRoute.class);
        addTarget(WildcardRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class, getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void wildcard_and_normal_work_together() {
        addTarget(WildcardRoute.class);
        addTarget(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class, getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
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
        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void has_url_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        addTarget(HasUrlRoute.class);
        addTarget(OptionalRoute.class);

        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));
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
        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
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
        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class, getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                getTarget(Arrays.asList("wild", "card", "target")));
    }

    /* Test exception cases */

    /* "normal" target registered first */
    @Test
    public void multiple_normal_routes_throw_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with the same route.",
                NormalRoute.class.getName(),
                SecondNormalRoute.class.getName()));

        addTarget(NormalRoute.class);
        addTarget(SecondNormalRoute.class);
    }

    @Test
    public void normal_and_optional_throws_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName()));

        addTarget(NormalRoute.class);
        addTarget(OptionalRoute.class);
    }

    /* Optional target registered first */

    @Test
    public void two_optionals_throw_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.",
                OptionalRoute.class.getName(),
                SecondOptionalRoute.class.getName()));

        addTarget(OptionalRoute.class);
        addTarget(SecondOptionalRoute.class);
    }

    @Test
    public void optional_and_normal_throws_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName()));

        addTarget(OptionalRoute.class);
        addTarget(NormalRoute.class);
    }

    /* HasUrl parameter */
    @Test
    public void two_has_url_parameters_throw_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.",
                HasUrlRoute.class.getName(),
                SecondHasUrlRoute.class.getName()));

        addTarget(HasUrlRoute.class);
        addTarget(SecondHasUrlRoute.class);
    }

    /* Wildcard parameters */
    @Test
    public void two_wildcard_parameters_throw_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with parameter have the same route.",
                WildcardRoute.class.getName(),
                SecondWildcardRoute.class.getName()));

        addTarget(WildcardRoute.class);
        addTarget(SecondWildcardRoute.class);
    }

    @Test
    public void removing_target_leaves_others() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        Assert.assertEquals("Expected three routes to be registered", 3,
                config().getTargetRoutes().size());

        registry.removeRoute(HasUrlRoute.class);

        Assert.assertEquals("Only 2 routes should remain after removing one.",
                2, config().getTargetRoutes().size());

        Assert.assertTrue("NormalRoute should still be available",
                config().hasRouteTarget(NormalRoute.class));
        Assert.assertTrue("WildcardRoute should still be available",
                config().hasRouteTarget(WildcardRoute.class));
    }

    @Test
    public void removing_all_targets_is_possible_and_returns_empty() {
        addTarget(NormalRoute.class);
        addTarget(HasUrlRoute.class);
        addTarget(WildcardRoute.class);

        Assert.assertEquals("Expected three routes to be registered", 3,
                config().getTargetRoutes().size());

        registry.removeRoute(HasUrlRoute.class);
        registry.removeRoute(NormalRoute.class);
        registry.removeRoute(WildcardRoute.class);

        Assert.assertTrue(
                "All routes should have been removed from the target.",
                config().getTargetRoutes().isEmpty());
    }

    @Test
    public void parent_layouts_are_given_for_correct_route() {
        addTarget(NormalRoute.class, Collections.singletonList(Parent.class));
        addTarget(HasUrlRoute.class);

        Assert.assertTrue("HasUrlRoute should not get parent layouts.",
                config().getParentLayouts(HasUrlRoute.class).isEmpty());

        Assert.assertEquals("NormaRoute should have exactly one parent layout.",
                1, config().getParentLayouts(NormalRoute.class).size());
        Assert.assertEquals("Received parent layout did not match expected.",
                Parent.class,
                config().getParentLayouts(NormalRoute.class).get(0));
    }

    @Test
    public void removing_route_removes_parent_layouts() {
        addTarget(NormalRoute.class, Collections.singletonList(Parent.class));

        Assert.assertEquals("NormaRoute should have exactly one parent layout.",
                1, config().getParentLayouts(NormalRoute.class).size());
        Assert.assertEquals("Received parent layout did not match expected.",
                Parent.class,
                config().getParentLayouts(NormalRoute.class).get(0));

        registry.removeRoute(NormalRoute.class);
        Assert.assertTrue("No targets should remain in RouteTarget",
                config().getTargetRoutes().isEmpty());
        Assert.assertTrue("No parents should be returned from NormalRoute",
                config().getParentLayouts(NormalRoute.class).isEmpty());
    }

    /* Private stuff */

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

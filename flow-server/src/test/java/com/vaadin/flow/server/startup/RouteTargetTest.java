/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.InvalidRouteConfigurationException;

public class RouteTargetTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Route("")
    @Tag(Tag.DIV)
    public static class NormalRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class SecondNormalRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class HasUrlRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class SecondHasUrlRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class OptionalRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter
                        String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class SecondOptionalRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter
                        String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class WildcardRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter
                        String parameter) {
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class SecondWildcardRoute extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter
                        String parameter) {
        }
    }

    @Tag(Tag.DIV)
    public static class Parent extends Component implements RouterLayout {
    }

    /* Test cases that should work as expected */

    @Test
    public void only_normal_target_works_as_expected()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));
    }

    @Test
    public void only_has_url_target_works_as_expected()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);

        Assert.assertNull(
                "No has url should have been returned without parameter",
                target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void only_optional_target_works_as_expected()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(OptionalRoute.class);

        Assert.assertEquals(
                "OptionalRoute should have been returned with no parameter",
                OptionalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class,
                target.getTarget(Arrays.asList("optional")));
    }

    @Test
    public void only_wildcard_target_works_as_expected()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);

        Assert.assertEquals(
                "WildcardRoute should have been returned with no parameter",
                WildcardRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals(
                "WildcardRoute should have been returned for one parameter",
                WildcardRoute.class, target.getTarget(Arrays.asList("wild")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void normal_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void has_url_and_normal_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void normal_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(WildcardRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void wildcard_and_normal_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void normal_and_has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        assertNormalHasUrlAndWildcard(target);
    }

    @Test
    public void normal_and_wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(WildcardRoute.class);
        target.addRoute(HasUrlRoute.class);

        assertNormalHasUrlAndWildcard(target);
    }

    @Test
    public void wildcard_and_normal_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(NormalRoute.class);

        assertNormalHasUrlAndWildcard(target);
    }

    @Test
    public void has_url_and_wildcard_and_normal_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);
        target.addRoute(NormalRoute.class);

        assertNormalHasUrlAndWildcard(target);
    }

    @Test
    public void has_url_and_normal_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(NormalRoute.class);
        target.addRoute(WildcardRoute.class);

        assertNormalHasUrlAndWildcard(target);
    }

    private void assertNormalHasUrlAndWildcard(RouteTarget target) {
        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void has_url_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(OptionalRoute.class);

        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void has_url_and_wildcard_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);
        target.addRoute(OptionalRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    @Test
    public void optional_parameter_and_has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(OptionalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    @Test
    public void optional_parameter_and_wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(OptionalRoute.class);
        target.addRoute(WildcardRoute.class);
        target.addRoute(HasUrlRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    @Test
    public void wildcard_and_has_url_and_optional_parameter_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(OptionalRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    @Test
    public void wildcard_and_optional_parameter_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(OptionalRoute.class);
        target.addRoute(HasUrlRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    @Test
    public void has_url_and_optional_parameter_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(OptionalRoute.class);
        target.addRoute(WildcardRoute.class);

        assertHasUrlOptionalAndWildcard(target);
    }

    private void assertHasUrlOptionalAndWildcard(RouteTarget target) {
        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
    }

    @Test
    public void has_url_and_wildcard_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        assertHasUrlAndWildcard(target);
    }

    @Test
    public void wildcard_and_has_url_work_together()
            throws InvalidRouteConfigurationException {
        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(HasUrlRoute.class);

        assertHasUrlAndWildcard(target);
    }

    private void assertHasUrlAndWildcard(RouteTarget target) {
        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class, target.getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                target.getTarget(Arrays.asList("parameter")));

        Assert.assertEquals(
                "WildcardRoute should have been returned for multiple parameters",
                WildcardRoute.class,
                target.getTarget(Arrays.asList("wild", "card", "target")));
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

        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(SecondNormalRoute.class);
    }

    @Test
    public void normal_and_optional_throws_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName()));

        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(OptionalRoute.class);
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

        RouteTarget target = new RouteTarget(OptionalRoute.class);
        target.addRoute(SecondOptionalRoute.class);
    }

    @Test
    public void optional_and_normal_throws_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                NormalRoute.class.getName(), OptionalRoute.class.getName(),
                OptionalRoute.class.getName()));

        RouteTarget target = new RouteTarget(OptionalRoute.class);
        target.addRoute(NormalRoute.class);
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

        RouteTarget target = new RouteTarget(HasUrlRoute.class);
        target.addRoute(SecondHasUrlRoute.class);
    }

    /* Wildcard parameters */
    @Test
    public void two_wildcard_parameters_throw_exception()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets must have unique routes, found navigation targets '%s' and '%s' with wildcard parameter have the same route.",
                WildcardRoute.class.getName(),
                SecondWildcardRoute.class.getName()));

        RouteTarget target = new RouteTarget(WildcardRoute.class);
        target.addRoute(SecondWildcardRoute.class);
    }

    /* Get routes */
    @Test
    public void get_routes_return_all_registered() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        List<Class<? extends Component>> routes = target.getRoutes();

        for (Class<? extends Component> component : Arrays
                .asList(NormalRoute.class, HasUrlRoute.class,
                        WildcardRoute.class)) {
            Assert.assertTrue("Returned routes was missing " + component,
                    routes.contains(component));
        }
    }

    /* Mutable tests */
    @Test
    public void adding_route_to_immutable_target_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(OptionalRoute.class, false);
        target.addRoute(NormalRoute.class);
    }

    @Test
    public void copy_of_immutable_contains_all_classes() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        RouteTarget immutable = target.copy(false);

        List<Class<? extends Component>> routes = immutable.getRoutes();
        Assert.assertEquals("All three routes should have been copied.", 3,
                routes.size());

        for (Class<? extends Component> component : Arrays
                .asList(NormalRoute.class, HasUrlRoute.class,
                        WildcardRoute.class)) {
            Assert.assertTrue("Immutable routes didn't contain " + component,
                    routes.contains(component));
        }

        RouteTarget immutableCopy = immutable.copy(false);

        routes = immutableCopy.getRoutes();
        Assert.assertEquals("All three routes should have been copied.", 3,
                routes.size());

        for (Class<? extends Component> component : Arrays
                .asList(NormalRoute.class, HasUrlRoute.class,
                        WildcardRoute.class)) {
            Assert.assertTrue("Immutable routes didn't contain " + component,
                    routes.contains(component));
        }
    }

    /* Remove target */
    @Test
    public void removing_from_immutable_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(OptionalRoute.class, false);
        target.remove(OptionalRoute.class);
    }

    @Test
    public void removing_target_leaves_others() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        Assert.assertEquals("Expected three routes to be registered", 3,
                target.getRoutes().size());

        target.remove(HasUrlRoute.class);

        Assert.assertEquals("Only 2 routes should remain after removing one.",
                2, target.getRoutes().size());

        Assert.assertTrue("NormalRoute should still be available",
                target.containsTarget(NormalRoute.class));
        Assert.assertTrue("WildcardRoute should still be available",
                target.containsTarget(WildcardRoute.class));
    }

    @Test
    public void removing_all_targets_is_possible_and_returns_empty() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.addRoute(WildcardRoute.class);

        Assert.assertEquals("Expected three routes to be registered", 3,
                target.getRoutes().size());

        target.remove(HasUrlRoute.class);
        target.remove(NormalRoute.class);
        target.remove(WildcardRoute.class);

        Assert.assertTrue(
                "All routes should have been removed from the target.",
                target.isEmpty());
    }

    /* Parent layouts */
    @Test
    public void setParentLayouts_throws_for_immutable() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(NormalRoute.class, false);
        target.setParentLayouts(NormalRoute.class, Collections.emptyList());
    }

    @Test
    public void setParentLayouts_throws_for_non_registered_target_class() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(
                "Tried to add parent layouts for a non existing target "
                        + OptionalRoute.class.getName());

        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.setParentLayouts(OptionalRoute.class, Collections.emptyList());
    }

    @Test
    public void parent_layouts_are_given_for_correct_route() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.addRoute(HasUrlRoute.class);
        target.setParentLayouts(NormalRoute.class,
                Collections.singletonList(Parent.class));

        Assert.assertTrue("HasUrlRoute should not get parent layouts.",
                target.getParentLayouts(HasUrlRoute.class).isEmpty());

        Assert.assertEquals("NormaRoute should have exactly one parent layout.",
                1, target.getParentLayouts(NormalRoute.class).size());
        Assert.assertEquals("Received parent layout did not match expected.",
                Parent.class,
                target.getParentLayouts(NormalRoute.class).get(0));
    }

    @Test
    public void removing_route_removes_parent_layouts() {
        RouteTarget target = new RouteTarget(NormalRoute.class);
        target.setParentLayouts(NormalRoute.class,
                Collections.singletonList(Parent.class));

        Assert.assertEquals("NormaRoute should have exactly one parent layout.",
                1, target.getParentLayouts(NormalRoute.class).size());
        Assert.assertEquals("Received parent layout did not match expected.",
                Parent.class,
                target.getParentLayouts(NormalRoute.class).get(0));

        target.remove(NormalRoute.class);
        Assert.assertTrue("No targets should remain in RouteTarget",
                target.isEmpty());
        Assert.assertTrue("No parents should be returned from NormalRoute",
                target.getParentLayouts(NormalRoute.class).isEmpty());
    }
}

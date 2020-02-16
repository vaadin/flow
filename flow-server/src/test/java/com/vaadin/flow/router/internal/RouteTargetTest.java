/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

    private AbstractRouteRegistry registry = new TestAbstractRouteRegistry();

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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void only_optional_target_works_as_expected() {
        addTarget(OptionalRoute.class);

        Assert.assertEquals(
                "OptionalRoute should have been returned with no parameter",
                OptionalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("OptionalRoute should have been returned",
                OptionalRoute.class,
                getTarget(Arrays.asList("optional")));
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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void has_url_and_normal_work_together() {
        addTarget(HasUrlRoute.class);
        addTarget(NormalRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("HasUrlRoute should have been returned",
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));
    }

    @Test
    public void normal_and_wildcard_work_together() {
        addTarget(NormalRoute.class);
        addTarget(WildcardRoute.class);

        Assert.assertEquals("NormalRoute should have been returned",
                NormalRoute.class, getTarget(new ArrayList<>()));

        Assert.assertEquals("WildcardRoute should have been returned",
                WildcardRoute.class,
                getTarget(Arrays.asList("parameter")));

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
                WildcardRoute.class,
                getTarget(Arrays.asList("parameter")));

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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));

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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));
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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));

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
                HasUrlRoute.class,
                getTarget(Arrays.asList("parameter")));

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

    /* Mutable tests */
    @Test
    public void adding_route_to_immutable_target_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Tried to mutate immutable configuration.");

        RouteTarget target = new RouteTarget(OptionalRoute.class, false);
        target.addRoute(NormalRoute.class);
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
        addTarget(NormalRoute.class,
                Collections.singletonList(Parent.class));

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

    public Class<? extends Component> getTarget(List<String> segments) {
        return registry.getNavigationTarget(PathUtil.getPath(segments)).orElse(null);
    }

}

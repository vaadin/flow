/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.AfterNavigationListener;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationListener;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.server.MockVaadinServletService;
import com.vaadin.server.MockVaadinSession;
import com.vaadin.server.VaadinSession;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class RouterTest extends RoutingTestBase {

    private static final String DYNAMIC_TITLE = "I am dynamic!";

    private static List<String> eventCollector = new ArrayList<>(0);

    private UI ui;

    @Route("")
    @Tag(Tag.DIV)
    public static class RootNavigationTarget extends Component {
    }

    @Route("foo")
    @Tag(Tag.DIV)
    public static class FooNavigationTarget extends Component {
    }

    @Route("foo/bar")
    @Tag(Tag.DIV)
    public static class FooBarNavigationTarget extends Component
            implements BeforeNavigationListener {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("FooBar " + event.getActivationState());
        }
    }

    @Route("reroute")
    @Tag(Tag.DIV)
    public static class ReroutingNavigationTarget extends Component
            implements BeforeNavigationListener {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Redirecting");
            event.rerouteTo(new NavigationStateBuilder()
                    .withTarget(FooBarNavigationTarget.class).build());
        }
    }

    @Route("param")
    @Tag(Tag.DIV)
    public static class ParameterRouteNoParameter extends Component {
    }

    @Route("param")
    @Tag(Tag.DIV)
    public static class RouteWithParameter extends Component
            implements BeforeNavigationListener, HasUrlParameter<String> {

        private String param;

        @Override
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
            eventCollector.add("Received param: " + parameter);
            param = parameter;
        }

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Stored parameter: " + param);
        }
    }

    @Route("param/static")
    @Tag(Tag.DIV)
    public static class StaticParameter extends Component {
    }

    @Route("optional")
    @Tag(Tag.DIV)
    public static class OptionalNoParameter extends Component {
    }

    @Route("optional")
    @Tag(Tag.DIV)
    public static class OptionalParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @com.vaadin.router.OptionalParameter String parameter) {
            eventCollector.add(parameter == null ? "No parameter" : parameter);
        }
    }

    @Route("usupported/wildcard")
    @Tag(Tag.DIV)
    public static class UnsupportedWildParameter extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @WildcardParameter Integer parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("fixed/wildcard")
    @Tag(Tag.DIV)
    public static class FixedWildParameter extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @WildcardParameter Integer parameter) {
            eventCollector.add("With parameter: " + parameter);
        }

        @Override
        public Integer deserializeUrlParameters(List<String> urlParameters) {
            Integer value = urlParameters.stream().map(Integer::valueOf)
                    .reduce(Integer::sum).orElse(0);
            return value;
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @WildcardParameter String parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildHasParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("integer")
    @Tag(Tag.DIV)
    public static class IntegerParameter extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                Integer parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("long")
    @Tag(Tag.DIV)
    public static class LongParameter extends Component
            implements HasUrlParameter<Long> {

        @Override
        public void setParameter(BeforeNavigationEvent event, Long parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("boolean")
    @Tag(Tag.DIV)
    public static class BooleanParameter extends Component
            implements HasUrlParameter<Boolean> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                Boolean parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildNormal extends Component {
    }

    @Route("redirect/to/param")
    @Tag(Tag.DIV)
    public static class RerouteToRouteWithParam extends Component
            implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            event.rerouteTo("param", "hello");
        }
    }

    @Route("fail/param")
    @Tag(Tag.DIV)
    public static class FailRerouteWithParam extends Component
            implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            event.rerouteTo("param", Boolean.TRUE);
        }
    }

    @Route("navigation-target-with-title")
    @PageTitle("Custom Title")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithTitle extends Component {
    }

    @RoutePrefix("parent-with-title")
    @PageTitle("Parent Title")
    @Tag(Tag.DIV)
    public static class ParentWithTitle extends Component
            implements RouterLayout {
    }

    @Route(value = "child", layout = ParentWithTitle.class)
    @Tag(Tag.DIV)
    public static class ChildWithoutTitle extends Component {
    }

    @Route("navigation-target-with-dynamic-title")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithDynamicTitle extends Component
            implements HasDynamicTitle {

        public NavigationTargetWithDynamicTitle() {
        }

        @Override
        public String getPageTitle() {
            return DYNAMIC_TITLE;
        }
    }

    @Route("url")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithDynamicTitleFromUrl extends
            Component implements HasDynamicTitle, HasUrlParameter<String> {

        private String title = DYNAMIC_TITLE;

        public NavigationTargetWithDynamicTitleFromUrl() {
        }

        @Override
        public String getPageTitle() {
            return title;
        }

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @com.vaadin.router.OptionalParameter String parameter) {
            title = parameter;
        }
    }

    @Route("url")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithDynamicTitleFromNavigation extends
            Component implements HasDynamicTitle, BeforeNavigationListener {

        private String title = DYNAMIC_TITLE;

        public NavigationTargetWithDynamicTitleFromNavigation() {
        }

        @Override
        public String getPageTitle() {
            return title;
        }

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            title = event.getActivationState().name();
        }
    }

    public static class RouterTestUI extends MockUI {
        final Router router;

        public RouterTestUI(Router router) {
            super(createMockSession());
            this.router = router;
        }

        private static VaadinSession createMockSession() {
            MockVaadinServletService service = new MockVaadinServletService();
            service.init();
            return new MockVaadinSession(service);
        }

        @Override
        public Optional<RouterInterface> getRouterInterface() {
            return Optional.of(router);
        }
    }

    @Route("navigationEvents")
    @Tag(Tag.DIV)
    public static class NavigationEvents extends Component {
        public NavigationEvents() {
            getElement().appendChild(new AfterNavigation().getElement());
            getElement().appendChild(new BeforeNavigation().getElement());
        }
    }

    @Tag(Tag.DIV)
    private static class AfterNavigation extends Component
            implements AfterNavigationListener {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            eventCollector.add("Event after navigation");
        }
    }

    @Tag(Tag.DIV)
    private static class BeforeNavigation extends Component
            implements BeforeNavigationListener {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Event before navigation");
        }
    }

    @RoutePrefix("parent")
    public static class RouteParent extends Component implements RouterLayout {
    }

    @Route(value = "child", layout = RouteParent.class)
    public static class RouteChild extends Component {
    }

    @Route(value = "single", layout = RouteParent.class, absolute = true)
    public static class LoneRoute extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class WildRootParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @WildcardParameter String parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class OptionalRootParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                @com.vaadin.router.OptionalParameter String parameter) {
            eventCollector.add(parameter == null ? "No parameter" : parameter);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class RootParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
            eventCollector.add(parameter);
        }
    }

    public static class ErrorTarget extends RouteNotFoundError implements
            HasErrorParameter<NotFoundException>, BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Redirected to error view, showing message: "
                    + getElement().getText());
        }
    }

    public static class CustomNotFoundTarget extends RouteNotFoundError
            implements HasErrorParameter<NotFoundException> {

        public static final String TEXT_CONTENT = "My custom not found class!";

        @Override
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText(TEXT_CONTENT);
            return 404;
        }
    }

    @Tag(Tag.DIV)
    public static class NonExtendingNotFoundTarget extends Component
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 404;
        }
    }

    @Route("exception")
    @Tag(Tag.DIV)
    public static class FailOnException extends Component
            implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            throw new RuntimeException("Failed on an exception");
        }
    }

    @Route("loop")
    @Tag(Tag.DIV)
    public static class LoopByReroute extends Component
            implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Loop");
            UI.getCurrent().navigateTo("loop");
        }
    }

    @Route("redirect/loop")
    @Tag(Tag.DIV)
    public static class RedirectToLoopByReroute extends Component
            implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            eventCollector.add("Redirect");
            UI.getCurrent().navigateTo("loop");
        }
    }

    @Override
    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        ui = new RouterTestUI(router);
        eventCollector.clear();
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void basic_navigation() throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(FooNavigationTarget.class, getUIComponent());

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());
    }

    @Test
    public void page_title_set_from_annotation()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(NavigationTargetWithTitle.class));
        router.navigate(ui, new Location("navigation-target-with-title"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Custom Title", ui.getInternals().getTitle());
    }

    @Test
    public void page_title_not_set_from_annotation_in_parent()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(ChildWithoutTitle.class));

        router.navigate(ui, new Location("parent-with-title/child"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("", ui.getInternals().getTitle());
    }

    @Test
    public void page_title_set_dynamically()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(NavigationTargetWithDynamicTitle.class));

        router.navigate(ui,
                new Location("navigation-target-with-dynamic-title"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is(DYNAMIC_TITLE));
    }

    @Test
    public void page_title_set_dynamically_from_url_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Collections
                .singleton(NavigationTargetWithDynamicTitleFromUrl.class));

        router.navigate(ui, new Location("url/hello"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is("hello"));
    }

    @Test
    public void page_title_set_dynamically_from_event_handler()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Collections.singleton(
                NavigationTargetWithDynamicTitleFromNavigation.class));

        router.navigate(ui, new Location("url"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is("ACTIVATING"));
    }

    @Test
    public void test_before_navigation_event_is_triggered()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());

    }

    @Test
    public void test_before_navigation_event_is_triggered_for_attach_and_detach()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("FooBar ACTIVATING", eventCollector.get(0));

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("FooBar DEACTIVATING", eventCollector.get(1));
    }

    @Test
    public void test_reroute_on_before_navigation_event()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(RootNavigationTarget.class, ReroutingNavigationTarget.class,
                        FooBarNavigationTarget.class)
                .collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        router.navigate(ui, new Location("reroute"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());

        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());

        Assert.assertEquals("Redirecting", eventCollector.get(0));
        Assert.assertEquals("FooBar ACTIVATING", eventCollector.get(1));
    }

    @Test
    public void before_and_after_event_fired_in_correct_order()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(NavigationEvents.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("navigationEvents"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Event before navigation", eventCollector.get(0));
        Assert.assertEquals("After navigation event was wrong.",
                "Event after navigation", eventCollector.get(1));
    }

    @Test
    public void after_event_not_fired_on_detach()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(NavigationEvents.class, FooNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("navigationEvents"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Event before navigation", eventCollector.get(0));
        Assert.assertEquals("After navigation event was wrong.",
                "Event after navigation", eventCollector.get(1));

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 3,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Event before navigation", eventCollector.get(2));
    }

    @Test
    public void basic_url_resolving()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(RootNavigationTarget.class,
                        FooNavigationTarget.class, FooBarNavigationTarget.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals("", router.getUrl(RootNavigationTarget.class));
        Assert.assertEquals("foo", router.getUrl(FooNavigationTarget.class));
        Assert.assertEquals("foo/bar",
                router.getUrl(FooBarNavigationTarget.class));
    }

    @Test
    public void nested_layouts_url_resolving()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(RouteChild.class, LoneRoute.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals("parent/child", router.getUrl(RouteChild.class));
        Assert.assertEquals("single", router.getUrl(LoneRoute.class));
    }

    @Test
    public void layout_with_url_parameter_url_resolving()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(GreetingNavigationTarget.class,
                                OtherGreetingNavigationTarget.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals("greeting/my_param",
                router.getUrl(GreetingNavigationTarget.class, "my_param"));
        Assert.assertEquals("greeting/true",
                router.getUrl(GreetingNavigationTarget.class, "true"));

        Assert.assertEquals("greeting/other",
                router.getUrl(GreetingNavigationTarget.class, "other"));
    }

    @Test
    public void test_reroute_with_url_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(GreetingNavigationTarget.class,
                        RouteWithParameter.class, RerouteToRouteWithParam.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("redirect/to/param"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Stored parameter: hello", eventCollector.get(1));
    }

    @Test
    public void fail_reroute_with_faulty_url_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(GreetingNavigationTarget.class,
                        RouteWithParameter.class, FailRerouteWithParam.class)
                        .collect(Collectors.toSet()));

        String locationString = "fail/param";
        router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        String message = "Given route parameter 'class java.lang.Boolean' is of the wrong type. Required 'class java.lang.String'.";
        String exceptionText = String.format(
                "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                locationString, message);

        assertExceptionComponent(exceptionText);
    }

    @Test
    public void test_route_precedence_when_one_has_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(RouteWithParameter.class, StaticParameter.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("param/param"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(RouteWithParameter.class, getUIComponent());

        // Expectation of 2 events is due to parameter and BeforeNavigation
        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Stored parameter: param", eventCollector.get(1));

        router.navigate(ui, new Location("param/static"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(
                "Did not get correct class even though StaticParameter should have precedence over RouteWithParameter due to exact url match.",
                StaticParameter.class, getUIComponent());
    }

    @Test
    public void test_optional_parameter_gets_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(OptionalParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("optional/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.", "parameter",
                eventCollector.get(0));
    }

    @Test
    public void test_optional_parameter_matches_no_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(OptionalParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("optional"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "No parameter", eventCollector.get(0));
    }

    @Test
    public void correctly_return_route_with_one_base_route_with_optionals()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(RouteWithParameter.class,
                                ParameterRouteNoParameter.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("param/parameter"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Failed", RouteWithParameter.class,
                getUIComponent());
    }

    @Test
    public void base_route_and_optional_parameter_throws_configuration_error()
            throws InvalidRouteConfigurationException {
        expectedEx.expect(InvalidRouteConfigurationException.class);
        expectedEx.expectMessage(String.format(
                "Navigation targets '%s' and '%s' have the same path and '%s' has an OptionalParameter that will never be used as optional.",
                OptionalNoParameter.class.getName(),
                OptionalParameter.class.getName(),
                OptionalParameter.class.getName()));

        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(OptionalParameter.class, OptionalNoParameter.class)
                        .collect(Collectors.toSet()));

    }

    @Test
    public void navigateToRoot_errorCode_dontRedirect()
            throws NoSuchFieldException, IllegalAccessException,
            InvalidRouteConfigurationException {

        router.getRegistry().setNavigationTargets(
                Collections.singleton(FooNavigationTarget.class));

        Assert.assertEquals(404, router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC));
    }

    @Test
    public void navigating_to_route_with_wildcard_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(WildParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "With parameter: ",
                eventCollector.get(0));

        router.navigate(ui, new Location("wild/single"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty",
                "With parameter: single", eventCollector.get(1));

        router.navigate(ui, new Location("wild/multi/part/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 3,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty",
                "With parameter: multi/part/parameter", eventCollector.get(2));
    }

    @Test
    public void route_with_wildcard_parameter_should_be_last_hit()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(WildParameter.class, WildHasParameter.class,
                        WildNormal.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 0,
                eventCollector.size());

        router.navigate(ui, new Location("wild/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "Parameter: parameter",
                eventCollector.get(0));

        router.navigate(ui, new Location("wild/multi/part/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty",
                "With parameter: multi/part/parameter", eventCollector.get(1));

    }

    @Test
    public void url_resolves_correctly_for_optional_and_wild_parameters()
            throws InvalidRouteConfigurationException, NotFoundException {
        router.getRegistry().setNavigationTargets(
                Stream.of(OptionalParameter.class, WildParameter.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals(
                "Optional value should be able to return even without any parameters",
                "optional", router.getUrl(OptionalParameter.class));

        Assert.assertEquals(
                "Wildcard value should be able to return even without any parameters",
                "wild", router.getUrl(WildParameter.class));

        Assert.assertEquals("optional/my_param",
                router.getUrl(OptionalParameter.class, "my_param"));

        Assert.assertEquals("wild/true",
                router.getUrl(WildParameter.class, "true"));

        Assert.assertEquals("wild/there/are/many/of/us",
                router.getUrl(WildParameter.class, "there/are/many/of/us"));
    }

    @Test
    public void root_navigation_target_with_wildcard_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(WildRootParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "With parameter: ",
                eventCollector.get(0));

        router.navigate(ui, new Location("my/wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty",
                "With parameter: my/wild", eventCollector.get(1));

        Assert.assertEquals("", router.getUrl(WildRootParameter.class));
        Assert.assertEquals("wild",
                router.getUrl(WildRootParameter.class, "wild"));
    }

    @Test
    public void root_navigation_target_with_optional_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(OptionalRootParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "No parameter",
                eventCollector.get(0));

        router.navigate(ui, new Location("optional"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "optional",
                eventCollector.get(1));

        Assert.assertEquals("", router.getUrl(OptionalRootParameter.class));
        Assert.assertEquals("optional",
                router.getUrl(OptionalRootParameter.class, "optional"));
    }

    @Test
    public void root_navigation_target_with_required_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(RootParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Has url with required parameter should not match to \"\"", 0,
                eventCollector.size());
    }

    @Test
    public void test_has_url_with_supported_parameters_navigation()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(IntegerParameter.class, LongParameter.class,
                                BooleanParameter.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("integer/5"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "Parameter: 5",
                eventCollector.get(0));

        router.navigate(ui, new Location("long/5"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "Parameter: 5",
                eventCollector.get(1));

        router.navigate(ui, new Location("boolean/true"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 3,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "Parameter: true",
                eventCollector.get(2));
    }

    @Test
    public void test_getUrl_for_has_url_with_supported_parameters()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(IntegerParameter.class, LongParameter.class,
                                BooleanParameter.class)
                        .collect(Collectors.toSet()));

        Assert.assertEquals("integer/5",
                router.getUrl(IntegerParameter.class, 5));

        Assert.assertEquals("long/5", router.getUrl(LongParameter.class, 5l));

        Assert.assertEquals("boolean/false",
                router.getUrl(BooleanParameter.class, false));
    }

    @Test
    public void default_wildcard_support_only_for_string()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(UnsupportedWildParameter.class)
                        .collect(Collectors.toSet()));

        String locationString = "usupported/wildcard/3/4/1";
        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Non existent route should have returned.", 500,
                result);

        String message = String.format(
                "Wildcard parameter can only be for String type by default. Implement `deserializeUrlParameters` for class %s",
                UnsupportedWildParameter.class.getName());
        String exceptionText = String.format(
                "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                locationString, message);

        assertExceptionComponent(exceptionText);
    }

    private void assertExceptionComponent(String exceptionText) {
        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();

        Assert.assertTrue("No navigation component visible",
                visibleComponent.isPresent());

        Assert.assertEquals(InternalServerError.class,
                visibleComponent.get().getClass());
        Assert.assertEquals(exceptionText,
                visibleComponent.get().getElement().getText());
    }

    @Test
    public void overridden_deserializer_wildcard_support_for_custom_type()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(FixedWildParameter.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("fixed/wildcard/3/4/1"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "With parameter: 8",
                eventCollector.get(0));

        Assert.assertEquals("fixed/wildcard/5/5/3", router
                .getUrl(FixedWildParameter.class, Arrays.asList(5, 5, 3)));
    }

    @Test
    public void redirect_to_routeNotFound_error_view_when_no_route_found()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(FixedWildParameter.class).collect(Collectors.toSet()));
        router.getRegistry().setErrorNavigationTargets(
                Collections.singleton(ErrorTarget.class));

        int result = router.navigate(ui, new Location("error"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.", 404,
                result);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("",
                "Redirected to error view, showing message: Could not navigate to 'error'",
                eventCollector.get(0));
    }

    @Test
    public void exception_during_navigation_is_caught_and_show_in_internalServerError()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(FailOnException.class));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.", 500,
                result);
    }

    @Test
    public void fail_for_multiple_of_the_same_class()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setErrorNavigationTargets(
                Stream.of(ErrorTarget.class, RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.", 404,
                result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                ErrorTarget.class, getUIComponent());
    }

    @Test
    public void do_not_accept_same_exception_targets() {

        expectedEx.expect(InvalidRouteLayoutConfigurationException.class);
        expectedEx.expectMessage(startsWith(
                "Only one target for an exception should be defined. Found "));

        router.getRegistry()
                .setErrorNavigationTargets(Stream
                        .of(NonExtendingNotFoundTarget.class,
                                RouteNotFoundError.class)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void custom_exception_target_is_used() {
        router.getRegistry().setErrorNavigationTargets(
                Stream.of(CustomNotFoundTarget.class, RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.", 404,
                result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                CustomNotFoundTarget.class, getUIComponent());

        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();
        Assert.assertEquals(CustomNotFoundTarget.TEXT_CONTENT,
                visibleComponent.get().getElement().getText());
    }

    @Test
    public void repeatedly_navigating_to_same_ur_through_ui_navigateTo_should_not_loop()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(LoopByReroute.class).collect(Collectors.toSet()));

        ui.navigateTo("loop");

        Assert.assertEquals("Expected only one request to loop", 1,
                eventCollector.size());
    }

    @Test
    public void navigateTo_should_not_loop()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(LoopByReroute.class, RedirectToLoopByReroute.class).collect(Collectors.toSet()));

        ui.navigateTo("redirect/loop");

        Assert.assertEquals("Expected two events", 2,
                eventCollector.size());
    }

    private Class<? extends Component> getUIComponent() {
        return ComponentUtil.findParentComponent(ui.getElement().getChild(0))
                .get().getClass();
    }

}

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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.AfterNavigationListener;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationListener;
import com.vaadin.server.InvalidRouteConfigurationException;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;

public class NewRouterTest extends NewRoutingTestBase {

    private UI ui;
    private static List<String> eventCollector = new ArrayList<>(0);

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
    public static class RouteWithParameter extends Component implements BeforeNavigationListener,
            HasUrlParameter<String> {

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

    @Route("redirect/to/param")
    @Tag(Tag.DIV)
    public static class RerouteToRouteWithParam extends Component implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            event.rerouteTo("param", "hello");
        }
    }

    @Route("fail/param")
    @Tag(Tag.DIV)
    public static class FailRerouteWithParam extends Component implements BeforeNavigationListener {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            event.rerouteTo("param", Boolean.TRUE);
        }
    }

    @Route("navigation-target-with-title")
    @Title("Custom Title")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithTitle extends Component {
    }

    public static class RouterTestUI extends UI {
        final Router router;

        public RouterTestUI() {
            this(new Router());
        }

        public RouterTestUI(Router router) {
            this.router = router;
        }

        @Override
        public Optional<RouterInterface> getRouter() {
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

    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        ui = new RouterTestUI(router);
        eventCollector.clear();
        Field field = RouteRegistry.getInstance().getClass()
                .getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(RouteRegistry.getInstance(), false);
    }

    @Test
    public void basic_navigation() throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance()
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
        RouteRegistry.getInstance().setNavigationTargets(
                Collections.singleton(NavigationTargetWithTitle.class));
        router.navigate(ui, new Location("navigation-target-with-title"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Custom Title", ui.getInternals().getTitle());
    }

    @Test
    public void test_before_navigation_event_is_triggered()
            throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance()
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
        RouteRegistry.getInstance()
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
        RouteRegistry.getInstance().setNavigationTargets(Stream
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
        RouteRegistry.getInstance().setNavigationTargets(
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
        RouteRegistry.getInstance().setNavigationTargets(
                Stream.of(NavigationEvents.class, FooNavigationTarget.class)
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

    public void basic_url_resolving()
            throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance()
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
        RouteRegistry.getInstance().setNavigationTargets(
                Stream.of(RouteChild.class, LoneRoute.class).collect(Collectors.toSet()));

        Assert.assertEquals("parent/child", router.getUrl(RouteChild.class));
        Assert.assertEquals("single", router.getUrl(LoneRoute.class));
    }

    @Test
    public void layout_with_url_parameter_url_resolving() throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance().setNavigationTargets(
                Stream.of(GreetingNavigationTarget.class, OtherGreetingNavigationTarget.class).collect(Collectors.toSet()));

        Assert.assertEquals("greeting/my_param", router.getUrl(GreetingNavigationTarget.class, "my_param"));
        Assert.assertEquals("greeting/true", router.getUrl(GreetingNavigationTarget.class, "true"));

        Assert.assertEquals("greeting/other", router.getUrl(GreetingNavigationTarget.class, "other"));
    }

    @Test
    public void test_reroute_with_url_parameter() throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance().setNavigationTargets(
                Stream.of(GreetingNavigationTarget.class, RouteWithParameter.class, RerouteToRouteWithParam.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("redirect/to/param"), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.", "Stored parameter: hello", eventCollector.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_reroute_with_faulty_url_parameter() throws InvalidRouteConfigurationException {
        RouteRegistry.getInstance().setNavigationTargets(
                Stream.of(GreetingNavigationTarget.class, RouteWithParameter.class, FailRerouteWithParam.class).collect(Collectors.toSet()));

        router.navigate(ui, new Location("fail/param"), NavigationTrigger.PROGRAMMATIC);
    }

    private Class<? extends Component> getUIComponent() {
        return ComponentUtil.findParentComponent(ui.getElement().getChild(0))
                .get().getClass();
    }
}

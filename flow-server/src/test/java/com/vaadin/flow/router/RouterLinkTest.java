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
package com.vaadin.flow.router;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@NotThreadSafe
class RouterLinkTest extends HasCurrentService {

    private ApplicationRouteRegistry registry;

    private Router router;

    private UI ui;

    @Tag(Tag.DIV)
    @Route("bar")
    public static class TestView extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }

    }

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException,
            InvalidRouteConfigurationException {
        VaadinService service = VaadinService.getCurrent();
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(config.getFrontendFolder())
                .thenReturn(new File("/frontend"));
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);

        registry = new TestRouteRegistry();
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(TestView.class, FooNavigationTarget.class,
                    ParameterNavigationTarget.class,
                    GreetingNavigationTarget.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });
        router = new Router(registry);

        ui = new RoutingTestBase.RouterTestUI(router);

        Mockito.when(service.getRouter()).thenReturn(router);
    }

    @Test
    public void createRouterLink_implicitCurrentVaadinServiceRouter() {
        // This method sets mock VaadinService instance which returns
        // Router from the UI.
        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");
        assertEquals("Show something", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_attachedLink() {
        UI ui = new UI();

        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(router, TestView.class, "foo");

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_withoutRouter() {
        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(FooNavigationTarget.class);

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_withoutRouterWithParameter() {
        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(GreetingNavigationTarget.class, "foo");

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("greeting/foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouter() {
        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");
        assertEquals("Show something", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_withTargetViewNoText() {
        RouterLink link = new RouterLink(FooNavigationTarget.class);
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_withTargetViewWithParameterNoText() {
        RouterLink link = new RouterLink(TestView.class, "something");
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_withTargetViewWithRouteParametersNoText() {
        RouteParameters routeParameters = HasUrlParameterFormat
                .getParameters("something");
        RouterLink link = new RouterLink(TestView.class, routeParameters);
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouterWithTargetViewNoText() {
        RouterLink link = new RouterLink(router, FooNavigationTarget.class);
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouterWithTargetViewWithParameterNoText() {
        RouterLink link = new RouterLink(router, TestView.class, "something");
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouterWithTargetViewWithRouteParametersNoText() {
        RouteParameters routeParameters = HasUrlParameterFormat
                .getParameters("something");
        RouterLink link = new RouterLink(router, TestView.class,
                routeParameters);
        assertEquals("", link.getText());
        assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        assertTrue(link.getElement().hasAttribute("href"));

        assertEquals("bar/something", link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_implicitCurrentVaadinServiceRouter() {
        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");

        link.setRoute(router, TestView.class, "other");

        assertEquals("bar/other", link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        assertEquals("bar/changed", link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_explicitRouter() {
        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");

        link.setRoute(router, TestView.class, "other");

        assertEquals("bar/other", link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        assertEquals("bar/changed", link.getElement().getAttribute("href"));
    }

    @Test
    public void reconfigureRouterLink_attachedLink() {
        RouterLink link = new RouterLink();
        ui.add(link);

        link.setRoute(router, TestView.class, "other");

        assertEquals("bar/other", link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        assertEquals("bar/changed", link.getElement().getAttribute("href"));
    }

    @Test
    public void noImplicitRouter() {
        VaadinService service = VaadinService.getCurrent();
        Mockito.when(service.getRouter()).thenReturn(null);
        assertThrows(IllegalStateException.class,
                () -> new RouterLink("Show something", TestView.class));
    }

    @Test
    public void routerLink_withoutRouter_WithRouteParameters() {
        assertRouterLinkRouteParameters(false);
    }

    @Test
    public void routerLink_WithRouteParameters() {
        assertRouterLinkRouteParameters(true);
    }

    private void assertRouterLinkRouteParameters(boolean useUI) {
        RouterLink link = new RouterLink("Foo", ParameterNavigationTarget.class,
                new RouteParameters("barId", "barValue"));

        if (useUI) {
            ui.add(link);
        }

        assertEquals("foo/barValue/bar",
                link.getElement().getAttribute("href"));

        assertRouterLinkSetRoute(link, ParameterNavigationTarget.class,
                new RouteParameters("fooId", "123"), "foo/123/foo");

        assertRouterLinkSetRoute(link, ParameterNavigationTarget.class,
                new RouteParameters("foos", "123/qwe"), "foo/123/qwe");

        try {
            link.setRoute(ParameterNavigationTarget.class,
                    new RouteParameters("fooId", "qwe"));
            fail("Route is not registered.");
        } catch (IllegalArgumentException e) {
        }
    }

    private void assertRouterLinkSetRoute(RouterLink link,
            Class<? extends Component> target, RouteParameters parameters,
            String url) {
        link.setRoute(target, parameters);
        assertEquals(url, link.getElement().getAttribute("href"));
    }

    private void triggerNavigationEvent(com.vaadin.flow.router.Router router,
            RouterLink link, String location) {
        AfterNavigationEvent event = new AfterNavigationEvent(
                new LocationChangeEvent(router, new UI(),
                        NavigationTrigger.ROUTER_LINK, new Location(location),
                        Collections.emptyList()));
        link.afterNavigation(event);
    }

    @Override
    protected VaadinService createService() {
        return Mockito.mock(VaadinService.class);
    }

    @Test
    public void routerLinkCreationForNormalRouteTarget()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        assertEquals("foo", link.getHref());
    }

    @Test
    public void routerLinkCreationForUrlParameterRouteTarget()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Greeting",
                GreetingNavigationTarget.class, "hello");
        assertEquals("greeting/hello", link.getHref());
    }

    @Test
    public void routerLinkDefaultHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);

        triggerNavigationEvent(router, link, "foo/bar");
        assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "baz");
        assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkSameLocationHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightCondition(HighlightConditions.sameLocation());

        triggerNavigationEvent(router, link, "foo/bar");
        assertFalse(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo");
        assertTrue(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkLocationPrefixHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightCondition(
                HighlightConditions.locationPrefix("foo/ba"));

        triggerNavigationEvent(router, link, "foo/bar");
        assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo/baz");
        assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo/qux");
        assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkClearOldHighlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        triggerNavigationEvent(router, link, "foo/bar");

        link.setHighlightAction(HighlightActions.toggleClassName("highlight"));
        triggerNavigationEvent(router, link, "foo/bar/baz");

        assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkClassNameHightlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightAction(HighlightActions.toggleClassName("highlight"));

        triggerNavigationEvent(router, link, "foo/bar");
        assertTrue(link.hasClassName("highlight"));

        triggerNavigationEvent(router, link, "bar");
        assertFalse(link.hasClassName("highlight"));
    }

    @Test
    public void routerLinkThemeHightlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightAction(HighlightActions.toggleTheme("highlight"));

        triggerNavigationEvent(router, link, "foo/bar");
        assertTrue(link.getElement().getThemeList().contains("highlight"));

        triggerNavigationEvent(router, link, "bar");
        assertFalse(link.getElement().getThemeList().contains("highlight"));
    }

    @Test
    public void routerLinkQueryParameters()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("foo", "bar");
        QueryParameters params = QueryParameters.simple(paramMap);
        link.setQueryParameters(params);
        String href = link.getHref();
        assertEquals("foo?foo=bar", href);

        link.setQueryParameters(null);
        href = link.getHref();
        assertEquals("foo", href);

        link.setQueryParameters(QueryParameters.empty());
        href = link.getHref();
        assertEquals("foo", href);

    }

    @Test
    public void routerLinkToNotRouterTarget_throwsIAE() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> {
                    new RouterLink("", Foo.class);
                });
    }

    @Route("foo")
    @Tag(Tag.DIV)
    public static class FooNavigationTarget extends Component {
    }

    @Route("foo/:barId?/bar")
    @RouteAlias("foo/:fooId(" + RouteParameterRegex.INTEGER + ")/foo")
    @RouteAlias("foo/:foos*")
    @Tag(Tag.DIV)
    public static class ParameterNavigationTarget extends Component {
    }

    @Route("greeting")
    @PageTitle("Custom Title")
    @Tag(Tag.DIV)
    public static class GreetingNavigationTarget extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Tag(Tag.DIV)
    public static class Foo extends Component {

    }
}

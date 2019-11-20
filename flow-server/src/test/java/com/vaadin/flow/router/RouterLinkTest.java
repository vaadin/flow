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
package com.vaadin.flow.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;

@NotThreadSafe
public class RouterLinkTest extends HasCurrentService {

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

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException,
            InvalidRouteConfigurationException {
        registry = new TestRouteRegistry();
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(TestView.class, FooNavigationTarget.class,
                    GreetingNavigationTarget.class)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });
        router = new Router(registry);

        ui = new UI() {
            @Override
            public Router getRouter() {
                return router;
            }
        };

        VaadinService service = VaadinService.getCurrent();
        Mockito.when(service.getRouter()).thenReturn(router);
    }

    @Test
    public void createRouterLink_implicitCurrentVaadinServiceRouter() {
        // This method sets mock VaadinService instance which returns
        // Router from the UI.
        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");
        Assert.assertEquals("Show something", link.getText());
        Assert.assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("bar/something",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_attachedLink() {
        UI ui = new UI();

        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(router, TestView.class, "foo");

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("bar/foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_withoutRouter() {
        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(FooNavigationTarget.class);

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_withoutRouterWithParameter() {
        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(GreetingNavigationTarget.class, "foo");

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("greeting/foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouter() {
        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");
        Assert.assertEquals("Show something", link.getText());
        Assert.assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("bar/something",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_implicitCurrentVaadinServiceRouter() {
        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");

        link.setRoute(router, TestView.class, "other");

        Assert.assertEquals("bar/other",
                link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        Assert.assertEquals("bar/changed",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_explicitRouter() {
        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");

        link.setRoute(router, TestView.class, "other");

        Assert.assertEquals("bar/other",
                link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        Assert.assertEquals("bar/changed",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void reconfigureRouterLink_attachedLink() {
        RouterLink link = new RouterLink();
        ui.add(link);

        link.setRoute(router, TestView.class, "other");

        Assert.assertEquals("bar/other",
                link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        Assert.assertEquals("bar/changed",
                link.getElement().getAttribute("href"));
    }

    @Test(expected = IllegalStateException.class)
    public void noImplicitRouter() {
        VaadinService service = VaadinService.getCurrent();
        Mockito.when(service.getRouter()).thenReturn(null);
        new RouterLink("Show something", TestView.class);
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
        Assert.assertEquals("foo", link.getHref());
    }

    @Test
    public void routerLinkCreationForUrlParameterRouteTarget()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Greeting",
                GreetingNavigationTarget.class, "hello");
        Assert.assertEquals("greeting/hello", link.getHref());
    }

    @Test
    public void routerLinkDefaultHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);

        triggerNavigationEvent(router, link, "foo/bar");
        Assert.assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "baz");
        Assert.assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkSameLocationHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightCondition(HighlightConditions.sameLocation());

        triggerNavigationEvent(router, link, "foo/bar");
        Assert.assertFalse(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo");
        Assert.assertTrue(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkLocationPrefixHighlightCondition()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightCondition(
                HighlightConditions.locationPrefix("foo/ba"));

        triggerNavigationEvent(router, link, "foo/bar");
        Assert.assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo/baz");
        Assert.assertTrue(link.getElement().hasAttribute("highlight"));

        triggerNavigationEvent(router, link, "foo/qux");
        Assert.assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkClearOldHighlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        triggerNavigationEvent(router, link, "foo/bar");

        link.setHighlightAction(HighlightActions.toggleClassName("highlight"));
        triggerNavigationEvent(router, link, "foo/bar/baz");

        Assert.assertFalse(link.getElement().hasAttribute("highlight"));
    }

    @Test
    public void routerLinkClassNameHightlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightAction(HighlightActions.toggleClassName("highlight"));

        triggerNavigationEvent(router, link, "foo/bar");
        Assert.assertTrue(link.hasClassName("highlight"));

        triggerNavigationEvent(router, link, "bar");
        Assert.assertFalse(link.hasClassName("highlight"));
    }

    @Test
    public void routerLinkThemeHightlightAction()
            throws InvalidRouteConfigurationException {
        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        link.setHighlightAction(HighlightActions.toggleTheme("highlight"));

        triggerNavigationEvent(router, link, "foo/bar");
        Assert.assertTrue(
                link.getElement().getThemeList().contains("highlight"));

        triggerNavigationEvent(router, link, "bar");
        Assert.assertFalse(
                link.getElement().getThemeList().contains("highlight"));
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
        Assert.assertEquals("foo?foo=bar", href);

        link.setQueryParameters(null);
        href = link.getHref();
        Assert.assertEquals("foo", href);
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Route("foo")
    @Tag(Tag.DIV)
    public static class FooNavigationTarget extends Component {
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
}

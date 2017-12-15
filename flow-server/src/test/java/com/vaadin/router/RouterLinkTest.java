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

import javax.servlet.ServletException;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterTest.RouterTestUI;
import com.vaadin.flow.router.View;
import com.vaadin.flow.router.ViewRendererTest.TestView;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.util.CurrentInstance;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.server.MockServletConfig;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.util.HasCurrentService;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class RouterLinkTest extends HasCurrentService {

    private RouteRegistry registry;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        registry = new TestRouteRegistry();
    }

    @Test
    public void buildUrlWithoutParameters() {
        String url = RouterLink.buildUrl("foo/bar");

        Assert.assertEquals("foo/bar", url);
    }

    @Test
    public void buildUrlWithParameters() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1",
                "param2/param3");

        Assert.assertEquals("param1/bar/param2/param3", url);
    }

    @Test
    public void buildUrlWithEmptyWildcard() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1", "");
        Assert.assertEquals("param1/bar/", url);
    }

    @Test
    public void buildUrlWithOmittedWildcard() {
        String url = RouterLink.buildUrl("{foo}/bar/*", "param1");
        Assert.assertEquals("param1/bar/", url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithTooFewParameters() {
        RouterLink.buildUrl("{foo}/bar/{baz}", "param1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithTooManyParameters() {
        RouterLink.buildUrl("{foo}/bar/*", "param1", "param2", "param3");
    }

    @Test
    public void buildUrlWithRouter() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> c.setRoute("foo/{bar}", TestView.class));

        String url = RouterLink.buildUrl(router, TestView.class, "asdf");

        Assert.assertEquals("foo/asdf", url);
    }

    @Test
    public void buildEmptyUrlWithRouter() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> c.setRoute("", TestView.class));

        String url = RouterLink.buildUrl(router, TestView.class);

        Assert.assertEquals("", url);

        RouterLink link = new RouterLink(router, "Home", TestView.class);
        Assert.assertEquals("", link.getHref());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithRouter_noRoutes() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();

        RouterLink.buildUrl(router, TestView.class, "asdf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrlWithRouter_multipleRoutes() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> {
            c.setRoute("foo/{bar}", TestView.class);
            c.setRoute("another/route", TestView.class);
        });

        RouterLink.buildUrl(router, TestView.class, "asdf");
    }

    @Test
    public void createRouterLink_implicitCurrentVaadinServiceRouter() {
        // This method sets mock VaadinService instance which returns
        // Router from the UI.
        RouterTestUI ui = createUI();
        ui.getRouterInterface().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");
        Assert.assertEquals("Show something", link.getText());
        Assert.assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("show/something",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void setRoute_attachedLink() {
        RouterTestUI ui = new RouterTestUI(new com.vaadin.flow.router.Router());
        ui.getRouterInterface().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink();

        ui.add(link);
        link.setRoute(TestView.class, "foo");

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("show/foo", link.getElement().getAttribute("href"));
    }

    @Test
    public void createRouterLink_explicitRouter() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");
        Assert.assertEquals("Show something", link.getText());
        Assert.assertTrue(link.getElement()
                .hasAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE));

        Assert.assertTrue(link.getElement().hasAttribute("href"));

        Assert.assertEquals("show/something",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_implicitCurrentVaadinServiceRouter() {
        // This method sets mock VaadinService instance which returns
        // Router from the UI.
        RouterTestUI ui = createUI();
        ui.getRouterInterface().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink("Show something", TestView.class,
                "something");

        link.setRoute(TestView.class, "other");

        Assert.assertEquals("show/other",
                link.getElement().getAttribute("href"));

        link.setRoute(TestView.class, "changed");

        Assert.assertEquals("show/changed",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void createReconfigureRouterLink_explicitRouter() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink(router, "Show something",
                TestView.class, "something");

        link.setRoute(router, TestView.class, "other");

        Assert.assertEquals("show/other",
                link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        Assert.assertEquals("show/changed",
                link.getElement().getAttribute("href"));
    }

    @Test
    public void reconfigureRouterLink_attachedLink() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        RouterTestUI ui = new RouterTestUI(router);
        router.reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink();
        ui.add(link);

        link.setRoute(TestView.class, "other");

        Assert.assertEquals("show/other",
                link.getElement().getAttribute("href"));

        link.setRoute(router, TestView.class, "changed");

        Assert.assertEquals("show/changed",
                link.getElement().getAttribute("href"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRoute_implicitCurrentVaadinServiceRouter() {
        // This method sets mock VaadinService instance which returns
        // Router from the UI.
        RouterTestUI ui = createUI();
        ui.getRouterInterface().get()
                .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        new RouterLink("Show something", TestView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRoute_explicitRouter() {
        com.vaadin.flow.router.Router router = new com.vaadin.flow.router.Router();
        router.reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        new RouterLink(router, "Show something", TestView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRoute_attachedLink() {
        com.vaadin.flow.router.Router router = new Router();
        RouterTestUI ui = new RouterTestUI(router);
        router.reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

        RouterLink link = new RouterLink();
        ui.add(link);
        link.setRoute(TestView.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRouteWhenConstructing() throws ServletException {
        VaadinServlet servlet = new VaadinServlet();
        Properties initParams = new Properties();
        initParams.setProperty(Constants.SERVLET_PARAMETER_USING_NEW_ROUTING,
                "false");
        servlet.init(new MockServletConfig(initParams));

        try {
            VaadinService.setCurrent(servlet.getService());

            servlet.getService().getRouter()
                    .reconfigure(c -> c.setRoute("show/{bar}", TestView.class));

            new RouterLink("Show something", TestView.class);
        } finally {
            VaadinService.setCurrent(null);
        }
    }

    private RouterTestUI createUI() {
        RouterTestUI ui = new RouterTestUI();
        VaadinService service = VaadinService.getCurrent();
        Mockito.when(service.getRouter())
                .thenReturn(ui.getRouterInterface().get());
        return ui;
    }

    @Override
    protected VaadinService createService() {
        return Mockito.mock(VaadinService.class);
    }

    @Test
    public void testRouterLinkCreationForNormatRouteTarget()
            throws InvalidRouteConfigurationException {

        registry.setNavigationTargets(Stream.of(FooNavigationTarget.class)
                .collect(Collectors.toSet()));

        com.vaadin.router.Router router = new com.vaadin.router.Router(
                registry);

        RouterLink link = new RouterLink(router, "Foo",
                FooNavigationTarget.class);
        Assert.assertEquals("foo", link.getHref());
    }

    @Test
    public void testRouterLinkCreationForUrlParameterRouteTarget()
            throws InvalidRouteConfigurationException {

        registry.setNavigationTargets(Stream.of(GreetingNavigationTarget.class)
                .collect(Collectors.toSet()));

        com.vaadin.router.Router router = new com.vaadin.router.Router(
                registry);

        RouterLink link = new RouterLink(router, "Greeting",
                GreetingNavigationTarget.class, "hello");
        Assert.assertEquals("greeting/hello", link.getHref());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testFailForWrongImplementation()
            throws InvalidRouteConfigurationException {
        registry.setNavigationTargets(
                Stream.of(FaultySetup.class).collect(Collectors.toSet()));

        com.vaadin.router.Router router = new com.vaadin.router.Router(
                registry);

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getRouter()).thenReturn(router);
        CurrentInstance.set(VaadinService.class, service);

        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage(
                "Only navigation targets for old Router should implement 'View'. Remove 'implements View' from '"
                        + FaultySetup.class.getName() + "'");

        RouterLink faulty = new RouterLink("Faulty", FaultySetup.class);
    }

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
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
        }
    }

    @Route("faulty")
    @Tag(Tag.DIV)
    public static class FaultySetup extends Component implements View {
    }
}

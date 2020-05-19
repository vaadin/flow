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

package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.MockUI;

@NotThreadSafe
public class RouterConfigurationUrlResolvingTest extends RoutingTestBase {
    private RouteConfiguration routeConfiguration;
    private UI ui;
    private VaadinService service = Mockito.mock(VaadinService.class);
    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        super.init();
        ui = new RouterTestUI(router);
        ui.getSession().lock();
        ui.getSession().setConfiguration(configuration);

        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(service.getRouter()).thenReturn(router);

        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        routeConfiguration = RouteConfiguration
                .forRegistry(router.getRegistry());
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private void setNavigationTargets(
            Class<? extends Component>... navigationTargets)
            throws InvalidRouteConfigurationException {
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(navigationTargets)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });
    }

    @Test
    public void basic_url_resolving()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

        Assert.assertEquals("",
                routeConfiguration.getUrl(RootNavigationTarget.class));
        Assert.assertEquals("foo",
                routeConfiguration.getUrl(FooNavigationTarget.class));
        Assert.assertEquals("foo/bar",
                routeConfiguration.getUrl(FooBarNavigationTarget.class));
    }

    @Test
    public void nested_layouts_url_resolving()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteChild.class, LoneRoute.class);

        Assert.assertEquals("parent/child",
                routeConfiguration.getUrl(RouteChild.class));
        Assert.assertEquals("single",
                routeConfiguration.getUrl(LoneRoute.class));
    }

    @Test
    public void layout_with_url_parameter_url_resolving()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                OtherGreetingNavigationTarget.class);

        Assert.assertEquals("greeting/my_param", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "my_param"));
        Assert.assertEquals("greeting/true", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "true"));

        Assert.assertEquals("greeting/other", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "other"));
    }

    @Test
    public void url_resolves_correctly_for_optional_and_wild_parameters()
            throws InvalidRouteConfigurationException, NotFoundException {
        setNavigationTargets(OptionalParameter.class, WildParameter.class);

        Assert.assertEquals(
                "Optional value should be able to return even without any parameters",
                "optional", routeConfiguration.getUrl(OptionalParameter.class));

        Assert.assertEquals(
                "Wildcard value should be able to return even without any parameters",
                "wild", routeConfiguration.getUrl(WildParameter.class));

        Assert.assertEquals("optional/my_param",
                routeConfiguration.getUrl(OptionalParameter.class, "my_param"));

        Assert.assertEquals("wild/true",
                routeConfiguration.getUrl(WildParameter.class, "true"));

        Assert.assertEquals("wild/there/are/many/of/us", routeConfiguration
                .getUrl(WildParameter.class, "there/are/many/of/us"));
    }

    @Test
    public void root_navigation_target_with_wildcard_parameter()
            throws InvalidRouteConfigurationException {
        WildRootParameter.events.clear();
        WildRootParameter.param = null;
        setNavigationTargets(WildRootParameter.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                WildRootParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "",
                WildRootParameter.param);

        router.navigate(ui, new Location("my/wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                WildRootParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "my/wild",
                WildRootParameter.param);

        Assert.assertEquals("",
                routeConfiguration.getUrl(WildRootParameter.class));
        Assert.assertEquals("wild",
                routeConfiguration.getUrl(WildRootParameter.class, "wild"));

        List<String> params = Arrays.asList("", null);
        Assert.assertEquals("", routeConfiguration
                .getUrl(WildRootParameter.class, params.get(1)));
    }

    @Test
    public void root_navigation_target_with_optional_parameter()
            throws InvalidRouteConfigurationException {
        OptionalRootParameter.events.clear();
        OptionalRootParameter.param = null;
        setNavigationTargets(OptionalRootParameter.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                OptionalRootParameter.events.size());
        Assert.assertNull("Parameter should be empty",
                OptionalRootParameter.param);

        router.navigate(ui, new Location("optional"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                OptionalRootParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "optional",
                OptionalRootParameter.param);

        Assert.assertEquals("",
                routeConfiguration.getUrl(OptionalRootParameter.class));
        Assert.assertEquals("optional", routeConfiguration
                .getUrl(OptionalRootParameter.class, "optional"));

        List<String> params = Arrays.asList("", null);
        Assert.assertEquals("", routeConfiguration
                .getUrl(OptionalRootParameter.class, params.get(1)));
    }

    @Test
    public void getUrl_for_has_url_with_supported_parameters()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(IntegerParameter.class, LongParameter.class,
                BooleanParameter.class);

        Assert.assertEquals("integer/5",
                routeConfiguration.getUrl(IntegerParameter.class, 5));

        Assert.assertEquals("long/5",
                routeConfiguration.getUrl(LongParameter.class, 5l));

        Assert.assertEquals("boolean/false",
                routeConfiguration.getUrl(BooleanParameter.class, false));
    }

    @Test // 3519
    public void getUrl_throws_for_required_parameter() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage(String.format(
                "Navigation target '%s' requires a parameter.",
                RouteWithParameter.class.getName()));
        setNavigationTargets(RouteWithParameter.class);

        routeConfiguration.getUrl(RouteWithParameter.class);
    }

    @Test // 3519
    public void getUrl_returns_url_if_parameter_is_wildcard_or_optional()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithMultipleParameters.class,
                OptionalParameter.class);

        String url = routeConfiguration
                .getUrl(RouteWithMultipleParameters.class);

        Assert.assertEquals("Returned url didn't match Wildcard parameter",
                RouteWithMultipleParameters.class.getAnnotation(Route.class)
                        .value(),
                url);
        url = routeConfiguration.getUrl(OptionalParameter.class);

        Assert.assertEquals("Returned url didn't match Optional parameter",
                OptionalParameter.class.getAnnotation(Route.class).value(),
                url);
    }

    @Test // 3519
    public void getUrlBase_returns_url_without_parameter_even_for_required_parameters()
            throws InvalidRouteConfigurationException {
            setNavigationTargets(RouteWithParameter.class,
                    RouteWithMultipleParameters.class, OptionalParameter.class,
                    FooNavigationTarget.class);

            Assert.assertEquals("Required parameter didn't match url base.",
                    RouteWithParameter.class.getAnnotation(Route.class).value(),
                    routeConfiguration.getUrlBase(RouteWithParameter.class)
                            .orElse(null));
        Assert.assertEquals("Wildcard parameter didn't match url base.",
                RouteWithMultipleParameters.class.getAnnotation(Route.class)
                        .value(),
                routeConfiguration.getUrlBase(RouteWithMultipleParameters.class)
                        .orElse(null));
        Assert.assertEquals("Optional parameter didn't match url base.",
                OptionalParameter.class.getAnnotation(Route.class).value(),
                routeConfiguration.getUrlBase(OptionalParameter.class)
                        .orElse(null));
        Assert.assertEquals("Non parameterized url didn't match url base.",
                FooNavigationTarget.class.getAnnotation(Route.class).value(),
                routeConfiguration.getUrlBase(FooNavigationTarget.class)
                        .orElse(null));
    }

    @Test // #2740
    public void getTemplate_returns_url_template()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithParameter.class,
                RouteWithMultipleParameters.class, OptionalParameter.class,
                FooNavigationTarget.class);

        Assert.assertEquals("Required parameter didn't match route template.",
                "param/" + HasUrlParameterFormat.PARAMETER,
                routeConfiguration.getTemplate(RouteWithParameter.class)
                        .orElse(null));
        Assert.assertEquals("Wildcard parameter didn't match route template.",
                "param/" + HasUrlParameterFormat.PARAMETER + "*",
                routeConfiguration.getTemplate(RouteWithMultipleParameters.class)
                        .orElse(null));
        Assert.assertEquals("Optional parameter didn't match route template.",
                "optional/" + HasUrlParameterFormat.PARAMETER + "?",
                routeConfiguration.getTemplate(OptionalParameter.class)
                        .orElse(null));
        Assert.assertEquals("Non parameterized url didn't match route template.",
                "foo",
                routeConfiguration.getTemplate(FooNavigationTarget.class)
                        .orElse(null));
    }

    @Test
    public void routerLinkInParent_updatesWhenNavigating()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LoneRoute.class, RouteChild.class);

        ui.navigate(routeConfiguration.getUrl(LoneRoute.class));

        RouteParent routeParent = (RouteParent) ui.getInternals()
                .getActiveRouterTargetsChain().get(1);
        RouterLink loneLink = routeParent.loneLink;

        Assert.assertTrue("Link should be attached",
                loneLink.getUI().isPresent());
        Assert.assertTrue(
                "Link should be highlighted when navigated to link target",
                loneLink.getElement().hasAttribute("highlight"));

        ui.navigate(routeConfiguration.getUrl(RouteChild.class));

        Assert.assertTrue("Link should be attached",
                loneLink.getUI().isPresent());
        Assert.assertFalse(
                "Link should not be highlighted when navigated to other target",
                loneLink.getElement().hasAttribute("highlight"));
    }

    @Test // #2740
    public void navigation_targets_remove_route_with_same_path()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class);

        assertMyPageAndWithParamAvailable();

        routeConfiguration.update(() -> routeConfiguration.removeRoute("my"));

        final List<RouteData> availableRoutes = routeConfiguration
                .getAvailableRoutes();
        Assert.assertEquals(1, availableRoutes.size());
        Assert.assertEquals("my/" + HasUrlParameterFormat.PARAMETER,
                availableRoutes.get(0).getTemplate());
        Assert.assertEquals(MyPageWithParam.class,
                availableRoutes.get(0).getNavigationTarget());
    }

    @Test // #2740
    public void navigation_targets_remove_route_with_same_path_and_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class);

        assertMyPageAndWithParamAvailable();

        routeConfiguration.update(() -> routeConfiguration
                .removeRoute("my/" + HasUrlParameterFormat.PARAMETER));

        assertMyPageAvailable();
    }

    @Test // #2740
    public void navigation_targets_remove_route_target_with_same_path_and_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class);

        assertMyPageAndWithParamAvailable();

        routeConfiguration.update(() -> routeConfiguration.removeRoute("my",
                MyPageWithParam.class));

        assertMyPageAvailable();
    }

    private void assertMyPageAvailable() {
        final List<RouteData> availableRoutes = routeConfiguration
                .getAvailableRoutes();

        Assert.assertEquals(1, availableRoutes.size());
        Assert.assertEquals("my", availableRoutes.get(0).getTemplate());
        Assert.assertEquals(MyPage.class,
                availableRoutes.get(0).getNavigationTarget());
    }

    private void assertMyPageAndWithParamAvailable() {
        Assert.assertEquals(MyPage.class, routeConfiguration.getRoute("my").get());
        Assert.assertEquals(MyPageWithParam.class, routeConfiguration
                .getRoute("my/" + HasUrlParameterFormat.PARAMETER).get());
        Assert.assertEquals(MyPageWithParam.class, routeConfiguration
                .getRoute("my", Arrays.asList(HasUrlParameterFormat.PARAMETER)).get());
    }

    @Test // #2740
    public void navigation_targets_with_same_route_and_one_with_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class);

        assertSameRouteWithParams();
    }

    @Test // #2740
    public void navigation_targets_with_same_route_and_two_with_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class,
                MyPageWithWildcardParam.class);

        assertSameRouteWithParams();

        Assert.assertEquals("my/:" + HasUrlParameterFormat.PARAMETER_NAME + "*",
                routeConfiguration.getTemplate(MyPageWithWildcardParam.class)
                        .get());

        Assert.assertEquals("my/wild/value", routeConfiguration
                .getUrl(MyPageWithWildcardParam.class, "wild/value"));
        Assert.assertEquals("my/wild/value",
                routeConfiguration.getUrl(MyPageWithWildcardParam.class,
                        new RouteParameters(HasUrlParameterFormat.PARAMETER_NAME,
                                "wild/value")));

        Assert.assertEquals(MyPageWithWildcardParam.class,
                routeConfiguration.getRoute("my/wild/param").get());
    }

    private void assertSameRouteWithParams() {
        Assert.assertEquals("my",
                routeConfiguration.getTemplate(MyPage.class).get());
        Assert.assertEquals("my/:" + HasUrlParameterFormat.PARAMETER_NAME,
                routeConfiguration.getTemplate(MyPageWithParam.class).get());

        Assert.assertEquals("my", routeConfiguration.getUrl(MyPage.class));
        Assert.assertEquals("my/value",
                routeConfiguration.getUrl(MyPageWithParam.class, "value"));
        Assert.assertEquals("my/value",
                routeConfiguration.getUrl(MyPageWithParam.class,
                        new RouteParameters(HasUrlParameterFormat.PARAMETER_NAME,
                                "value")));

        Assert.assertEquals(MyPage.class,
                routeConfiguration.getRoute("my").get());
        Assert.assertEquals(MyPageWithParam.class,
                routeConfiguration.getRoute("my/param").get());
    }

    @RoutePrefix("parent")
    @Tag(Tag.DIV)
    public static class RouteParent extends Component implements RouterLayout {
        private final RouterLink loneLink = new RouterLink("lone",
                LoneRoute.class);

        public RouteParent() {
            getElement().appendChild(loneLink.getElement());
        }
    }

    @Route(value = "single", layout = RouteParent.class, absolute = true)
    @Tag(Tag.DIV)
    public static class LoneRoute extends Component
            implements BeforeEnterObserver {

        static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }

    }

    @Route(value = "child", layout = RouteParent.class)
    @Tag(Tag.DIV)
    public static class RouteChild extends Component
            implements BeforeLeaveObserver, BeforeEnterObserver {

        static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add(event);
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildParameter extends Component
            implements HasUrlParameter<String> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("optional")
    @Tag(Tag.DIV)
    public static class OptionalParameter extends Component
            implements HasUrlParameter<String> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class WildRootParameter extends Component
            implements HasUrlParameter<String> {

        private static List<EventObject> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class OptionalRootParameter extends Component
            implements HasUrlParameter<String> {

        private static List<EventObject> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("integer")
    @Tag(Tag.DIV)
    public static class IntegerParameter extends Component
            implements HasUrlParameter<Integer> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static Integer param;

        @Override
        public void setParameter(BeforeEvent event, Integer parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("long")
    @Tag(Tag.DIV)
    public static class LongParameter extends Component
            implements HasUrlParameter<Long> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static Long param;

        @Override
        public void setParameter(BeforeEvent event, Long parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("boolean")
    @Tag(Tag.DIV)
    public static class BooleanParameter extends Component
            implements HasUrlParameter<Boolean> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static Boolean param;

        @Override
        public void setParameter(BeforeEvent event, Boolean parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("param")
    @Tag(Tag.DIV)
    public static class RouteWithParameter extends Component
            implements BeforeEnterObserver, HasUrlParameter<String> {

        private static String param;

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            events.add(event);
            param = parameter;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }
    }

    @Route("param")
    @Tag(Tag.DIV)
    public static class RouteWithMultipleParameters extends Component
            implements BeforeEnterObserver, HasUrlParameter<String> {

        private static String param;

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            events.add(event);
            param = parameter;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }
    }

    @Route("my")
    @Tag(Tag.DIV)
    public class MyPage extends Component {
    }

    @Route("my")
    @Tag(Tag.DIV)
    public class MyPageWithParam extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    @Route("my")
    @Tag(Tag.DIV)
    public class MyPageWithWildcardParam extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.WildcardParameter String parameter) {
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
        public Router getRouter() {
            return router;
        }

    }

}

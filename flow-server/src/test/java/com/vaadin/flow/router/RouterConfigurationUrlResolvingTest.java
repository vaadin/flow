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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.internal.HasUrlParameterFormat;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.VaadinService;

@NotThreadSafe
class RouterConfigurationUrlResolvingTest extends RoutingTestBase {
    private RouteConfiguration routeConfiguration;
    private UI ui;
    private VaadinService service = Mockito.mock(VaadinService.class);
    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @BeforeEach
    public void init() throws NoSuchFieldException, IllegalAccessException {
        super.init();
        ui = new RouterTestMockUI(router);
        ui.getSession().lock();

        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(service.getRouter()).thenReturn(router);

        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        routeConfiguration = RouteConfiguration
                .forRegistry(router.getRegistry());
    }

    @AfterEach
    public void tearDown() {
        CurrentInstance.clearAll();
    }

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

        Assertions.assertEquals("",
                routeConfiguration.getUrl(RootNavigationTarget.class));
        Assertions.assertEquals("foo",
                routeConfiguration.getUrl(FooNavigationTarget.class));
        Assertions.assertEquals("foo/bar",
                routeConfiguration.getUrl(FooBarNavigationTarget.class));
    }

    @Test
    public void nested_layouts_url_resolving()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteChild.class, LoneRoute.class);

        Assertions.assertEquals("parent/child",
                routeConfiguration.getUrl(RouteChild.class));
        Assertions.assertEquals("single",
                routeConfiguration.getUrl(LoneRoute.class));
    }

    @Test
    public void layout_with_url_parameter_url_resolving()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                OtherGreetingNavigationTarget.class);

        Assertions.assertEquals("greeting/my_param", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "my_param"));
        Assertions.assertEquals("greeting/true", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "true"));

        Assertions.assertEquals("greeting/other", routeConfiguration
                .getUrl(GreetingNavigationTarget.class, "other"));
    }

    @Test
    public void url_resolves_correctly_for_optional_and_wild_parameters()
            throws InvalidRouteConfigurationException, NotFoundException {
        setNavigationTargets(OptionalParameter.class, WildParameter.class);

        Assertions.assertEquals("optional",
                routeConfiguration.getUrl(OptionalParameter.class),
                "Optional value should be able to return even without any parameters");

        Assertions.assertEquals("wild",
                routeConfiguration.getUrl(WildParameter.class),
                "Wildcard value should be able to return even without any parameters");

        Assertions.assertEquals("optional/my_param",
                routeConfiguration.getUrl(OptionalParameter.class, "my_param"));

        Assertions.assertEquals("wild/true",
                routeConfiguration.getUrl(WildParameter.class, "true"));

        Assertions.assertEquals("wild/there/are/many/of/us", routeConfiguration
                .getUrl(WildParameter.class, "there/are/many/of/us"));
    }

    @Test
    public void getUrl_with_wildcard_parameter_requires_pre_encoded_special_chars()
            throws InvalidRouteConfigurationException, NotFoundException {
        setNavigationTargets(WildParameter.class);

        // When generating URLs, special characters that should be preserved
        // as data (not path structure) must be encoded BEFORE calling getUrl()

        // Example: If you want a parameter value of "a/b" (with actual slash)
        // you must encode it before passing to getUrl()
        String parameterWithSlash = "a%2Fb"; // %2F will be preserved in URL
        Assertions.assertEquals("wild/a%2Fb", routeConfiguration
                .getUrl(WildParameter.class, parameterWithSlash));

        // Unencoded slashes are treated as path separators
        Assertions.assertEquals("wild/a/b",
                routeConfiguration.getUrl(WildParameter.class, "a/b"));

        // Other special characters should also be pre-encoded if needed
        String parameterWithQuestion = "test%3Fquestion";
        Assertions.assertEquals("wild/test%3Fquestion", routeConfiguration
                .getUrl(WildParameter.class, parameterWithQuestion));
    }

    @Test
    public void wildcardPathWithEmptyParameter_emptyParameterIsAvailable() {
        WildParameter.events.clear();
        WildParameter.param = null;
        setNavigationTargets(WildParameter.class);

        router.navigate(ui, new Location("wild//two/three"),
                NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals("/two/three", WildParameter.param);

        router.navigate(ui, new Location("wild////four/five"),
                NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals("///four/five", WildParameter.param);

        router.navigate(ui, new Location("wild//two//four"),
                NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals("/two//four", WildParameter.param);
    }

    @Test
    public void root_navigation_target_with_wildcard_parameter()
            throws InvalidRouteConfigurationException {
        WildRootParameter.events.clear();
        WildRootParameter.param = null;
        setNavigationTargets(WildRootParameter.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals(1, WildRootParameter.events.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals("", WildRootParameter.param,
                "Parameter should be empty");

        router.navigate(ui, new Location("my/wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals(2, WildRootParameter.events.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals("my/wild", WildRootParameter.param,
                "Parameter should be empty");

        Assertions.assertEquals("",
                routeConfiguration.getUrl(WildRootParameter.class));
        Assertions.assertEquals("wild",
                routeConfiguration.getUrl(WildRootParameter.class, "wild"));

        List<String> params = Arrays.asList("", null);
        Assertions.assertEquals("", routeConfiguration
                .getUrl(WildRootParameter.class, params.get(1)));
    }

    @Test
    public void root_navigation_target_with_optional_parameter()
            throws InvalidRouteConfigurationException {
        OptionalRootParameter.events.clear();
        OptionalRootParameter.param = null;
        setNavigationTargets(OptionalRootParameter.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals(1, OptionalRootParameter.events.size(),
                "Expected event amount was wrong");
        Assertions.assertNull(OptionalRootParameter.param,
                "Parameter should be empty");

        router.navigate(ui, new Location("optional"),
                NavigationTrigger.PROGRAMMATIC);

        Assertions.assertEquals(2, OptionalRootParameter.events.size(),
                "Expected event amount was wrong");
        Assertions.assertEquals("optional", OptionalRootParameter.param,
                "Parameter should be empty");

        Assertions.assertEquals("",
                routeConfiguration.getUrl(OptionalRootParameter.class));
        Assertions.assertEquals("optional", routeConfiguration
                .getUrl(OptionalRootParameter.class, "optional"));

        List<String> params = Arrays.asList("", null);
        Assertions.assertEquals("", routeConfiguration
                .getUrl(OptionalRootParameter.class, params.get(1)));
    }

    @Test
    public void getUrl_for_has_url_with_supported_parameters()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(IntegerParameter.class, LongParameter.class,
                BooleanParameter.class);

        Assertions.assertEquals("integer/5",
                routeConfiguration.getUrl(IntegerParameter.class, 5));

        Assertions.assertEquals("long/5",
                routeConfiguration.getUrl(LongParameter.class, 5l));

        Assertions.assertEquals("boolean/false",
                routeConfiguration.getUrl(BooleanParameter.class, false));
    }

    @Test // 3519
    public void getUrl_throws_for_required_parameter() {
        IllegalArgumentException ex = Assertions
                .assertThrows(IllegalArgumentException.class, () -> {
                    setNavigationTargets(RouteWithParameter.class);

                    routeConfiguration.getUrl(RouteWithParameter.class);
                });
    }

    @Test // 3519
    public void getUrl_returns_url_if_parameter_is_wildcard_or_optional()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithMultipleParameters.class,
                OptionalParameter.class);

        String url = routeConfiguration
                .getUrl(RouteWithMultipleParameters.class);

        Assertions.assertEquals(
                RouteWithMultipleParameters.class.getAnnotation(Route.class)
                        .value(),
                url, "Returned url didn't match Wildcard parameter");
        url = routeConfiguration.getUrl(OptionalParameter.class);

        Assertions.assertEquals(
                OptionalParameter.class.getAnnotation(Route.class).value(), url,
                "Returned url didn't match Optional parameter");
    }

    @Test // 3519
    public void getUrlBase_returns_url_without_parameter_even_for_required_parameters()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithParameter.class,
                RouteWithMultipleParameters.class, OptionalParameter.class,
                FooNavigationTarget.class);

        Assertions.assertEquals(
                RouteWithParameter.class.getAnnotation(Route.class).value(),
                routeConfiguration.getUrlBase(RouteWithParameter.class).orElse(
                        null),
                "Required parameter didn't match url base.");
        Assertions.assertEquals(
                RouteWithMultipleParameters.class.getAnnotation(Route.class)
                        .value(),
                routeConfiguration.getUrlBase(RouteWithMultipleParameters.class)
                        .orElse(null),
                "Wildcard parameter didn't match url base.");
        Assertions.assertEquals(
                OptionalParameter.class.getAnnotation(Route.class).value(),
                routeConfiguration.getUrlBase(OptionalParameter.class).orElse(
                        null),
                "Optional parameter didn't match url base.");
        Assertions.assertEquals(
                FooNavigationTarget.class.getAnnotation(Route.class).value(),
                routeConfiguration.getUrlBase(FooNavigationTarget.class).orElse(
                        null),
                "Non parameterized url didn't match url base.");
    }

    @Test // #2740
    public void getTemplate_returns_url_template()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithParameter.class,
                RouteWithMultipleParameters.class, OptionalParameter.class,
                FooNavigationTarget.class);

        Assertions.assertEquals("param/" + HasUrlParameterFormat.PARAMETER,
                routeConfiguration.getTemplate(RouteWithParameter.class)
                        .orElse(null),
                "Required parameter didn't match route template.");
        Assertions.assertEquals(
                "param/" + HasUrlParameterFormat.PARAMETER + "*",
                routeConfiguration
                        .getTemplate(RouteWithMultipleParameters.class)
                        .orElse(null),
                "Wildcard parameter didn't match route template.");
        Assertions.assertEquals(
                "optional/" + HasUrlParameterFormat.PARAMETER + "?",
                routeConfiguration.getTemplate(OptionalParameter.class)
                        .orElse(null),
                "Optional parameter didn't match route template.");
        Assertions.assertEquals("foo",
                routeConfiguration.getTemplate(FooNavigationTarget.class)
                        .orElse(null),
                "Non parameterized url didn't match route template.");
    }

    @Test
    public void routerLinkInParent_updatesWhenNavigating()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LoneRoute.class, RouteChild.class);

        ui.navigate(routeConfiguration.getUrl(LoneRoute.class));

        RouteParent routeParent = (RouteParent) ui.getInternals()
                .getActiveRouterTargetsChain().get(1);
        RouterLink loneLink = routeParent.loneLink;

        Assertions.assertTrue(loneLink.getUI().isPresent(),
                "Link should be attached");
        Assertions.assertTrue(loneLink.getElement().hasAttribute("highlight"),
                "Link should be highlighted when navigated to link target");

        ui.navigate(routeConfiguration.getUrl(RouteChild.class));

        Assertions.assertTrue(loneLink.getUI().isPresent(),
                "Link should be attached");
        Assertions.assertFalse(loneLink.getElement().hasAttribute("highlight"),
                "Link should not be highlighted when navigated to other target");
    }

    @Test // #2740
    public void navigation_targets_remove_route_with_same_path()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(MyPage.class, MyPageWithParam.class);

        assertMyPageAndWithParamAvailable();

        routeConfiguration.update(() -> routeConfiguration.removeRoute("my"));

        final List<RouteData> availableRoutes = routeConfiguration
                .getAvailableRoutes();
        Assertions.assertEquals(1, availableRoutes.size());
        Assertions.assertEquals("my/" + HasUrlParameterFormat.PARAMETER,
                availableRoutes.get(0).getTemplate());
        Assertions.assertEquals(MyPageWithParam.class,
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

        Assertions.assertEquals(1, availableRoutes.size());
        Assertions.assertEquals("my", availableRoutes.get(0).getTemplate());
        Assertions.assertEquals(MyPage.class,
                availableRoutes.get(0).getNavigationTarget());
    }

    private void assertMyPageAndWithParamAvailable() {
        Assertions.assertEquals(MyPage.class,
                routeConfiguration.getRoute("my").get());
        Assertions.assertEquals(MyPageWithParam.class, routeConfiguration
                .getRoute("my/" + HasUrlParameterFormat.PARAMETER).get());
        Assertions.assertEquals(MyPageWithParam.class,
                routeConfiguration
                        .getRoute("my",
                                Arrays.asList(HasUrlParameterFormat.PARAMETER))
                        .get());
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

        Assertions.assertEquals(
                "my/:" + HasUrlParameterFormat.PARAMETER_NAME + "*",
                routeConfiguration.getTemplate(MyPageWithWildcardParam.class)
                        .get());

        Assertions.assertEquals("my/wild/value", routeConfiguration
                .getUrl(MyPageWithWildcardParam.class, "wild/value"));
        Assertions.assertEquals("my/wild/value",
                routeConfiguration.getUrl(MyPageWithWildcardParam.class,
                        new RouteParameters(
                                HasUrlParameterFormat.PARAMETER_NAME,
                                "wild/value")));

        Assertions.assertEquals(MyPageWithWildcardParam.class,
                routeConfiguration.getRoute("my/wild/param").get());
    }

    private void assertSameRouteWithParams() {
        Assertions.assertEquals("my",
                routeConfiguration.getTemplate(MyPage.class).get());
        Assertions.assertEquals("my/:" + HasUrlParameterFormat.PARAMETER_NAME,
                routeConfiguration.getTemplate(MyPageWithParam.class).get());

        Assertions.assertEquals("my", routeConfiguration.getUrl(MyPage.class));
        Assertions.assertEquals("my/value",
                routeConfiguration.getUrl(MyPageWithParam.class, "value"));
        Assertions.assertEquals("my/value",
                routeConfiguration.getUrl(MyPageWithParam.class,
                        new RouteParameters(
                                HasUrlParameterFormat.PARAMETER_NAME,
                                "value")));

        Assertions.assertEquals(MyPage.class,
                routeConfiguration.getRoute("my").get());
        Assertions.assertEquals(MyPageWithParam.class,
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

}

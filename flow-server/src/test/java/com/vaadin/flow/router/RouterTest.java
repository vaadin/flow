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
package com.vaadin.flow.router;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.internal.ContinueNavigationAction;
import com.vaadin.flow.server.BootstrapHandlerTest;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.tests.util.MockUI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@NotThreadSafe
public class RouterTest extends RoutingTestBase {

    private static final String DYNAMIC_TITLE = "I am dynamic!";
    public static final String EXCEPTION_WRAPPER_MESSAGE = "There was an exception while trying to navigate to '%s' with the exception message '%s'";

    private static List<String> eventCollector = new ArrayList<>(0);

    private UI ui;

    private VaadinService service = Mockito.mock(VaadinService.class);

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

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
            implements BeforeEnterObserver, BeforeLeaveObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("FooBar ACTIVATING");
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            eventCollector.add("FooBar DEACTIVATING");
        }
    }

    @Route("enteringTarget")
    @Tag(Tag.DIV)
    public static class EnteringNavigationTarget extends Component
            implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("EnterListener got event");
        }
    }

    @Route("leavingTarget")
    @Tag(Tag.DIV)
    public static class LeavingNavigationTarget extends Component
            implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            eventCollector.add("LeaveListener got event");
        }
    }

    @Route("combined")
    @Tag(Tag.DIV)
    public static class CombinedObserverTarget extends Component {
        @Tag(Tag.DIV)
        public static class Enter extends Component
                implements BeforeEnterObserver {

            @Override
            public void beforeEnter(BeforeEnterEvent event) {
                eventCollector.add("EnterListener got event");
            }
        }

        @Tag(Tag.DIV)
        public static class Leave extends Component
                implements BeforeLeaveObserver {

            @Override
            public void beforeLeave(BeforeLeaveEvent event) {
                eventCollector.add("LeaveListener got event");
            }
        }

        @Tag(Tag.DIV)
        public static class Before extends Component
                implements BeforeEnterObserver, BeforeLeaveObserver {
            @Override
            public void beforeEnter(BeforeEnterEvent event) {
                eventCollector.add(
                        "BeforeNavigation got event with state ACTIVATING");
            }

            @Override
            public void beforeLeave(BeforeLeaveEvent event) {
                eventCollector.add(
                        "BeforeNavigation got event with state DEACTIVATING");
            }
        }

        public CombinedObserverTarget() {
            getElement().appendChild(new Enter().getElement(),
                    new Leave().getElement(), new Before().getElement());
        }
    }

    @Route("reroute")
    @Tag(Tag.DIV)
    public static class ReroutingNavigationTarget extends Component
            implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
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
            implements BeforeEnterObserver, HasUrlParameter<String> {

        private String param;

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            eventCollector.add("Received param: " + parameter);
            param = parameter;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("Stored parameter: " + param);
        }
    }

    @Route("param")
    @Tag(Tag.DIV)
    public static class RouteWithMultipleParameters extends Component
            implements BeforeEnterObserver, HasUrlParameter<String> {

        private String param;

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            eventCollector.add("Received param: " + parameter);
            param = parameter;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
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
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            eventCollector.add(parameter == null ? "No parameter" : parameter);
        }
    }

    @Route("usupported/wildcard")
    @Tag(Tag.DIV)
    public static class UnsupportedWildParameter extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter Integer parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildHasParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("integer")
    @Tag(Tag.DIV)
    public static class IntegerParameter extends Component
            implements HasUrlParameter<Integer> {

        @Override
        public void setParameter(BeforeEvent event, Integer parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("long")
    @Tag(Tag.DIV)
    public static class LongParameter extends Component
            implements HasUrlParameter<Long> {

        @Override
        public void setParameter(BeforeEvent event, Long parameter) {
            eventCollector.add("Parameter: " + parameter);
        }
    }

    @Route("boolean")
    @Tag(Tag.DIV)
    public static class BooleanParameter extends Component
            implements HasUrlParameter<Boolean> {

        @Override
        public void setParameter(BeforeEvent event, Boolean parameter) {
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
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteTo("param", "hello");
        }
    }

    @Route("fail/param")
    @Tag(Tag.DIV)
    public static class FailRerouteWithParam extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteTo("param", Boolean.TRUE);
        }
    }

    @Route("redirect/to/params")
    @Tag(Tag.DIV)
    public static class RerouteToRouteWithMultipleParams extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteTo("param", Arrays.asList("this", "must", "work"));
        }
    }

    @Route("fail/params")
    @Tag(Tag.DIV)
    public static class FailRerouteWithParams extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteTo("param", Arrays.asList(1L, 2L));
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
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            title = parameter;
        }
    }

    @Route("url")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithDynamicTitleFromNavigation
            extends Component implements HasDynamicTitle, BeforeEnterObserver {

        private String title = DYNAMIC_TITLE;

        public NavigationTargetWithDynamicTitleFromNavigation() {
        }

        @Override
        public String getPageTitle() {
            return title;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            title = "ACTIVATING";
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
            implements AfterNavigationObserver {
        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            eventCollector.add("Event after navigation");
        }
    }

    @Tag(Tag.DIV)
    private static class BeforeNavigation extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("Event before navigation");
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            eventCollector.add("Event before navigation");
        }
    }

    @RoutePrefix("parent")
    @Tag(Tag.DIV)
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
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            eventCollector.add("With parameter: " + parameter);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class OptionalRootParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            eventCollector.add(parameter == null ? "No parameter" : parameter);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class RootParameter extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            eventCollector.add(parameter);
        }
    }

    public static class ErrorTarget extends RouteNotFoundError
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("Redirected to error view, showing message: "
                    + getElement().getText());
        }
    }

    public static final String EXCEPTION_TEXT = "My custom not found class!";

    public static class CustomNotFoundTarget extends RouteNotFoundError {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText(EXCEPTION_TEXT);
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @Tag(Tag.DIV)
    public static class NonExtendingNotFoundTarget extends Component
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText(EXCEPTION_TEXT);
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @Tag(Tag.DIV)
    @ParentLayout(RouteParent.class)
    public static class ErrorTargetWithParent extends Component
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText(EXCEPTION_TEXT);
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @Tag(Tag.DIV)
    public static class DuplicateNotFoundTarget extends Component
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText(EXCEPTION_TEXT);
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @Route("exception")
    @Tag(Tag.DIV)
    public static class FailOnException extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            throw new RuntimeException("Failed on an exception");
        }
    }

    @Tag(Tag.DIV)
    public static class FaultyErrorView extends Component
            implements HasErrorParameter<IllegalArgumentException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IllegalArgumentException> parameter) {
            // Return faulty status code.
            return 0;
        }
    }

    @Route("beforeToError/exception")
    @Tag(Tag.DIV)
    public static class RerouteToError extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteToError(IllegalArgumentException.class);
        }
    }

    @Route("beforeToError/message")
    @Tag(Tag.DIV)
    public static class RerouteToErrorWithMessage extends Component
            implements BeforeEnterObserver, HasUrlParameter<String> {

        String message;

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteToError(IllegalArgumentException.class, message);
        }

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            message = parameter;
        }
    }

    @Tag(Tag.DIV)
    public static class IllegalTarget extends Component
            implements HasErrorParameter<IllegalArgumentException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IllegalArgumentException> parameter) {
            eventCollector
                    .add("Error location: " + event.getLocation().getPath());
            if (parameter.hasCustomMessage()) {
                getElement().setText(parameter.getCustomMessage());
            } else {
                getElement().setText("Illegal argument exception.");
            }
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }

    @Route("loop")
    @Tag(Tag.DIV)
    public static class LoopByReroute extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("Loop");
            UI.getCurrent().navigateTo("loop");
        }
    }

    @Route("redirect/loop")
    @Tag(Tag.DIV)
    public static class RedirectToLoopByReroute extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("Redirect");
            UI.getCurrent().navigateTo("loop");
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingForeverNavigationTarget extends Component
            implements BeforeLeaveObserver {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.postpone();
            eventCollector.add("Postponed");
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingAndResumingNavigationTarget extends Component
            implements BeforeLeaveObserver {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            ContinueNavigationAction action = event.postpone();
            eventCollector.add("Postponed");
            sleepThenRun(100, action);
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingFirstTimeNavigationTarget extends Component
            implements BeforeLeaveObserver {

        private int counter = 0;

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            counter++;
            if (counter < 2) {
                ContinueNavigationAction action = event.postpone();
                eventCollector.add("Postponed");
                sleepThenRun(50, action);
            } else {
                eventCollector.add("Not postponing anymore");
            }
        }
    }

    @Tag(Tag.DIV)
    public static class ChildListener extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            eventCollector.add("ChildListener notified");
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            eventCollector.add("ChildListener notified");
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingAndResumingCompoundNavigationTarget
            extends Component implements BeforeLeaveObserver {

        public PostponingAndResumingCompoundNavigationTarget() {
            getElement().appendChild(new ChildListener().getElement());
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            ContinueNavigationAction action = event.postpone();
            eventCollector.add("Postponed");
            sleepThenRun(100, action);
        }
    }

    static void sleepThenRun(int millis, ContinueNavigationAction action) {
        new Thread(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                fail("Resuming thread was interrupted");
            }
            eventCollector.add("Resuming");
            action.proceed();
        }).start();
    }

    @Route("toNotFound")
    @Tag(Tag.DIV)
    public static class RedirectToNotFoundInHasParam extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    @Route("param/reroute")
    @Tag(Tag.DIV)
    public static class RedirectOnSetParam extends Component
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            // NOTE! Expects RootParameter.class to be registered!
            event.rerouteTo("", parameter);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class Translations extends Component
            implements LocaleChangeObserver {

        @Override
        public void localeChange(LocaleChangeEvent event) {
            eventCollector.add("Received locale change event for locale: "
                    + event.getLocale().getDisplayName());
        }
    }

    @Tag(Tag.DIV)
    public static class MainLayout extends Component implements RouterLayout {
    }

    @Route(value = "base", layout = MainLayout.class)
    @ParentLayout(MainLayout.class)
    @Tag(Tag.DIV)
    public static class BaseLayout extends Component implements RouterLayout {
    }

    @Route(value = "sub", layout = BaseLayout.class)
    @Tag(Tag.DIV)
    public static class SubLayout extends Component {
    }

    @HtmlImport("frontend://bower_components/vaadin-lumo-styles/color.html")
    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }

        @Override
        public List<String> getBodyInlineContents() {
            return Arrays.asList(
                    "<custom-style><style include=\"lumo-typography\"></style></custom-style>");
        }
    }

    @Theme(MyTheme.class)
    @Tag(Tag.DIV)
    public static abstract class AbstractMain extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class ExtendingView extends
            AbstractMain {
    }
    @Override
    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        ui = new RouterTestUI(router);
        eventCollector.clear();

        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        Mockito.when(configuration.isProductionMode()).thenReturn(true);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
        VaadinService.setCurrent(null);
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
    public void leave_and_enter_listeners_only_receive_correct_state()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(LeavingNavigationTarget.class,
                                EnteringNavigationTarget.class,
                                RootNavigationTarget.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("enteringTarget"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeEnterObserver should have fired.", 1,
                eventCollector.size());
        Assert.assertEquals("EnterListener got event", eventCollector.get(0));

        router.navigate(ui, new Location("leavingTarget"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("No leave or enter target should have fired.", 1,
                eventCollector.size());

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeLeaveObserver should have fired", 2,
                eventCollector.size());
        Assert.assertEquals("LeaveListener got event", eventCollector.get(1));
    }

    @Test
    public void leave_navigate_and_enter_listeners_execute_in_correct_order()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(CombinedObserverTarget.class,
                                RootNavigationTarget.class)
                        .collect(Collectors.toSet()));

        // Observer execution order should be BeforeNavigation before
        // EnterListener, but BeforeLeave before BeforeNavigation
        router.navigate(ui, new Location("combined"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeNavigationObserver and BeforeEnterObserver should have fired.",
                2, eventCollector.size());
        Assert.assertEquals("EnterListener got event", eventCollector.get(0));
        Assert.assertEquals("BeforeNavigation got event with state ACTIVATING",
                eventCollector.get(1));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeNavigationObserver and BeforeLeaveObserver target should have fired.",
                4, eventCollector.size());

        Assert.assertEquals("LeaveListener got event", eventCollector.get(2));
        Assert.assertEquals(
                "BeforeNavigation got event with state DEACTIVATING",
                eventCollector.get(3));
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
    public void reroute_with_url_parameter()
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
    public void reroute_fails_with_no_url_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(GreetingNavigationTarget.class,
                                ParameterRouteNoParameter.class,
                                RerouteToRouteWithParam.class)
                        .collect(Collectors.toSet()));
        String locationString = "redirect/to/param";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "The navigation target for route 'param' doesn't accept the parameters [hello].";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_fails_with_faulty_url_parameter()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream.of(GreetingNavigationTarget.class,
                        RouteWithParameter.class, FailRerouteWithParam.class)
                        .collect(Collectors.toSet()));

        String locationString = "fail/param";
        router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        String message = "Given route parameter 'class java.lang.Boolean' is of the wrong type. Required 'class java.lang.String'.";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);

        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_with_multiple_url_parameters()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(GreetingNavigationTarget.class,
                                RouteWithMultipleParameters.class,
                                RerouteToRouteWithMultipleParams.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("redirect/to/params"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "Stored parameter: this/must/work", eventCollector.get(1));
    }

    @Test
    public void reroute_fails_with_faulty_url_parameters()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(GreetingNavigationTarget.class,
                                RouteWithMultipleParameters.class,
                                FailRerouteWithParams.class)
                        .collect(Collectors.toSet()));
        String locationString = "fail/params";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "Given route parameter 'class java.lang.Long' is of the wrong type. Required 'class java.lang.String'.";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_with_multiple_url_parameters_fails_to_parameterless_target()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(GreetingNavigationTarget.class,
                                ParameterRouteNoParameter.class,
                                RerouteToRouteWithMultipleParams.class)
                        .collect(Collectors.toSet()));
        String locationString = "redirect/to/params";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "The navigation target for route 'param' doesn't accept the parameters [this, must, work].";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_with_multiple_url_parameters_fails_to_single_parameter_target()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(GreetingNavigationTarget.class, RouteWithParameter.class,
                        RerouteToRouteWithMultipleParams.class)
                .collect(Collectors.toSet()));
        String locationString = "redirect/to/params";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "The navigation target for route 'param' doesn't accept the parameters [this, must, work].";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
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

        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, router.navigate(
                ui, new Location(""), NavigationTrigger.PROGRAMMATIC));
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

        List<String> params = Arrays.asList("", null);
        Assert.assertEquals("",
                router.getUrl(WildRootParameter.class, params.get(1)));
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

        List<String> params = Arrays.asList("", null);
        Assert.assertEquals("",
                router.getUrl(OptionalRootParameter.class, params.get(1)));
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
    public void reroute_on_hasParameter_step()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(RootParameter.class, RedirectOnSetParam.class)
                        .collect(Collectors.toSet()));

        router.navigate(ui, new Location("param/reroute/hello"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Parameter should be empty", "hello",
                eventCollector.get(0));
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

        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);

        String message = String.format(
                "Invalid wildcard parameter in class %s. Only String is supported for wildcard parameters.",
                UnsupportedWildParameter.class.getName());
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);

        assertExceptionComponent(exceptionText);
    }

    @Test
    public void redirect_to_routeNotFound_error_view_when_no_route_found()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(Stream
                .of(FooNavigationTarget.class).collect(Collectors.toSet()));
        router.getRegistry().setErrorNavigationTargets(
                Collections.singleton(ErrorTarget.class));

        int result = router.navigate(ui, new Location("error"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

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
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
    }

    @Test
    public void fail_for_multiple_of_the_same_class()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setErrorNavigationTargets(
                Stream.of(ErrorTarget.class, RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

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
                                DuplicateNotFoundTarget.class)
                        .collect(Collectors.toSet()));
    }

    @Test
    public void custom_exception_target_should_override_default_ones() {
        router.getRegistry()
                .setErrorNavigationTargets(Stream
                        .of(NonExtendingNotFoundTarget.class,
                                RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                NonExtendingNotFoundTarget.class, getUIComponent());

        assertExceptionComponent(EXCEPTION_TEXT,
                NonExtendingNotFoundTarget.class);
    }

    @Test
    public void custom_exception_target_is_used() {
        router.getRegistry().setErrorNavigationTargets(
                Stream.of(CustomNotFoundTarget.class, RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                CustomNotFoundTarget.class, getUIComponent());

        assertExceptionComponent(EXCEPTION_TEXT, CustomNotFoundTarget.class);
    }

    @Test
    public void error_target_has_parent_layout() {
        router.getRegistry().setErrorNavigationTargets(
                Stream.of(ErrorTargetWithParent.class, RouteNotFoundError.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Component parenComponent = ComponentUtil
                .findParentComponent(ui.getElement().getChild(0)).get();

        Assert.assertEquals(RouteParent.class, parenComponent.getClass());

        Assert.assertEquals("Expected only one child component", 1,
                parenComponent.getChildren().count());
        Assert.assertEquals("Error target should have been the only child.",
                ErrorTargetWithParent.class,
                parenComponent.getChildren().findFirst().get().getClass());
    }

    @Test
    public void reroute_to_error_opens_expected_error_target()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(RerouteToError.class));
        router.getRegistry().setErrorNavigationTargets(
                Collections.singleton(IllegalTarget.class));

        int result = router.navigate(ui,
                new Location("beforeToError/exception"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Target should have rerouted to exception target.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);

        Assert.assertEquals(IllegalTarget.class, getUIComponent());

        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();
        Assert.assertEquals("Illegal argument exception.",
                visibleComponent.get().getElement().getText());
    }

    @Test
    public void reroute_to_error_with_custom_message_message_is_used()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(RerouteToErrorWithMessage.class));
        router.getRegistry().setErrorNavigationTargets(
                Collections.singleton(IllegalTarget.class));

        int result = router.navigate(ui,
                new Location("beforeToError/message/CustomMessage"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Target should have rerouted to exception target.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);

        Assert.assertEquals(IllegalTarget.class, getUIComponent());

        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();
        Assert.assertEquals("CustomMessage",
                visibleComponent.get().getElement().getText());

        Assert.assertEquals("Expected only one event message from error view",
                1, eventCollector.size());
        Assert.assertEquals("Parameter should be empty",
                "Error location: beforeToError/message/CustomMessage",
                eventCollector.get(0));

    }

    @Test
    public void reroute_to_error_from_has_param()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(RedirectToNotFoundInHasParam.class)
                        .collect(Collectors.toSet()));

        int result = router.navigate(ui, new Location("toNotFound/error"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Target should have rerouted to exception target.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(RouteNotFoundError.class, getUIComponent());
    }

    @Test
    public void faulty_error_response_code_should_throw_exception()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(RerouteToError.class));
        router.getRegistry().setErrorNavigationTargets(
                Collections.singleton(FaultyErrorView.class));

        String location = "beforeToError/exception";
        int result = router.navigate(ui, new Location(location),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Target should have failed on an internal exception.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);

        String validationMessage = String.format(
                "Error state code must be a valid HttpServletResponse value. Received invalid value of '%s' for '%s'",
                0, FaultyErrorView.class.getName());

        String errorMessage = String.format(
                "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                location, validationMessage);

        assertExceptionComponent(errorMessage, InternalServerError.class);
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
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(LoopByReroute.class, RedirectToLoopByReroute.class)
                        .collect(Collectors.toSet()));

        ui.navigateTo("redirect/loop");

        Assert.assertEquals("Expected two events", 2, eventCollector.size());
    }

    @Test
    public void postpone_then_resume_on_before_navigation_event()
            throws InvalidRouteConfigurationException, InterruptedException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(RootNavigationTarget.class,
                                PostponingAndResumingNavigationTarget.class)
                        .collect(Collectors.toSet()));

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);
        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);
        Assert.assertEquals(PostponingAndResumingNavigationTarget.class,
                getUIComponent());

        Thread.sleep(200);

        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());
        Assert.assertEquals("Expected event amount was wrong", 2,
                eventCollector.size());
        Assert.assertEquals("Postponed", eventCollector.get(0));
        Assert.assertEquals("Resuming", eventCollector.get(1));
    }

    @Test
    public void postpone_forever_on_before_navigation_event()
            throws InvalidRouteConfigurationException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(RootNavigationTarget.class,
                                PostponingForeverNavigationTarget.class)
                        .collect(Collectors.toSet()));

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);
        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);
        Assert.assertEquals(PostponingForeverNavigationTarget.class,
                getUIComponent());
        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals("Postponed", eventCollector.get(0));
    }

    @Test
    public void postpone_obsoleted_by_new_navigation_transition()
            throws InvalidRouteConfigurationException, InterruptedException {
        router.getRegistry().setNavigationTargets(Stream
                .of(FooNavigationTarget.class, FooBarNavigationTarget.class,
                        PostponingFirstTimeNavigationTarget.class)
                .collect(Collectors.toSet()));

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);
        int status2 = router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        int status3 = router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);
        Assert.assertEquals("Third transition failed",
                HttpServletResponse.SC_OK, status3);
        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());

        Thread.sleep(200);

        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());
        Assert.assertEquals("Expected event amount was wrong", 4,
                eventCollector.size());
        Assert.assertEquals("Postponed", eventCollector.get(0));
        Assert.assertEquals("Not postponing anymore", eventCollector.get(1));
        Assert.assertEquals("FooBar ACTIVATING", eventCollector.get(2));
        Assert.assertEquals("Resuming", eventCollector.get(3));
    }

    @Test //3384
    public void theme_is_gotten_from_the_super_class()
            throws InvalidRouteConfigurationException, Exception {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(ExtendingView.class));

        router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Field theme = UIInternals.class.getDeclaredField("theme");
        theme.setAccessible(true);
        Object themeObject = theme.get(ui.getInternals());

        Assert.assertEquals(MyTheme.class, themeObject.getClass());
    }

    @Test
    public void postpone_then_resume_with_multiple_listeners()
            throws InvalidRouteConfigurationException, InterruptedException {
        router.getRegistry()
                .setNavigationTargets(Stream
                        .of(RootNavigationTarget.class,
                                PostponingAndResumingCompoundNavigationTarget.class)
                        .collect(Collectors.toSet()));

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);
        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);
        Assert.assertEquals(PostponingAndResumingCompoundNavigationTarget.class,
                getUIComponent());

        Thread.sleep(200);

        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());
        Assert.assertEquals("Expected event amount was wrong", 4,
                eventCollector.size());
        Assert.assertEquals("ChildListener notified", eventCollector.get(0));
        Assert.assertEquals("Postponed", eventCollector.get(1));
        Assert.assertEquals("Resuming", eventCollector.get(2));
        Assert.assertEquals("ChildListener notified", eventCollector.get(3));
    }

    @Test
    public void navigation_should_fire_locale_change_observer()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Collections.singleton(Translations.class));

        ui.navigateTo("");

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));
    }

    @Test
    public void away_navigation_should_not_inform_observer()
            throws InvalidRouteConfigurationException, InterruptedException {
        router.getRegistry().setNavigationTargets(
                Stream.of(FooNavigationTarget.class, Translations.class)
                        .collect(Collectors.toSet()));

        ui.navigateTo("");

        Assert.assertEquals("Expected event amount was wrong", 1,
                eventCollector.size());
        Assert.assertEquals(
                "Received locale change event for locale: "
                        + Locale.getDefault().getDisplayName(),
                eventCollector.get(0));

        ui.navigateTo("foo");

        Assert.assertEquals("Recorded event amount should have stayed the same",
                1, eventCollector.size());
    }

    @Test // 3424
    public void route_as_parent_layout_handles_as_expected()
            throws InvalidRouteConfigurationException {
        router.getRegistry().setNavigationTargets(
                Stream.of(BaseLayout.class, SubLayout.class)
                        .collect(Collectors.toSet()));

        ui.navigateTo("base");
        Assert.assertEquals(MainLayout.class, getUIComponent());

        List<Component> children = ui.getChildren()
                .collect(Collectors.toList());
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(MainLayout.class, children.get(0).getClass());
        children = children.get(0).getChildren().collect(Collectors.toList());
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(BaseLayout.class, children.get(0).getClass());
        children = children.get(0).getChildren().collect(Collectors.toList());
        Assert.assertTrue(children.isEmpty());

        ui.navigateTo("sub");
        Assert.assertEquals(MainLayout.class, getUIComponent());

        children = ui.getChildren()
                .collect(Collectors.toList());
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(MainLayout.class, children.get(0).getClass());
        children = children.get(0).getChildren().collect(Collectors.toList());
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(BaseLayout.class, children.get(0).getClass());
        children = children.get(0).getChildren().collect(Collectors.toList());
        Assert.assertEquals(1, children.size());
        Assert.assertEquals(SubLayout.class, children.get(0).getClass());
        children = children.get(0).getChildren().collect(Collectors.toList());
        Assert.assertTrue(children.isEmpty());

    }

    private Class<? extends Component> getUIComponent() {
        return ComponentUtil.findParentComponent(ui.getElement().getChild(0))
                .get().getClass();
    }

    private void assertExceptionComponent(String exceptionText) {
        assertExceptionComponent(exceptionText, InternalServerError.class);
    }

    private void assertExceptionComponent(String exceptionText,
            Class<?> errorClass) {
        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();

        Assert.assertTrue("No navigation component visible",
                visibleComponent.isPresent());

        Assert.assertEquals(errorClass, visibleComponent.get().getClass());
        Assert.assertEquals(exceptionText,
                visibleComponent.get().getElement().getText());
    }
}

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

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.RouterTest.CombinedObserverTarget.Enter;
import com.vaadin.flow.router.RouterTest.CombinedObserverTarget.Leave;
import com.vaadin.flow.router.internal.RouteModelTest;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static com.vaadin.flow.router.internal.RouteModelTest.varargs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@NotThreadSafe
public class RouterTest extends RoutingTestBase {

    private static final String DYNAMIC_TITLE = "I am dynamic!";
    public static final String EXCEPTION_WRAPPER_MESSAGE = "There was an exception while trying to navigate to '%s' with the exception message '%s'";

    private UI ui;

    private VaadinService service = Mockito.mock(VaadinService.class);

    private DeploymentConfiguration configuration = Mockito
            .mock(DeploymentConfiguration.class);

    @Route("")
    @Tag(Tag.DIV)
    public static class RootNavigationTarget extends Component
            implements AfterNavigationObserver {

        static List<EventObject> events = new ArrayList<>();

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            events.add(event);
        }
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class AfterNavigationTarget extends Component
            implements AfterNavigationObserver {

        static List<String> events = new ArrayList<>();

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            events.add("AfterNavigation Observer");
        }
    }

    @Route("foo")
    @Tag(Tag.DIV)
    public static class FooNavigationTarget extends Component {

    }

    @Route("foo/bar")
    @Tag(Tag.DIV)
    public static class FooBarNavigationTarget extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add(event);
        }
    }

    @Route("manual")
    @Tag(Tag.DIV)
    public static class ManualNavigationTarget extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {

        private static List<String> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add("Before enter");
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add("Before leave");
        }
    }

    @Route("enteringTarget")
    @Tag(Tag.DIV)
    public static class EnteringNavigationTarget extends Component
            implements BeforeEnterObserver {

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }
    }

    @Route("leavingTarget")
    @Tag(Tag.DIV)
    public static class LeavingNavigationTarget extends Component
            implements BeforeLeaveObserver {

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add(event);
        }
    }

    @Route("combined")
    @Tag(Tag.DIV)
    public static class CombinedObserverTarget extends Component {
        @Tag(Tag.DIV)
        public static class Enter extends Component
                implements BeforeEnterObserver {

            private static List<BeforeEvent> events = new ArrayList<>();

            @Override
            public void beforeEnter(BeforeEnterEvent event) {
                events.add(event);
            }
        }

        @Tag(Tag.DIV)
        public static class Leave extends Component
                implements BeforeLeaveObserver {

            private static List<BeforeEvent> events = new ArrayList<>();

            @Override
            public void beforeLeave(BeforeLeaveEvent event) {
                events.add(event);
            }
        }

        @Tag(Tag.DIV)
        public static class Before extends Component
                implements BeforeEnterObserver, BeforeLeaveObserver {

            private static List<BeforeEvent> events = new ArrayList<>();

            @Override
            public void beforeEnter(BeforeEnterEvent event) {
                events.add(event);
            }

            @Override
            public void beforeLeave(BeforeLeaveEvent event) {
                events.add(event);
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

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            event.rerouteTo(new NavigationStateBuilder(event.getSource())
                    .withTarget(FooBarNavigationTarget.class).build());
        }
    }

    @Route("reroute")
    @Tag(Tag.DIV)
    public static class ReroutingOnLeaveNavigationTarget extends Component
            implements BeforeLeaveObserver {
        private static List<BeforeLeaveEvent> events = new ArrayList<>();

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            // Note possible loop problem with redirecting on beforeLeave!
            if (events.isEmpty()) {
                events.add(event);
                event.rerouteTo(new NavigationStateBuilder(event.getSource())
                        .withTarget(FooBarNavigationTarget.class).build());
            }
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

        private static List<BeforeEvent> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event,
                @com.vaadin.flow.router.OptionalParameter String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("optional")
    @Tag(Tag.DIV)
    public static class WithoutOptionalParameter extends Component
            implements HasUrlParameter<String> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    @Route("usupported/wildcard")
    @Tag(Tag.DIV)
    public static class UnsupportedWildParameter extends Component
            implements HasUrlParameter<Integer> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static Integer param;

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter Integer parameter) {
            events.add(event);
            param = parameter;
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

    @Route("wild")
    @Tag(Tag.DIV)
    public static class WildHasParameter extends Component
            implements HasUrlParameter<String> {

        private static List<BeforeEvent> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
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
        public Router getRouter() {
            return router;
        }

    }

    @Route("navigationEvents")
    @Tag(Tag.DIV)
    public static class NavigationEvents extends Component {

        private static List<EventObject> events = new ArrayList<>();

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
            NavigationEvents.events.add(event);
        }
    }

    @Tag(Tag.DIV)
    private static class BeforeNavigation extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            NavigationEvents.events.add(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            NavigationEvents.events.add(event);
        }
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

    @Route(value = "after-navigation-child", layout = RouteParent.class)
    @Tag(Tag.DIV)
    public static class AfterNavigationChild extends Component
            implements AfterNavigationObserver {

        static List<EventObject> events = new ArrayList<>();

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            events.add(event);
        }
    }

    @Route(value = "after-navigation-within-same-parent", layout = RouteParent.class)
    @Tag(Tag.DIV)
    public static class AfterNavigationWithinSameParent extends Component
            implements AfterNavigationObserver {

        static List<EventObject> events = new ArrayList<>();

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
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

    @Route(value = "childWithParameter", layout = RouteParent.class)
    @Tag(Tag.DIV)
    public static class RouteChildWithParameter extends Component implements
            BeforeLeaveObserver, BeforeEnterObserver, HasUrlParameter<String> {

        static List<EventObject> events = new ArrayList<>();
        static List<String> parameters = new ArrayList<>();

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            parameters.add(parameter);
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add(event);
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

    @Route("")
    @Tag(Tag.DIV)
    public static class RootParameter extends Component
            implements HasUrlParameter<String> {

        private static List<EventObject> events = new ArrayList<>();

        private static String param;

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            events.add(event);
            param = parameter;
        }
    }

    public static class ErrorTarget extends RouteNotFoundError
            implements BeforeEnterObserver {

        private static List<EventObject> events = new ArrayList<>();

        private static String message;

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            message = ((Html) getChildren().findFirst().get()).getInnerHtml();
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

    @Tag(Tag.DIV)
    public static class FileNotFound extends Component
            implements HasErrorParameter<NotFoundException> {
        private static NavigationTrigger trigger;

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            trigger = event.getTrigger();
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @Tag(Tag.DIV)
    public static class FailingErrorHandler extends Component
            implements HasErrorParameter<RuntimeException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<RuntimeException> parameter) {
            throw new RuntimeException(parameter.getException());
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

    @Route("forwardAndReroute/exception")
    @Tag(Tag.DIV)
    public static class ForwardingAndReroutingNavigationTarget extends Component
            implements BeforeEnterObserver {

        private static List<BeforeEvent> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            event.forwardTo(new NavigationStateBuilder(event.getSource())
                    .withTarget(FooBarNavigationTarget.class).build());

            event.rerouteTo(new NavigationStateBuilder(event.getSource())
                    .withTarget(FooBarNavigationTarget.class).build());
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

        private String message;

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

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IllegalArgumentException> parameter) {
            events.add(event);
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
    public static class LoopByUINavigate extends Component
            implements BeforeEnterObserver {

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            UI.getCurrent().navigate("loop");
        }
    }

    @Route("loop")
    @Tag(Tag.DIV)
    public static class LoopOnRouterNavigate extends Component
            implements BeforeEnterObserver {
        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            UI ui = UI.getCurrent();
            ui.getRouter().navigate(ui, new Location("loop"),
                    NavigationTrigger.PROGRAMMATIC);
        }
    }

    @Route("redirect/loop")
    @Tag(Tag.DIV)
    public static class RedirectToLoopByReroute extends Component
            implements BeforeEnterObserver {

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
            UI.getCurrent().navigate("loop");
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingForeverNavigationTarget extends Component
            implements BeforeLeaveObserver {

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.postpone();
            events.add(event);
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingAndResumingNavigationTarget extends Component
            implements BeforeLeaveObserver {

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            ContinueNavigationAction action = event.postpone();
            events.add(event);
            action.proceed();
        }

    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingFirstTimeNavigationTarget extends Component
            implements BeforeLeaveObserver {

        private int counter = 0;

        private static List<BeforeLeaveEvent> events = new ArrayList<>();

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            counter++;
            if (counter < 2) {
                event.postpone();
            }
            events.add(event);
        }
    }

    @Tag(Tag.DIV)
    public static class ChildListener extends Component
            implements BeforeEnterObserver, BeforeLeaveObserver {

        private static List<EventObject> events = new ArrayList<>();

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            events.add(event);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            events.add(event);
        }
    }

    @Route("postpone")
    @Tag(Tag.DIV)
    public static class PostponingAndResumingCompoundNavigationTarget
            extends Component implements BeforeLeaveObserver {

        private static List<BeforeLeaveEvent> events = new ArrayList<>();
        private static ContinueNavigationAction postpone;

        public PostponingAndResumingCompoundNavigationTarget() {
            getElement().appendChild(new ChildListener().getElement());
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            postpone = event.postpone();
            events.add(event);
        }
    }

    @Route("foo")
    @Tag(Tag.DIV)
    public static class ProceedRightAfterPospone extends Component
            implements BeforeLeaveObserver {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.postpone().proceed();
        }

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

        private static List<LocaleChangeEvent> events = new ArrayList<>();

        @Override
        public void localeChange(LocaleChangeEvent event) {
            events.add(event);
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

    @Tag(Tag.DIV)
    public static abstract class AbstractMain extends Component {
    }

    @Route("")
    @Tag(Tag.DIV)
    public static class ExtendingView extends AbstractMain {
    }

    @Route
    @Tag(Tag.DIV)
    public static class Main extends Component {
    }

    @Route
    @Tag(Tag.DIV)
    public static class MainView extends Component {
    }

    @Route
    @Tag(Tag.DIV)
    public static class NamingConvention extends Component {
    }

    @Route
    @Tag(Tag.DIV)
    public static class NamingConventionView extends Component {
    }

    @Route
    @Tag(Tag.DIV)
    public static class View extends Component {
    }

    @Route(value = "1", layout = NoRemoveLayout.class)
    @Tag(Tag.DIV)
    public static class NoRemoveContent1 extends Component {

    }

    @Route(value = "2", layout = NoRemoveLayout.class)
    @Tag(Tag.DIV)
    public static class NoRemoveContent2 extends Component {

    }

    @Tag(Tag.DIV)
    public static class NoRemoveLayout extends Component
            implements RouterLayout {
        @Override
        public void removeRouterLayoutContent(HasElement oldContent) {
            // Do nothing
        }
    }

    /**
     * This class is used as a based for some navigation chains. It will log
     * into the static lists <code>init</code>, <code>beforeLeave</code>,
     * <code>beforeEnter</code> and <code>afterNavigation</code> the respective
     * events in the order they are triggered, where <code>init</code> is the
     * constructor. The value logged is the <code>id</code> field of the class
     * which by default is the class name.
     */
    @Tag(Tag.DIV)
    public static class ProcessEventsBase extends Component
            implements BeforeLeaveObserver, BeforeEnterObserver,
            AfterNavigationObserver, HasComponents {

        static List<String> init = new ArrayList<>();

        static List<String> beforeLeave = new ArrayList<>();

        static List<String> beforeEnter = new ArrayList<>();

        static List<String> afterNavigation = new ArrayList<>();

        static void clear() {
            init.clear();
            beforeLeave.clear();
            beforeEnter.clear();
            afterNavigation.clear();
        }

        private String id;

        public ProcessEventsBase() {
            this(null);
        }

        public ProcessEventsBase(String id) {
            this.id = id != null ? id : getClass().getSimpleName();
            init.add(this.id);
        }

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            beforeLeave.add(id);
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            beforeEnter.add(id);
        }

        public void setParameter(BeforeEvent event, String parameter) {
            beforeEnter.add(parameter);
        }

        @Override
        public void afterNavigation(AfterNavigationEvent event) {
            afterNavigation.add(id);
        }

    }

    /**
     * This is the root layout of the navigation chain. It also adds some
     * children components used in the assertion of the event order, as being
     * children of the layout in the chain instead of being part of the layout
     * chain itself.
     * 
     * So any children of an instance of this class should receive the
     * navigation events right after the instance of this class receives them
     * and in the order they are added.
     */
    public static class ProcessEventsRoot extends ProcessEventsBase
            implements RouterLayout {

        public ProcessEventsRoot() {
            ProcessEventsBase child1 = new ProcessEventsBase("rootChild1");
            child1.add(new ProcessEventsBase("rootChild11"));

            add(child1);
            add(new ProcessEventsBase("rootChild2"));

        }
    }

    /**
     * Just a navigation chain layout.
     */
    @ParentLayout(ProcessEventsRoot.class)
    public static class ProcessEventsTrunk extends ProcessEventsBase
            implements RouterLayout {

    }

    /**
     * Just another layout in the navigation chain. See
     * {@link ProcessEventsRoot} for more details.
     */
    @ParentLayout(ProcessEventsTrunk.class)
    public static class ProcessEventsBranch extends ProcessEventsBase
            implements RouterLayout {

        public ProcessEventsBranch() {
            add(new ProcessEventsBase("branchChild1"));

            ProcessEventsBase child1 = new ProcessEventsBase("branchChild2");
            add(child1);

            child1.add(new ProcessEventsBase("branchChild21"));
        }
    }

    /**
     * Simple navigation target.
     */
    @Route(value = "event/flower", layout = ProcessEventsBranch.class)
    public static class ProcessEventsFlower extends ProcessEventsBase {

    }

    /**
     * Simple navigation target with preserve on refresh.
     */
    @Route(value = "event/fruit", layout = ProcessEventsBranch.class)
    @PreserveOnRefresh
    public static class ProcessEventsFruit extends ProcessEventsBase {

    }

    /**
     * Navigation target using one parameter. We want to assert whether the
     * <code>setParameter</code> is triggered right before
     * <code>beforeEvent</code> does.
     */
    @Route(value = "event/leaf", layout = ProcessEventsBranch.class)
    public static class ProcessEventsLeaf extends ProcessEventsBase
            implements HasUrlParameter<String> {

        public ProcessEventsLeaf() {
            // This child should get the last beforeEvent, after setParameter
            // and this instance's beforeEvent.
            add(new ProcessEventsBase("leafChild"));
        }

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            super.setParameter(event, parameter);
        }
    }

    /**
     * Navigation target using one parameter. We want to assert whether
     * <code>setParameter</code> is triggered before this component's child,
     * considering it doesn't observe the event.
     */
    @Route(value = "event/needle", layout = ProcessEventsBranch.class)
    @Tag(Tag.DIV)
    public static class ProcessEventsNeedle extends Component
            implements HasComponents, HasUrlParameter<String> {

        public ProcessEventsNeedle() {
            ProcessEventsBase.init.add(getClass().getSimpleName());

            // This child should get the last beforeEvent, after setParameter
            // and this instance's beforeEvent.
            add(new ProcessEventsBase("needleChild"));
        }

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            ProcessEventsBase.beforeEnter.add(parameter);
        }
    }

    /**
     * A navigation layout used to redirect. This is used to assert that any
     * following layouts and the navigation target won't be created when a
     * redirect happens.
     */
    @ParentLayout(ProcessEventsTrunk.class)
    public static class ProcessEventsRotten extends ProcessEventsBase
            implements RouterLayout {

        public ProcessEventsRotten() {
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            super.beforeEnter(event);

            event.rerouteTo("event/flower");
        }
    }

    /**
     * Just a navigation chain layout.
     */
    @ParentLayout(ProcessEventsRotten.class)
    public static class ProcessEventsStick extends ProcessEventsBase
            implements RouterLayout {

        public ProcessEventsStick() {
        }
    }

    /**
     * Navigating to this target will reroute from
     * <code>ProcessEventsRotten</code> which is a class on the parent layout
     * chain. So this class shouldn't even be initialized when navigating to
     * it.
     */
    @Route(value = "event/twig", layout = ProcessEventsStick.class)
    public static class ProcessEventsTwig extends ProcessEventsBase {

    }

    /**
     * Parent layout used to reroute to login when not logged in.
     */
    public static class SecurityParent extends ProcessEventsBase
            implements RouterLayout {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            super.beforeLeave(event);

            // Only testing beforeLeave that same target redirect is not
            // processed.
            event.forwardTo("security/login");
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            super.beforeEnter(event);

            event.rerouteTo("security/login");
        }

    }

    @Route(value = "security/login", layout = SecurityParent.class)
    public static class SecurityLogin extends ProcessEventsBase  {

    }

    @Route(value = "security/document", layout = SecurityParent.class)
    public static class SecurityDocument extends ProcessEventsBase {

    }

    @Tag(Tag.DIV)
    public static class UrlParametersBase extends Component
            implements BeforeEnterObserver {

        static Map<String, UrlParameters> parametersLog = new HashMap<>();

        static UrlParameters parameters;

        static void clear() {
            parametersLog.clear();
            parameters = null;
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            parametersLog.put(event.getLocation().getPath(), event.getUrlParameters());
            parameters = event.getUrlParameters();
        }
    }

    @RoutePrefix(":parentID")
    public static class ParentWithParameter extends UrlParametersBase
            implements RouterLayout {

    }

    @Route(value = "", layout = ParentWithParameter.class)
    @RoutePrefix("link/:chainLinkID")
    @ParentLayout(ParentWithParameter.class)
    public static class ChainLinkWithParameter extends UrlParametersBase
            implements RouterLayout {

    }

    @Route(value = "target/:targetChainLinkID/bar", layout = ChainLinkWithParameter.class)
    public static class TargetWithParameter extends UrlParametersBase {

    }

    @Route(value = "[:optional]/[:anotherOptional]", layout = ChainLinkWithParameter.class)
    public static class TargetWithOptionalParameters extends UrlParametersBase {

    }

    @Route(value = ":targetChainLinkID", layout = ParentWithParameter.class)
    @RoutePrefix("targetLink/[:chainLinkID]/chainLink")
    @ParentLayout(ParentWithParameter.class)
    public static class ChainLinkWithParameterAndTarget
            extends UrlParametersBase implements RouterLayout {

    }

    @Route(value = ":anotherTargetID/:yetAnotherID/foo/...:varargsFoo", layout = ChainLinkWithParameterAndTarget.class)
    public static class AnotherTargetWithParameter extends UrlParametersBase {

    }

    @Route(":threadID:int/:messageID:int")
    @RouteAlias(":threadID:int/last")
    @RouteAlias(":threadID:int/[:something]")
    @RoutePrefix("forum/thread")
    public static class ParametersForumThreadView extends UrlParametersBase
            implements RouterLayout {

    }

    @Route(":alias:framework|platform|vaadin-spring|vaadin-spring-boot/[:version:v?\\d.*]/...:path")
    @RouteAlias(":groupId:\\w[\\w\\d]+\\.[\\w\\d\\-\\.]+/:artifactId/[:version:v?\\d.*]/...:path")
    @RouteAlias("...:path")
    @RoutePrefix("api")
    public static class ParametersApiView extends UrlParametersBase
            implements RouterLayout {

    }

    @Route(":urlIdentifier/[:versionIdentifier:v?\\d.*]/[:tabIdentifier:api]/...:apiPath")
    @RouteAlias(":urlIdentifier/[:versionIdentifier:v?\\d.*]/[:tabIdentifier:overview|samples|links|reviews|discussions]")
    @RoutePrefix("directory/component")
    public static class DetailsView extends UrlParametersBase
            implements RouterLayout {

    }

    @Override
    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        super.init();
        ui = new RouterTestUI(router);
        ui.getSession().lock();
        ui.getSession().setConfiguration(configuration);

        VaadinService.setCurrent(service);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        Mockito.when(service.getRouter()).thenReturn(router);

        Mockito.when(configuration.isProductionMode()).thenReturn(true);
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void basic_navigation() throws InvalidRouteConfigurationException {
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

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
    public void resolveNavigation_pathContainsDots_dotSegmentIsNotParentReference_noException() {
        router.resolveNavigationTarget("/.../dsfsdfsdf",
                Collections.emptyMap());
        // doesn't throw
    }

    @Test
    public void resolveNavigation_pathContainsDots_pathIsRelative_noException() {
        router.resolveNavigationTarget("/../dsfsdfsdf", Collections.emptyMap());
        // doesn't throw
    }

    @Test
    public void page_title_set_from_annotation()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(NavigationTargetWithTitle.class);
        router.navigate(ui, new Location("navigation-target-with-title"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Custom Title", ui.getInternals().getTitle());
    }

    @Test
    public void page_title_not_set_from_annotation_in_parent()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(ChildWithoutTitle.class);

        router.navigate(ui, new Location("parent-with-title/child"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("", ui.getInternals().getTitle());
    }

    @Test
    public void page_title_set_dynamically()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(NavigationTargetWithDynamicTitle.class);

        router.navigate(ui,
                new Location("navigation-target-with-dynamic-title"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is(DYNAMIC_TITLE));
    }

    @Test
    public void page_title_set_dynamically_from_url_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(NavigationTargetWithDynamicTitleFromUrl.class);

        router.navigate(ui, new Location("url/hello"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is("hello"));
    }

    @Test
    public void page_title_set_dynamically_from_event_handler()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(
                NavigationTargetWithDynamicTitleFromNavigation.class);

        router.navigate(ui, new Location("url"),
                NavigationTrigger.PROGRAMMATIC);

        assertThat("Dynamic title is wrong", ui.getInternals().getTitle(),
                is("ACTIVATING"));
    }

    @Test
    public void before_navigation_event_is_triggered()
            throws InvalidRouteConfigurationException {
        FooBarNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                FooBarNavigationTarget.events.size());
        Assert.assertEquals("Unexpected event type", BeforeEnterEvent.class,
                FooBarNavigationTarget.events.get(0).getClass());
    }

    @Test
    public void leave_and_enter_listeners_only_receive_correct_state()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LeavingNavigationTarget.class,
                EnteringNavigationTarget.class, RootNavigationTarget.class);

        router.navigate(ui, new Location("enteringTarget"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeEnterObserver should have fired.", 1,
                EnteringNavigationTarget.events.size());

        router.navigate(ui, new Location("leavingTarget"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("No leave or enter target should have fired.", 1,
                EnteringNavigationTarget.events.size());

        Assert.assertEquals("No leave or enter target should have fired.", 0,
                LeavingNavigationTarget.events.size());

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeLeaveObserver should have fired", 1,
                LeavingNavigationTarget.events.size());
    }

    @Test
    public void leave_navigate_and_enter_listeners_execute_in_correct_order()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(CombinedObserverTarget.class,
                RootNavigationTarget.class);

        // Observer execution order should be BeforeNavigation before
        // EnterListener, but BeforeLeave before BeforeNavigation
        router.navigate(ui, new Location("combined"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeEnterObserver should have fired.", 1,
                Enter.events.size());

        Assert.assertEquals("BeforeNavigationObserver should have fired.", 1,
                CombinedObserverTarget.Before.events.size());

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeLeaveObserver target should have fired.", 1,
                Leave.events.size());

        Assert.assertEquals(
                "BeforeNavigationObserver target should have fired.", 2,
                CombinedObserverTarget.Before.events.size());

        Assert.assertEquals("LeaveListener got event", BeforeLeaveEvent.class,
                CombinedObserverTarget.Before.events.get(1).getClass());
    }

    @Test
    public void before_navigation_event_is_triggered_for_attach_and_detach()
            throws InvalidRouteConfigurationException {
        FooBarNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                FooBarNavigationTarget.events.size());
        Assert.assertEquals(BeforeEnterEvent.class,
                FooBarNavigationTarget.events.get(0).getClass());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 2,
                FooBarNavigationTarget.events.size());
        Assert.assertEquals(BeforeLeaveEvent.class,
                FooBarNavigationTarget.events.get(1).getClass());
    }

    @Test
    public void reroute_on_before_navigation_event()
            throws InvalidRouteConfigurationException {
        FooBarNavigationTarget.events.clear();
        ReroutingNavigationTarget.events.clear();
        RootNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                ReroutingNavigationTarget.class, FooBarNavigationTarget.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Map<String, String> params = new HashMap<>();
        params.put("foo", "bar");
        QueryParameters queryParameters = QueryParameters.simple(params);

        router.navigate(ui, new Location("reroute", queryParameters),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                ReroutingNavigationTarget.events.size());

        Assert.assertEquals("Expected event amount was wrong", 1,
                FooBarNavigationTarget.events.size());

        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());

        Assert.assertEquals(BeforeEnterEvent.class,
                ReroutingNavigationTarget.events.get(0).getClass());
        Assert.assertEquals(BeforeEnterEvent.class,
                FooBarNavigationTarget.events.get(0).getClass());

        QueryParameters rerouteQueryParameters = FooBarNavigationTarget.events
                .get(0).getLocation().getQueryParameters();
        Assert.assertNotNull(rerouteQueryParameters);

        List<String> foo = rerouteQueryParameters.getParameters().get("foo");
        Assert.assertNotNull(foo);
        Assert.assertFalse(foo.isEmpty());
        Assert.assertEquals(foo.get(0), "bar");
    }

    @Test
    public void before_and_after_event_fired_in_correct_order()
            throws InvalidRouteConfigurationException {
        NavigationEvents.events.clear();
        setNavigationTargets(NavigationEvents.class);

        router.navigate(ui, new Location("navigationEvents"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                NavigationEvents.events.size());

        Assert.assertEquals("Before navigation event was wrong.",
                BeforeEnterEvent.class,
                NavigationEvents.events.get(0).getClass());

        Assert.assertEquals("After navigation event was wrong.",
                AfterNavigationEvent.class,
                NavigationEvents.events.get(1).getClass());
    }

    @Test
    public void after_event_not_fired_on_detach()
            throws InvalidRouteConfigurationException {
        NavigationEvents.events.clear();
        setNavigationTargets(NavigationEvents.class, FooNavigationTarget.class);

        router.navigate(ui, new Location("navigationEvents"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                NavigationEvents.events.size());
        Assert.assertEquals("Before navigation event was wrong.",
                BeforeEnterEvent.class,
                NavigationEvents.events.get(0).getClass());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 3,
                NavigationEvents.events.size());
        Assert.assertEquals("After navigation event was wrong.",
                BeforeLeaveEvent.class,
                NavigationEvents.events.get(2).getClass());
    }

    @Test
    public void reroute_with_url_parameter()
            throws InvalidRouteConfigurationException {
        RouteWithParameter.events.clear();
        setNavigationTargets(GreetingNavigationTarget.class,
                RouteWithParameter.class, RerouteToRouteWithParam.class);

        router.navigate(ui, new Location("redirect/to/param"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                RouteWithParameter.events.size());
        Assert.assertEquals("Before navigation event was wrong.", "hello",
                RouteWithParameter.param);
    }

    @Test
    public void reroute_fails_with_no_url_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                ParameterRouteNoParameter.class, RerouteToRouteWithParam.class);
        String locationString = "redirect/to/param";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "No route 'param' accepting the parameters [hello] was found.";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_fails_with_faulty_url_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                RouteWithParameter.class, FailRerouteWithParam.class);

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
        setNavigationTargets(GreetingNavigationTarget.class,
                RouteWithMultipleParameters.class,
                RerouteToRouteWithMultipleParams.class);

        router.navigate(ui, new Location("redirect/to/params"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                RouteWithMultipleParameters.events.size());
        Assert.assertEquals("Before navigation event was wrong.",
                "this/must/work", RouteWithMultipleParameters.param);
    }

    @Test
    public void reroute_fails_with_faulty_url_parameters()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                RouteWithMultipleParameters.class, FailRerouteWithParams.class);
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
        setNavigationTargets(GreetingNavigationTarget.class,
                ParameterRouteNoParameter.class,
                RerouteToRouteWithMultipleParams.class);
        String locationString = "redirect/to/params";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "No route 'param' accepting the parameters [this, must, work] was found.";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void reroute_with_multiple_url_parameters_fails_to_single_parameter_target()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(GreetingNavigationTarget.class,
                RouteWithParameter.class,
                RerouteToRouteWithMultipleParams.class);
        String locationString = "redirect/to/params";

        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Routing with mismatching parameters should have failed -",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
        String message = "No route 'param' accepting the parameters [this, must, work] was found.";
        String exceptionText = String.format(EXCEPTION_WRAPPER_MESSAGE,
                locationString, message);
        assertExceptionComponent(exceptionText);
    }

    @Test
    public void route_precedence_when_one_has_parameter()
            throws InvalidRouteConfigurationException {
        RouteWithParameter.events.clear();
        setNavigationTargets(RouteWithParameter.class, StaticParameter.class);

        router.navigate(ui, new Location("param/param"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(RouteWithParameter.class, getUIComponent());

        // Expectation of 2 events is due to parameter and BeforeNavigation
        Assert.assertEquals("Expected event amount was wrong", 2,
                RouteWithParameter.events.size());
        Assert.assertEquals("Before navigation event was wrong.", "param",
                RouteWithParameter.param);

        router.navigate(ui, new Location("param/static"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals(
                "Did not get correct class even though StaticParameter should have precedence over RouteWithParameter due to exact url match.",
                StaticParameter.class, getUIComponent());
    }

    @Test
    public void optional_parameter_gets_parameter()
            throws InvalidRouteConfigurationException {
        OptionalParameter.events.clear();
        setNavigationTargets(OptionalParameter.class);

        router.navigate(ui, new Location("optional/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                OptionalParameter.events.size());
        Assert.assertEquals("Before navigation event was wrong.", "parameter",
                OptionalParameter.param);
    }

    @Test
    public void optional_parameter_matches_no_parameter()
            throws InvalidRouteConfigurationException {
        OptionalParameter.events.clear();
        OptionalParameter.param = null;
        setNavigationTargets(OptionalParameter.class);

        router.navigate(ui, new Location("optional"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                OptionalParameter.events.size());
        Assert.assertNull("Before navigation event was wrong.",
                OptionalParameter.param);
    }

    @Test
    public void correctly_return_route_with_one_base_route_with_optionals()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithParameter.class,
                ParameterRouteNoParameter.class);

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

        setNavigationTargets(OptionalParameter.class,
                OptionalNoParameter.class);

    }

    @Test
    public void navigateToRoot_errorCode_dontRedirect()
            throws InvalidRouteConfigurationException {

        setNavigationTargets(FooNavigationTarget.class);

        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, router.navigate(
                ui, new Location(""), NavigationTrigger.PROGRAMMATIC));
    }

    @Test
    public void navigating_to_route_with_wildcard_parameter()
            throws InvalidRouteConfigurationException {
        WildParameter.events.clear();
        WildParameter.param = null;
        setNavigationTargets(WildParameter.class);

        router.navigate(ui, new Location("wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                WildParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "",
                WildParameter.param);

        router.navigate(ui, new Location("wild/single"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 2,
                WildParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "single",
                WildParameter.param);

        router.navigate(ui, new Location("wild/multi/part/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 3,
                WildParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "multi/part/parameter",
                WildParameter.param);
    }

    @Test
    public void route_with_wildcard_parameter_should_be_last_hit()
            throws InvalidRouteConfigurationException {
        WildParameter.events.clear();
        WildParameter.param = null;
        setNavigationTargets(WildParameter.class, WildHasParameter.class,
                WildNormal.class);

        router.navigate(ui, new Location("wild"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 0,
                WildHasParameter.events.size());

        router.navigate(ui, new Location("wild/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                WildHasParameter.events.size());
        Assert.assertEquals("Parameter didn't match expected value",
                "parameter", WildHasParameter.param);

        router.navigate(ui, new Location("wild/multi/part/parameter"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                WildParameter.events.size());
        Assert.assertEquals("Parameter didn't match expected value",
                "multi/part/parameter", WildParameter.param);

    }

    @Test
    public void root_navigation_target_with_required_parameter()
            throws InvalidRouteConfigurationException {
        RootParameter.events.clear();
        setNavigationTargets(RootParameter.class);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "Has url with required parameter should not match to \"\"", 0,
                RootParameter.events.size());
    }

    @Test
    public void reroute_on_hasParameter_step()
            throws InvalidRouteConfigurationException {
        RootParameter.events.clear();
        setNavigationTargets(RootParameter.class, RedirectOnSetParam.class);

        router.navigate(ui, new Location("param/reroute/hello"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                RootParameter.events.size());
        Assert.assertEquals("Parameter should be empty", "hello",
                RootParameter.param);
    }

    @Test
    public void has_url_with_supported_parameters_navigation()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(IntegerParameter.class, LongParameter.class,
                BooleanParameter.class);

        router.navigate(ui, new Location("integer/5"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                IntegerParameter.events.size());
        Assert.assertEquals("Parameter should be empty", 5,
                IntegerParameter.param.intValue());

        router.navigate(ui, new Location("long/5"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                LongParameter.events.size());
        Assert.assertEquals("Parameter should be empty", 5,
                LongParameter.param.longValue());

        router.navigate(ui, new Location("boolean/true"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Expected event amount was wrong", 1,
                BooleanParameter.events.size());
        Assert.assertEquals("Parameter should be empty", true,
                BooleanParameter.param);
    }

    @Test
    public void default_wildcard_support_only_for_string()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(UnsupportedWildParameter.class);

        String locationString = "usupported/wildcard/3/4/1";
        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        String message = String.format(
                "Invalid wildcard parameter in class %s. Only String is supported for wildcard parameters.",
                UnsupportedWildParameter.class.getName());
        String exceptionText1 = String.format("Could not navigate to '%s'",
                locationString);
        String exceptionText2 = String.format(
                "Reason: Failed to parse url parameter, exception: %s",
                new UnsupportedOperationException(message));

        assertExceptionComponent(RouteNotFoundError.class, exceptionText1,
                exceptionText2);
    }

    @Test
    public void unparsable_url_parameter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LongParameter.class);

        String locationString = "long/unsupportedParam";
        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        String exceptionText1 = String.format("Could not navigate to '%s'",
                locationString);
        String exceptionText2 = String.format(
                "Reason: No navigation target found for path '%s'.",
                locationString);

        assertExceptionComponent(RouteNotFoundError.class, exceptionText1,
                exceptionText2);
    }

    @Test
    public void redirect_to_routeNotFound_error_view_when_no_route_found()
            throws InvalidRouteConfigurationException {
        ErrorTarget.events.clear();
        setNavigationTargets(FooNavigationTarget.class);
        setErrorNavigationTargets(ErrorTarget.class);

        String locationString = "error";
        int result = router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals("Expected event amount was wrong", 1,
                ErrorTarget.events.size());

        String errorMessage = ErrorTarget.message;
        Assert.assertTrue(errorMessage.contains(
                String.format("Could not navigate to '%s'", locationString)));
        Assert.assertTrue(errorMessage.contains(
                String.format("No navigation target found for path '%s'", locationString)));
    }

    @Test
    public void exception_during_navigation_is_caught_and_show_in_internalServerError()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(FailOnException.class);

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
    }

    @Test
    public void fail_for_multiple_of_the_same_class()
            throws InvalidRouteConfigurationException {
        setErrorNavigationTargets(ErrorTarget.class, RouteNotFoundError.class);

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

        setErrorNavigationTargets(NonExtendingNotFoundTarget.class,
                DuplicateNotFoundTarget.class);
    }

    @Test
    public void custom_exception_target_should_override_default_ones() {
        setErrorNavigationTargets(NonExtendingNotFoundTarget.class,
                RouteNotFoundError.class);

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                NonExtendingNotFoundTarget.class, getUIComponent());

        assertExceptionComponent(NonExtendingNotFoundTarget.class,
                EXCEPTION_TEXT);
    }

    @Test
    public void custom_exception_target_is_used() {
        setErrorNavigationTargets(CustomNotFoundTarget.class,
                RouteNotFoundError.class);

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(
                "Expected the extending class to be used instead of the super class",
                CustomNotFoundTarget.class, getUIComponent());

        assertExceptionComponent(CustomNotFoundTarget.class, EXCEPTION_TEXT);
    }

    @Test
    public void error_target_has_parent_layout()
            throws InvalidRouteConfigurationException {
        // Needed for the router link in the parent used by the error views
        setNavigationTargets(LoneRoute.class);
        setErrorNavigationTargets(ErrorTargetWithParent.class,
                RouteNotFoundError.class);

        int result = router.navigate(ui, new Location("exception"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Component parenComponent = ComponentUtil
                .findParentComponent(ui.getElement().getChild(0)).get();

        Assert.assertEquals(RouteParent.class, parenComponent.getClass());

        List<Class<?>> childClasses = parenComponent.getChildren()
                .map(Object::getClass).collect(Collectors.toList());
        Assert.assertEquals(
                Arrays.asList(RouterLink.class, ErrorTargetWithParent.class),
                childClasses);
    }

    @Test
    public void reroute_to_error_opens_expected_error_target()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RerouteToError.class);
        setErrorNavigationTargets(IllegalTarget.class);

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
        IllegalTarget.events.clear();
        setNavigationTargets(RerouteToErrorWithMessage.class);
        setErrorNavigationTargets(IllegalTarget.class);

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
                1, IllegalTarget.events.size());
        BeforeEnterEvent event = (BeforeEnterEvent) IllegalTarget.events.get(0);
        Assert.assertEquals("Parameter should be empty",
                "beforeToError/message/CustomMessage",
                event.getLocation().getPath());

    }

    @Test
    public void reroute_to_error_from_has_param()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RedirectToNotFoundInHasParam.class);

        int result = router.navigate(ui, new Location("toNotFound/error"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Target should have rerouted to exception target.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(RouteNotFoundError.class, getUIComponent());
    }

    @Test
    public void forward_and_reroute_at_the_same_time_exception()
            throws InvalidRouteConfigurationException {
        String location = "forwardAndReroute/exception";

        FooBarNavigationTarget.events.clear();
        ForwardingAndReroutingNavigationTarget.events.clear();
        RootNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                ForwardingAndReroutingNavigationTarget.class,
                FooBarNavigationTarget.class);

        router.navigate(ui, new Location(location),
                NavigationTrigger.PROGRAMMATIC);

        String validationMessage = "Error forward & reroute can not be set at the same time";

        String errorMessage = String.format(
                "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                location, validationMessage);

        assertExceptionComponent(InternalServerError.class, errorMessage);
    }

    @Test
    public void faulty_error_response_code_should_throw_exception()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RerouteToError.class);
        setErrorNavigationTargets(FaultyErrorView.class);

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

        assertExceptionComponent(InternalServerError.class, errorMessage);
    }

    @Test
    public void repeatedly_navigating_to_same_ur_through_ui_navigate_should_not_loop()
            throws InvalidRouteConfigurationException {
        LoopByUINavigate.events.clear();
        setNavigationTargets(LoopByUINavigate.class);

        ui.navigate("loop");

        Assert.assertEquals("Expected only one request to loop", 1,
                LoopByUINavigate.events.size());
        Assert.assertNull("Last handled location should have been cleared",
                ui.getInternals().getLastHandledLocation());
    }

    @Test
    public void ui_navigate_should_not_loop()
            throws InvalidRouteConfigurationException {
        LoopByUINavigate.events.clear();
        RedirectToLoopByReroute.events.clear();
        setNavigationTargets(LoopByUINavigate.class,
                RedirectToLoopByReroute.class);

        ui.navigate("redirect/loop");

        Assert.assertEquals("Expected one events", 1,
                LoopByUINavigate.events.size());
        Assert.assertEquals("Expected onve events", 1,
                RedirectToLoopByReroute.events.size());
    }

    @Test
    public void ui_navigate_should_only_have_one_history_marking_on_loop()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LoopByUINavigate.class);

        ui.navigate("loop");

        long historyInvocations = ui.getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .filter(js -> js.getInvocation().getExpression()
                        .startsWith("history.pushState"))
                .count();
        assertEquals(1, historyInvocations);

        Assert.assertNull("Last handled location should have been cleared",
                ui.getInternals().getLastHandledLocation());
    }

    @Test
    public void router_navigate_should_not_loop()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(LoopOnRouterNavigate.class);

        ui.navigate("loop");

        Assert.assertEquals("Expected only one request", 1,
                LoopOnRouterNavigate.events.size());
        Assert.assertNull("Last handled location should have been cleared",
                ui.getInternals().getLastHandledLocation());
    }

    @Test
    public void exception_while_navigating_should_succeed_and_clear_last_handled()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(FailOnException.class);

        ui.navigate("exception");

        Assert.assertNull("Last handled location should have been cleared",
                ui.getInternals().getLastHandledLocation());
    }

    @Test
    public void exception_in_exception_handler_while_navigating_should_clear_last_handled()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(FailOnException.class);
        setErrorNavigationTargets(FailingErrorHandler.class);

        try {
            ui.navigate("exception");
            Assert.fail("No runtime exception was thrown from navigation");
        } catch (Exception re) {
            Assert.assertNull(
                    "Last handled location should have been cleared even though navigation failed",
                    ui.getInternals().getLastHandledLocation());
        }
    }

    @Test
    public void postpone_then_resume_on_before_navigation_event()
            throws InvalidRouteConfigurationException, InterruptedException {
        RootNavigationTarget.events.clear();
        PostponingAndResumingNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                PostponingAndResumingNavigationTarget.class);

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals(PostponingAndResumingNavigationTarget.class,
                getUIComponent());

        Assert.assertEquals("Expected event amount was wrong", 0,
                PostponingAndResumingNavigationTarget.events.size());

        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);

        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());
        Assert.assertEquals(
                "Expected event in the first target amount was wrong", 1,
                PostponingAndResumingNavigationTarget.events.size());
        Assert.assertEquals(
                "Expected event amount in the last target was wrong", 1,
                RootNavigationTarget.events.size());
    }

    @Test
    public void postpone_forever_on_before_navigation_event()
            throws InvalidRouteConfigurationException {
        RootNavigationTarget.events.clear();
        PostponingAndResumingNavigationTarget.events.clear();
        setNavigationTargets(RootNavigationTarget.class,
                PostponingForeverNavigationTarget.class);

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals(PostponingForeverNavigationTarget.class,
                getUIComponent());

        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);

        Assert.assertEquals(PostponingForeverNavigationTarget.class,
                getUIComponent());
        Assert.assertEquals("Expected event amount in the target was wrong", 1,
                PostponingForeverNavigationTarget.events.size());

        Assert.assertEquals("Expected event amount in the root was wrong", 0,
                RootNavigationTarget.events.size());
    }

    @Test
    public void postpone_obsoleted_by_new_navigation_transition()
            throws InvalidRouteConfigurationException, InterruptedException {
        FooBarNavigationTarget.events.clear();
        FooBarNavigationTarget.events.clear();
        setNavigationTargets(FooNavigationTarget.class,
                FooBarNavigationTarget.class,
                PostponingFirstTimeNavigationTarget.class);

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);
        int status2 = router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("Expected event amount was wrong", 1,
                PostponingFirstTimeNavigationTarget.events.size());
        BeforeLeaveEvent event = PostponingFirstTimeNavigationTarget.events
                .get(0);

        int status3 = router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());

        event.postpone().proceed();

        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);
        Assert.assertEquals("Third transition failed",
                HttpServletResponse.SC_OK, status3);

        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());
        Assert.assertEquals("Expected event amount was wrong", 2,
                PostponingFirstTimeNavigationTarget.events.size());

        Assert.assertEquals("Expected event amount was wrong", 1,
                FooBarNavigationTarget.events.size());
    }

    @Test
    public void postpone_then_resume_with_multiple_listeners()
            throws InvalidRouteConfigurationException, InterruptedException {
        setNavigationTargets(RootNavigationTarget.class,
                PostponingAndResumingCompoundNavigationTarget.class);

        int status1 = router.navigate(ui, new Location("postpone"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("First transition failed",
                HttpServletResponse.SC_OK, status1);
        Assert.assertEquals(PostponingAndResumingCompoundNavigationTarget.class,
                getUIComponent());

        int status2 = router.navigate(ui, new Location(""),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Second transition failed",
                HttpServletResponse.SC_OK, status2);

        Assert.assertNotNull(
                PostponingAndResumingCompoundNavigationTarget.postpone);

        PostponingAndResumingCompoundNavigationTarget.postpone.proceed();

        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());
        Assert.assertEquals(1,
                PostponingAndResumingCompoundNavigationTarget.events.size());
        Assert.assertEquals(2, ChildListener.events.size());
        Assert.assertEquals(BeforeEnterEvent.class,
                ChildListener.events.get(0).getClass());
        Assert.assertEquals(BeforeLeaveEvent.class,
                ChildListener.events.get(1).getClass());
    }

    @Test
    public void navigation_should_fire_locale_change_observer()
            throws InvalidRouteConfigurationException {
        Translations.events.clear();
        setNavigationTargets(Translations.class);

        ui.navigate("");

        Assert.assertEquals("Expected event amount was wrong", 1,
                Translations.events.size());
        Assert.assertEquals(Locale.getDefault(),
                Translations.events.get(0).getLocale());
    }

    @Test
    public void away_navigation_should_not_inform_observer()
            throws InvalidRouteConfigurationException, InterruptedException {
        Translations.events.clear();
        setNavigationTargets(FooNavigationTarget.class, Translations.class);

        ui.navigate("");

        Assert.assertEquals("Expected event amount was wrong", 1,
                Translations.events.size());
        Assert.assertEquals(Locale.getDefault(),
                Translations.events.get(0).getLocale());

        ui.navigate("foo");

        Assert.assertEquals("Recorded event amount should have stayed the same",
                1, Translations.events.size());
    }

    @Test // 3424
    public void route_as_parent_layout_handles_as_expected()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(BaseLayout.class, SubLayout.class);

        ui.navigate("base");
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

        ui.navigate("sub");
        Assert.assertEquals(MainLayout.class, getUIComponent());

        children = ui.getChildren().collect(Collectors.toList());
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

    @Test
    public void proceedRightAfterPostpone_navigationIsDone()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(ProceedRightAfterPospone.class,
                RootNavigationTarget.class);

        RootNavigationTarget.events.clear();

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);
        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        // View ProceedRightAfterPospone postpones the navigation and
        // immediately proceed, it means that RootNavigationTarget should be
        // informed about AfterNavigationEvent
        Assert.assertEquals(1, RootNavigationTarget.events.size());
        Assert.assertEquals(AfterNavigationEvent.class,
                RootNavigationTarget.events.get(0).getClass());
    }

    @Test
    public void navigateWithinOneParent_oneLeaveEventOneEnterEvent()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteChild.class, LoneRoute.class);

        router.navigate(ui, new Location("parent/child"),
                NavigationTrigger.PROGRAMMATIC);
        RouteChild.events.clear();
        router.navigate(ui, new Location("single"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(1, RouteChild.events.size());
        Assert.assertEquals(BeforeLeaveEvent.class,
                RouteChild.events.get(0).getClass());

        Assert.assertEquals(1, LoneRoute.events.size());
        Assert.assertEquals(BeforeEnterEvent.class,
                LoneRoute.events.get(0).getClass());
    }

    @Test
    public void navigateWithinOneParent_oneAfterNavigationEventOneEventOnly()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(AfterNavigationChild.class,
                AfterNavigationWithinSameParent.class, LoneRoute.class);

        router.navigate(ui, new Location("parent/after-navigation-child"),
                NavigationTrigger.PROGRAMMATIC);
        AfterNavigationChild.events.clear();
        router.navigate(ui,
                new Location("parent/after-navigation-within-same-parent"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "After navigation event should not be fired for "
                        + AfterNavigationChild.class.getSimpleName(),
                0, AfterNavigationChild.events.size());

        Assert.assertEquals(
                "Only one navigation event should be fired for "
                        + AfterNavigationWithinSameParent.class.getSimpleName(),
                1, AfterNavigationWithinSameParent.events.size());
        Assert.assertEquals(
                "The fired event type should be "
                        + AfterNavigationEvent.class.getSimpleName(),
                AfterNavigationEvent.class,
                AfterNavigationWithinSameParent.events.get(0).getClass());
    }

    @Test // #2754
    public void manually_registered_listeners_should_fire_for_every_navigation()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

        AtomicInteger leaveCount = new AtomicInteger(0);
        AtomicInteger enterCount = new AtomicInteger(0);
        AtomicInteger afterCount = new AtomicInteger(0);

        ui.addBeforeLeaveListener(event -> leaveCount.incrementAndGet());
        ui.addBeforeEnterListener(event -> enterCount.incrementAndGet());
        ui.addAfterNavigationListener(event -> afterCount.incrementAndGet());

        Assert.assertEquals(
                "No event should have happened due to adding listener.", 0,
                leaveCount.get());
        Assert.assertEquals(
                "No event should have happened due to adding listener.", 0,
                enterCount.get());
        Assert.assertEquals(
                "No event should have happened due to adding listener.", 0,
                afterCount.get());

        router.navigate(ui, new Location("foo/bar"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeLeaveListener should have been invoked.", 1,
                leaveCount.get());
        Assert.assertEquals("BeforeEnterListener should have been invoked.", 1,
                enterCount.get());
        Assert.assertEquals("AfterNavigationListener should have been invoked.",
                1, afterCount.get());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("BeforeLeaveListener should have been invoked.", 2,
                leaveCount.get());
        Assert.assertEquals("BeforeEnterListener should have been invoked.", 2,
                enterCount.get());
        Assert.assertEquals("AfterNavigationListener should have been invoked.",
                2, afterCount.get());
    }

    @Test // #2754
    public void after_navigation_listener_is_only_invoked_once_for_redirect()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(ReroutingNavigationTarget.class,
                FooBarNavigationTarget.class);

        AtomicInteger afterCount = new AtomicInteger(0);

        ui.addAfterNavigationListener(event -> afterCount.incrementAndGet());

        router.navigate(ui, new Location("reroute"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "AfterNavigationListener should have been invoked only after redirect.",
                1, afterCount.get());
    }

    @Test // #2754
    public void before_leave_listener_is_invoked_for_each_redirect()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(ReroutingNavigationTarget.class,
                FooBarNavigationTarget.class);

        AtomicInteger leaveCount = new AtomicInteger(0);
        ui.addBeforeLeaveListener(event -> leaveCount.incrementAndGet());

        router.navigate(ui, new Location("reroute"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeLeaveListener should have been invoked for initial navigation and redirect.",
                2, leaveCount.get());
    }

    @Test // #2754
    public void before_enter_listener_is_invoked_for_each_redirect_when_redirecting_on_before_enter()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(ReroutingNavigationTarget.class,
                FooBarNavigationTarget.class);

        AtomicInteger enterCount = new AtomicInteger(0);
        ui.addBeforeEnterListener(event -> enterCount.incrementAndGet());

        router.navigate(ui, new Location("reroute"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeEnterListener should have been invoked for initial navigation and redirect.",
                2, enterCount.get());
    }

    @Test // #2754
    public void before_enter_listener_is_invoked_once_and_before_leave_twice_when_redirecting_on_before_leave()
            throws InvalidRouteConfigurationException {
        ReroutingOnLeaveNavigationTarget.events.clear();

        setNavigationTargets(ReroutingOnLeaveNavigationTarget.class,
                FooBarNavigationTarget.class, FooNavigationTarget.class);

        router.navigate(ui, new Location("reroute"),
                NavigationTrigger.PROGRAMMATIC);

        AtomicInteger leaveCount = new AtomicInteger(0);
        AtomicInteger enterCount = new AtomicInteger(0);
        ui.addBeforeLeaveListener(event -> leaveCount.incrementAndGet());
        ui.addBeforeEnterListener(event -> enterCount.incrementAndGet());

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeLeaveListener should have been invoked for initial navigation and redirect.",
                2, leaveCount.get());
        Assert.assertEquals(
                "BeforeEnterListener should have been invoked for initial navigation and redirect.",
                1, enterCount.get());
    }

    @Test // #2754
    public void manual_before_listeners_are_fired_before_observers()
            throws InvalidRouteConfigurationException {
        ManualNavigationTarget.events.clear();
        setNavigationTargets(ManualNavigationTarget.class,
                FooNavigationTarget.class);

        Registration beforeEnter = ui.addBeforeEnterListener(
                event -> ManualNavigationTarget.events.add("Manual event"));

        router.navigate(ui, new Location("manual"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("not enough events", 2,
                ManualNavigationTarget.events.size());

        Assert.assertEquals("Manual event",
                ManualNavigationTarget.events.get(0));
        Assert.assertEquals("Before enter",
                ManualNavigationTarget.events.get(1));

        // Deactivate before enter and add beforeLeave listener
        beforeEnter.remove();
        ui.addBeforeLeaveListener(
                event -> ManualNavigationTarget.events.add("Manual event"));

        router.navigate(ui, new Location("foo"),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("not enough events", 4,
                ManualNavigationTarget.events.size());

        Assert.assertEquals("Manual event",
                ManualNavigationTarget.events.get(2));
        Assert.assertEquals("Before leave",
                ManualNavigationTarget.events.get(3));
    }

    @Test // #2754
    public void manual_after_listener_is_fired_before_observer()
            throws InvalidRouteConfigurationException {
        AfterNavigationTarget.events.clear();
        setNavigationTargets(AfterNavigationTarget.class);

        ui.addAfterNavigationListener(
                event -> AfterNavigationTarget.events.add("Manual event"));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("not enough events", 2,
                AfterNavigationTarget.events.size());

        Assert.assertEquals("Manual event",
                AfterNavigationTarget.events.get(0));
        Assert.assertEquals("AfterNavigation Observer",
                AfterNavigationTarget.events.get(1));
    }

    @Test // #3616
    public void navigating_with_class_gets_correct_component()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RootNavigationTarget.class,
                FooNavigationTarget.class, FooBarNavigationTarget.class);

        ui.navigate(RootNavigationTarget.class);
        Assert.assertEquals(RootNavigationTarget.class, getUIComponent());

        ui.navigate(FooNavigationTarget.class);
        Assert.assertEquals(FooNavigationTarget.class, getUIComponent());

        ui.navigate(FooBarNavigationTarget.class);
        Assert.assertEquals(FooBarNavigationTarget.class, getUIComponent());
    }

    @Test // #3616
    public void navigating_with_class_and_parameter_gets_correct_component()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(RouteWithParameter.class, BooleanParameter.class,
                WildParameter.class, OptionalParameter.class);

        ui.navigate(RouteWithParameter.class, "Parameter");
        Assert.assertEquals(RouteWithParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", "Parameter",
                RouteWithParameter.param);

        ui.navigate(OptionalParameter.class, "optional");
        Assert.assertEquals(OptionalParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", "optional",
                OptionalParameter.param);
        ui.navigate(OptionalParameter.class);
        Assert.assertEquals(OptionalParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", null,
                OptionalParameter.param);
        ui.navigate(OptionalParameter.class, (String) null);
        Assert.assertEquals(OptionalParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", null,
                OptionalParameter.param);

        ui.navigate(BooleanParameter.class, false);
        Assert.assertEquals(BooleanParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", false,
                BooleanParameter.param);

        ui.navigate(WildParameter.class);
        Assert.assertEquals(WildParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", "",
                WildParameter.param);
        ui.navigate(WildParameter.class, (String) null);
        Assert.assertEquals(WildParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", "",
                WildParameter.param);
        ui.navigate(WildParameter.class, "");
        Assert.assertEquals(WildParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.", "",
                WildParameter.param);
        ui.navigate(WildParameter.class, "my/wild/param");
        Assert.assertEquals(WildParameter.class, getUIComponent());
        Assert.assertEquals("Before navigation event was wrong.",
                "my/wild/param", WildParameter.param);

    }

    @Test // #3988
    public void exception_event_should_keep_original_trigger() {
        setErrorNavigationTargets(FileNotFound.class);

        int result = router.navigate(ui, new Location("programmatic"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("Non existent route should have returned.",
                HttpServletResponse.SC_NOT_FOUND, result);

        Assert.assertEquals(NavigationTrigger.PROGRAMMATIC,
                FileNotFound.trigger);

        router.navigate(ui, new Location("router_link"),
                NavigationTrigger.ROUTER_LINK);

        Assert.assertEquals(NavigationTrigger.ROUTER_LINK,
                FileNotFound.trigger);

        router.navigate(ui, new Location("history"), NavigationTrigger.HISTORY);

        Assert.assertEquals(NavigationTrigger.HISTORY, FileNotFound.trigger);

        router.navigate(ui, new Location("page_load"),
                NavigationTrigger.PAGE_LOAD);

        Assert.assertEquals(NavigationTrigger.PAGE_LOAD, FileNotFound.trigger);
    }

    private String resolve(Class<?> clazz) {
        Route annotation = clazz.getAnnotation(Route.class);
        return RouteUtil.resolve(clazz, annotation);
    }

    @Test
    public void test_router_resolve() {
        Assert.assertEquals("", resolve(Main.class));
        Assert.assertEquals("", resolve(MainView.class));
        Assert.assertEquals("", resolve(View.class));
        Assert.assertEquals("namingconvention",
                resolve(NamingConvention.class));
        Assert.assertEquals("namingconvention",
                resolve(NamingConventionView.class));
    }

    @Test
    public void basic_naming_based_routes()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(NamingConvention.class, Main.class);

        Assert.assertEquals(Main.class,
                router.resolveNavigationTarget("/", Collections.emptyMap())
                        .get().getNavigationTarget());

        Assert.assertEquals(
                NamingConvention.class, router
                        .resolveNavigationTarget("/namingconvention",
                                Collections.emptyMap())
                        .get().getNavigationTarget());
    }

    @Test
    public void basic_naming_based_routes_with_trailing_view()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(NamingConventionView.class, MainView.class);

        Assert.assertEquals(MainView.class,
                router.resolveNavigationTarget("/", Collections.emptyMap())
                        .get().getNavigationTarget());

        Assert.assertEquals(
                NamingConventionView.class, router
                        .resolveNavigationTarget("/namingconvention",
                                Collections.emptyMap())
                        .get().getNavigationTarget());
    }

    @Test
    public void test_naming_based_routes_with_name_view()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(View.class);

        Assert.assertEquals(View.class,
                router.resolveNavigationTarget("/", Collections.emptyMap())
                        .get().getNavigationTarget());
    }

    @Tag("div")
    @Route("noParent")
    @RouteAlias(value = "twoParents", layout = BaseLayout.class)
    public static class AliasLayout extends Component {

    }

    @Test
    public void alias_has_two_parents_even_if_route_doesnt() {
        RouteConfiguration.forRegistry(router.getRegistry())
                .setAnnotatedRoute(AliasLayout.class);

        List<Class<? extends RouterLayout>> parents = router.getRegistry()
                .getRouteLayouts("noParent", AliasLayout.class);

        Assert.assertTrue("Main route should have no parents.",
                parents.isEmpty());

        parents = router.getRegistry().getRouteLayouts("twoParents",
                AliasLayout.class);

        Assert.assertEquals("Route alias should have two parents", 2,
                parents.size());
    }

    @Test
    public void verify_collisions_not_allowed_with_naming_convention() {
        InvalidRouteConfigurationException exception = null;
        try {
            setNavigationTargets(NamingConvention.class,
                    NamingConventionView.class);
        } catch (InvalidRouteConfigurationException e) {
            exception = e;
        }
        Assert.assertNotNull(
                "Routes with same navigation target should not be allowed",
                exception);
    }

    @Test
    public void preserve_initial_ui_contents()
            throws InvalidRouteConfigurationException {
        setNavigationTargets(View.class);

        Element specialChild = new Element("div");
        ui.getElement().appendChild(specialChild);

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(ui.getElement(), specialChild.getParent());
    }

    @Test
    public void noRemoveLayout_oldContentRetained() {
        setNavigationTargets(NoRemoveContent1.class, NoRemoveContent2.class);

        ui.navigate(NoRemoveContent1.class);
        NoRemoveLayout layout = (NoRemoveLayout) ui.getChildren().findFirst()
                .get();

        Assert.assertEquals(Arrays.asList(NoRemoveContent1.class),
                layout.getChildren().map(Component::getClass)
                        .collect(Collectors.toList()));

        ui.navigate(NoRemoveContent2.class);

        Assert.assertEquals(
                Arrays.asList(NoRemoveContent1.class, NoRemoveContent2.class),
                layout.getChildren().map(Component::getClass)
                        .collect(Collectors.toList()));
    }

    @Test // 5388
    public void layout_chain_is_included_in_before_events() {
        setNavigationTargets(LoneRoute.class, RouteChildWithParameter.class);

        RouteChildWithParameter.events.clear();
        ui.navigate(RouteChildWithParameter.class, "foobar");

        BeforeEnterEvent beforeEnterEvent = (BeforeEnterEvent) RouteChildWithParameter.events
                .get(0);
        Assert.assertEquals(
                "There is not exactly one layout in the layout chain", 1,
                beforeEnterEvent.getLayouts().size());
        Assert.assertTrue("RouteParent was not included in the layout chain",
                beforeEnterEvent.getLayouts().contains(RouteParent.class));

        RouteChildWithParameter.events.clear();
        ui.navigate(LoneRoute.class);

        BeforeLeaveEvent beforeLeaveEvent = (BeforeLeaveEvent) RouteChildWithParameter.events
                .get(0);
        Assert.assertEquals(
                "There is not exactly one layout in the layout chain", 1,
                beforeLeaveEvent.getLayouts().size());
        Assert.assertTrue("RouteParent was not included in the layout chain",
                beforeLeaveEvent.getLayouts().contains(RouteParent.class));
    }

    @Test
    public void optional_parameter_non_existing_route()
            throws InvalidRouteConfigurationException {
        OptionalParameter.events.clear();
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        setNavigationTargets(OptionalParameter.class);

        String locationString = "optional/doesnotExist/parameter";
        router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        String exceptionText1 = String.format("Could not navigate to '%s'",
                locationString);

        String exceptionText2 = String
                .format("No navigation target found for path '%s'", locationString);

        String exceptionText3 = "<li><a href=\"optional/[:"
                + HasUrlParameterUtil.PARAMETER_NAME + "]\">optional/[:"
                + HasUrlParameterUtil.PARAMETER_NAME
                + "] (supports optional parameter)</a></li>";

        assertExceptionComponent(RouteNotFoundError.class, exceptionText1,
                exceptionText2, exceptionText3);
    }

    @Test
    public void without_optional_parameter()
            throws InvalidRouteConfigurationException {
        OptionalParameter.events.clear();
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        setNavigationTargets(WithoutOptionalParameter.class);

        String locationString = "optional";
        router.navigate(ui, new Location(locationString),
                NavigationTrigger.PROGRAMMATIC);

        String exceptionText1 = String.format("Could not navigate to '%s'",
                locationString);

        String exceptionText2 = String.format(
                "Reason: No navigation target found for path '%s'",
                locationString);

        String exceptionText3 = "<li>optional/:"
                + HasUrlParameterUtil.PARAMETER_NAME
                + " (requires parameter)</li>";

        assertExceptionComponent(RouteNotFoundError.class, exceptionText1,
                exceptionText2, exceptionText3);
    }

    @Test // #4595
    public void reroute_and_forward_from_parent_layout() {
        ProcessEventsBase.clear();

        setNavigationTargets(SecurityDocument.class, SecurityLogin.class);

        // On init and beforeEnter, SecurityParent is invoked twice, since on
        // the initial request it reroutes.
        final List<String> expectedInitially = Arrays.asList("SecurityParent",
                "SecurityParent", "SecurityLogin");
        final List<String> expected = Arrays.asList("SecurityParent",
                "SecurityLogin");

        // beforeEnter is going to reroute to login.
        router.navigate(ui, new Location("security/document"),
                NavigationTrigger.PROGRAMMATIC);

        assertEventOrder(expectedInitially, null, expectedInitially,
                expected);

        ProcessEventsBase.clear();

        // beforeLeave is going to forward to same url.
        router.navigate(ui, new Location("security/login"),
                NavigationTrigger.PROGRAMMATIC);

        // Instances already exists from previous navigation, so expectedInit is
        // null.
        assertExistingChainEventOrder(expected);
    }

    @Test // #4595
    public void event_listeners_are_invoked_starting_with_parent_component()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        setNavigationTargets(ProcessEventsFlower.class);

        router.navigate(ui, new Location("event/flower"),
                NavigationTrigger.PROGRAMMATIC);

        assertInitialChainEventOrder(
                getProcessEventsBranchChainNames("ProcessEventsFlower"));
    }

    @Test // #4595
    public void event_listeners_are_invoked_starting_with_parent_component_when_preserved_on_refresh()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        // This is null by default.
        ExtendedClientDetails previousClientDetails = ui.getInternals()
                .getExtendedClientDetails();

        // Used with PreserveOnRefresh.
        ExtendedClientDetails clientDetails = Mockito.mock(ExtendedClientDetails.class);
        ui.getInternals().setExtendedClientDetails(clientDetails);

        Mockito.when(clientDetails.getWindowName()).thenReturn("mock");

        setNavigationTargets(ProcessEventsFruit.class);

        router.navigate(ui, new Location("event/fruit"),
                NavigationTrigger.PROGRAMMATIC);

        ProcessEventsBase.clear();

        router.navigate(ui, new Location("event/fruit"),
                NavigationTrigger.PROGRAMMATIC);

        assertExistingChainEventOrder(
                getProcessEventsBranchChainNames("ProcessEventsFruit"));

        // Set back the previous client details.
        ui.getInternals().setExtendedClientDetails(previousClientDetails);
    }

    @Test // #4595
    public void parent_layouts_are_reused_when_change_url()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        setNavigationTargets(ProcessEventsFlower.class,
                ProcessEventsLeaf.class);

        router.navigate(ui, new Location("event/flower"),
                NavigationTrigger.PROGRAMMATIC);
        
        ProcessEventsBase.clear();

        final String parameter = "green";
        router.navigate(ui, new Location("event/leaf/" + parameter),
                NavigationTrigger.PROGRAMMATIC);
        
        assertEventOrder(Arrays.asList("ProcessEventsLeaf", "leafChild"),
                getProcessEventsBranchChainNames("ProcessEventsFlower"),
                getProcessEventsBranchChainNames(parameter, "ProcessEventsLeaf", "leafChild"),
                getProcessEventsBranchChainNames("ProcessEventsLeaf", "leafChild"));
    }

    @Test // #4595
    public void components_are_not_created_when_parent_layout_redirects()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        setNavigationTargets(ProcessEventsFlower.class,
                ProcessEventsTwig.class);

        router.navigate(ui, new Location("event/twig"),
                NavigationTrigger.PROGRAMMATIC);

        // This is expected after reroute.
        final List<String> expectedOnReroute = getProcessEventsBranchChainNames(
                "ProcessEventsFlower");
        
        // This is expected on init and BeforeEnter since the ProcessEventsRotten
        // parent of ProcessEventsTwig will reroute, so ProcessEventsTwig and
        // ProcessEventsStick won't be created.
        final List<String> expected = Stream
                .concat(getProcessEventsTrunkChainNames("ProcessEventsRotten")
                        .stream(), expectedOnReroute.stream())
                .collect(Collectors.toList());
        assertEventOrder(expected, null, expected, expectedOnReroute);
    }

    @Test // #4595
    public void url_parameter_is_invoked_right_before_enter_events()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        setNavigationTargets(ProcessEventsLeaf.class);

        final String parameter = "red";
        router.navigate(ui, new Location("event/leaf/" + parameter),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeEnter events aren't triggered in correct order",
                getProcessEventsBranchChainNames(parameter, "ProcessEventsLeaf",
                        "leafChild"),
                ProcessEventsBase.beforeEnter);
    }

    @Test // #4595
    public void url_parameter_is_invoked_where_before_enter_is_not_observed()
            throws InvalidRouteConfigurationException {
        ProcessEventsBase.clear();

        setNavigationTargets(ProcessEventsNeedle.class);

        final String parameter = "green";
        router.navigate(ui, new Location("event/needle/" + parameter),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(
                "BeforeEnter events aren't triggered in correct order",
                getProcessEventsBranchChainNames(parameter,
                        "needleChild"),
                ProcessEventsBase.beforeEnter);
    }

    @Test // #2740 #4213
    public void url_parameters_fail_to_be_extracted_from_views() {
        setNavigationTargets(TargetWithParameter.class,
                AnotherTargetWithParameter.class,
                ChainLinkWithParameterAndTarget.class,
                ChainLinkWithParameter.class,
                TargetWithOptionalParameters.class);

        // TODO: add failing url test
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_view1() {
        setNavigationTargets(ChainLinkWithParameter.class);

        assertUrlParameters("qwe/link/123",
                parameters("parentID", "qwe", "chainLinkID", "123"));
    }
    
    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_view2() {
        setNavigationTargets(TargetWithOptionalParameters.class);

        assertUrlParameters("qwe/link/123/456", parameters("parentID", "qwe",
                "chainLinkID", "123", "optional", "456"));
        assertUrlParameters("qwe/link/123/456/789",
                parameters("parentID", "qwe", "chainLinkID", "123", "optional",
                        "456", "anotherOptional", "789"));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_view3() {
        setNavigationTargets(TargetWithParameter.class);

        assertUrlParameters("123/link/456/target/789/bar",
                parameters("parentID", "123", "chainLinkID", "456",
                        "targetChainLinkID", "789"));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_view4() {
        setNavigationTargets(AnotherTargetWithParameter.class);

        assertUrlParameters(
                "123/targetLink/456/chainLink/789/987/foo/a/b/c/d/e/f",
                parameters("parentID", "123", "chainLinkID", "456",
                        "anotherTargetID", "789", "yetAnotherID", "987",
                        "varargsFoo", varargs("a", "b", "c", "d", "e", "f")));
        assertUrlParameters("abc/targetLink/def/chainLink/ghi/jkl/foo",
                parameters("parentID", "abc", "chainLinkID", "def",
                        "anotherTargetID", "ghi", "yetAnotherID", "jkl"));

        assertUrlParameters("012/targetLink/chainLink/345/678/foo/1/2/3/4",
                parameters("parentID", "012", "anotherTargetID", "345",
                        "yetAnotherID", "678", "varargsFoo",
                        varargs("1", "2", "3", "4")));
        assertUrlParameters("012/targetLink/chainLink/345/678/foo",
                parameters("parentID", "012", "anotherTargetID", "345",
                        "yetAnotherID", "678"));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_view5() {
        setNavigationTargets(ChainLinkWithParameterAndTarget.class);

        assertUrlParameters("987/targetLink/765/chainLink/543",
                parameters("parentID", "987", "chainLinkID", "765",
                        "targetChainLinkID", "543"));
        assertUrlParameters("987/targetLink/chainLink/543",
                parameters("parentID", "987", "targetChainLinkID", "543"));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_forum_view() {
        setNavigationTargets(ParametersForumThreadView.class);

        assertUrlParameters("forum/thread/123/456",
                parameters("threadID", "123", "messageID", "456"));
        assertUrlParameters("forum/thread/123/last",
                parameters("threadID", "123"));
        assertUrlParameters("forum/thread/123", parameters("threadID", "123"));
        assertUrlParameters("forum/thread/123/thread-name",
                parameters("threadID", "123", "something", "thread-name"));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_api_view() {
        setNavigationTargets(ParametersApiView.class);

        // path is empty
        assertUrlParameters("api", parameters());

        // with path
        assertUrlParameters("api/com/vaadin/client/package-summary.html",
                parameters("path", varargs("com", "vaadin", "client",
                        "package-summary.html")));

        // alias=framework, version is empty
        assertUrlParameters("api/framework/com/vaadin/client/package-summary.html",
                parameters("alias", "framework", "path", varargs("com",
                        "vaadin", "client", "package-summary.html")));

        // alias=framework, version=8.9.4
        assertUrlParameters(
                "api/framework/8.9.4/com/vaadin/client/package-summary.html",
                parameters("alias", "framework", "version", "8.9.4", "path",
                        varargs("com", "vaadin", "client",
                                "package-summary.html")));

        // groupId=com.vaadin, artifactId=vaadin-all, version is empty
        assertUrlParameters(
                "api/com.vaadin/vaadin-all/com/vaadin/client/package-summary.html",
                parameters("groupId", "com.vaadin", "artifactId", "vaadin-all",
                        "path", varargs("com", "vaadin", "client",
                                "package-summary.html")));

        // groupId=com.vaadin, artifactId=vaadin-all, version=8.9.4
        assertUrlParameters(
                "api/com.vaadin/vaadin-all/8.9.4/com/vaadin/client/package-summary.html",
                parameters("groupId", "com.vaadin", "version", "8.9.4",
                        "artifactId", "vaadin-all", "path", varargs("com",
                                "vaadin", "client", "package-summary.html")));
    }

    @Test // #2740 #4213
    public void url_parameters_are_extracted_for_details_view() {
        setNavigationTargets(DetailsView.class);

        assertUrlParameters("directory/component/url-parameter-mapping",
                parameters("urlIdentifier", "url-parameter-mapping"));
        assertUrlParameters(
                "directory/component/url-parameter-mapping/discussions",
                parameters("urlIdentifier", "url-parameter-mapping",
                        "tabIdentifier", "discussions"));
        assertUrlParameters(
                "directory/component/url-parameter-mapping/api/org/vaadin/flow/helper/HasAbsoluteUrlParameterMapping.html",
                parameters("urlIdentifier", "url-parameter-mapping",
                        "tabIdentifier", "api", "apiPath",
                        varargs("org", "vaadin", "flow", "helper",
                                "HasAbsoluteUrlParameterMapping.html")));
        assertUrlParameters(
                "directory/component/url-parameter-mapping/1.0.0-alpha7/api/org/vaadin/flow/helper/HasAbsoluteUrlParameterMapping.html",
                parameters("urlIdentifier", "url-parameter-mapping",
                        "versionIdentifier", "1.0.0-alpha7", "tabIdentifier",
                        "api", "apiPath",
                        varargs("org", "vaadin", "flow", "helper",
                                "HasAbsoluteUrlParameterMapping.html")));
        assertUrlParameters(
                "directory/component/url-parameter-mapping/1.0.0-alpha7/discussions",
                parameters("urlIdentifier", "url-parameter-mapping",
                        "versionIdentifier", "1.0.0-alpha7", "tabIdentifier",
                        "discussions"));
        assertUrlParameters("directory/component/url-parameter-mapping/1.0.0-alpha7",
                parameters("urlIdentifier", "url-parameter-mapping",
                        "versionIdentifier", "1.0.0-alpha7"));
    }

    private void assertUrlParameters(String url, UrlParameters parameters) {
        UrlParametersBase.clear();

        navigate(url);

        Assert.assertEquals("Incorrect parameters", parameters,
                UrlParametersBase.parameters);
    }

    private UrlParameters parameters(Object... keysAndValues) {
        return new UrlParameters(RouteModelTest.parameters(keysAndValues));
    }


    private List<String> getProcessEventsTrunkChainNames(String... leaf) {
        final List<String> chainNames = new ArrayList<>(
                Arrays.asList("ProcessEventsRoot", "rootChild1", "rootChild11",
                        "rootChild2", "ProcessEventsTrunk"));

        chainNames.addAll(Arrays.asList(leaf));

        return chainNames;
    }

    private List<String> getProcessEventsBranchChainNames(String... leaf) {
        final List<String> chainNames = getProcessEventsTrunkChainNames(
                "ProcessEventsBranch", "branchChild1", "branchChild2",
                "branchChild21");

        chainNames.addAll(Arrays.asList(leaf));

        return chainNames;
    }

    private void assertInitialChainEventOrder(List<String> expected) {
        assertEventOrder(expected, null, expected, expected);
    }

    private void assertExistingChainEventOrder(List<String> expected) {
        assertEventOrder(null, expected, expected, expected);
    }

    private void assertEventOrder(List<String> expectedInit,
            List<String> expectedBeforeLeave, List<String> expectedBeforeEnter,
            List<String> expectedAfterNavigation) {

        if (expectedInit == null) {
            Assert.assertTrue("There should be no component initialization",
                    ProcessEventsBase.init.isEmpty());
        } else {
            Assert.assertEquals(
                    "Component initialization is done in incorrect order",
                    expectedInit, ProcessEventsBase.init);
        }

        if (expectedBeforeLeave == null) {
            Assert.assertTrue("There should be no BeforeLeave events triggered",
                    ProcessEventsBase.beforeLeave.isEmpty());
        } else {
            Assert.assertEquals(
                    "BeforeLeave events aren't triggered in correct order",
                    expectedBeforeLeave, ProcessEventsBase.beforeLeave);
        }

        Assert.assertEquals(
                "BeforeEnter events aren't triggered in correct order",
                expectedBeforeEnter, ProcessEventsBase.beforeEnter);

        Assert.assertEquals(
                "AfterNavigation events aren't triggered in correct order",
                expectedAfterNavigation, ProcessEventsBase.afterNavigation);
    }

    private void setNavigationTargets(
            Class<? extends Component>... navigationTargets)
            throws InvalidRouteConfigurationException {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(router.getRegistry());
        routeConfiguration.update(() -> {
            routeConfiguration.getHandledRegistry().clean();
            Arrays.asList(navigationTargets)
                    .forEach(routeConfiguration::setAnnotatedRoute);
        });
    }

    private void setErrorNavigationTargets(
            Class<? extends Component>... errorNavigationTargets) {
        ((ApplicationRouteRegistry) router.getRegistry())
                .setErrorNavigationTargets(
                        new HashSet<>(Arrays.asList(errorNavigationTargets)));
    }

    private Class<? extends Component> getUIComponent() {
        return ComponentUtil.findParentComponent(ui.getElement().getChild(0))
                .get().getClass();
    }

    private void assertExceptionComponent(String exceptionText) {
        assertExceptionComponent(InternalServerError.class, exceptionText);
    }

    private void assertExceptionComponent(Class<?> errorClass,
            String... exceptionTexts) {
        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();

        Assert.assertTrue("No navigation component visible",
                visibleComponent.isPresent());

        Component routeNotFoundError = visibleComponent.get();
        Assert.assertEquals(errorClass, routeNotFoundError.getClass());
        String errorText = getErrorText(routeNotFoundError);
        for (String exceptionText : exceptionTexts) {
            Assert.assertTrue(
                    "Expected the error text to contain '" + exceptionText
                            + "', but it is '" + errorText + "'",
                    errorText.contains(exceptionText));
        }
    }

    private String getErrorText(Component routeNotFoundError) {
        if (routeNotFoundError.getClass() == RouteNotFoundError.class) {
            Component errorContent = routeNotFoundError.getChildren()
                    .findFirst().get();
            Assert.assertEquals(Html.class, errorContent.getClass());
            return ((Html) errorContent).getInnerHtml().toString();
        } else {
            return routeNotFoundError.getElement().getText();
        }
    }

    private void navigate(String url) {
        router.navigate(ui, new Location(url), NavigationTrigger.PROGRAMMATIC);
    }

}

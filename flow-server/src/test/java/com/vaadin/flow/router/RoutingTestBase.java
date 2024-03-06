/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import org.junit.Before;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.MockUI;

public class RoutingTestBase {

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
    public static class FooBarNavigationTarget extends Component {
    }

    @Route("navigation-target-with-title")
    @PageTitle("Custom Title")
    @Tag(Tag.DIV)
    public static class NavigationTargetWithTitle extends Component {
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

    @Route("greeting/other")
    @PageTitle("Custom Title")
    @Tag(Tag.DIV)
    public static class OtherGreetingNavigationTarget extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class RouterTestMockUI extends MockUI {

        public RouterTestMockUI(Router router) {
            super(createMockSession(router));
        }

        private static VaadinSession createMockSession(Router router) {
            MockVaadinServletService service = new MockVaadinServletService();
            service.setRouter(router);
            return new MockVaadinSession(service);
        }

    }

    public static class RouterTestUI extends UI {

        public RouterTestUI(Router router) {
            super();

            getInternals().setSession(createMockSession(router));
        }

        private static VaadinSession createMockSession(Router router) {

            VaadinSession session = Mockito.mock(VaadinSession.class);
            VaadinService service = Mockito.mock(VaadinService.class);

            Mockito.when(session.getService()).thenReturn(service);
            Mockito.when(service.getRouter()).thenReturn(router);

            return session;
        }

    }

    protected Router router;

    @Before
    public void init() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        router = new Router(new TestRouteRegistry());
    }
}

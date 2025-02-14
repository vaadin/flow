/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Before;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
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
            return new AlwaysLockedVaadinSession(service);
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

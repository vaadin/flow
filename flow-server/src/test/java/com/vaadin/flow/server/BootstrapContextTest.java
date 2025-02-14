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
package com.vaadin.flow.server;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.BootstrapHandler.BootstrapContext;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler.JavaScriptBootstrapContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class BootstrapContextTest {

    private MockVaadinSession session;
    private UI ui;
    private VaadinRequest request = Mockito.mock(VaadinRequest.class);

    private Function<VaadinRequest, String> callback = request -> "";;

    @Tag(Tag.A)
    @Push(value = PushMode.MANUAL, transport = Transport.LONG_POLLING)
    private static class MainView extends Component implements RouterLayout {

    }

    @Tag(Tag.A)
    private static class OtherView extends Component {

    }

    @Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
    private static class CustomRouteNotFound extends RouteNotFoundError {

    }

    @ParentLayout(MainView.class)
    private static class AnotherCustomRouteNotFound extends RouteNotFoundError {

    }

    @Before
    public void setUp() throws ServiceException {
        MockVaadinSession session = new MockVaadinSession();
        session.lock();
        ui = new UI();
        ui.getInternals().setSession(session);
    }

    @Test
    public void getPushAnnotation_routeTargetPresents_pushFromTheClassDefinitionIsUsed() {
        ui.getInternals().getRouter().getRegistry().setRoute("foo",
                MainView.class, Collections.emptyList());
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_LOCATION_PARAMETER))
                .thenReturn("foo");

        BootstrapContext context = new JavaScriptBootstrapContext(request,
                Mockito.mock(VaadinResponse.class), ui, callback);

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);
        Assert.assertTrue(push.isPresent());
        Push pushAnnotation = push.get();
        Assert.assertEquals(PushMode.MANUAL, pushAnnotation.value());
        Assert.assertEquals(Transport.LONG_POLLING, pushAnnotation.transport());
    }

    @Test
    public void getPushAnnotation_routeTargetPresents_pushDefinedOnParentLayout_pushFromTheClassDefinitionIsUsed() {
        ui.getInternals().getRouter().getRegistry().setRoute("foo",
                OtherView.class, Collections.singletonList(MainView.class));
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_LOCATION_PARAMETER))
                .thenReturn("foo");

        BootstrapContext context = new JavaScriptBootstrapContext(request,
                Mockito.mock(VaadinResponse.class), ui, callback);

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);
        Assert.assertTrue(push.isPresent());
        Push pushAnnotation = push.get();
        Assert.assertEquals(PushMode.MANUAL, pushAnnotation.value());
        Assert.assertEquals(Transport.LONG_POLLING, pushAnnotation.transport());
    }

    @Test
    public void getPushAnnotation_routeTargetIsAbsent_pushFromTheErrorNavigationTargetIsUsed() {
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_LOCATION_PARAMETER))
                .thenReturn("bar");

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(ui.getSession().getService().getContext());
        registry.setErrorNavigationTargets(
                Collections.singleton(CustomRouteNotFound.class));

        BootstrapContext context = new BootstrapContext(request,
                Mockito.mock(VaadinResponse.class), session, ui, request -> "");

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);
        Assert.assertTrue(push.isPresent());
        Push pushAnnotation = push.get();
        Assert.assertEquals(PushMode.AUTOMATIC, pushAnnotation.value());
        Assert.assertEquals(Transport.WEBSOCKET, pushAnnotation.transport());
    }

    @Test
    public void getPushAnnotation_routeTargetIsAbsent_pushIsDefinedOnParentLayout_pushFromTheErrorNavigationTargetParentLayoutIsUsed() {
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_LOCATION_PARAMETER))
                .thenReturn("bar");

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(ui.getSession().getService().getContext());
        registry.setErrorNavigationTargets(
                Collections.singleton(AnotherCustomRouteNotFound.class));

        BootstrapContext context = new BootstrapContext(request,
                Mockito.mock(VaadinResponse.class), session, ui, request -> "");

        Optional<Push> push = context
                .getPageConfigurationAnnotation(Push.class);
        Assert.assertTrue(push.isPresent());
        Push pushAnnotation = push.get();
        Assert.assertEquals(PushMode.MANUAL, pushAnnotation.value());
        Assert.assertEquals(Transport.LONG_POLLING, pushAnnotation.transport());
    }
}

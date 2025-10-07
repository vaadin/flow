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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

@NotThreadSafe
public class ErrorHandlerUtilTest {

    @Mock
    UI ui;
    @Mock
    VaadinService vaadinService;
    @Mock
    DeploymentConfiguration config;

    AlwaysLockedVaadinSession session;
    UIInternals internals;
    ApplicationRouteRegistry registry;

    @Tag("div")
    protected static class ErrorView extends Component
            implements HasErrorParameter<NullPointerException> {

        public static boolean setError = false;
        public static boolean initialized = false;

        public ErrorView() {
            initialized = true;
            setError = false;
        }

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NullPointerException> parameter) {
            getElement().setText("Nope!");
            setError = true;
            return 0;
        }
    }

    @Tag("div")
    @ParentLayout(ParentView.class)
    protected static class ErrorWithParentView extends Component
            implements HasErrorParameter<NullPointerException> {

        public static boolean setError = false;
        public static boolean initialized = false;

        public ErrorWithParentView() {
            initialized = true;
            setError = false;
        }

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NullPointerException> parameter) {
            getElement().setText("Nope!");
            setError = true;
            return 0;
        }
    }

    @Tag("div")
    protected static class ParentView extends Component
            implements RouterLayout {

        public static boolean initialized = false;

        public ParentView() {
            initialized = true;
        }
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        ErrorView.setError = false;
        ErrorView.initialized = false;

        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);
        Mockito.when(ui.isNavigationSupported()).thenReturn(true);

        internals = new UIInternals(ui);

        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Mockito.when(ui.getInternals()).thenReturn(internals);

        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        Mockito.when(routeRegistry.getRegisteredRoutes())
                .thenReturn(new ArrayList<>());
        Mockito.when(vaadinService.getRouteRegistry())
                .thenReturn(routeRegistry);

        session = new AlwaysLockedVaadinSession(vaadinService);
        VaadinContext context = new MockVaadinContext();
        Mockito.when(vaadinService.getContext()).thenReturn(context);
        Mockito.when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));
        internals.setSession(session);
        Mockito.when(vaadinService.getRouter())
                .thenReturn(Mockito.mock(Router.class));

        Mockito.when(ui.getSession()).thenReturn(session);

        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(config);

        Mockito.when(vaadinService.accessSession(
                Mockito.any(VaadinSession.class), Mockito.any(Command.class)))
                .thenCallRealMethod();
        Mockito.when(ui.access(Mockito.any(Command.class)))
                .thenCallRealMethod();

        registry = ApplicationRouteRegistry.getInstance(context);

        VaadinService.setCurrent(vaadinService);
        UI.setCurrent(ui);
    }

    @After
    public void cleanup() {
        VaadinService.setCurrent(null);
        UI.setCurrent(null);
    }

    @Test
    public void nullPointerException_executesErrorView() {
        registry.setErrorNavigationTargets(
                Collections.singleton(ErrorView.class));

        Assert.assertEquals(0, ui.getElement().getChildren().count());

        ui.access(() -> {
        });

        session.getPendingAccessQueue().forEach(futureAccess -> futureAccess
                .handleError(new NullPointerException("NPE")));

        Assert.assertTrue(ErrorView.initialized);
        Assert.assertTrue(ErrorView.setError);
        Assert.assertEquals(1, ui.getElement().getChildren().count());
    }

    @Test
    public void illegalArgumentException_doesNotExecuteErrorView() {
        registry.setErrorNavigationTargets(
                Collections.singleton(ErrorView.class));

        Assert.assertEquals(0, ui.getElement().getChildren().count());

        ui.access(() -> {
        });

        session.getPendingAccessQueue().forEach(futureAccess -> futureAccess
                .handleError(new IllegalArgumentException("IAE")));

        Assert.assertFalse(ErrorView.initialized);
        Assert.assertFalse(ErrorView.setError);
        Assert.assertEquals(0, ui.getElement().getChildren().count());
    }

    @Test
    public void redrawnExceptionView_alsoInitializesParent() {
        registry.setErrorNavigationTargets(
                Collections.singleton(ErrorWithParentView.class));

        Assert.assertEquals(0, ui.getElement().getChildren().count());

        ui.access(() -> {
        });

        session.getPendingAccessQueue().forEach(futureAccess -> futureAccess
                .handleError(new NullPointerException("NPE")));

        Assert.assertTrue(ErrorWithParentView.initialized);
        Assert.assertTrue(ErrorWithParentView.setError);

        Assert.assertTrue(ParentView.initialized);

        Assert.assertEquals(1, ui.getElement().getChildren().count());
    }
}

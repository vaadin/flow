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
package com.vaadin.flow.component.internal;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UI.BrowserLeaveNavigationEvent;
import com.vaadin.flow.component.UI.BrowserNavigateEvent;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.component.UI.CLIENT_NAVIGATE_TO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JavaScriptBootstrapUITest {

    private static final String CLIENT_PUSHSTATE_TO = "setTimeout(() => { window.history.pushState($0, '', $1); window.dispatchEvent(new CustomEvent('vaadin-navigated')); })";
    private static final String REACT_PUSHSTATE_TO = "window.dispatchEvent(new CustomEvent('vaadin-navigate', { detail: { state: $0, url: $1, replace: false, callback: $2 } }));";

    private MockServletServiceSessionSetup mocks;
    private UI ui;

    @PageTitle("app-shell-title")
    public static class AppShell implements AppShellConfigurator {
    }

    @Tag(Tag.H2)
    public static class CleanChild extends Component
            implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    @Route("clean")
    @Tag(Tag.HEADER)
    public static class Clean extends Component implements HasComponents {
        public Clean() {
            add(new CleanChild());
        }
    }

    @Tag(Tag.H1)
    public static class DirtyChild extends Component
            implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.postpone();
        }
    }

    @Route("dirty")
    @Tag(Tag.SPAN)
    public static class Dirty extends Component implements HasComponents {
        public Dirty() {
            add(new DirtyChild());
        }
    }

    @Route("product")
    @Tag(Tag.SPAN)
    @PageTitle("my-product")
    public static class ProductView extends Component implements HasComponents {
        public ProductView() {
            // this should be ignored
            UI.getCurrent().navigate("product");
            setId("productView");
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

    @Route("forwardToClientSideViewOnBeforeEnter")
    @Tag(Tag.DIV)
    public static class ForwardToClientSideViewOnBeforeEnter extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo("client-view");
        }
    }

    @Route("forwardToClientSideViewOnBeforeLeave")
    @Tag(Tag.DIV)
    public static class ForwardToClientSideViewOnBeforeLeave extends Component
            implements BeforeLeaveObserver {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.forwardTo("client-view");
        }
    }

    @Route("rerouteToClientSideViewOnReroute")
    @Tag(Tag.DIV)
    public static class ForwardToClientSideViewOnReroute extends Component
            implements BeforeLeaveObserver, BeforeEnterObserver {

        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
            event.rerouteTo("client-view");
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.rerouteTo("client-view");
        }
    }

    @Route("forwardToServerSideViewOnBeforeEnter")
    @Tag(Tag.DIV)
    public static class ForwardToServerViewOnBeforeEnter extends Component
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo("clean");
        }
    }

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        mocks.getService().getRouter().getRegistry().setRoute("clean",
                Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("clean/1",
                Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("dirty",
                Dirty.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("product",
                ProductView.class, Collections.emptyList());

        Class<? extends ProductView> routeProxyClass = new ByteBuddy()
                .subclass(ProductView.class)
                .modifiers(Visibility.PUBLIC, SyntheticState.SYNTHETIC).make()
                .load(ProductView.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        mocks.getService().getRouter().getRegistry().setRoute("proxy-product",
                routeProxyClass, Collections.emptyList());

        mocks.getService().getRouter().getRegistry().setRoute("exception",
                FailOnException.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute(
                "forwardToClientSideViewOnBeforeEnter",
                ForwardToClientSideViewOnBeforeEnter.class,
                Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute(
                "forwardToClientSideViewOnBeforeLeave",
                ForwardToClientSideViewOnBeforeLeave.class,
                Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute(
                "rerouteToClientSideViewOnReroute",
                ForwardToClientSideViewOnReroute.class,
                Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute(
                "forwardToServerSideViewOnBeforeEnter",
                ForwardToServerViewOnBeforeEnter.class,
                Collections.emptyList());
        ui = new UI();
        ui.getInternals().setSession(mocks.getSession());
        ui.doInit(null, 0, "appIdABC");

        CurrentInstance.setCurrent(ui);
    }

    @After
    public void cleanup() {
        mocks.cleanup();
    }

    @Test
    public void should_allow_navigation() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        // Dirty view is allowed after clean view
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/dirty", "", "", null, ""));
        assertEquals(Tag.SPAN, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_navigate_when_endingSlash() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true, "/clean/", "", "",
                null, ""));
        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());
    }

    @Test
    public void getChildren_should_notReturnAnEmptyList() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        assertEquals(1, ui.getChildren().count());
    }

    @Test
    public void addRemoveComponent_clientSideRouting_addsToBody() {
        final Element uiElement = ui.getElement();

        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        // router outlet is a virtual child that is not reflected on element
        // level
        assertEquals(1, ui.getChildren().count());
        assertEquals(0, uiElement.getChildCount());
        assertEquals(0, ui.getElement().getChildCount());

        final RouterLink routerLink = new RouterLink();
        ui.add(routerLink);

        assertEquals(2, ui.getChildren().count());
        assertEquals(1, ui.getElement().getChildCount());
        assertEquals(1, uiElement.getChildCount());

        ui.add(new RouterLink());

        assertEquals(3, ui.getChildren().count());
        assertEquals(2, ui.getElement().getChildCount());
        assertEquals(2, uiElement.getChildCount());

        ui.remove(routerLink);

        assertEquals(2, ui.getChildren().count());
        assertEquals(1, ui.getElement().getChildCount());
        assertEquals(1, uiElement.getChildCount());
    }

    @Test
    public void addComponent_clientSideRouterAndNavigation_componentsRemain() {
        final Element uiElement = ui.getElement();
        // trigger route via client
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        final RouterLink routerLink = new RouterLink();
        ui.add(routerLink);

        assertEquals(2, ui.getChildren().count());
        assertEquals(1, ui.getElement().getChildCount());
        assertEquals(1, uiElement.getChildCount());

        ui.navigate("product");

        assertEquals(2, ui.getChildren().count());
        assertEquals(1, ui.getElement().getChildCount());
        assertEquals(1, uiElement.getChildCount());
    }

    @Test
    public void should_prevent_navigation_on_dirty() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/dirty", "", "", null, ""));
        assertEquals(Tag.SPAN, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        // clean view cannot be rendered after dirty
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        // an error route cannot be rendered after dirty
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/errr", "", "", null, ""));
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_remove_content_on_leaveNavigation() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        ui.leaveNavigation(
                new BrowserLeaveNavigationEvent(ui, true, "/client-view", ""));

        assertEquals(0, ui.getWrapperElement().getChildCount());
    }

    @Test
    public void should_keep_content_on_leaveNavigation_postpone() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/dirty", "", "", null, ""));
        assertEquals(Tag.SPAN, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        ui.leaveNavigation(
                new BrowserLeaveNavigationEvent(ui, true, "/client-view", ""));
        assertEquals(Tag.SPAN, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H1,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_handle_forward_to_client_side_view_on_beforeEnter() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true,
                "/forwardToClientSideViewOnBeforeEnter", "", "", null, ""));

        assertEquals("client-view", ui.getForwardToClientUrl());
    }

    @Test
    public void should_not_handle_forward_to_client_side_view_on_beforeLeave() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true,
                "/forwardToClientSideViewOnBeforeLeave", "", "", null, ""));

        assertNull(ui.getForwardToClientUrl());
    }

    @Test
    public void should_not_handle_forward_to_client_side_view_on_reroute() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true,
                "/forwardToClientSideViewOnReroute", "", "", null, ""));

        assertNull(ui.getForwardToClientUrl());
    }

    @Test
    public void should_handle_forward_to_server_side_view_on_beforeEnter_and_update_url() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true,
                "/forwardToServerSideViewOnBeforeEnter", "", "", null, ""));

        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        ui.navigate("product");
        assertEquals("my-product", ui.getInternals().getTitle());
        assertEquals("productView",
                ui.getChildren().findFirst().get().getId().get());

        ui.navigate("forwardToServerSideViewOnBeforeEnter");
        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_show_error_page() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/err", "", "", null, ""));
        assertEquals(Tag.DIV, ui.getWrapperElement().getChild(0).getTag());
        assertTrue(ui.getWrapperElement().toString().contains("Available routes:"));
    }

    @Test
    public void should_invoke_clientRoute_when_navigationHasNotBeenStarted() {
        ui = Mockito.spy(ui);
        Page page = mockPage();

        ArgumentCaptor<String> execJs = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> execArg = ArgumentCaptor.forClass(String.class);

        ui.navigate("whatever");
        Mockito.verify(page).executeJs(execJs.capture(), execArg.capture());

        assertEquals(CLIENT_NAVIGATE_TO, execJs.getValue());
        assertEquals("whatever", execArg.getValue());
    }

    @Test
    public void should_update_pushState_when_navigationHasBeenAlreadyStarted() {
        ui = Mockito.spy(ui);
        Page page = mockPage();

        UIInternals internals = mockUIInternals();

        VaadinSession session = mocks.getSession();
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(internals.getSession()).thenReturn(session);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        ((MockDeploymentConfiguration) session.getService()
                .getDeploymentConfiguration()).setReactEnabled(false);

        Mockito.when(internals.hasLastHandledLocation()).thenReturn(true);
        Location lastLocation = new Location("clean");
        Mockito.when(internals.getLastHandledLocation())
                .thenReturn(lastLocation);
        StateTree stateTree = Mockito.mock(StateTree.class);
        Mockito.when(internals.getStateTree()).thenReturn(stateTree);
        Mockito.when(internals.getTitle()).thenReturn("");

        StateNode stateNode = BasicElementStateProvider
                .createStateNode("foo-element");
        Mockito.when(stateTree.getRootNode()).thenReturn(stateNode);

        ArgumentCaptor<String> execJs = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> execArg = ArgumentCaptor
                .forClass(Object[].class);

        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {

            menuRegistry
                    .when(() -> MenuRegistry.hasClientRoute("clean/1", true))
                    .thenReturn(false);

            ui.navigate("clean/1");
            Mockito.verify(page).executeJs(execJs.capture(), execArg.capture());

            boolean reactEnabled = ui.getSession().getConfiguration()
                    .isReactEnabled();

            final Object[] execValues = execArg.getValue();
            if (reactEnabled) {
                assertEquals(REACT_PUSHSTATE_TO, execJs.getValue());
                assertEquals(1, execValues.length);
                assertEquals("clean/1", execValues[0]);
            } else {
                assertEquals(CLIENT_PUSHSTATE_TO, execJs.getValue());
                assertEquals(2, execValues.length);
                assertNull(execValues[0]);
                assertEquals("clean/1", execValues[1]);
            }
        }
    }

    @Test
    public void should_not_notify_clientRoute_when_navigatingToTheSame() {
        ui = Mockito.spy(ui);
        Page page = mockPage();

        UIInternals internals = mockUIInternals();

        Mockito.when(internals.hasLastHandledLocation()).thenReturn(true);
        Location lastLocation = new Location("clean");
        Mockito.when(internals.getLastHandledLocation())
                .thenReturn(lastLocation);

        ui.navigate("clean/");
        Mockito.verify(page, Mockito.never()).executeJs(Mockito.anyString(),
                Mockito.anyString());
    }

    @Test
    public void server_should_not_doClientRoute_when_navigatingToServer() {
        ui.browserNavigate(
                new BrowserNavigateEvent(ui, true, "/clean", "", "", null, ""));
        assertEquals(Tag.HEADER, ui.getWrapperElement().getChild(0).getTag());
        assertEquals(Tag.H2,
                ui.getWrapperElement().getChild(0).getChild(0).getTag());

        ui = Mockito.spy(ui);
        Page page = mockPage();

        ArgumentCaptor<String> execJs = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> execArg = ArgumentCaptor
                .forClass(Object[].class);

        // Dirty view is allowed after clean view
        ui.navigate("dirty");
        // A server navigation happens
        assertEquals(Tag.SPAN, ui.getWrapperElement().getChild(0).getTag());
        Mockito.verify(page).executeJs(execJs.capture(), execArg.capture());

        boolean reactEnabled = ui.getSession().getConfiguration()
                .isReactEnabled();

        final Object[] execValues = execArg.getValue();
        if (reactEnabled) {
            assertEquals(REACT_PUSHSTATE_TO, execJs.getValue());
        } else {
            assertEquals(CLIENT_PUSHSTATE_TO, execJs.getValue());
        }
        assertEquals(3, execValues.length);
        assertNull(execValues[0]);
        assertEquals("dirty", execValues[1]);
    }

    @Test
    public void should_updatePageTitle_when_serverNavigation() {
        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("product");
        assertEquals("my-product", ui.getInternals().getTitle());
    }

    @Test
    public void should_updatePageTitle_when_serverNavigationToProxyViewClass() {
        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("proxy-product");
        assertEquals("my-product", ui.getInternals().getTitle());
    }

    @Test
    public void should_removeTitle_when_noAppShellTitle() {
        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("dirty");
        assertEquals("", ui.getInternals().getTitle());
    }

    @Test
    public void should_restoreIndexHtmlTitle() {
        ui.browserNavigate(new BrowserNavigateEvent(ui, true, "empty", "",
                "app-shell-title", null, ""));
        assertEquals("", ui.getInternals().getTitle());
        ui.browserNavigate(new BrowserNavigateEvent(ui, true, "dirty", "",
                "app-shell-title", null, ""));
        assertEquals("app-shell-title", ui.getInternals().getTitle());
    }

    @Test
    public void should_not_share_dynamic_app_title_for_different_UIs() {
        String dynamicTitle = UUID.randomUUID().toString();
        ui.browserNavigate(new BrowserNavigateEvent(ui, true, "clean", "",
                dynamicTitle, null, ""));
        assertEquals(dynamicTitle, ui.getInternals().getTitle());

        String anotherDynamicTitle = UUID.randomUUID().toString();
        UI anotherUI = new UI();
        anotherUI.getInternals().setSession(mocks.getSession());
        anotherUI.doInit(null, 0, "anotherUiId");
        anotherUI.browserNavigate(new BrowserNavigateEvent(anotherUI, true,
                "clean", "", anotherDynamicTitle, null, ""));
        assertEquals(anotherDynamicTitle, anotherUI.getInternals().getTitle());

        ui.navigate("dirty");
        assertEquals(dynamicTitle, ui.getInternals().getTitle());
    }

    @Test
    public void navigate_firsClientSideRoutingThrows_navigationInProgressIsReset_secondClientSideRoutingWorks() {
        VaadinSession session = Mockito.mock(VaadinSession.class);

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(session.getService()).thenReturn(service);
        Router router = Mockito.mock(Router.class);
        Mockito.when(service.getRouter()).thenReturn(router);

        Mockito.doThrow(RuntimeException.class).when(router)
                .resolveNavigationTarget(Mockito.any());

        UI ui = new UI();

        ui.getInternals().setSession(session);

        try {
            ui.navigate("foo", QueryParameters.empty());
        } catch (RuntimeException expected) {
            router = Mockito.mock(Router.class);
            Mockito.when(service.getRouter()).thenReturn(router);

            Mockito.when(router.resolveNavigationTarget(Mockito.any()))
                    .thenReturn(Optional.empty());
            ui.navigate("foo", QueryParameters.empty());

            Mockito.verify(router).resolveNavigationTarget(Mockito.any());
            return;
        }
        // self control: code inside catch should be invoked
        Assert.fail();
    }

    private void assertExceptionComponent(Class<?> errorClass,
            String... exceptionTexts) {
        Optional<Component> visibleComponent = ui.getElement().getChild(0)
                .getComponent();

        Assert.assertTrue("No navigation component visible",
                visibleComponent.isPresent());

        Component internalServerError = visibleComponent.get();
        Assert.assertEquals(errorClass, internalServerError.getClass());
        String errorText = internalServerError.getElement().getText();
        for (String exceptionText : exceptionTexts) {
            Assert.assertTrue("Expected the error text to contain '"
                    + exceptionText + "'", errorText.contains(exceptionText));
        }
    }

    private Page mockPage() {
        Page page = Mockito.mock(Page.class);
        Mockito.when(ui.getPage()).thenReturn(page);

        History history = new History(ui);
        Mockito.when(page.getHistory()).thenReturn(history);

        return page;
    }

    private UIInternals mockUIInternals() {
        UIInternals internals = Mockito.mock(UIInternals.class);
        Mockito.when(ui.getInternals()).thenReturn(internals);

        Mockito.when(internals.getRouter())
                .thenReturn(mocks.getService().getRouter());

        return internals;
    }

}

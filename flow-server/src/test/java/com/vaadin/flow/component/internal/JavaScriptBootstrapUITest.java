package com.vaadin.flow.component.internal;

import java.util.Collections;
import java.util.Optional;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.InternalServerError;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinRequest;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.CLIENT_NAVIGATE_TO;
import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.CLIENT_PUSHSTATE_TO;
import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.SERVER_ROUTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JavaScriptBootstrapUITest  {
    private MockServletServiceSessionSetup mocks;
    private JavaScriptBootstrapUI ui;

    @PageTitle("app-shell-title")
    public static class AppShell implements AppShellConfigurator {
    }

    @Tag(Tag.H2)
    public static class CleanChild extends Component implements BeforeLeaveObserver {
        @Override
        public void beforeLeave(BeforeLeaveEvent event) {
        }
    }

    @Route("clean")
    @Tag(Tag.HEADER)
    public static class Clean extends Component implements HasComponents{
        public Clean() {
            add(new CleanChild());
        }
    }

    @Tag(Tag.H1)
    public static class DirtyChild extends Component implements BeforeLeaveObserver {
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
    public static class ForwardToClientSideViewOnBeforeEnter extends Component implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            event.forwardTo("client-view");
        }
    }

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        mocks.getService().getRouter().getRegistry().setRoute("clean", Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("clean/1", Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("dirty", Dirty.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("product",
                ProductView.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("exception",
                FailOnException.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("forwardToClientSideViewOnBeforeEnter",
                ForwardToClientSideViewOnBeforeEnter.class, Collections.emptyList());
        ui = new JavaScriptBootstrapUI();
        ui.getInternals().setSession(mocks.getSession());

        Mockito.when(mocks.getSession().getAttribute(SERVER_ROUTING))
                .thenReturn(Boolean.FALSE);

        CurrentInstance.setCurrent(ui);
    }

    @Test
    public void should_allow_navigation() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());

        // Dirty view is allowed after clean view
        ui.connectClient("foo", "bar", "/dirty");
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_navigate_when_endingSlash() {
        ui.connectClient("foo", "bar", "/clean/");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());
    }

    @Test
    public void getChildren_should_notReturnAnEmptyList() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(1, ui.getChildren().count());
    }

    @Test
    public void should_prevent_navigation_on_dirty() {
        ui.connectClient("foo", "bar", "/dirty");
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());

        // clean view cannot be rendered after dirty
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());

        // an error route cannot be rendered after dirty
        ui.connectClient("foo", "bar", "/errr");
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_remove_content_on_leaveNavigation() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());

        ui.leaveNavigation("/client-view");

        assertEquals(0, ui.wrapperElement.getChildCount());
    }

    @Test
    public void should_keep_content_on_leaveNavigation_postpone() {
        ui.connectClient("foo", "bar", "/dirty");
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());

        ui.leaveNavigation("/client-view");
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_handle_forward_to_client_side_view_on_beforeEnter() {
        ui.connectClient("foo", "bar", "/forwardToClientSideViewOnBeforeEnter");

        assertEquals("client-view", ui.getForwardToUrl());
    }

    @Test
    public void should_show_error_page() {
        ui.connectClient("foo", "bar", "/err");
        assertEquals(Tag.DIV, ui.wrapperElement.getChild(0).getTag());
        assertTrue(ui.wrapperElement.toString().contains("Available routes:"));
    }

    @Test
    public void should_initializeUI_when_wrapperElement_null() {
        VaadinRequest request = mocks.createRequest(mocks, "/foo");
        ui.getRouter().initializeUI(ui, request);
        assertNull(ui.wrapperElement);
        // attached to body
        assertTrue(ui.getElement().toString().contains("Available routes:"));
    }

    @Test
    public void should_navigate_when_server_routing() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());


        Mockito.when(mocks.getSession().getAttribute(SERVER_ROUTING))
                .thenReturn(Boolean.TRUE);

        ui.navigate("dirty");
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H1, ui.wrapperElement.getChild(0).getChild(0).getTag());
    }

    @Test
    public void should_not_create_navigation_loop_when_server_routing() {
        Mockito.when(mocks.getSession().getAttribute(SERVER_ROUTING))
                .thenReturn(Boolean.TRUE);

        ui.navigate("product");
        assertTrue(ui.getChildren().findFirst().isPresent());
        assertEquals("productView",
                ui.getChildren().findFirst().get().getId().get());
    }

    @Test
    public void should_clearLastHandledNavigation_when_navigateToOtherViews() {
        Mockito.when(mocks.getSession().getAttribute(SERVER_ROUTING))
                .thenReturn(Boolean.TRUE);

        ui.navigate("clean");
        assertFalse("Should clear lastHandledNavigation after finishing",
                ui.getInternals().hasLastHandledLocation());
        ui.navigate("product");
        assertFalse("Should clear lastHandledNavigation after finishing",
                ui.getInternals().hasLastHandledLocation());
    }

    @Test
    public void should_invoke_clientRoute_when_navigationHasNotBeenStarted() {
        ui = Mockito.spy(ui);
        Page page = Mockito.mock(Page.class);

        Mockito.when(ui.getPage()).thenReturn(page);

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
        Page page = Mockito.mock(Page.class);
        UIInternals internals = Mockito.mock(UIInternals.class);

        Mockito.when(ui.getPage()).thenReturn(page);
        Mockito.when(ui.getInternals()).thenReturn(internals);

        Mockito.when(internals.hasLastHandledLocation()).thenReturn(true);
        Location lastLocation = new Location("clean");
        Mockito.when(internals.getLastHandledLocation()).thenReturn(lastLocation);
        StateTree stateTree = Mockito.mock(StateTree.class);
        Mockito.when(internals.getStateTree()).thenReturn(stateTree);
        Mockito.when(internals.getTitle()).thenReturn("");

        StateNode stateNode = BasicElementStateProvider.createStateNode("foo-element");
        Mockito.when(stateTree.getRootNode()).thenReturn(stateNode);

        ArgumentCaptor<String> execJs = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> execArg = ArgumentCaptor.forClass(String.class);

        ui.navigate("clean/1");
        Mockito.verify(page).executeJs(execJs.capture(), execArg.capture());

        assertEquals(CLIENT_PUSHSTATE_TO, execJs.getValue());
        assertEquals("clean/1", execArg.getValue());
    }

    @Test
    public void should_not_notify_clientRoute_when_navigatingToTheSame() {
        ui = Mockito.spy(ui);
        Page page = Mockito.mock(Page.class);
        UIInternals internals = Mockito.mock(UIInternals.class);

        Mockito.when(ui.getPage()).thenReturn(page);
        Mockito.when(ui.getInternals()).thenReturn(internals);

        Mockito.when(internals.hasLastHandledLocation()).thenReturn(true);
        Location lastLocation = new Location("clean");
        Mockito.when(internals.getLastHandledLocation()).thenReturn(lastLocation);

        ui.navigate("clean/");
        Mockito.verify(page, Mockito.never()).executeJs(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void server_should_not_doClientRoute_when_navigatingToServer() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());

        ui = Mockito.spy(ui);
        Page page = Mockito.mock(Page.class);
        Mockito.when(ui.getPage()).thenReturn(page);
        ArgumentCaptor<String> execJs = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> execArg = ArgumentCaptor.forClass(String.class);

        // Dirty view is allowed after clean view
        ui.navigate("dirty");
        // A server navigation happens
        assertEquals(Tag.SPAN, ui.wrapperElement.getChild(0).getTag());
        Mockito.verify(page).executeJs(execJs.capture(), execArg.capture());

        assertEquals(CLIENT_PUSHSTATE_TO, execJs.getValue());
        assertEquals("dirty", execArg.getValue());
    }

    @Test
    public void should_updatePageTitle_when_serverNavigation() {
        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("product");
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
    public void should_restoreAppShellTitle() {
        AppShellRegistry registry = new AppShellRegistry();
        registry.setShell(AppShell.class);
        mocks.setAppShellRegistry(registry);

        registry.modifyIndexHtml(Document.createShell(""),
                mocks.createRequest(mocks, "/foo"));

        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("dirty");
        assertEquals("app-shell-title", ui.getInternals().getTitle());
    }

    @Test
    public void should_restoreIndexHtmlTitle() {
        AppShellRegistry registry = new AppShellRegistry();
        mocks.setAppShellRegistry(registry);
        VaadinRequest request = mocks.createRequest(mocks, "/foo");

        Document document = Document.createShell("");
        org.jsoup.nodes.Element title = document.createElement("title");
        title.appendText("index-html-title");
        document.head().appendChild(title);

        registry.modifyIndexHtml(document, request);

        ui.navigate("empty");
        assertNull(ui.getInternals().getTitle());
        ui.navigate("dirty");
        assertEquals("index-html-title", ui.getInternals().getTitle());
    }

    @Test
    public void should_caught_and_show_exception_during_navigation_in_internalServerError()
            throws InvalidRouteConfigurationException {
        String location = "exception";
        String validationMessage = "Failed on an exception";
        Mockito.when(mocks.getSession().getAttribute(SERVER_ROUTING))
                .thenReturn(Boolean.TRUE);

        ui.navigate(location);
        String errorMessage = String.format(
                "There was an exception while trying to navigate to '%s' with the exception message '%s'",
                location, validationMessage);
        assertExceptionComponent(InternalServerError.class, errorMessage);
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
}

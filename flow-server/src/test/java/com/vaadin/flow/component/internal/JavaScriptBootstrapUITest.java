package com.vaadin.flow.component.internal;

import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.VaadinRequest;

import static com.vaadin.flow.component.internal.JavaScriptBootstrapUI.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JavaScriptBootstrapUITest  {
    private MockServletServiceSessionSetup mocks;
    private JavaScriptBootstrapUI ui;

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
    public static class ProductView extends Component implements HasComponents {
        public ProductView() {
            // this should be ignored
            UI.getCurrent().navigate("product");
            setId("productView");
        }
    }

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        mocks.getService().getRouter().getRegistry().setRoute("clean", Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("dirty", Dirty.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("product",
                ProductView.class, Collections.emptyList());
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
    public void should_not_navigate_when_client_routing() {
        ui.connectClient("foo", "bar", "/clean");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
        assertEquals(Tag.H2, ui.wrapperElement.getChild(0).getChild(0).getTag());

        // Dirty view is allowed after clean view
        ui.navigate("dirty");
        assertEquals(Tag.HEADER, ui.wrapperElement.getChild(0).getTag());
    }

}

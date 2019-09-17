package com.vaadin.flow.component.internal;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.MockServletServiceSessionSetup;

import static org.junit.Assert.assertEquals;
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

    @Before
    public void setup() throws Exception {
        mocks = new MockServletServiceSessionSetup();
        mocks.getService().getRouter().getRegistry().setRoute("clean", Clean.class, Collections.emptyList());
        mocks.getService().getRouter().getRegistry().setRoute("dirty", Dirty.class, Collections.emptyList());
        ui = new JavaScriptBootstrapUI();
        ui.getInternals().setSession(mocks.getSession());
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
}

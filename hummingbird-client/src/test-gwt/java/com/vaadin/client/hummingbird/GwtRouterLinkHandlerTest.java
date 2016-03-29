package com.vaadin.client.hummingbird;

import com.google.gwt.core.client.JavaScriptException;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.shared.ApplicationConstants;

import elemental.client.Browser;
import elemental.dom.Document.Events;
import elemental.dom.Element;
import elemental.events.MouseEvent;
import elemental.html.BaseElement;
import elemental.html.DivElement;

public class GwtRouterLinkHandlerTest extends ClientEngineTestBase {

    private JsArray<String> invocations;

    private MouseEvent currentEvent;

    private Registry registry;

    private DivElement boundElement;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        invocations = JsCollections.array();

        ServerConnector connector = new ServerConnector(null) {
            @Override
            public void sendNavigationMessage(String location,
                    Object stateObject) {
                invocations.push(location);
            };
        };

        UILifecycle lifecycle = new UILifecycle();
        lifecycle.setState(UIState.RUNNING);
        registry = new Registry() {
            {
                set(UILifecycle.class, lifecycle);
                set(ServerConnector.class, connector);
                set(MessageHandler.class, new MessageHandler(this));
            }
        };
        boundElement = Browser.getDocument().createDivElement();
        BaseElement baseElement = Browser.getDocument().createBaseElement();
        baseElement.setHref("/");
        Browser.getDocument().getHead().appendChild(baseElement);
        Browser.getDocument().getBody().appendChild(boundElement);
        RouterLinkHandler.bind(registry, boundElement);
    }

    public void testRouterLink_anchorWithRouterLink_eventIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target);

        assertInvocations(1);
        assertEventDefaultPrevented();
    }

    public void testRouterLink_notAnchor_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("div", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_anchorNoRouterLink_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", false);
        boundElement.appendChild(target);
        fireClickEvent(target);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_altClick_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target, true, false, false, false);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_ctrlClick_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target, false, true, false, false);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_metaClick_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target, false, false, true, false);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_shiftClick_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target, false, false, false, true);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_anchorWithExternalRouterLink_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "http://localhost:120/bar", true);
        boundElement.appendChild(target);
        try {
            fireClickEvent(target);
        } catch (JavaScriptException e) {
            // Happens because localhost:120 does not answer
            assertTrue(e.getMessage().contains("failed: Connection refused"));
        }

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    private void assertInvocations(int size) {
        assertEquals("Invalid rpc invocations amount", size,
                invocations.length());
    }

    private void assertEventDefaultPrevented() {
        assertTrue("Click event should be prevented",
                currentEvent.isDefaultPrevented());
    }

    private void assertEventDefaultNotPrevented() {
        assertFalse("Click event shouldn't be prevented",
                currentEvent.isDefaultPrevented());
    }

    private Element createTarget(String tag, String href, boolean routerLink) {
        Element target = Browser.getDocument().createElement(tag);
        target.setAttribute("href", href);
        if (routerLink) {
            target.setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "");
        }
        return target;
    }

    private void fireClickEvent(Element target) {
        fireClickEvent(target, false, false, false, false);
    }

    private void fireClickEvent(Element target, boolean alt, boolean ctrl,
            boolean meta, boolean shift) {
        currentEvent = (MouseEvent) Browser.getDocument()
                .createEvent(Events.MOUSE);
        currentEvent.initMouseEvent("click", true, true, Browser.getWindow(), 0,
                0, 0, 0, 0, ctrl, alt, shift, meta, 0, target);
        target.dispatchEvent(currentEvent);
    }

    public void testRouterLink_anchorWithRouterLink_ui_stopped() {
        currentEvent = null;
        assertInvocations(0);
        registry.getUILifecycle().setState(UIState.TERMINATED);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target);

        assertInvocations(0);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_clickOnImage_handled() {
        currentEvent = null;
        assertInvocations(0);

        Element routerLinkElement = createTarget("a", "foobar", true);
        boundElement.appendChild(routerLinkElement);
        Element image = Browser.getDocument().createImageElement();
        routerLinkElement.appendChild(image);
        fireClickEvent(image);

        assertInvocations(1);
        assertEventDefaultPrevented();
    }

    public void testRouterLink_anchorWithRouterLinkAndFragment_eventNotIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target);
        assertInvocations(1);

        Element targetWithFragment = createTarget("a", "foobar#anchor", true);
        boundElement.appendChild(targetWithFragment);
        fireClickEvent(targetWithFragment);

        assertInvocations(1);
        assertEventDefaultNotPrevented();
    }

    public void testRouterLink_anchorWithRouterLinkAndAnotherSegmentWithFragment_eventIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target = createTarget("a", "foobar", true);
        boundElement.appendChild(target);
        fireClickEvent(target);

        Element targetWithFragment = createTarget("a", "foobar1#anchor", true);
        boundElement.appendChild(targetWithFragment);
        fireClickEvent(targetWithFragment);

        assertInvocations(2);
        assertEventDefaultPrevented();
    }

    public void testRouterLink_anchorWithRouterLinkWithFragmentAndAnotherSegment_eventIntercepted() {
        currentEvent = null;
        assertInvocations(0);

        Element target1 = createTarget("a", "foobar1#anchor1", true);
        boundElement.appendChild(target1);
        fireClickEvent(target1);

        Element target2 = createTarget("a", "foobar2#anchor", true);
        boundElement.appendChild(target2);
        fireClickEvent(target2);

        assertInvocations(2);
        assertEventDefaultPrevented();
    }
}

package com.vaadin.client.hummingbird;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.PopStateHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;

import elemental.client.Browser;

public class GwtPopStateHandlerTest extends ClientEngineTestBase {

    private JsArray<String> invocations;
    private Registry registry;

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

        new PopStateHandler().bind(registry);
    }

    public void testDifferentPath_serverMessage() {
        setLocation("foobar");
        invocations.clear();

        setLocation("baz");
        assertInvocations(1);
        String location = invocations.get(0);
        assertEquals("invalid location sent", "baz", location);
    }

    public void testDifferentPathWithHash_hashNotSentToServer() {
        setLocation("foo");
        invocations.clear();

        setLocation("bar#baz");
        assertInvocations(1);
        String location = invocations.get(0);
        assertEquals("invalid location sent", "bar", location);
    }

    public void testSamePathDifferentHash_noServerMessage() {
        setLocation("foobar#baz");
        invocations.clear();

        setLocation("foobar#biz");
        assertInvocations(0);
    }

    public void testSamePathSameHash_serverMessage() {
        setLocation("foobar#baz");
        invocations.clear();

        setLocation("foobar#baz");
        assertInvocations(1);
        String location = invocations.get(0);
        assertEquals("invalid location sent", "foobar", location);
    }

    public void testUINotRunning_noServerMessage() {
        registry.getUILifecycle().setState(UIState.TERMINATED);

        setLocation("abcd");
        assertInvocations(0);
    }

    private void assertInvocations(int size) {
        assertEquals("Invalid rpc invocations amount", size,
                invocations.length());
    }

    private void setLocation(String newLocation) {
        Browser.getWindow().getLocation().assign(newLocation);
    }

    private native void firePopStateEvent()
    /*-{
        var e = new PopStateEvent('popstate', {
            'bubbles': true,
            'cancelable': false
        });
        window.dispatchEvent(event);
    }-*/;
}

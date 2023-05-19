package com.vaadin.client.communication;

import com.google.gwt.xhr.client.XMLHttpRequest;
import com.vaadin.client.*;
import com.vaadin.client.flow.StateTree;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.ReconnectDialogConfigurationMap;
import elemental.client.Browser;
import elemental.events.Event;

public class GwtDefaultConnectionStateHandlerTest extends ClientEngineTestBase {

    private DefaultConnectionStateHandler handler;
    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        createDummyConnectionState();

        registry = new Registry() {
            {
                UILifecycle uiLifecycle = new UILifecycle();
                uiLifecycle.setState(UILifecycle.UIState.RUNNING);
                set(UILifecycle.class, uiLifecycle);
                set(ApplicationConfiguration.class, new ApplicationConfiguration() {{
                    setHeartbeatInterval(10);
                    setServiceUrl("");
                }});
                set(StateTree.class, new StateTree(this) {{
                    getRootNode().getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION)
                            .getProperty(ReconnectDialogConfigurationMap.RECONNECT_ATTEMPTS_KEY).setValue((double)3);
                    // keep the timer from interfering with the test:
                    getRootNode().getMap(NodeFeatures.RECONNECT_DIALOG_CONFIGURATION)
                            .getProperty(ReconnectDialogConfigurationMap.RECONNECT_INTERVAL_KEY).setValue((double)10000000);
                }});
                set(ReconnectConfiguration.class, new ReconnectConfiguration(this));
                set(Heartbeat.class, new Heartbeat(this));
                set(RequestResponseTracker.class,
                        new RequestResponseTracker(this));
                set(ConnectionStateHandler.class,
                        handler = new DefaultConnectionStateHandler(this));
            }
        };
    }

    public void test_browserEvents_stopsHeartbeats() {
        registry.getHeartbeat().setInterval(10);
        Browser.getWindow().dispatchEvent(createEvent("offline"));
        assertEquals(0, registry.getHeartbeat().getInterval());

        Browser.getWindow().dispatchEvent(createEvent("online"));
        assertEquals(10, registry.getHeartbeat().getInterval());
    }

    public void test_onlineEventFollowedByOffline_connectionLost() {
        Browser.getWindow().dispatchEvent(createEvent("offline"));
        assertEquals(ConnectionIndicator.CONNECTION_LOST,
                ConnectionIndicator.getState());

        Browser.getWindow().dispatchEvent(createEvent("online"));
        assertEquals(ConnectionIndicator.RECONNECTING,
                ConnectionIndicator.getState());

        Browser.getWindow().dispatchEvent(createEvent("offline"));
        assertEquals(ConnectionIndicator.CONNECTION_LOST,
                ConnectionIndicator.getState());
    }

    public void test_onlineEventHeartbeatSucceeds_connected() {
        Browser.getWindow().dispatchEvent(createEvent("offline"));
        assertEquals(ConnectionIndicator.CONNECTION_LOST,
                ConnectionIndicator.getState());

        Browser.getWindow().dispatchEvent(createEvent("online"));
        assertEquals(ConnectionIndicator.RECONNECTING,
                ConnectionIndicator.getState());

        handler.heartbeatOk();
        assertEquals(ConnectionIndicator.CONNECTED,
                ConnectionIndicator.getState());
    }

    public void test_onlineEventButHeartbeatFails_continuesReconnectingAndFinallyGivesUp() {
        Browser.getWindow().dispatchEvent(createEvent("offline"));
        assertEquals(ConnectionIndicator.CONNECTION_LOST,
                ConnectionIndicator.getState());

        Browser.getWindow().dispatchEvent(createEvent("online"));
        assertEquals(ConnectionIndicator.RECONNECTING,
                ConnectionIndicator.getState());

        // second attempt (first attempt immediately after transitioning to
        // ConnectionState.RECONNECTING): should keep reconnecting
        handler.heartbeatException(XMLHttpRequest.create(), new Exception("some exception"));
        assertEquals(ConnectionIndicator.RECONNECTING,
                ConnectionIndicator.getState());

        // third attempt: should transition to CONNECTION_LOST
        handler.heartbeatException(XMLHttpRequest.create(), new Exception("some exception"));
        assertEquals(ConnectionIndicator.CONNECTION_LOST,
                ConnectionIndicator.getState());
    }

    private static native Event createEvent(String type)
    /*-{
        return new Event(type);
    }-*/;

    private static native void createDummyConnectionState()
    /*-{
      if (!$wnd.Vaadin) {
        $wnd.Vaadin = {};
      }
      if (!$wnd.Vaadin.connectionState) {
        $wnd.Vaadin.connectionState = { state: 'connected' };
      }
    }-*/;
}

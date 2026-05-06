package com.vaadin.client.communication;

import com.google.gwt.core.client.impl.SchedulerImpl;
import com.vaadin.client.ApplicationConfiguration;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ConnectionIndicator;
import com.vaadin.client.CustomScheduler;
import com.vaadin.client.DependencyLoader;
import com.vaadin.client.Registry;
import com.vaadin.client.ResourceLoader;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.URIResolver;
import com.vaadin.client.ValueMap;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.shared.JsonConstants;
import elemental.client.Browser;
import elemental.dom.Element;
import elemental.json.Json;
import elemental.json.JsonObject;

public class GwtLoadingIndicatorStateHandlerTest extends ClientEngineTestBase {

    private LoadingIndicatorStateHandler handler;
    private TestMessageHandler messageHandler;
    private StateTree stateTree;
    private StateNode stateNode;

    private static class TestMessageSender extends MessageSender {
        private final Registry registry;

        public TestMessageSender(Registry registry) {
            super(registry);
            this.registry = registry;
        }

        @Override
        public void send(final JsonObject payload) {
            if (!registry.getRequestResponseTracker().hasActiveRequest()) {
                registry.getRequestResponseTracker().startRequest();
            }
        }
    }

    private static class TestMessageHandler extends MessageHandler {
        public TestMessageHandler(Registry registry) {
            super(registry);
        }

        public void simulateResponse() {
            handleJSON(createJSONResponse(getLastSeenServerSyncId() + 1));
        }
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        // Minimum setup for simulating RPC request and response
        createDummyConnectionState();
        setUpAtmosphere();
        initScheduler(new CustomScheduler()); // enforces sync debounce

        new Registry() {
            {
                UILifecycle uiLifecycle = new UILifecycle();
                uiLifecycle.setState(UILifecycle.UIState.RUNNING);
                set(UILifecycle.class, uiLifecycle);
                set(ApplicationConfiguration.class, new ApplicationConfiguration() {{
                    setServiceUrl("");
                    setContextRootUrl("/");
                }});
                set(StateTree.class, stateTree = new StateTree(this));
                set(RequestResponseTracker.class,
                        new RequestResponseTracker(this));
                set(ConnectionStateHandler.class,
                        new DefaultConnectionStateHandler(this));
                set(LoadingIndicatorStateHandler.class, handler = new LoadingIndicatorStateHandler(this));
                set(DependencyLoader.class, new DependencyLoader(this));
                set(ResourceLoader.class, new ResourceLoader(this, false));
                set(ServerConnector.class, new ServerConnector(this));
                set(ServerRpcQueue.class, new ServerRpcQueue(this));
                set(URIResolver.class, new URIResolver(this));
                set(MessageSender.class, new TestMessageSender(this));
                set(MessageHandler.class, messageHandler = new TestMessageHandler(this));
                set(PushConfiguration.class, new PushConfiguration(this) {
                    @Override
                    public boolean isPushEnabled() {
                        return false;
                    }

                    @Override
                    public JsMap<String, String> getParameters() {
                        return JsCollections.map();
                    }
                });
                set(AtmospherePushConnection.class, new AtmospherePushConnection(this));
            }
        };

        // Create a valid element node to reference in event RPC messages
        stateNode = new StateNode(0, stateTree);
        stateTree.registerNode(stateNode);
        stateNode.getMap(NodeFeatures.ELEMENT_DATA);
        final Element mainElement = Browser.getDocument().createElement("main");
        Binder.bind(stateNode, mainElement);
    }

    public void test_default_loadingMuted() {
        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());

        handler.startLoading();

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());

        handler.stopLoading();

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
    }

    public void test_navigationFlow_loadingVisible() {
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());

        handler.processMessage(JsonConstants.RPC_TYPE_NAVIGATION, null);
        handler.startLoading();

        assertEquals(1, getRequestCount());
        assertEquals(ConnectionIndicator.LOADING, ConnectionIndicator.getState());

        handler.stopLoading();

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
    }

    public void test_regularUiEventFlow_loadingVisible() {
        final String[] regularEvents = new String[] { "click", "change", "submit" };
        for (String event : regularEvents) {
            handler.processMessage(JsonConstants.RPC_EVENT_TYPE, "click");
            handler.startLoading();

            assertEquals(ConnectionIndicator.LOADING, ConnectionIndicator.getState());

            handler.stopLoading();

            assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
        }
    }

    public void test_mutedUiEventFlow_loadingMuted() {
        final String[] mutedEvents = new String[] { "mousemove", "touchmove",
                "drag", "keydown", "keyup", "keypress", "wheel", "scroll", "input" };
        for (String event : mutedEvents) {
            handler.processMessage(JsonConstants.RPC_EVENT_TYPE, event);
            handler.startLoading();

            assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());

            handler.stopLoading();

            assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
        }
    }

    public void test_clickEventRpc_loadingVisible() {
        stateTree.sendEventToServer(stateNode, "click", Json.createObject());

        assertEquals(1, getRequestCount());
        assertEquals(ConnectionIndicator.LOADING, ConnectionIndicator.getState());

        messageHandler.simulateResponse();

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
    }

    public void test_mousemoveEventRpc_loadingMuted() {
        stateTree.sendEventToServer(stateNode, "mousemove", Json.createObject());

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());

        messageHandler.simulateResponse();

        assertEquals(0, getRequestCount());
        assertEquals(ConnectionIndicator.CONNECTED, ConnectionIndicator.getState());
    }

    private static native int getRequestCount()
    /*-{
      return $wnd.Vaadin.connectionState.requestCount;
    }-*/;

    private native void setUpAtmosphere()/*-{
                                         $wnd.vaadinPush={};
                                         $wnd.vaadinPush.atmosphere ={};
                                         $wnd.vaadinPush.atmosphere.subscribe = function(config){
                                             $wnd.subscribeUrl =  config.url;
                                         };
                                         $wnd.vaadinPush.atmosphere.unsubscribeUrl = function(uri){
                                             $wnd.unsubscribeUri =  uri;
                                         };
                                         }-*/;

    private static native void createDummyConnectionState()
    /*-{
      if (!$wnd.Vaadin) {
        $wnd.Vaadin = {};
      }
      if (!$wnd.Vaadin.connectionState) {
        $wnd.Vaadin.connectionState = {
          state: 'connected',
          requestCount: 0,
          loadingStarted: function() {
            this.state = 'loading';
            this.requestCount++;
          },
          loadingFinished: function() {
            if (this.requestCount == 0) { return; }
            this.requestCount--;
            if (this.requestCount == 0) { this.state = 'connected'; }
          },
          loadingFailed: function() {
            if (this.requestCount == 0) { return; }
            this.requestCount--;
            if (this.requestCount == 0) { this.state = 'connection-lost'; }
          }
        };
      }
    }-*/;

    private native void initScheduler(SchedulerImpl scheduler)
    /*-{
       @com.google.gwt.core.client.impl.SchedulerImpl::INSTANCE = scheduler;
    }-*/;

    private static native ValueMap createJSONResponse(int serverId)
    /*-{
       return { "serverId": serverId };
    }-*/;
}

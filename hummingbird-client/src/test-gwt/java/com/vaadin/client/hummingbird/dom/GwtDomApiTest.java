package com.vaadin.client.hummingbird.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.ValueMap;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.communication.ServerRpcQueue;

public class GwtDomApiTest extends ClientEngineTestBase {

    private MessageHandler messageHandler;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        UILifecycle uiLifecycle = new UILifecycle();
        uiLifecycle.setState(UIState.RUNNING);

        Registry registry = new Registry() {
            RequestResponseTracker requestResponseTracker = new RequestResponseTracker(
                    this);

            @Override
            public UILifecycle getUILifecycle() {
                return uiLifecycle;
            }

            @Override
            public RequestResponseTracker getRequestResponseTracker() {
                return requestResponseTracker;
            }

            @Override
            public ServerRpcQueue getServerRpcQueue() {
                return new ServerRpcQueue(this);
            }
        };

        registry.getRequestResponseTracker().startRequest();
        messageHandler = new MessageHandler(registry);
    }

    private void initTest() {
        DomApi.polymerFullyLoaded = false;
        DomApi.polymerMicroLoaded = false;
        DomApi.impl = node -> (DomElement) node;

        GwtPolymerApiImplTest.clearPolymer();

        verifyPolymerDomApiUsed(false);
        verifyPolymerFullyLoaded(false);
        verifyPolymerMicroLoaded(false);
    }

    public void testPolymerMicroDependencyLoaded() {
        initTest();

        GwtPolymerApiImplTest.setPolymerMicro();

        startMessageHandling();

        verifyPolymerDomApiUsed(true);
        verifyPolymerMicroLoaded(true);
        verifyPolymerFullyLoaded(false);
    }

    public void testPolymerFullDependencyLoaded() {
        initTest();

        GwtPolymerApiImplTest.setPolymerFull();

        startMessageHandling();

        verifyPolymerDomApiUsed(true);
        verifyPolymerMicroLoaded(true);
        verifyPolymerFullyLoaded(true);
    }

    private void startMessageHandling() {
        JavaScriptObject message = JavaScriptObject.createObject();

        messageHandler.handleMessage((ValueMap) message);
    }

    private void verifyPolymerDomApiUsed(boolean used) {
        assertEquals(
                "PolymerDomApi should " + (used ? "" : "not ") + "be in use",
                used, DomApi.impl.getClass().equals(PolymerDomApiImpl.class));
    }

    private void verifyPolymerMicroLoaded(boolean used) {
        assertEquals(
                "PolymerMicro should " + (used ? "" : "not ") + "be in use",
                used, DomApi.polymerMicroLoaded);
    }

    private void verifyPolymerFullyLoaded(boolean used) {
        assertEquals(
                "Polymer should " + (used ? "" : "not ") + "be fully in use",
                used, DomApi.polymerFullyLoaded);
    }
}

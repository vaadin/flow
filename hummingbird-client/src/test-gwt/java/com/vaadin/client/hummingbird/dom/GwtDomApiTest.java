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

    private Registry registry;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        registry = new Registry() {
            {
                UILifecycle uiLifecycle = new UILifecycle();
                uiLifecycle.setState(UIState.RUNNING);
                set(UILifecycle.class, uiLifecycle);
                set(RequestResponseTracker.class,
                        new RequestResponseTracker(this));
                set(MessageHandler.class, new MessageHandler(this));
                set(ServerRpcQueue.class, new ServerRpcQueue(this));
            }

        };

        registry.getRequestResponseTracker().startRequest();
    }

    private void initTest() {
        DomApi.polymerMicroLoaded = false;
        DomApi.impl = node -> (DomElement) node;

        GwtPolymerApiImplTest.clearPolymer();

        verifyPolymerDomApiUsed(false);
        verifyPolymerMicroLoaded(false);
    }

    public void testPolymerMicroDependencyLoaded() {
        initTest();

        GwtPolymerApiImplTest.setPolymerMicro();

        startMessageHandling();

        verifyPolymerDomApiUsed(true);
        verifyPolymerMicroLoaded(true);
    }

    public void testPolymerFullDependencyLoaded() {
        initTest();

        GwtPolymerApiImplTest.setPolymerFull();

        startMessageHandling();

        verifyPolymerDomApiUsed(true);
        verifyPolymerMicroLoaded(true);
    }

    private void startMessageHandling() {
        JavaScriptObject message = JavaScriptObject.createObject();

        registry.getMessageHandler().handleMessage((ValueMap) message);
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

}

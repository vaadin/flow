package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.ApplicationConnection.Client;
import com.vaadin.client.HummingbirdClientTest;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.communication.tree.NodeListener.Change;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

public class AbstractTreeUpdaterTest extends HummingbirdClientTest {

    protected static final int containerElementId = 2;

    protected static class MockTreeUpdater extends TreeUpdater {
        private List<MethodInvocation> enqueuedInvocations = new ArrayList<>();
        private Element rootElement;

        public MockTreeUpdater() {
            ServerRpcQueue rpcQueue = new ServerRpcQueue() {
                @Override
                public void add(MethodInvocation invocation, boolean lastOnly) {
                    enqueuedInvocations.add(invocation);
                }

                @Override
                public void flush() {
                    // nop
                }
            };

            Client client = new Client() {
                @Override
                public JavaScriptObject getModules() {
                    return JavaScriptObject.createObject();
                }
            };

            Element bodyElement = RootPanel.getBodyElement();
            rootElement = Document.get().createElement("div");
            bodyElement.appendChild(rootElement);

            init(rootElement, rpcQueue, client);

            // initialize the containerElement node
            update(Json.createObject(), buildChanges(
                    Changes.putNode(1, "containerElement", containerElementId)),
                    null);
        }

        public Element getRootElement() {
            return rootElement;
        }

        public List<MethodInvocation> getEnqueuedInvocations() {
            return enqueuedInvocations;
        }
    }

    protected MockTreeUpdater updater;

    private static JsonArray buildChanges(Change... changes) {
        JsonArray array = Json.createArray();
        for (int i = 0; i < changes.length; i++) {
            array.set(i, (JsonValue) changes[i]);
        }
        return array;
    }

    public void gwtSetUp() throws Exception {
        // Can't run this in the class initializer since it uses GWT.create
        updater = new MockTreeUpdater();
    }

    public AbstractTreeUpdaterTest() {
        super();
    }

    protected void applyChanges(Change... changes) {
        JsonArray changesJson = buildChanges(changes);
        updater.update(Json.createObject(), changesJson, null);
    }

}
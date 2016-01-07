package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.client.ApplicationConnection.Client;
import com.vaadin.client.ChangeUtil;
import com.vaadin.client.ChangeUtil.Change;
import com.vaadin.client.HummingbirdClientTest;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class AbstractTreeUpdaterTest extends HummingbirdClientTest {

    protected static final int containerElementId = 2;

    protected static class MockTreeUpdater extends TreeUpdater {
        private List<MethodInvocation> enqueuedInvocations = new ArrayList<>();
        private List<JsonObject> enqueuedNodeChanges = new ArrayList<>();
        private Element rootElement;

        public MockTreeUpdater() {
            ServerRpcQueue rpcQueue = new ServerRpcQueue() {
                @Override
                public void add(MethodInvocation invocation, boolean lastOnly) {
                    throw new RuntimeException(
                            "Mock should never invoke this method");
                }

                @Override
                public void flush() {
                    throw new RuntimeException(
                            "Mock should never invoke this method");
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
            update(Json.createObject(),
                    buildChanges(ChangeUtil.createMap(containerElementId),
                            ChangeUtil.putMap(1, "containerElement",
                                    containerElementId)),
                    null);
        }

        @Override
        public void sendRpc(String callbackName, JsonArray arguments) {
            enqueuedInvocations
                    .add(new MethodInvocation(callbackName, arguments));
        }

        @Override
        public void addPendingNodeChange(JsonObject nodeChnage) {
            enqueuedNodeChanges.add(nodeChnage);
        }

        public Element getRootElement() {
            return rootElement;
        }

        public List<MethodInvocation> getEnqueuedInvocations() {
            return enqueuedInvocations;
        }

        public List<JsonObject> getEnqueuedNodeChanges() {
            return enqueuedNodeChanges;
        }
    }

    protected MockTreeUpdater updater;

    private static JsonArray buildChanges(Change... changes) {
        JsonArray array = Json.createArray();
        for (int i = 0; i < changes.length; i++) {
            array.set(i, Json.parse(((JsonValue) changes[i]).toJson()));
        }
        return array;
    }

    @Override
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

    protected void applyRpc(JsonArray rpcData) {
        updater.update(Json.createObject(), Json.createArray(), rpcData);
    }

    protected void applyTemplate(int id, JsonObject template) {
        if (!template.hasKey("type")) {
            throw new RuntimeException("Template should have a type");
        }

        JsonObject templateMap = Json.createObject();
        templateMap.put(Integer.toString(id), template);

        applyTemplates(templateMap);
    }

    protected void applyTemplates(JsonObject templateMap) {
        for (String key : templateMap.keys()) {
            if (!templateMap.getObject(key).hasKey("type")) {
                throw new RuntimeException("Template " + key + " has no type");
            }
        }

        updater.update(templateMap, Json.createArray(), null);
    }

}
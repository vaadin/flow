package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;

public abstract class NodeContext {
    private final Map<String, Node> localIds = new HashMap<>();

    private final JavaScriptObject serverProxy;

    private final JavaScriptObject modelProxy;

    public NodeContext(JavaScriptObject modelProxy,
            JavaScriptObject serverProxy) {
        this.modelProxy = modelProxy;
        this.serverProxy = serverProxy;
    }

    public NodeContext(NodeContext parentContext) {
        this(parentContext.modelProxy, parentContext.serverProxy);
    }

    public void registerLocalId(String id, Node element) {
        assert !localIds.containsKey(id) : id + " already registered";

        localIds.put(id, element);
    }

    public abstract ListTreeNode resolveListTreeNode(String name);

    public void populateEventHandlerContext(
            JavaScriptObjectWithUsefulMethods evalContext) {
        for (String localId : localIds.keySet()) {
            evalContext.put(localId, localIds.get(localId));
        }
    }

    public abstract JavaScriptObject getExpressionContext();

    public JavaScriptObject getServerProxy() {
        return serverProxy;
    }

    public JavaScriptObject getModelProxy() {
        return modelProxy;
    }
}
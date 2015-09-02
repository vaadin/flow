package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;

import elemental.json.JsonObject;

public abstract class Template {
    private int id;

    public Template(int id) {
        this.id = id;
    }

    public abstract Node createElement(JsonObject node, NodeContext context);

    public JavaScriptObject createServerProxy(Integer nodeId) {
        throw new RuntimeException("Not supported for " + getClass().getName());
    }

    public JavaScriptObject createModelProxy(JsonObject node) {
        throw new RuntimeException("Not supported for " + getClass().getName());
    }

    public int getId() {
        return id;
    }
}
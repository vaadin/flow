package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;

public abstract class Template {
    private int id;

    public Template(int id) {
        this.id = id;
    }

    public abstract Node createElement(TreeNode node, NodeContext context);

    public JavaScriptObject createServerProxy(int nodeId) {
        throw new RuntimeException("Not supported for " + getClass().getName());
    }

    public int getId() {
        return id;
    }
}
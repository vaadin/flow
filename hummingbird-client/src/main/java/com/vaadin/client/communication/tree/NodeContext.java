package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public interface NodeContext {
    public TreeNodeProperty getProperty(String name);

    public void listenToProperty(String name,
            TreeNodePropertyValueChangeListener listener);

    public ListTreeNode resolveListTreeNode(String name);

    public Map<String, JavaScriptObject> buildEventHandlerContext();

    public Map<String, JavaScriptObject> buildExpressionContext();
}
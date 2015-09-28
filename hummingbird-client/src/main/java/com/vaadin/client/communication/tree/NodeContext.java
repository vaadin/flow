package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

public interface NodeContext {
    public void listenToProperty(String name,
            TreeNodePropertyValueChangeListener listener);

    public EventArray resolveArrayProperty(String name);

    public Map<String, JavaScriptObject> buildEventHandlerContext();
}
package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

public interface NodeContext {
    public TreeNodeProperty resolveProperty(String name);

    public EventArray resolveArrayProperty(String name);

    public Map<String, JavaScriptObject> buildEventHandlerContext();
}
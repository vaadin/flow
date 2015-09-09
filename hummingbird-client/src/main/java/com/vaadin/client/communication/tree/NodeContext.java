package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;

public interface NodeContext {

    public JavaScriptObject getServerProxy();

    public TreeNodeProperty resolveProperty(String name);

    public EventArray resolveArrayProperty(String name);
}
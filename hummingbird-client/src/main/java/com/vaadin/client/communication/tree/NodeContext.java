package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;

public class NodeContext {
    private final ElementNotifier notifier;
    private final JavaScriptObject serverProxy;
    private final JavaScriptObject modelProxy;

    public NodeContext(ElementNotifier notifier, JavaScriptObject serverProxy,
            JavaScriptObject modelProxy) {
        this.notifier = notifier;
        this.serverProxy = serverProxy;
        this.modelProxy = modelProxy;
    }

    public ElementNotifier getNotifier() {
        return notifier;
    }

    public JavaScriptObject getServerProxy() {
        return serverProxy;
    }

    public JavaScriptObject getModelProxy() {
        return modelProxy;
    }
}
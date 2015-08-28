package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;

public class NodeContext {
    private final ElementNotifier notifier;
    private final JavaScriptObject serverProxy;

    public NodeContext(ElementNotifier notifier,
            JavaScriptObject serverProxy) {
        this.notifier = notifier;
        this.serverProxy = serverProxy;
    }

    public ElementNotifier getNotifier() {
        return notifier;
    }

    public JavaScriptObject getServerProxy() {
        return serverProxy;
    }
}
package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.Namespace;

public class NamespaceChange extends NodeChange {

    private final Class<? extends Namespace> namespace;

    public NamespaceChange(Namespace namespace) {
        super(namespace.getNode());

        this.namespace = namespace.getClass();
    }

    public Class<? extends Namespace> getNamespace() {
        return namespace;
    }

}

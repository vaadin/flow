package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.Namespace;

import elemental.json.Json;
import elemental.json.JsonObject;

public abstract class NamespaceChange extends NodeChange {

    private final Class<? extends Namespace> namespace;

    public NamespaceChange(Namespace namespace) {
        super(namespace.getNode());

        this.namespace = namespace.getClass();
    }

    public Class<? extends Namespace> getNamespace() {
        return namespace;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("ns", Json.create(Namespace.getId(namespace)));
    }
}

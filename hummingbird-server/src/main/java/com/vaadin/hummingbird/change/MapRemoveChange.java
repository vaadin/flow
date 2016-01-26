package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;

public class MapRemoveChange extends NamespaceChange {

    private final String key;

    public MapRemoveChange(MapNamespace namespace, String key) {
        super(namespace);

        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

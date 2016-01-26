package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;

public class MapPutChange extends NamespaceChange {

    private final String key;
    private final Object value;

    public MapPutChange(MapNamespace namespace, String key, Object value) {
        super(namespace);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}

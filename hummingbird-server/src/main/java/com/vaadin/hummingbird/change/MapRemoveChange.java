package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.MapNamespace;

import elemental.json.JsonObject;

public class MapRemoveChange extends NamespaceChange {

    private final String key;

    public MapRemoveChange(MapNamespace namespace, String key) {
        super(namespace);

        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "remove");

        super.populateJson(json);

        json.put("key", key);
    }
}

package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.MapNamespace;

import elemental.json.Json;
import elemental.json.JsonObject;

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

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "put");

        json.put("key", key);

        super.populateJson(json);

        if (value instanceof StateNode) {
            StateNode node = (StateNode) value;
            json.put("nodeValue", Json.create(node.getId()));
        } else {
            json.put("value", NodeChange.encodeValue(value));
        }
    }
}

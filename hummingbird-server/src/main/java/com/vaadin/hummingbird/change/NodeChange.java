package com.vaadin.hummingbird.change;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public abstract class NodeChange implements Serializable {
    private final StateNode node;

    public NodeChange(StateNode node) {
        this.node = node;
    }

    public StateNode getNode() {
        return node;
    }

    public JsonObject toJson() {
        JsonObject json = Json.createObject();

        json.put("node", node.getId());

        populateJson(json);

        return json;
    }

    protected abstract void populateJson(JsonObject json);

    protected static JsonValue encodeValue(Object value) {
        if (value instanceof String) {
            return Json.create((String) value);
        } else if (value instanceof Number) {
            return Json.create(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            return Json.create(((Boolean) value).booleanValue());
        } else if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value == null) {
            return Json.createNull();
        } else {
            throw new IllegalArgumentException("Can't serialize"
                    + value.getClass() + " as a state node value");
        }
    }
}

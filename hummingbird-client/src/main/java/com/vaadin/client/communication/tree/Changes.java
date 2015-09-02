package com.vaadin.client.communication.tree;

import com.vaadin.client.communication.tree.NodeListener.PutChange;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class Changes {

    public static PutChange put(int id, String key, JsonValue value) {
        JsonObject change = createChange(id, "put");
        change.put("key", key);
        change.put("value", value);
        return (NodeListener.PutChange) change;
    }

    private static JsonObject createChange(int id, String type) {
        JsonObject change = Json.createObject();
        change.put("id", id);
        change.put("type", type);
        return change;
    }
}

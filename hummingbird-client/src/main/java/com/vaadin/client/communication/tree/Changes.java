package com.vaadin.client.communication.tree;

import com.vaadin.client.communication.tree.NodeListener.Change;
import com.vaadin.client.communication.tree.NodeListener.ListInsertChange;
import com.vaadin.client.communication.tree.NodeListener.ListInsertNodeChange;
import com.vaadin.client.communication.tree.NodeListener.ListRemoveChange;
import com.vaadin.client.communication.tree.NodeListener.PutChange;
import com.vaadin.client.communication.tree.NodeListener.PutNodeChange;
import com.vaadin.client.communication.tree.NodeListener.RemoveChange;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class Changes {

    private static JsonObject createChange(int id, String type, String key) {
        JsonObject change = Json.createObject();
        change.put("id", id);
        change.put("type", type);
        if (key != null) {
            change.put("key", key);
        }
        return change;
    }

    public static PutChange put(int id, String key, JsonValue value) {
        JsonObject change = createChange(id, "put", key);
        change.put("value", value);
        return (PutChange) change;
    }

    public static Change put(int id, String key, String value) {
        return put(id, key, Json.create(value));
    }

    public static PutNodeChange putNode(int id, String key, int nodeId) {
        JsonObject change = createChange(id, "putNode", key);
        change.put("value", nodeId);
        return (PutNodeChange) change;
    }

    public static ListInsertNodeChange listInsertNode(int id, String key,
            int index, int childId) {
        JsonObject change = createChange(id, "listInsertNode", key);
        change.put("index", index);
        change.put("value", childId);
        return (ListInsertNodeChange) change;
    }

    public static RemoveChange remove(int id, String key) {
        JsonObject change = createChange(id, "remove", key);
        return (RemoveChange) change;
    }

    public static ListRemoveChange listRemove(int id, String key, int index) {
        JsonObject change = createChange(id, "listRemove", key);
        change.put("index", index);
        return (ListRemoveChange) change;
    }

    public static ListInsertChange listInsert(int id, String key, int index,
            JsonValue value) {
        JsonObject change = createChange(id, "listInsert", key);
        change.put("index", index);
        change.put("value", value);
        return (ListInsertChange) change;
    }

    public static ListInsertChange listInsert(int id, String key, int index,
            String value) {
        return listInsert(id, key, index, Json.create(value));
    }
}

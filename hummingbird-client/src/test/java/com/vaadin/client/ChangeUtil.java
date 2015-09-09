package com.vaadin.client;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class ChangeUtil {

    @JsType
    public interface Change {
        @JsProperty
        public int getId();

        @JsProperty
        public String getType();
    }

    @JsType
    public interface RemoveChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();

        @JsProperty
        void setValue(JsonValue value);
    }

    @JsType
    public interface ListRemoveChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        int getIndex();

        @JsProperty
        JsonValue getRemovedValue();

        @JsProperty
        void setRemovedValue(JsonValue value);
    }

    @JsType
    public interface ListInsertChange extends PutChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface ListInsertNodeChange extends PutNodeChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface PutChange extends Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();
    }

    @JsType
    public interface PutOverrideChange extends Change {
        @JsProperty
        int getKey();

        @JsProperty
        int getValue();
    }

    @JsType
    public interface PutNodeChange extends Change {
        @JsProperty
        int getValue();

        @JsProperty
        String getKey();
    }

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

    public static PutOverrideChange putOverrideNode(int id, int templateId,
            int overrideId) {
        JsonObject change = createChange(id, "putOverride",
                String.valueOf(templateId));
        change.put("value", overrideId);
        return (PutOverrideChange) change;
    }
}

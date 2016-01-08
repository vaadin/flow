package com.vaadin.client;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.vaadin.client.communication.tree.ValueTypeMap;

import elemental.json.Json;
import elemental.json.JsonArray;
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
    public interface SpliceChange extends Change {
        @JsProperty
        int getIndex();

        @JsProperty
        int getRemove();

        @JsProperty
        JsonArray getValue();

        @JsProperty
        JsonArray getMapValue();

        @JsProperty
        JsonArray getListValue();
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
        int getMapValue();

        @JsProperty
        int getValue();
    }

    @JsType
    public interface PutMapChange extends Change {
        @JsProperty
        int getMapValue();

        @JsProperty
        String getKey();
    }

    @JsType
    public interface PutListChange extends Change {
        @JsProperty
        int getListValue();

        @JsProperty
        String getKey();
    }

    @JsType
    public interface CreateChange extends Change {
        @JsProperty
        String getNodeType();
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

    public static PutMapChange putMap(int id, String key, int nodeId) {
        JsonObject change = createChange(id, "put", key);
        change.put("mapValue", nodeId);
        return (PutMapChange) change;
    }

    public static PutListChange putList(int id, String key, int nodeId) {
        JsonObject change = createChange(id, "put", key);
        change.put("listValue", nodeId);
        return (PutListChange) change;
    }

    public static SpliceChange listInsertNode(int id, int index, int childId) {
        JsonObject change = createChange(id, "splice", null);
        change.put("index", index);
        change.put("mapValue", createArray(Json.create(childId)));
        return (SpliceChange) change;
    }

    public static RemoveChange remove(int id, String key) {
        JsonObject change = createChange(id, "remove", key);
        return (RemoveChange) change;
    }

    public static SpliceChange listRemove(int id, int index) {
        JsonObject change = createChange(id, "splice", null);
        change.put("index", index);
        change.put("remove", 1);
        return (SpliceChange) change;
    }

    public static SpliceChange listInsert(int id, int index, JsonValue value) {
        return listInsert(id, index, createArray(value));
    }

    public static SpliceChange listInsert(int id, int index, JsonArray array) {
        JsonObject change = createChange(id, "splice", null);
        change.put("index", index);
        change.put("value", array);
        return (SpliceChange) change;
    }

    public static SpliceChange listInsert(int id, int index, String value) {
        return listInsert(id, index, Json.create(value));
    }

    public static PutOverrideChange putOverrideNode(int id, int templateId,
            int overrideId) {
        JsonObject change = createChange(id, "putOverride",
                String.valueOf(templateId));
        change.put("mapValue", overrideId);
        return (PutOverrideChange) change;
    }

    public static CreateChange createMap(int id) {
        return create(id, ValueTypeMap.EMPTY_OBJECT);
    }

    public static CreateChange createList(int id) {
        return create(id, ValueTypeMap.UNDEFINED_ARRAY);
    }

    private static CreateChange create(int id, int valueTypeId) {
        JsonObject change = createChange(id, "create", null);
        change.put("nodeType", valueTypeId);
        return (CreateChange) change;
    }

    private static JsonArray createArray(JsonValue... values) {
        JsonArray array = Json.createArray();
        for (JsonValue child : values) {
            array.set(array.length(), child);
        }
        return array;
    }
}

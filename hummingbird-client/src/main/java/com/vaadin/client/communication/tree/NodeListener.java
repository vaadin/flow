package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.json.JsonValue;

public interface NodeListener {
    @JsType
    public interface Change {
        @JsProperty
        public int getId();

        @JsProperty
        public String getType();
    }

    @JsType
    public interface RemoveChange extends NodeListener.Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();

        @JsProperty
        void setValue(JsonValue value);
    }

    @JsType
    public interface ListRemoveChange extends NodeListener.Change {
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
    public interface ListInsertChange extends NodeListener.PutChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface ListInsertNodeChange extends NodeListener.PutNodeChange {
        @JsProperty
        int getIndex();
    }

    @JsType
    public interface PutChange extends NodeListener.Change {
        @JsProperty
        String getKey();

        @JsProperty
        JsonValue getValue();
    }

    @JsType
    public interface PutOverrideChange extends NodeListener.Change {
        @JsProperty
        int getKey();

        @JsProperty
        int getValue();
    }

    @JsType
    public interface PutNodeChange extends NodeListener.Change {
        @JsProperty
        int getValue();

        @JsProperty
        String getKey();
    }

    void putNode(NodeListener.PutNodeChange change);

    void put(NodeListener.PutChange change);

    void listInsertNode(NodeListener.ListInsertNodeChange change);

    void listInsert(NodeListener.ListInsertChange change);

    void listRemove(NodeListener.ListRemoveChange change);

    void remove(NodeListener.RemoveChange change);

    void putOverride(NodeListener.PutOverrideChange change);

    default void notify(Change change) {
        switch (change.getType()) {
        case "putNode":
            putNode((PutNodeChange) change);
            break;
        case "put":
            put((PutChange) change);
            break;
        case "listInsertNode":
            listInsertNode((ListInsertNodeChange) change);
            break;
        case "listInsert":
            listInsert((ListInsertChange) change);
            break;
        case "listRemove":
            listRemove((ListRemoveChange) change);
            break;
        case "remove":
            remove((RemoveChange) change);
            break;
        case "putOverride":
            putOverride((PutOverrideChange) change);
            break;
        default:
            throw new RuntimeException(
                    "Unsupported change type: " + change.getType());
        }
    }
}
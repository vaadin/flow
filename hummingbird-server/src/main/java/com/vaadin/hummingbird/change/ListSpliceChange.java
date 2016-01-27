package com.vaadin.hummingbird.change;

import java.util.List;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.ListNamespace;
import com.vaadin.util.JsonStream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class ListSpliceChange extends NamespaceChange {
    private final int index;
    private final int removeCount;
    private final List<?> newItems;
    private final boolean nodeValues;

    public ListSpliceChange(ListNamespace namespace, int index, int removeCount,
            List<?> newItems) {
        super(namespace);
        this.index = index;
        this.removeCount = removeCount;
        this.newItems = newItems;
        nodeValues = namespace.isNodeValues();
    }

    public int getIndex() {
        return index;
    }

    public int getRemoveCount() {
        return removeCount;
    }

    public List<?> getNewItems() {
        return newItems;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "splice");

        super.populateJson(json);

        json.put("index", index);
        if (removeCount > 0) {
            json.put("remove", removeCount);
        }

        if (newItems != null && !newItems.isEmpty()) {

            Function<Object, JsonValue> mapper;
            String addKey;
            if (nodeValues) {
                addKey = "addNodes";
                mapper = item -> Json.create(((StateNode) item).getId());
            } else {
                addKey = "add";
                mapper = NodeChange::encodeValue;
            }

            JsonArray newItemsJson = newItems.stream().map(mapper)
                    .collect(JsonStream.asArray());
            json.put(addKey, newItemsJson);
        }
    }
}

/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.hummingbird.change;

import java.util.List;
import java.util.function.Function;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.ListNamespace;
import com.vaadin.util.JsonStream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Change describing a splice operation on a list namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ListSpliceChange extends NamespaceChange {
    private final int index;
    private final int removeCount;
    private final List<?> newItems;
    private final boolean nodeValues;

    /**
     * Creates a new splice change.
     *
     * @param namespace
     *            the changed namespace
     * @param index
     *            the index of the splice operation
     * @param removeCount
     *            the number of removed items
     * @param newItems
     *            a list of new items
     */
    public ListSpliceChange(ListNamespace<?> namespace, int index,
            int removeCount, List<?> newItems) {
        super(namespace);
        this.index = index;
        this.removeCount = removeCount;
        this.newItems = newItems;
        nodeValues = namespace.isNodeValues();
    }

    /**
     * Gets the index of the change
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the number of removed items
     *
     * @return the number of removed items
     */
    public int getRemoveCount() {
        return removeCount;
    }

    /**
     * Gets the newly added items
     *
     * @return a list of added items
     */
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
                mapper = JsonCodec::encodePrimitiveValue;
            }

            JsonArray newItemsJson = newItems.stream().map(mapper)
                    .collect(JsonStream.asArray());
            json.put(addKey, newItemsJson);
        }
    }
}

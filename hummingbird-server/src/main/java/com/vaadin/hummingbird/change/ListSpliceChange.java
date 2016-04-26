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
import com.vaadin.hummingbird.nodefeature.NodeList;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Change describing a splice operation on a list node feature.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ListSpliceChange extends NodeFeatureChange {
    private final int index;
    private final int removeCount;
    private final List<?> newItems;
    private final boolean nodeValues;

    /**
     * Creates a new splice change.
     *
     * @param list
     *            the changed list
     * @param nodeValues
     *            true if the values are {@link StateNode}s, false otherwise
     * @param index
     *            the index of the splice operation
     * @param removeCount
     *            the number of removed items
     * @param newItems
     *            a list of new items
     */
    public ListSpliceChange(NodeList<?> list, boolean nodeValues, int index,
            int removeCount, List<?> newItems) {
        super(list);
        this.index = index;
        this.removeCount = removeCount;
        this.newItems = newItems;
        this.nodeValues = nodeValues;
    }

    /**
     * Gets the index of the change.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the number of removed items.
     *
     * @return the number of removed items
     */
    public int getRemoveCount() {
        return removeCount;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends NodeList<?>> getFeature() {
        return (Class<? extends NodeList<?>>) super.getFeature();
    }

    /**
     * Gets the newly added items.
     *
     * @return a list of added items
     */
    public List<?> getNewItems() {
        return newItems;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, index);
        if (removeCount > 0) {
            json.put(JsonConstants.CHANGE_SPLICE_REMOVE, removeCount);
        }

        if (newItems != null && !newItems.isEmpty()) {

            Function<Object, JsonValue> mapper;
            String addKey;
            if (nodeValues) {
                addKey = JsonConstants.CHANGE_SPLICE_ADD_NODES;
                mapper = item -> Json.create(((StateNode) item).getId());
            } else {
                addKey = JsonConstants.CHANGE_SPLICE_ADD;
                mapper = JsonCodec::encodeWithoutTypeInfo;
            }

            JsonArray newItemsJson = newItems.stream().map(mapper)
                    .collect(JsonUtil.asArray());
            json.put(addKey, newItemsJson);
        }
    }
}

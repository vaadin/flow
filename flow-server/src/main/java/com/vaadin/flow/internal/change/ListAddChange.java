/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.internal.change;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Change describing an add operation in a {@link NodeList list} node feature.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public class ListAddChange<T extends Serializable>
        extends AbstractListChange<T> {

    private final List<? extends T> newItems;
    private final boolean nodeValues;

    /**
     * Creates a new list add change.
     *
     * @param list
     *            the changed list
     * @param nodeValues
     *            true if the values are {@link StateNode}s, false otherwise
     * @param index
     *            the index of the add operations
     * @param newItems
     *            a list of new items
     */
    public ListAddChange(NodeList<T> list, boolean nodeValues, int index,
            List<? extends T> newItems) {
        super(list, index);
        this.newItems = newItems;
        this.nodeValues = nodeValues;
    }

    /**
     * Gets the added items.
     *
     * @return the added items
     */
    public List<T> getNewItems() {
        return Collections.unmodifiableList(newItems);
    }

    /**
     * Returns whether this add operation still contains any new items.
     *
     * @return <code>true</code> if no new items, <code>false</code> if still
     *         has new items
     */
    public boolean isEmpty() {
        return newItems.isEmpty();
    }

    /**
     * Gets a copy of the change with the same data except a list of new
     * {@code items}.
     * 
     * @param items
     *            new list of items
     * @return a copy of the change based on new items
     */
    public ListAddChange<T> copy(List<? extends T> items) {
        return new ListAddChange<>(getNodeList(), nodeValues, getIndex(),
                items);
    }

    @Override
    public ListAddChange<T> copy(int indx) {
        return new ListAddChange<>(getNodeList(), nodeValues, indx, newItems);
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, getIndex());

        Function<Object, JsonValue> mapper;
        String addKey;
        if (nodeValues) {
            addKey = JsonConstants.CHANGE_SPLICE_ADD_NODES;
            mapper = item -> Json.create(((StateNode) item).getId());
        } else {
            addKey = JsonConstants.CHANGE_SPLICE_ADD;
            mapper = item -> JsonCodec.encodeWithConstantPool(item,
                    constantPool);
        }

        JsonArray newItemsJson = newItems.stream().map(mapper)
                .collect(JsonUtils.asArray());
        json.put(addKey, newItemsJson);
    }

}

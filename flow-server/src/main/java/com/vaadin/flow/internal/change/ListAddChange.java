/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Change describing an add operation in a {@link NodeList list} node feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
    public List<? extends T> getNewItems() {
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
    protected void populateJson(ObjectNode json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, getIndex());

        Function<Object, JsonNode> mapper;
        String addKey;
        if (nodeValues) {
            addKey = JsonConstants.CHANGE_SPLICE_ADD_NODES;
            mapper = item -> JacksonUtils
                    .createNode(((StateNode) item).getId());
        } else {
            addKey = JsonConstants.CHANGE_SPLICE_ADD;
            mapper = item -> JacksonCodec.encodeWithConstantPool(item,
                    constantPool);
        }

        ArrayNode newItemsJson = newItems.stream().map(mapper)
                .collect(JacksonUtils.asArray());
        json.set(addKey, newItemsJson);
    }

    /**
     * Removes item from the change list.
     * <p>
     * Note: This should be used only when list of changes is being re-indexed
     * after adding a new change.
     *
     * @param item
     *            Item to be removed.
     */
    public void removeItem(T item) {
        assert (newItems.size() > 1)
                : "Item list can't be empty after item removal";
        newItems.remove(item);
    }

}

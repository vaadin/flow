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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.vaadin.hummingbird.ConstantPool;
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
 * Change describing an add operation in a {@link NodeList list} node feature.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the type of the item in the node list
 */
public class ListAddChange<T extends Serializable> extends NodeFeatureChange {

    private int index;
    private final List<T> newItems;
    private final boolean nodeValues;
    private final NodeList<T> list;

    /**
     * Creates a new list add change.
     *
     * @param list
     *            the changed list
     * @param nodeValues
     *            the values added
     * @param index
     *            the index of the add operations
     * @param newItems
     *            a list of new items
     */
    public ListAddChange(NodeList<T> list, boolean nodeValues, int index,
            List<T> newItems) {
        super(list);
        this.list = list;
        this.index = index;
        this.newItems = newItems;
        this.nodeValues = nodeValues;
    }

    /**
     * Gets the added items.
     *
     * @return the added items
     */
    public List<T> getNewItems() {
        return newItems;
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
     * Checks whether this operation adds an item that has been removed, and if
     * so, discards it.
     *
     * @return <code>true</code> if the removed item was added by this
     *         operation, <code>false</code> if not
     */
    public boolean checkAndDiscardRemovedItem() {
        for (Iterator<T> iterator = newItems.iterator(); iterator.hasNext();) {
            // if there is another item in the list that equals this,
            // indexOf does not tell whether this item was removed
            if (list.indexOf(iterator.next()) == -1) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Adjusts the index of this change if there was an item removed with a
     * lower index than the items added here.
     *
     * @param removedItemIndex
     *            the index of the removed item
     */
    public void adjustIndex(int removedItemIndex) {
        // adjust index if removed from before these additions
        if (removedItemIndex < index) {
            index--;
        }
    }

    /**
     * Gets the number of new items added in this operation before the given
     * index.
     *
     * @param indexComparedTo
     *            the index to compare to
     * @return the amount of items added before the given index
     */
    public int getNumberOfNewItemsBeforeIndex(int indexComparedTo) {
        if (index >= indexComparedTo) {
            return 0;
        }
        return indexComparedTo > index + newItems.size() ? newItems.size()
                : indexComparedTo - index;
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

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        if (newItems.isEmpty()) {
            return;
        }

        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, index);

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
                .collect(JsonUtil.asArray());
        json.put(addKey, newItemsJson);
    }

}

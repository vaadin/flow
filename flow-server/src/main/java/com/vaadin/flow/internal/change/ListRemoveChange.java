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

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Change describing a remove operation in a {@link NodeList list} node feature.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public class ListRemoveChange<T extends Serializable>
        extends AbstractListChange<T> {

    private final T removedItem;

    /**
     * Creates a new list remove change.
     *
     *
     * @param list
     *            the changed list
     * @param index
     *            the index of the remove operation
     * @param removedItem
     *            a removed item
     */
    public ListRemoveChange(NodeList<T> list, int index, T removedItem) {
        super(list, index);
        this.removedItem = removedItem;
    }

    /**
     * Gets the removed item.
     * <p>
     * This method is public only for testing reasons.
     *
     * @return the removed item
     */
    public T getRemovedItem() {
        return removedItem;
    }

    @Override
    public AbstractListChange<T> copy(int indx) {
        return new ListRemoveChange<>(getNodeList(), indx, getRemovedItem());
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, getIndex());
        json.put(JsonConstants.CHANGE_SPLICE_REMOVE, 1);
    }
}

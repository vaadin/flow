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
package com.vaadin.client.hummingbird;

import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;

/**
 * A state node namespace that structures data as a list.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ListNamespace extends AbstractNamespace {

    private final JsArray<Object> values = JsCollections.array();

    /**
     * Creates a new list namespace.
     *
     * @param id
     *            the if of the namespace
     * @param node
     *            the node of the namespace
     */
    public ListNamespace(int id, StateNode node) {
        super(id, node);
    }

    /**
     * Gets the number of items in this namespace.
     *
     * @return the number of items
     */
    public int length() {
        return values.length();
    }

    /**
     * Gets the item at the given index.
     *
     * @param index
     *            the index
     * @return the item at the index
     */
    public Object get(int index) {
        return values.get(index);
    }

    /**
     * Sets the value at the given index.
     *
     * @param index
     *            the index
     * @param value
     *            the value to set
     */
    public void set(int index, Object value) {
        values.set(index, value);
    }

    /**
     * Removes a number of items at the given index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     */
    public void splice(int index, int remove) {
        values.splice(index, remove);
    }

    /**
     * Removes and adds a number of items at the given index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            a new item to add
     */
    @SafeVarargs
    public final <T> void splice(int index, int remove, T... add) {
        values.splice(index, remove, add);
    }
}

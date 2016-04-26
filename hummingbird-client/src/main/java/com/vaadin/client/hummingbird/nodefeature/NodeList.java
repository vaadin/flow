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
package com.vaadin.client.hummingbird.nodefeature;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.reactive.Computation;
import com.vaadin.client.hummingbird.reactive.ReactiveChangeListener;
import com.vaadin.client.hummingbird.reactive.ReactiveEventRouter;
import com.vaadin.client.hummingbird.reactive.ReactiveValue;

import elemental.events.EventRemover;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * A state node feature that structures data as a list.
 * <p>
 * The list works as a reactive value with regards to its structure. A
 * {@link Computation} will get a dependency on this list for any read operation
 * that depends on the list structure, such as querying the length, iterating
 * the list or finding the index of an item. Accessing an item by index does not
 * create a dependency. The <code>Computation</code> is invalidated when items
 * are added, removed, reordered or replaced. It is not invalidated when the
 * contents of an item is updated since all items are expected to be either
 * immutable or reactive values of their own.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NodeList extends NodeFeature implements ReactiveValue {

    private final JsArray<Object> values = JsCollections.array();

    private final ReactiveEventRouter<ListSpliceListener, ListSpliceEvent> eventRouter = new ReactiveEventRouter<ListSpliceListener, ListSpliceEvent>(
            this) {
        @Override
        protected ListSpliceListener wrap(ReactiveChangeListener listener) {
            return listener::onChange;
        }

        @Override
        protected void dispatchEvent(ListSpliceListener listener,
                ListSpliceEvent event) {
            listener.onSplice(event);
        }
    };

    /**
     * Creates a new list.
     *
     * @param id
     *            the id of the list
     * @param node
     *            the node of the list
     */
    public NodeList(int id, StateNode node) {
        super(id, node);
    }

    /**
     * Gets the number of items in this list.
     *
     * @return the number of items
     */
    public int length() {
        eventRouter.registerRead();
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
     * Shorthand for adding the given item at the given index. This method
     * delegates to {@link #splice(int, int, JsArray)} which updates the list
     * contents and fires the appropriate event.
     *
     * @param index
     *            the index where the item should be added
     * @param item
     *            the new item to add
     */
    public void add(int index, Object item) {
        splice(index, 0, JsCollections.array(item));
    }

    /**
     * Removes a number of items at the given index. This causes a
     * {@link ListSpliceEvent} to be fired.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     */
    public void splice(int index, int remove) {
        JsArray<Object> removed = values.splice(index, remove);
        eventRouter.fireEvent(new ListSpliceEvent(this, index, removed,
                JsCollections.array()));
    }

    /**
     * Removes and adds a number of items at the given index.
     * <p>
     * This causes a {@link ListSpliceEvent} to be fired.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            an array of new items to add
     */
    public final void splice(int index, int remove, JsArray<?> add) {
        @SuppressWarnings("unchecked")
        JsArray<Object> addObject = (JsArray<Object>) add;
        JsArray<Object> removed = values.spliceArray(index, remove, addObject);
        eventRouter.fireEvent(new ListSpliceEvent(this, index, removed, add));
    }

    @Override
    public JsonValue getDebugJson() {
        JsonArray json = Json.createArray();

        for (int i = 0; i < values.length(); i++) {
            Object value = values.get(i);
            JsonValue jsonValue = getAsDebugJson(value);

            json.set(json.length(), jsonValue);
        }

        return json;
    }

    /**
     * Adds a listener that will be notified when the list structure changes.
     *
     * @param listener
     *            the list change listener
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addSpliceListener(ListSpliceListener listener) {
        return eventRouter.addListener(listener);
    }

    @Override
    public EventRemover addReactiveChangeListener(
            ReactiveChangeListener listener) {
        return eventRouter.addReactiveListener(listener);
    }
}

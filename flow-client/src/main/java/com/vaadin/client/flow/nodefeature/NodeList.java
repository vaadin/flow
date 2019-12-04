/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import java.util.function.Function;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.ReactiveEventRouter;
import com.vaadin.client.flow.reactive.ReactiveValue;
import com.vaadin.client.flow.reactive.ReactiveValueChangeListener;

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
 * @author Vaadin Ltd
 * @since 1.0
 */
public class NodeList extends NodeFeature implements ReactiveValue {

    private final JsArray<Object> values = JsCollections.array();

    private boolean hasBeenCleared;

    private final ReactiveEventRouter<ListSpliceListener, ListSpliceEvent> eventRouter = new ReactiveEventRouter<ListSpliceListener, ListSpliceEvent>(
            this) {
        @Override
        protected ListSpliceListener wrap(
                ReactiveValueChangeListener reactiveValueChangeListener) {
            return reactiveValueChangeListener::onValueChange;
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
                JsCollections.array(), false));
    }

    /**
     * Removes all the nodes from the list. This causes a
     * {@link ListSpliceEvent} to be fired, with
     * {@link ListSpliceEvent#isClear()} as <code>true</code>.
     */
    public void clear() {
        hasBeenCleared = true;
        JsArray<Object> removed = values.splice(0, values.length());
        eventRouter.fireEvent(new ListSpliceEvent(this, 0, removed,
                JsCollections.array(), true));
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
        eventRouter.fireEvent(
                new ListSpliceEvent(this, index, removed, add, false));
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

    @Override
    public JsonValue convert(Function<Object, JsonValue> converter) {
        JsonArray json = Json.createArray();

        for (int i = 0; i < values.length(); i++) {
            Object value = values.get(i);
            // Crazy cast since otherwise SDM fails
            // for primitives values since primitives are not a JSO
            json.set(json.length(),
                    WidgetUtil.crazyJsoCast(converter.apply(value)));
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
    public EventRemover addReactiveValueChangeListener(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        return eventRouter.addReactiveListener(reactiveValueChangeListener);
    }

    /**
     * Iterates all values in this list.
     *
     * @param callback
     *            the callback to invoke for each value
     */
    public void forEach(ForEachCallback<Object> callback) {
        eventRouter.registerRead();
        values.forEach(callback);
    }

    /**
     * Returns {@code true} if the list instance has been cleared at some point.
     *
     * @return {@code true} if the list instance has been cleared
     */
    public boolean hasBeenCleared() {
        return hasBeenCleared;
    }
}

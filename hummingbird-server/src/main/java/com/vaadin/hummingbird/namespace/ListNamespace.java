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

package com.vaadin.hummingbird.namespace;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

/**
 * A state node namespace that structures data as a list.
 *
 * @since
 * @author Vaadin Ltd
 * @param <T>
 *            the type of the items in the list
 */
public abstract class ListNamespace<T extends Serializable> extends Namespace {

    private List<T> values = new ArrayList<>();

    private List<ListSpliceChange> changes = new ArrayList<>();

    private boolean nodeValues;

    /**
     * Creates a new list namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     * @param nodeValues
     *            <code>true</code> if this list contains child nodes;
     *            <code>false</code> if this list contains primitive value
     */
    public ListNamespace(StateNode node, boolean nodeValues) {
        super(node);
        this.nodeValues = nodeValues;
    }

    /**
     * Gets the number of items in this namespace.
     *
     * @return the number of items
     */
    public int size() {
        setAccessed();
        return values.size();
    }

    /**
     * Gets the item at the given index.
     *
     * @param index
     *            the of the desired item
     * @return the item at the given index
     */
    protected T get(int index) {
        setAccessed();
        return values.get(index);
    }

    /**
     * Adds an item to the end of the list.
     *
     * @param item
     *            the item to add
     */
    protected void add(T item) {
        add(values.size(), item);
    }

    /**
     * Inserts an item at the given index of the list.
     *
     * @param index
     *            index to insert at
     * @param item
     *            the item to insert
     */
    protected void add(int index, T item) {
        assert item == null || (item instanceof StateNode == nodeValues);

        if (nodeValues) {
            attachPotentialChild(item);
        }
        values.add(index, item);

        addChange(new ListSpliceChange(this, index, 0,
                Collections.singletonList(item)));
    }

    /**
     * Removes the item at the given index.
     *
     * @param index
     *            index of the item to remove
     */
    public void remove(int index) {
        Object removed = values.remove(index);
        detatchPotentialChild(removed);

        addChange(
                new ListSpliceChange(this, index, 1, Collections.emptyList()));
    }

    private void addChange(ListSpliceChange change) {
        getNode().markAsDirty();

        // XXX combine with previous changes if possible
        changes.add(change);

        // TODO Fire some listeners
    }

    private void setAccessed() {
        // TODO Set up listener if we're in a computation
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        changes.forEach(collector);
        changes.clear();
    }

    @Override
    public void resetChanges() {
        changes.clear();
        if (!values.isEmpty()) {
            changes.add(
                    new ListSpliceChange(this, 0, 0, new ArrayList<>(values)));
        }
    }

    /**
     * Checks whether this list contains node values.
     *
     * @return <code>true</code> if this list contains node values;
     *         <code>false</code> if this list contains primitive values
     */
    public boolean isNodeValues() {
        return nodeValues;
    }
}

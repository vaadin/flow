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

package com.vaadin.hummingbird.nodefeature;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListAddChange;
import com.vaadin.hummingbird.change.ListRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeFeatureChange;

/**
 * A state node feature that structures data as a list.
 * <p>
 * Should not be used directly, use one of the extending classes instead, which
 * provide a type safe API while ensuring the list is {@link Serializable}.
 *
 * @author Vaadin Ltd
 * @param <T>
 *            the type of the items in the list
 */
public abstract class NodeList<T extends Serializable> extends NodeFeature {

    /**
     * Provides access to a {@link NodeList} as a {@link Set}.
     *
     * @param <T>
     *            the type of objects in the list (and set)
     */
    protected abstract static class SetView<T extends Serializable>
            extends AbstractSet<T> implements Serializable {

        private NodeList<T> nodeList;

        /**
         * Creates a new view for the given list.
         *
         * @param nodeList
         *            the list to wrap
         */
        public SetView(NodeList<T> nodeList) {
            this.nodeList = nodeList;
        }

        @Override
        public int size() {
            return nodeList.size();
        }

        @Override
        public void clear() {
            nodeList.clear();
        }

        @Override
        public boolean add(T o) {
            validate(o);
            if (contains(o)) {
                return false;
            }

            nodeList.add(size(), o);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            // Uses iterator() which supports proper remove()
            return super.remove(o);
        }

        protected abstract void validate(T o);

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o) {
            return nodeList.indexOf((T) o) != -1;
        }

        @Override
        public Iterator<T> iterator() {
            return nodeList.iterator();
        }
    }

    private List<T> values;

    private ListRemoveChange removeChange;

    /**
     * Creates a new list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    protected NodeList(StateNode node) {
        super(node);
    }

    /**
     * Gets the number of items in this list.
     *
     * @return the number of items
     */
    protected int size() {
        setAccessed();
        if (values == null) {
            return 0;
        }
        return values.size();
    }

    private void ensureValues() {
        if (values == null) {
            values = new ArrayList<>();
        }
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
        if (values == null) {
            throw new IndexOutOfBoundsException();
        }
        return values.get(index);
    }

    /**
     * Adds an item to the end of the list.
     *
     * @param item
     *            the item to add
     */
    protected void add(T item) {
        ensureValues();
        add(values.size(), item);
    }

    /**
     * Adds all provided items to the end of the list.
     *
     * @param items
     *            a collection of items to add, not null
     */
    protected void addAll(Collection<? extends T> items) {
        assert items != null;
        assert items.stream().filter(item -> indexOf(item) != -1)
                .count() == 0 : "Cannot add duplicates to NodeList";
        if (items.isEmpty()) {
            return;
        }

        ArrayList<? extends T> itemsList = new ArrayList<>(items);

        ensureValues();

        int startIndex = values.size();
        values.addAll(itemsList);

        addAddChange(
                new ListAddChange(this, isNodeValues(), startIndex, itemsList));
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
        ensureValues();
        values.add(index, item);
        ArrayList<T> list = new ArrayList<>();
        list.add(item);
        addAddChange(new ListAddChange(this, isNodeValues(), index, list));
    }

    /**
     * Removes the item at the given index.
     *
     * @param index
     *            index of the item to remove
     * @return the element previously at the specified position
     */
    protected T remove(int index) {
        if (values == null) {
            throw new IndexOutOfBoundsException();
        }

        T removed = values.remove(index);
        addRemoveChange(index);

        if (values.isEmpty()) {
            values = null;
        }
        return removed;
    }

    /**
     * Gets or creates the list used to track changes that should be sent to the
     * client.
     * <p>
     * Protected for testing purposes.
     *
     * @return the list to track changes in
     */
    protected ArrayList<NodeFeatureChange> getChangeTracker() {
        return getNode().getChangeTracker(this, ArrayList::new);
    }

    private void addAddChange(ListAddChange<T> change) {
        getNode().markAsDirty();

        // TODO combine with previous additions (good luck with that)
        getChangeTracker().add(change);

        // TODO Fire some listeners
    }

    private void addRemoveChange(int removedItemIndex) {
        getNode().markAsDirty();

        ArrayList<NodeFeatureChange> previousChanges = getChangeTracker();

        // check if the removed item has been added in an add operation not yet
        // collected and can be ignored
        ListAddChange<?> addChangeWithDiscardedItem = null;
        for (NodeFeatureChange change : previousChanges) {
            ListAddChange<?> previousChange = (ListAddChange<?>) change;
            if (addChangeWithDiscardedItem == null) {
                if (previousChange.checkAndDiscardRemovedItem()) {
                    addChangeWithDiscardedItem = previousChange;
                }
            } else {
                // update later add operation indices since indexing has changed
                previousChange.adjustIndex(removedItemIndex);
            }
        }

        // the remove was for recently added item that was discarded
        if (addChangeWithDiscardedItem != null) {
            // remove possibly empty add
            if (addChangeWithDiscardedItem.isEmpty()) {
                previousChanges.remove(addChangeWithDiscardedItem);
            }
            return;
        }

        // the remove was for a item that is in client side

        // update add operations' indices because remove operation is run before
        // adds in the client side
        int adjustedRemovedItemIndex = removedItemIndex;
        previousChanges
                .forEach(previousChange -> ((ListAddChange<?>) previousChange)
                        .adjustIndex(removedItemIndex));

        // update remove operation index to what list was before any current
        // additions
        for (NodeFeatureChange change : previousChanges) {
            ListAddChange<?> previousChange = (ListAddChange<?>) change;
            adjustedRemovedItemIndex -= previousChange
                    .getNumberOfNewItemsBeforeIndex(removedItemIndex);
        }

        if (removeChange == null) {
            removeChange = new ListRemoveChange(this);
        }
        removeChange.add(Integer.valueOf(adjustedRemovedItemIndex));

        // TODO Fire some listeners
    }

    private void setAccessed() {
        // TODO Set up listener if we're in a computation
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {

        ArrayList<NodeFeatureChange> changes = getChangeTracker();
        if (removeChange != null) {
            changes.add(0, removeChange);
            removeChange = null;
        }
        assert changes.stream()
                .filter(change -> change instanceof ListRemoveChange)
                .count() <= 1 : "There should be at most only one ListRemoveChange";

        changes.forEach(collector);
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
    }

    @Override
    public void generateChangesFromEmpty() {
        if (values != null) {
            assert !values.isEmpty();
            addAddChange(new ListAddChange<>(this, isNodeValues(), 0,
                    new ArrayList<>(values)));
        }
    }

    /**
     * Checks whether this list contains node values.
     *
     * @return <code>true</code> if this list contains node values;
     *         <code>false</code> if this list contains primitive values
     */
    protected boolean isNodeValues() {
        return false;
    }

    /**
     * Removes all items.
     */
    protected void clear() {
        if (values == null || values.isEmpty()) {
            return;
        }

        // clear any previous removes and re-add by removing everything in the
        // current list, all add operations will be discarded
        removeChange = null;

        while (values != null) {
            remove(values.size() - 1);
        }
    }

    /**
     * Gets the position of a value in the list.
     *
     * @param value
     *            the value to look for
     * @return the position in the list or -1 if not found
     */
    public int indexOf(T value) {
        setAccessed();
        if (values == null) {
            return -1;
        }
        return values.indexOf(value);
    }

    /**
     * Gets an iterator returning all items in this list.
     *
     * @return an iterator returning all items
     */
    protected Iterator<T> iterator() {
        if (values == null) {
            return Collections.<T> emptyList().iterator();
        }
        Iterator<T> arrayIterator = values.iterator();
        return new Iterator<T>() {
            int index = -1;

            @Override
            public boolean hasNext() {
                return arrayIterator.hasNext();
            }

            @Override
            public T next() {
                index++;
                return arrayIterator.next();
            }

            @Override
            public void remove() {
                arrayIterator.remove();
                addRemoveChange(index);
                index--;
            }
        };
    }

}

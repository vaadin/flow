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

package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.AbstractListChange;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListClearChange;
import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;

/**
 * A state node feature that structures data as a list.
 * <p>
 * Should not be used directly, use one of the extending classes instead, which
 * provide a type safe API while ensuring the list is {@link Serializable}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the items in the list
 */
public abstract class NodeList<T extends Serializable> extends NodeFeature {

    private final class NodeListIterator implements Iterator<T> {
        private int index = -1;
        private T current;
        private Iterator<T> arrayIterator = values.iterator();

        @Override
        public boolean hasNext() {
            return arrayIterator.hasNext();
        }

        @Override
        public T next() {
            index++;
            current = arrayIterator.next();
            return current;
        }

        @Override
        public void remove() {
            arrayIterator.remove();
            addChange(new ListRemoveChange<>(NodeList.this, index, current));
            index--;
        }
    }

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

    private boolean isRemoveAllCalled;

    private boolean isPopulated;

    /**
     * Creates a new list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    protected NodeList(StateNode node) {
        super(node);
        isPopulated = !node.isReportedFeature(getClass());
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
            values = new ArrayList<>(1);
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
        if (items.isEmpty()) {
            return;
        }

        List<? extends T> itemsList = new ArrayList<>(items);

        ensureValues();

        int startIndex = values.size();
        values.addAll(itemsList);

        addChange(new ListAddChange<>(this, isNodeValues(), startIndex,
                itemsList));
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

        addChange(new ListAddChange<>(this, isNodeValues(), index,
                Collections.singletonList(item)));
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

        addChange(new ListRemoveChange<>(this, index, removed));

        if (values.isEmpty()) {
            values = null;
        }
        return removed;
    }

    /**
     * Gets or creates the list used to track changes that should be sent to the
     * client.
     * <p>
     * This method is non-private for testing purposes.
     *
     * @return the list to track changes in
     */
    protected List<AbstractListChange<T>> getChangeTracker() {
        return getNode().getChangeTracker(this, ArrayList::new);
    }

    private void addChange(AbstractListChange<T> change) {
        getNode().markAsDirty();

        // XXX combine with previous changes if possible
        getChangeTracker().add(change);

        // TODO Fire some listeners
    }

    private void setAccessed() {
        // TODO Set up listener if we're in a computation
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        boolean hasRemoveAll = false;

        // This map contains items wrapped by AbstractListChanges as keys and
        // index in the following allChanges list as a value (it allows to get
        // AbstractListChange by the index)
        Map<Object, Integer> indices = new IdentityHashMap<>();

        // This list contains all changes in the tracker. These changes will be
        // modified: each "remove" change following by a corresponding "add"
        // will be replaced by null and "add" will be adjusted. Indeces in
        // changes in between will be adjusted
        List<AbstractListChange<T>> allChanges = new ArrayList<>();
        int index = 0;
        for (AbstractListChange<T> change : getChangeTracker()) {
            if (change instanceof ListRemoveChange<?>) {
                // the remove change => find an appropriate "add" event, adjust
                // it and adjust everything in between
                adjustChanges(index, (ListRemoveChange<T>) change, indices,
                        allChanges);
            } else if (change instanceof ListAddChange<?>) {
                allChanges.add(change);
                int i = index;
                // put all items into "indices" as keys and "index" as a value
                // so that the "change" can be retrieved from the "allChanges"
                // by the index
                ((ListAddChange<T>) change).getNewItems()
                        .forEach(item -> indices.put(item, i));
            } else if (change instanceof ListClearChange<?>) {
                hasRemoveAll = true;
                allChanges.clear();
                indices.clear();
                index = 0;
                allChanges.add(change);
            } else {
                assert false : "AbstractListChange has only three subtypes: add, remove and clear";
            }
            index++;
        }

        List<AbstractListChange<T>> changes;

        if (isRemoveAllCalled && !hasRemoveAll) {
            changes = Stream
                    .concat(Stream.of(new ListClearChange<>(this)),
                            allChanges.stream())
                    .filter(this::acceptChange).collect(Collectors.toList());
        } else {
            changes = allChanges.stream().filter(this::acceptChange)
                    .collect(Collectors.toList());
        }

        isRemoveAllCalled = false;

        if (isPopulated) {
            changes.forEach(collector);
        } else {
            if (changes.isEmpty()) {
                collector.accept(new EmptyChange(this));
            } else {
                changes.forEach(collector);
            }
            isPopulated = true;
        }
    }

    private boolean acceptChange(AbstractListChange<T> change) {
        if (change == null) {
            return false;
        }
        if (change instanceof ListAddChange<?>) {
            return !((ListAddChange<?>) change).isEmpty();
        }
        return true;
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
    }

    @Override
    public void generateChangesFromEmpty() {
        if (values != null) {
            assert !values.isEmpty();
            getChangeTracker().add(new ListAddChange<>(this, isNodeValues(), 0,
                    new ArrayList<>(values)));
        } else if (!isPopulated) {
            // make change tracker available so that an empty change can be
            // reported
            getChangeTracker();
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
     * Removes all nodes, including those not known by the server.
     */
    protected void clear() {
        if (values != null) {
            values.clear();
            values = null;
        }

        isRemoveAllCalled = true;
        addChange(new ListClearChange<>(this));
    }

    /**
     * Gets the position of a value in the list.
     *
     * @param value
     *            the value to look for
     * @return the position in the list or -1 if not found
     */
    protected int indexOf(T value) {
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
        return new NodeListIterator();
    }

    private void adjustChanges(int removeChangeIndex,
            ListRemoveChange<T> change, Map<Object, Integer> indices,
            List<AbstractListChange<T>> allChanges) {
        T removedItem = change.getRemovedItem();
        // retrieve index of the item (it's supposed to be the index of an Add
        // change)
        Integer addChangeIndex = indices.get(removedItem);
        if (addChangeIndex == null) {
            // if there is no suitable "add" change then just add "remove"
            // change as is
            allChanges.add(change);
        } else {
            // retrieve the "add" change by the index
            AbstractListChange<T> addChange = allChanges.get(addChangeIndex);
            assert addChange instanceof ListAddChange<?>;

            ListAddChange<T> add = (ListAddChange<T>) addChange;
            // "add" change has to be adjusted : we need to remove "removedItem"
            // from it
            int indexToCorrect = add.getNewItems().indexOf(removedItem);
            assert indexToCorrect != -1;
            indexToCorrect += add.getIndex();
            // replace "add" change whose index is "addChangeIndex" with a new
            // change which is the copy of the original one but has newItems
            allChanges.set(addChangeIndex,
                    add.copy(add.getNewItems().stream()
                            .filter(item -> item != removedItem)
                            .collect(Collectors.toList())));

            // now go through all the changes in between "add" and "remove" and
            // reindex them
            for (int i = addChangeIndex + 1; i < removeChangeIndex; i++) {
                AbstractListChange<T> listChange = allChanges.get(i);
                // listChange can be null for handled "removed" changes ( see
                // the code below the cycle)
                if (listChange != null
                        && listChange.getIndex() > indexToCorrect) {
                    // make a copy with the adjusted index in case change has an
                    // index which is greater than index of the discarded item
                    allChanges.set(i,
                            listChange.copy(listChange.getIndex() - 1));
                    // listChange can't be ListRemoveChange actually here since
                    // it has to have the index greater than "indexToCorrect".
                    // But it means that there was a corresponding ListAddChange
                    // which has been already adjusted previously and the
                    // "remove" has been replaced with "null".
                }
            }
            allChanges.add(null);
        }
    }

    @Override
    public void onDetach() {
        if (isPopulated && values == null) {
            isPopulated = false;
        }
    }
}

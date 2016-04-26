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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

/**
 * A state node feature that structures data as a list.
 * <p>
 * Should not be used directly, use one of the extending classes instead, which
 * provide a type safe API while ensuring the list is {@link Serializable}.
 *
 * @since
 * @author Vaadin Ltd
 * @param <T>
 *            the type of the items in the list
 */
public abstract class NodeList<T> extends NodeFeature {

    /**
     * Provides access to a {@link NodeList} as a {@link Set}.
     *
     * @param <T>
     *            the type of objects in the list (and set)
     */
    protected abstract static class SetView<T> extends AbstractSet<T>
            implements Serializable {

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

    private transient List<T> values = new ArrayList<>();

    private transient List<ListSpliceChange> changes = new ArrayList<>();

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
        values.add(index, item);

        addChange(new ListSpliceChange(this, isNodeValues(), index, 0,
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
        T removed = values.remove(index);

        addChange(new ListSpliceChange(this, isNodeValues(), index, 1,
                Collections.emptyList()));
        return removed;
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
    public void forEachChild(Consumer<StateNode> action) {
    }

    @Override
    public void resetChanges() {
        changes.clear();
        if (!values.isEmpty()) {
            changes.add(new ListSpliceChange(this, isNodeValues(), 0, 0,
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
        while (size() > 0) {
            remove(0);
        }
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
        return values.indexOf(value);
    }

    /**
     * Gets an iterator returning all items in this list.
     *
     * @return an iterator returning all items
     */
    protected Iterator<T> iterator() {
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
                addChange(new ListSpliceChange(NodeList.this, isNodeValues(),
                        index, 1, Collections.emptyList()));
                index--;
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        changes = (List<ListSpliceChange>) stream.readObject();
        values = (List<T>) stream.readObject();

        postReadChanges(changes);
        postReadList((List) values);
    }

    /**
     * Called after deserializing the internal value list.
     * <p>
     * Extending classes can override this method to do in-place transformations
     * of the list.
     *
     * @param list
     *            the list which was deserialized
     */
    protected void postReadList(List<Object> list) {
    }

    /**
     * Called after deserializing the internal changes list.
     * <p>
     * Extending classes can override this method to do inplace transformations
     * of the list, if needed.
     *
     * @param list
     *            the list which was deserialized
     */
    protected void postReadChanges(List<ListSpliceChange> list) {
    }

    /**
     * Called before serializing the internal values list.
     * <p>
     * Extending classes can override this method and to do transformations on a
     * copy of the list, if needed. No modifications can be done to the list
     * itself.
     *
     * @return the original list or a modified copy of the list
     */
    protected Serializable preWriteValues(List<T> list) {
        return (Serializable) list;
    }

    /**
     * Called before serializing the internal changes list.
     * <p>
     * Extending classes can override this method and to do transformations on a
     * copy of the list, if needed. No modifications can be done to the list
     * itself.
     *
     * @return the original list or a modified copy of the list
     */
    protected Serializable preWriteChanges(List<ListSpliceChange> list) {
        return (Serializable) list;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();

        stream.writeObject(preWriteChanges(changes));
        stream.writeObject(preWriteValues(values));
    }

}

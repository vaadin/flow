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
package com.vaadin.signals;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.NodeSignal.NodeSignalState;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.SynchronousSignalTree;
import com.vaadin.signals.operations.InsertOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A signal representing a node in a tree structure. The {@link #value()} of a
 * node signal is an immutable object that consists of:
 * <ul>
 * <li>the node's own value</li>
 * <li>the parent node</li>
 * <li>child nodes accessed by order (list children)</li>
 * <li>child nodes accessed by key (map children</li>
 * </ul>
 *
 * A child node is always either a list child or a map child but it cannot have
 * both roles at the same time. The {@link #value()} of a detached node is
 * <code>null</code>.
 * <p>
 * This class does not provide methods for all possible operation you could do
 * with a signal but only for the operations that have some special meaning in
 * the context of a node in a tree. You can use the various <code>as</code>
 * methods to get an instance of that specific type that you can use for
 * applying some specific operation.
 */
public class NodeSignal extends Signal<NodeSignalState> {
    /**
     * The snapshot of the state of a node signal. Gives access to the value and
     * child nodes.
     */
    public static class NodeSignalState {
        private final JsonNode value;
        private final NodeSignal parent;
        private final List<NodeSignal> listChildren;
        private final Map<String, NodeSignal> mapChildren;

        /**
         * Creates a new state snapshot based on the given JSON value, list
         * children and map children.
         *
         * @param value
         *            the JSON value, or <code>null</code> if there is no value
         * @param parent
         *            the parent node, nor <code>null</code> for the value of
         *            the root node
         * @param listChildren
         *            a list of children accessed by order, or an empty list if
         *            there are no list children. Not <code>null</code>.
         * @param mapChildren
         *            a map of children access by key, or an empty map if there
         *            are no map children. Not <code>null</code>.
         */
        public NodeSignalState(JsonNode value, NodeSignal parent,
                List<NodeSignal> listChildren,
                Map<String, NodeSignal> mapChildren) {
            this.value = value;
            this.parent = parent;
            this.listChildren = listChildren;
            this.mapChildren = mapChildren;
        }

        /**
         * Gets the value as the given type.
         *
         * @param <T>
         *            the value type
         * @param valueType
         *            the value type, not <code>null</code>
         * @return the value, or <code>null</code> if there is no value
         */
        public <T> T value(Class<T> valueType) {
            return fromJson(value, valueType);
        }

        /**
         * Gets the parent node of this signal.
         *
         * @return the parent node, or <code>null</code> for the root node
         */
        public NodeSignal parent() {
            return parent;
        }

        /**
         * Gets a list with the children accessed by order.
         *
         * @return a list of children, not <code>null</code>
         */
        public List<NodeSignal> listChildren() {
            return listChildren;
        }

        /**
         * Gets a map of the children accessed by key.
         *
         * @return a map of children, not <code>null</code>
         */
        public Map<String, NodeSignal> mapChildren() {
            return mapChildren;
        }
    }

    /**
     * Creates a new empty node signal that serves as a root for a hierarchical
     * node structure. The signal does not support clustering.
     */
    public NodeSignal() {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES);
    }

    /**
     * Creates a new node signal based on the given tree, node id and validator.
     *
     * @param tree
     *            the tree to use, not <code>null</code>
     * @param id
     *            the node id to use, not <code>null</code>
     * @param validator
     *            the validator to check operations submitted to this singal,
     *            not <code>null</code>
     */
    protected NodeSignal(SignalTree tree, Id id,
            Predicate<SignalCommand> validator) {
        super(tree, id, validator);
    }

    private NodeSignal child(Id id) {
        return new NodeSignal(tree(), id, validator());
    }

    @Override
    protected NodeSignalState extractValue(Data data) {
        if (data == null) {
            return null;
        }
        Id parentId = data.parent();
        return new NodeSignalState(data.value(),
                parentId != null ? child(parentId) : null,
                ListSignal.children(data, this::child),
                MapSignal.children(data, this::child));
    }

    @Override
    protected Object usageChangeValue(Data data) {
        /*
         * Any change in the node changes the value() of a node signal so it's
         * easiest to use the update id to represent the changing of the value.
         */
        return data.lastUpdate();
    }

    /**
     * Creates a value signal backed by the node value of this node. The value
     * of the value signal is the same as {@link NodeSignalState#value(Class)}
     * of this signal. The new signal uses the same validator as this signal.
     *
     * @param <T>
     *            the value type
     * @param valueType
     *            the value type, not <code>null</code>
     * @return this signal as a value signal, not <code>null</code>
     */
    public <T> ValueSignal<T> asValue(Class<T> valueType) {
        return new ValueSignal<>(tree(), id(), validator(), valueType);
    }

    /**
     * Creates a number signal backed by the node value of this node. The value
     * of the number signal is the same as {@link NodeSignalState#value(Class)}
     * of this signal. The new signal uses the same validator as this signal.
     * Accessing the value of the signal will throw an exception if the
     * underlying value is not a JSON number.
     *
     * @return this signal as a number signal, not <code>null</code>
     */
    public NumberSignal asNumber() {
        return new NumberSignal(tree(), id(), validator());
    }

    /**
     * Creates a list signal backed by the list children of this node. The value
     * of the list signal is the same as {@link NodeSignalState#listChildren()}
     * of this signal. The new signal uses the same validator as this signal.
     * Accessing the value of child signal will throw an exception if the
     * underlying value cannot be JSON deserialized as the provided element
     * type.
     *
     * @param <T>
     *            the element type
     * @param elementType
     *            the element type, not <code>null</code>
     * @return this signal as a list signal, not <code>null</code>
     */
    public <T> ListSignal<T> asList(Class<T> elementType) {
        return new ListSignal<>(tree(), id(), validator(), elementType);
    }

    /**
     * Creates a map signal backed by the map children of this node. The value
     * of the map signal is the same as {@link NodeSignalState#mapChildren()} of
     * this signal. The new signal uses the same validator as this
     * signal.Accessing the value of child signal will throw an exception if the
     * underlying value cannot be JSON deserialized as the provided element
     * type.
     *
     * @param <T>
     *            the element type
     * @param elementType
     *            the element type, not <code>null</code>
     * @return this signal as a map signal, not <code>null</code>
     */
    public <T> MapSignal<T> asMap(Class<T> elementType) {
        return new MapSignal<>(tree(), id(), validator(), elementType);
    }

    /**
     * Inserts a new node with the given value as a list node at the given list
     * position. The operation fails if the position is not valid at the time
     * when the operation is processed.
     *
     * @param value
     *            the value to insert
     * @param at
     *            the insert position, not <code>null</code>
     * @return an operation containing a signal for the inserted entry and the
     *         eventual result
     */
    public InsertOperation<NodeSignal> insertChildWithValue(Object value,
            ListPosition at) {
        return submitInsert(new SignalCommand.InsertCommand(Id.random(), id(),
                null, toJson(value), at), this::child);
    }

    /**
     * Inserts a new node with no value as a list node at the given list
     * position. The operation fails if the position is not valid at the time
     * when the operation is processed.
     *
     * @param at
     *            the insert position, not <code>null</code>
     * @return an operation containing a signal for the inserted entry and the
     *         eventual result
     */
    public InsertOperation<NodeSignal> insertChild(ListPosition at) {
        return insertChildWithValue(null, at);
    }

    /**
     * Associates the given value with the given key. If a map child already
     * exists for the given key, then the value of that node is updated. If no
     * map child exists, then a new node is created with the given value.
     * <p>
     * Note that this operation does not give direct access to the child signal
     * that was created or updated. Use
     * {@link #putChildWithValue(String, Object)} for that purpose.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @param value
     *            the value to set
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> putChildWithValue(String key, Object value) {
        return submit(new SignalCommand.PutCommand(Id.random(), id(),
                Objects.requireNonNull(key), toJson(value)));
    }

    /**
     * Creates a new node with no value if a map node with the given key doesn't
     * already exist. The returned operation has a reference to a signal that
     * corresponds to the given key regardless of whether a node existed for the
     * key. The operation will be resolved as successful regardless of whether
     * the key was already used.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @return an operation containing a signal for the entry and the eventual
     *         result
     */
    public InsertOperation<NodeSignal> putChildIfAbsent(String key) {
        return submitInsert(new SignalCommand.PutIfAbsentCommand(Id.random(),
                id(), null, Objects.requireNonNull(key), null), this::child);
    }

    /**
     * Adopts the given node as a list child at the given location. The node
     * must be a member of the same node tree. It will be detached from its
     * current location in the tree. The operation fails if the position is not
     * valid at the time when the operation is processed.
     *
     * @param node
     *            the signal to adopt, not <code>null</code>
     * @param at
     *            the target list location, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> adoptAt(Signal<?> node, ListPosition at) {
        return submit(new SignalCommand.AdoptAtCommand(Id.random(), id(),
                node.id(), Objects.requireNonNull(at)));
    }

    /**
     * Adopts the given node as a map child with the given key. The node must be
     * a member of the same node tree. It will be detached from its current
     * location in the tree. The operation fails if there is already a map child
     * with the same key at the time when the operation is processed.
     *
     * @param signal
     *            the signal to adopt, not <code>null</code>
     * @param key
     *            the key to use, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> adoptAs(Signal<?> signal, String key) {
        return submit(new SignalCommand.AdoptAsCommand(Id.random(), id(),
                signal.id(), Objects.requireNonNull(key)));
    }

    /**
     * Removes the given child from this node. The operation fails if the child
     * is not a child of this node at the time when the operation is processed.
     *
     * @param child
     *            the child to remove, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> removeChild(NodeSignal child) {
        // Override to make public
        return super.remove(child);
    }

    /**
     * Removes the map child with the given key. The operation will be resolved
     * as successful if a mapping existed and as a failure if there was no
     * mapping when the operation was processed.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> removeChild(String key) {
        return submit(new SignalCommand.RemoveByKeyCommand(Id.random(), id(),
                Objects.requireNonNull(key)));
    }

    /**
     * Removes all list children and map children from this node.
     *
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> clear() {
        // Override to make public
        return super.clear();
    }

    /**
     * Wraps this signal with a validator. The validator is used to check all
     * value changing commands issued through the new signal instance and all
     * child signals. If this signal has a validator, then the new signal will
     * use both validators.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @param validator
     *            the validator to use, not <code>null</code>
     * @return a new node signal that uses the validator, not <code>null</code>
     */
    public NodeSignal withValidator(Predicate<SignalCommand> validator) {
        return new NodeSignal(tree(), id(), mergeValidators(validator));
    }

    /**
     * Wraps this signal to not accept changes. Child node signals retrieved
     * through the wrapped signal will also not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly node signal, not <code>null</code>
     */
    public NodeSignal asReadonly() {
        return withValidator(anything -> false);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof NodeSignal other
                && Objects.equals(tree(), other.tree())
                && Objects.equals(id(), other.id())
                && Objects.equals(validator(), other.validator());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree(), id(), validator());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("NodeSignal[");

        NodeSignalState value = peek();
        if (value != null) {
            boolean needsComma = false;

            if (value.value != null) {
                builder.append("value: ").append(value.value);
                needsComma = true;
            }

            if (!value.listChildren.isEmpty()) {
                if (needsComma) {
                    builder.append(", ");
                }
                builder.append("listChildren: ")
                        .append(value.listChildren.stream()
                                .map(NodeSignal::toString)
                                .collect(Collectors.joining(", ", "[", "]")));
                needsComma = true;
            }

            if (!value.mapChildren.isEmpty()) {
                if (needsComma) {
                    builder.append(", ");
                }
                builder.append("mapChildren: ").append(value.mapChildren
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + entry.getValue())
                        .collect(Collectors.joining(", ", "[", "]")));
            }
        }

        builder.append(']');
        return builder.toString();
    }
}

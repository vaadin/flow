/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.signals.shared;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.Node.Data;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.function.TransactionTask;
import com.vaadin.flow.signals.operations.InsertOperation;
import com.vaadin.flow.signals.operations.SignalOperation;
import com.vaadin.flow.signals.shared.impl.SignalTree;
import com.vaadin.flow.signals.shared.impl.SynchronousSignalTree;

/**
 * A signal containing a list of values. Supports atomic updates to the list
 * structure. Each value in the list is accessed as a separate
 * {@link SharedValueSignal} instance which enables atomic updates to the value
 * of that list entry.
 *
 * @param <T>
 *            the element type
 */
public class SharedListSignal<T extends @Nullable Object>
        extends AbstractSignal<@NonNull List<SharedValueSignal<T>>> {

    /**
     * A list insertion position before and/or after the referenced entries. If
     * both entries are defined, then this position represents an exact match
     * that is valid only if the two entries are adjacent. If only one is
     * defined, then the position is relative to only that position. A position
     * with neither reference is not valid for inserts but it is valid to test a
     * parent-child relationship regardless of the child position.
     * {@link Id#ZERO} represents the edge of the list, i.e. the first or the
     * last position.
     *
     * @param after
     *            id of the node to insert immediately after, or
     *            <code>null</code> to not define a constraint
     * @param before
     *            id of the node to insert immediately before, or
     *            <code>null</code> to not define a constraint
     */
    public record ListPosition(@Nullable Id after, @Nullable Id before) {
        /**
         * Gets the insertion position that corresponds to the beginning of the
         * list.
         *
         * @return a list position for the beginning of the list, not
         *         <code>null</code>
         */
        public static ListPosition first() {
            // After edge
            return new ListPosition(Id.EDGE, null);
        }

        /**
         * Gets the insertion position that corresponds to the end of the list.
         *
         * @return a list position for the end of the list, not
         *         <code>null</code>
         */
        public static ListPosition last() {
            // Before edge
            return new ListPosition(null, Id.EDGE);
        }

        /**
         * Gets the insertion position immediately after the given signal.
         * Inserting after <code>null</code> is interpreted as inserting after
         * the start of the list, i.e. as the first child.
         *
         * @param after
         *            the signal to insert after, or <code>null</code> to insert
         *            first
         * @return a list position after the given signal, not <code>null</code>
         */
        public static ListPosition after(@Nullable AbstractSignal<?> after) {
            return new ListPosition(idOf(after), null);
        }

        /**
         * Gets the insertion position immediately before the given signal.
         * Inserting before <code>null</code> signal is interpreted as inserting
         * before the end of the list, i.e. as the last child.
         *
         * @param before
         *            the signal to insert before, or <code>null</code> to
         *            insert last
         * @return a list position after the given signal, not <code>null</code>
         */
        public static ListPosition before(@Nullable AbstractSignal<?> before) {
            return new ListPosition(null, idOf(before));
        }

        /**
         * Gets the insertion position between the given signals, assuming those
         * signals are currently adjacent. Inserting after <code>null</code> is
         * interpreted as inserting after the start of the list, i.e. as the
         * first child. Inserting before <code>null</code> signal is interpreted
         * as inserting before the end of the list, i.e. as the last child.
         *
         * @param after
         *            the signal to insert after, or <code>null</code> to insert
         *            first
         * @param before
         *            the signal to insert before, or <code>null</code> to
         *            insert last
         * @return a list position between the given signals, not
         *         <code>null</code>
         */
        public static ListPosition between(@Nullable AbstractSignal<?> after,
                @Nullable AbstractSignal<?> before) {
            return new ListPosition(idOf(after), idOf(before));
        }

        private static Id idOf(@Nullable AbstractSignal<?> signal) {
            if (signal == null) {
                return Id.EDGE;
            } else {
                return signal.id();
            }
        }
    }

    private final Class<T> elementType;

    /**
     * Creates a new list signal with the given element type. The signal does
     * not support clustering.
     *
     * @param elementType
     *            the element type, not <code>null</code>
     */
    public SharedListSignal(Class<T> elementType) {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES,
                elementType);
    }

    /**
     * Creates a new list signal instance with the given id and validator for
     * the given signal tree with the given element type.
     *
     * @param tree
     *            the signal tree that contains the value for this signal, not
     *            <code>null</code>
     * @param id
     *            the id of the signal node within the signal tree, not
     *            <code>null</code>
     * @param validator
     *            the validator to check operations submitted to this singal,
     *            not <code>null</code>
     * @param elementType
     *            the element type, not <code>null</code>
     */
    protected SharedListSignal(SignalTree tree, Id id,
            CommandValidator validator, Class<T> elementType) {
        super(tree, id, validator);
        this.elementType = Objects.requireNonNull(elementType);
    }

    @Override
    public List<SharedValueSignal<T>> get() {
        return Objects.requireNonNull(super.get());
    }

    @Override
    public List<SharedValueSignal<T>> peek() {
        return Objects.requireNonNull(super.peek());
    }

    @Override
    public List<SharedValueSignal<T>> peekConfirmed() {
        return Objects.requireNonNull(super.peekConfirmed());
    }

    private SharedValueSignal<T> child(Id childId) {
        return new SharedValueSignal<T>(tree(), childId, validator(),
                elementType);
    }

    @Override
    protected List<SharedValueSignal<T>> extractValue(@Nullable Data data) {
        if (data == null) {
            return List.of();
        } else {
            return children(data, this::child);
        }
    }

    @Override
    protected Object usageChangeValue(Data data) {
        return data.listChildren();
    }

    /**
     * Inserts a value as the first entry in this list.
     *
     * @param value
     *            the value to insert
     * @return an operation containing a signal for the inserted entry and the
     *         eventual result
     */
    public InsertOperation<SharedValueSignal<T>> insertFirst(T value) {
        return insertAt(value, ListPosition.first());
    }

    /**
     * Helper method to convert a list of child node IDs to a list of signal
     * instances using the provided factory function.
     *
     * @param <T>
     *            the signal type
     * @param node
     *            the node data containing the list of child IDs
     * @param factory
     *            the factory function to create signal instances from IDs
     * @return a list of signal instances, not <code>null</code>
     */
    static <T extends Signal<?>> List<T> children(Data node,
            SerializableFunction<Id, T> factory) {
        return node.listChildren().stream().map(factory).toList();
    }

    /**
     * Inserts a value as the last entry in this list.
     *
     * @param value
     *            the value to insert
     * @return an operation containing a signal for the inserted entry and the
     *         eventual result
     */
    public InsertOperation<SharedValueSignal<T>> insertLast(T value) {
        return insertAt(value, ListPosition.last());
    }

    /**
     * Inserts a value at the given position in this list. The operation fails
     * if the position is not valid at the time when the operation is processed.
     *
     * @param value
     *            the value to insert
     * @param at
     *            the insert position, not <code>null</code>
     * @return an operation containing a signal for the inserted entry and the
     *         eventual result
     */
    public InsertOperation<SharedValueSignal<T>> insertAt(T value,
            ListPosition at) {
        return submitInsert(
                new SignalCommand.InsertCommand(Id.random(), id(), null,
                        toJson(value), Objects.requireNonNull(at)),
                this::child);
    }

    /**
     * Moves the given child signal to the given position in this list. The
     * operation fails if the child is not a child or if this list of if
     * position is not valid at the time when the operation is processed.
     *
     * @param child
     *            the child signal to move, not <code>null</code>
     * @param to
     *            the position to move to, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> moveTo(AbstractSignal<T> child,
            ListPosition to) {
        var verifyChild = new SignalCommand.PositionCondition(Id.random(), id(),
                child.id(), new ListPosition(null, null));
        var adopt = new SignalCommand.AdoptAtCommand(Id.random(), id(),
                child.id(), Objects.requireNonNull(to));

        return submit(new SignalCommand.TransactionCommand(Id.random(),
                List.of(verifyChild, adopt)));
    }

    /**
     * Removes the given child from this list. The operation fails if the child
     * is not a child of this list at the time when the operation is processed.
     *
     * @param child
     *            the child to remove, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> remove(SharedValueSignal<T> child) {
        // Override to make public
        return super.remove(child);
    }

    /**
     * Removes all children from this list. Note that is this list shares data
     * with a {@link SharedNodeSignal} that has map children, then the map
     * children will also be removed.
     *
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> clear() {
        // Override to make public
        return super.clear();
    }

    /**
     * Checks that the given child is at the given position in this list. This
     * operation is only meaningful to use as a condition in a
     * {@link Signal#runInTransaction(TransactionTask) transaction}. The result
     * of the returned operation will be resolved as successful if the given
     * child is a child of this list and at the given position when the
     * operation is processed.
     *
     * @param child
     *            the child to test, not <code>null</code>
     * @param expectedPosition
     *            the expected position of the child, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyPosition(AbstractSignal<?> child,
            ListPosition expectedPosition) {
        return submit(new SignalCommand.PositionCondition(Id.random(), id(),
                child.id(), Objects.requireNonNull(expectedPosition)));
    }

    /**
     * Checks that the given signal is a child in this list. This operation is
     * only meaningful to use as a condition in a
     * {@link Signal#runInTransaction(TransactionTask) transaction}. The result
     * of the returned operation will be resolved as successful if the given
     * child is a child of this list and at the given position when the
     * operation is processed.
     *
     * @param child
     *            the child to look for test, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyChild(AbstractSignal<?> child) {
        return verifyPosition(child, new ListPosition(null, null));
    }

    /**
     * Wraps this signal with a validator. The validator is used to check all
     * value changing commands issued through the new signal instance and all
     * child signals. If this signal has a validator, then the new signal will
     * use both validators. Note that due to the way validators are retained by
     * {@link #asNode()}, there's a possibility that the validator also receives
     * commands that cannot be directly issued for a list signal or its
     * children.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @param validator
     *            the validator to use, not <code>null</code>
     * @return a new list signal that uses the validator, not <code>null</code>
     */
    public SharedListSignal<T> withValidator(CommandValidator validator) {
        return new SharedListSignal<>(tree(), id(), mergeValidators(validator),
                elementType);
    }

    /**
     * Wraps this signal to not accept changes. Child value signals retrieved
     * through the wrapped signal will also not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly list signal, not <code>null</code>
     */
    public SharedListSignal<T> asReadonly() {
        return withValidator(anything -> false);
    }

    public SharedNodeSignal asNode() {
        // Override to make public
        return super.asNode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof SharedListSignal<?> other
                && Objects.equals(tree(), other.tree())
                && Objects.equals(id(), other.id())
                && Objects.equals(validator(), other.validator())
                && Objects.equals(elementType, other.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree(), id(), validator(), elementType);
    }

    @Override
    public String toString() {
        var value = peek();
        if (value == null) {
            return "SharedListSignal[null]";
        }
        return value.stream().map(SharedValueSignal::peek)
                .map(Objects::toString)
                .collect(Collectors.joining(", ", "SharedListSignal[", "]"));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        LoggerFactory.getLogger(SharedListSignal.class).warn(
                "Serializing SharedListSignal. Sharing signals across a cluster is not yet implemented.");
        out.defaultWriteObject();
    }
}

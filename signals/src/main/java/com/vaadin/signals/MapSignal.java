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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.vaadin.signals.function.CommandValidator;
import java.util.stream.Collectors;

import com.vaadin.signals.Node.Data;
import com.vaadin.signals.impl.CommandResult.NodeModification;
import com.vaadin.signals.impl.SignalTree;
import com.vaadin.signals.impl.SynchronousSignalTree;
import com.vaadin.signals.operations.InsertOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A signal containing a map of values with string keys. Supports atomic updates
 * to the map structure. Each value in the map is accessed as a separate
 * {@link ValueSignal} instance which enables atomic updates to the value of
 * that map entry.
 *
 * @param <T>
 *            the element type
 */
public class MapSignal<T> extends AbstractSignal<Map<String, ValueSignal<T>>> {

    private Class<T> elementType;

    /**
     * Creates a new map signal with the given element type. The signal does not
     * support clustering.
     *
     * @param elementType
     *            the element type, not <code>null</code>
     */
    public MapSignal(Class<T> elementType) {
        this(new SynchronousSignalTree(false), Id.ZERO, ANYTHING_GOES,
                elementType);
    }

    /**
     * Creates a new map signal instance with the given id and validator for the
     * given signal tree with the given element type.
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
    protected MapSignal(SignalTree tree, Id id,
            CommandValidator validator, Class<T> elementType) {
        super(tree, id, validator);
        this.elementType = Objects.requireNonNull(elementType);
    }

    private ValueSignal<T> child(Id childId) {
        return new ValueSignal<T>(tree(), childId, validator(), elementType);
    }

    @Override
    protected Map<String, ValueSignal<T>> extractValue(Data data) {
        if (data == null) {
            return Map.of();
        } else {
            return children(data, this::child);
        }
    }

    @Override
    protected Object usageChangeValue(Data data) {
        return data.mapChildren();
    }

    /**
     * Associates the given value with the given key. If an entry already exists
     * for the given key, then that entry is updated. If no entry exists, then a
     * new entry is created.
     * <p>
     * The result of the returned operation will be resolved with the previous
     * value at the time when this operation was confirmed. The previous value
     * is <code>null</code> if no entry existed or if it existed with a
     * <code>null</code> value.
     * <p>
     * Note that this operation does not give direct access to the child signal
     * that was created or updated. Use {@link #putIfAbsent(String, Object)} for
     * that purpose.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @param value
     *            the value to set
     * @return an operation containing the eventual result
     */
    public SignalOperation<T> put(String key, T value) {
        return submit(
                new SignalCommand.PutCommand(Id.random(), id(),
                        Objects.requireNonNull(key), toJson(value)),
                success -> {
                    if (success.updates().size() == 1) {
                        return nodeValue(success.onlyUpdate().oldNode(),
                                elementType);
                    } else {
                        // New node and mapChildren update -> no previous value
                        assert success.updates().size() == 2;
                        return null;
                    }
                });
    }

    /**
     * Creates a new entry with the given value if an entry with the given key
     * doesn't already exist. If an entry exists, then the given value is
     * ignored. The returned operation has a reference to a signal that
     * corresponds to the given key regardless of whether an entry existed for
     * the key. The operation will be resolved as successful regardless of
     * whether they key was already used.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @param value
     *            the value to set
     * @return an operation containing a signal for the entry and the eventual
     *         result
     */
    public InsertOperation<ValueSignal<T>> putIfAbsent(String key, T value) {
        return submitInsert(
                new SignalCommand.PutIfAbsentCommand(Id.random(), id(), null,
                        Objects.requireNonNull(key), toJson(value)),
                this::child);
    }

    /**
     * Removes the mapping for the given key. The operation will be resolved as
     * successful if a mapping existed and as a failure if there was no mapping.
     * In case of a successful operation, the result value will be the value
     * associated with the key when the operation was processed.
     *
     * @param key
     *            the key to use, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<T> remove(String key) {
        return submit(new SignalCommand.RemoveByKeyCommand(Id.random(), id(),
                Objects.requireNonNull(key)), success -> {
                    NodeModification removal = success.updates().values()
                            .stream().filter(update -> update.newNode() == null)
                            .findAny().get();
                    return nodeValue(removal.oldNode(), elementType);
                });
    }

    /**
     * Removes all entries from this map. Note that is this map shares data with
     * a {@link NodeSignal} that has list children, then the list children will
     * also be removed.
     *
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> clear() {
        // Override to make public
        return super.clear();
    }

    private SignalOperation<Void> submitKeyCondition(String key,
            Id expectedChildId) {
        return submit(new SignalCommand.KeyCondition(Id.random(), id(), key,
                expectedChildId));
    }

    /**
     * Checks that the given child is mapped to the given key in this map. This
     * operation is only meaningful to use as a condition in a
     * {@link Signal#runInTransaction(Runnable) transaction}. The result of the
     * returned operation will be resolved as successful if the given child is a
     * mapped to the given key in this map when the operation is processed.
     *
     *
     * @param key
     *            the key to check, not <code>null</code>
     * @param expectedChild
     *            the expected child signal, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyKey(String key,
            AbstractSignal<?> expectedChild) {
        return submitKeyCondition(Objects.requireNonNull(key),
                expectedChild.id());
    }

    /**
     * Checks that there is a mapping for the given key in this map. This
     * operation is only meaningful to use as a condition in a
     * {@link Signal#runInTransaction(Runnable) transaction}. The result of the
     * returned operation will be resolved as successful if the given key has a
     * mapping in this map when the operation is processed.
     *
     * @param key
     *            the key to check, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyHasKey(String key) {
        return submitKeyCondition(key, null);
    }

    /**
     * Checks that there is no mapping for the given key in this map. This
     * operation is only meaningful to use as a condition in a
     * {@link Signal#runInTransaction(Runnable) transaction}. The result of the
     * returned operation will be resolved as successful if the given key has no
     * mapping in this map when the operation is processed.
     *
     * @param key
     *            the key to check, not <code>null</code>
     * @return an operation containing the eventual result
     */
    public SignalOperation<Void> verifyKeyAbsent(String key) {
        return submitKeyCondition(key, Id.ZERO);
    }

    /**
     * Wraps this signal with a validator. The validator is used to check all
     * value changing commands issued through the new signal instance and all
     * child signals. If this signal has a validator, then the new signal will
     * use both validators. Note that due to the way validators are retained by
     * {@link #asNode()}, there's a possibility that the validator also receives
     * commands that cannot be directly issued for a map signal or its children.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @param validator
     *            the validator to use, not <code>null</code>
     * @return a new map signal that uses the validator, not <code>null</code>
     */
    public MapSignal<T> withValidator(CommandValidator validator) {
        return new MapSignal<>(tree(), id(), mergeValidators(validator),
                elementType);
    }

    /**
     * Wraps this signal to not accept changes. Child value signals retrieved
     * through the wrapped signal will also not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly map signal, not <code>null</code>
     */
    public MapSignal<T> asReadonly() {
        return withValidator(anything -> false);
    }

    public NodeSignal asNode() {
        // Override to make public
        return super.asNode();
    }

    /**
     * Helper method to convert a map of child node IDs to a map of signal
     * instances using the provided factory function.
     *
     * @param <T>
     *            the signal type
     * @param node
     *            the node data containing the map of child IDs
     * @param factory
     *            the factory function to create signal instances from IDs
     * @return an unmodifiable map of signal instances, not <code>null</code>
     */
    static <T extends Signal<?>> Map<String, T> children(Data node,
            Function<Id, T> factory) {
        LinkedHashMap<String, T> children = new LinkedHashMap<String, T>();

        node.mapChildren()
                .forEach((key, id) -> children.put(key, factory.apply(id)));

        return Collections.unmodifiableMap(children);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof MapSignal<?> other
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
        return peek().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().value())
                .collect(Collectors.joining(", ", "MapSignal[", "]"));
    }

}

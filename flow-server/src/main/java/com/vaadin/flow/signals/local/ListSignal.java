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
package com.vaadin.flow.signals.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableBiPredicate;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.impl.Transaction;

/**
 * A local list signal that holds a list of writable signals, enabling per-entry
 * reactivity.
 * <p>
 * Local signals are non-serializable and intended for UI-local state only. They
 * do not participate in clustering and are simpler than shared signals.
 * <p>
 * Structural mutations (add, remove, clear) trigger list-level dependents.
 * Entry-level mutations (updating an entry's value) only trigger that entry's
 * dependents.
 * <p>
 * Local list signals can't be used inside signal transactions.
 *
 * @param <T>
 *            the element type
 */
public class ListSignal<T extends @Nullable Object>
        extends AbstractLocalSignal<@NonNull List<ValueSignal<T>>> {

    private final SerializableBiPredicate<T, T> equalityChecker;

    /**
     * Creates a new empty list signal.
     */
    public ListSignal() {
        this(Objects::equals);
    }

    /**
     * Creates a new empty list signal with a custom equality checker.
     * <p>
     * The equality checker is used to determine if a new value is equal to the
     * current value of an entry signal when that entry's value is updated via
     * {@link ValueSignal#set(Object)}. It does <em>not</em> apply when
     * inserting new items into the list (e.g., via
     * {@link #insertLast(Object)}). If the equality checker returns
     * {@code true}, the value update is skipped, and no change notification is
     * triggered, i.e., no dependent effect function is triggered.
     * <p>
     * The equality checker is applied to all {@link ValueSignal} instances
     * created for entries in this list.
     *
     * @param equalityChecker
     *            the predicate used to compare entry values for equality, not
     *            <code>null</code>
     */
    public ListSignal(SerializableBiPredicate<T, T> equalityChecker) {
        super(List.of());
        this.equalityChecker = Objects.requireNonNull(equalityChecker,
                "Equality checker must not be null");
    }

    @Override
    public List<ValueSignal<T>> get() {
        return Objects.requireNonNull(super.get());
    }

    @Override
    public List<ValueSignal<T>> peek() {
        return Objects.requireNonNull(super.peek());
    }

    /**
     * Gets a stream with all values in this signal. This registers a dependency
     * for both the structure of the list and the values of all child signals.
     * 
     * @return a stream of signal values, not <code>null</code>
     */
    public Stream<T> getValues() {
        return get().stream().map(Signal::get);
    }

    /**
     * Gets a stream with all values in this signal without registering any
     * dependencies.
     * 
     * @return a stream of signal values, not <code>null</code>
     */
    public Stream<T> peekValues() {
        return peek().stream().map(Signal::peek);
    }

    @Override
    protected void checkPreconditions() {
        assertLockHeld();
        super.checkPreconditions();

        if (Transaction.inExplicitTransaction()) {
            throw new IllegalStateException(
                    "ListSignal cannot be used inside signal transactions because it can hold a reference to a mutable object that can be mutated directly, bypassing transaction control. Use SharedListSignal instead.");
        }
    }

    /**
     * Inserts a value as the first entry in this list.
     *
     * @param value
     *            the value to insert
     * @return a signal for the inserted entry
     */
    public ValueSignal<T> insertFirst(T value) {
        lock();
        try {
            checkPreconditions();
            return insertAtInternal(0, value);
        } finally {
            unlock();
        }
    }

    /**
     * Inserts a value as the last entry in this list.
     *
     * @param value
     *            the value to insert
     * @return a signal for the inserted entry
     */
    public ValueSignal<T> insertLast(T value) {
        lock();
        try {
            checkPreconditions();
            return insertAtInternal(
                    Objects.requireNonNull(getSignalValue()).size(), value);
        } finally {
            unlock();
        }
    }

    /**
     * Inserts a value at the given index in this list.
     * <p>
     * <b>Note:</b> This method should only be used in non-concurrent cases
     * where the list structure is not being modified by other threads. The
     * index is sensitive to concurrent modifications and may lead to unexpected
     * results if the list is modified between determining the index and calling
     * this method. For concurrent cases, prefer using
     * {@link #insertFirst(Object)} or {@link #insertLast(Object)}.
     *
     * @param index
     *            the index at which to insert (0 for first, size() for last)
     * @param value
     *            the value to insert
     * @return a signal for the inserted entry
     * @throws IndexOutOfBoundsException
     *             if index is negative or greater than size()
     */
    public ValueSignal<T> insertAt(int index, T value) {
        lock();
        try {
            checkPreconditions();
            List<ValueSignal<T>> entries = Objects
                    .requireNonNull(getSignalValue());
            if (index < 0 || index > entries.size()) {
                throw new IndexOutOfBoundsException(
                        "Index: " + index + ", Size: " + entries.size());
            }
            return insertAtInternal(index, value);
        } finally {
            unlock();
        }
    }

    private ValueSignal<T> insertAtInternal(int index, T value) {
        assertLockHeld();
        ValueSignal<T> entry = new ValueSignal<>(value, equalityChecker);
        List<ValueSignal<T>> newEntries = new ArrayList<>(
                Objects.requireNonNull(getSignalValue()));
        newEntries.add(index, entry);
        setSignalValue(Collections.unmodifiableList(newEntries));
        return entry;
    }

    private List<ValueSignal<T>> createSignals(Collection<? extends T> values) {
        List<ValueSignal<T>> signals = new ArrayList<>(values.size());
        for (T value : values) {
            signals.add(new ValueSignal<>(value, equalityChecker));
        }
        return signals;
    }

    /**
     * Inserts all values as the last entries in this list. All entries are
     * added with a single change notification.
     * <p>
     * Individual null values are permitted if the element type allows null.
     *
     * @param values
     *            the values to insert, not <code>null</code>
     * @return an unmodifiable list of signals for the inserted entries
     */
    public List<ValueSignal<T>> insertAllLast(Collection<? extends T> values) {
        Objects.requireNonNull(values, "Values must not be null");
        if (values.isEmpty()) {
            return List.of();
        }
        lock();
        try {
            checkPreconditions();
            return insertAllAtInternal(
                    Objects.requireNonNull(getSignalValue()).size(), values);
        } finally {
            unlock();
        }
    }

    /**
     * Inserts all values as the first entries in this list, preserving the
     * order of the provided collection. All entries are added with a single
     * change notification.
     * <p>
     * Individual null values are permitted if the element type allows null.
     *
     * @param values
     *            the values to insert, not <code>null</code>
     * @return an unmodifiable list of signals for the inserted entries
     */
    public List<ValueSignal<T>> insertAllFirst(Collection<? extends T> values) {
        return insertAllAt(0, values);
    }

    /**
     * Inserts all values at the given index in this list, preserving the order
     * of the provided collection. All entries are added with a single change
     * notification.
     * <p>
     * Individual null values are permitted if the element type allows null.
     * <p>
     * <b>Note:</b> This method should only be used in non-concurrent cases
     * where the list structure is not being modified by other threads. The
     * index is sensitive to concurrent modifications and may lead to unexpected
     * results if the list is modified between determining the index and calling
     * this method.
     *
     * @param index
     *            the index at which to insert (0 for first, size() for last)
     * @param values
     *            the values to insert, not <code>null</code>
     * @return an unmodifiable list of signals for the inserted entries
     * @throws IndexOutOfBoundsException
     *             if index is negative or greater than size()
     */
    public List<ValueSignal<T>> insertAllAt(int index,
            Collection<? extends T> values) {
        Objects.requireNonNull(values, "Values must not be null");
        if (values.isEmpty()) {
            return List.of();
        }
        lock();
        try {
            checkPreconditions();
            List<ValueSignal<T>> currentEntries = Objects
                    .requireNonNull(getSignalValue());
            if (index < 0 || index > currentEntries.size()) {
                throw new IndexOutOfBoundsException(
                        "Index: " + index + ", Size: " + currentEntries.size());
            }
            return insertAllAtInternal(index, values);
        } finally {
            unlock();
        }
    }

    private List<ValueSignal<T>> insertAllAtInternal(int index,
            Collection<? extends T> values) {
        assertLockHeld();
        List<ValueSignal<T>> created = createSignals(values);
        List<ValueSignal<T>> newEntries = new ArrayList<>(
                Objects.requireNonNull(getSignalValue()));
        newEntries.addAll(index, created);
        setSignalValue(Collections.unmodifiableList(newEntries));
        return Collections.unmodifiableList(created);
    }

    /**
     * Removes the given entry from this list. Does nothing if the entry is not
     * in the list.
     *
     * @param entry
     *            the entry to remove
     */
    public void remove(ValueSignal<T> entry) {
        lock();
        try {
            checkPreconditions();
            List<ValueSignal<T>> entries = Objects
                    .requireNonNull(getSignalValue());
            List<ValueSignal<T>> newEntries = entries.stream()
                    .filter(e -> e != entry).toList();
            if (newEntries.size() < entries.size()) {
                setSignalValue(newEntries);
            }
        } finally {
            unlock();
        }
    }

    /**
     * Moves an existing entry to a new position in this list. The same
     * {@code ValueSignal} instance is preserved — no new signal is created.
     * <p>
     * Does nothing if the entry is already at the target index.
     * <p>
     * <b>Note:</b> This method should only be used in non-concurrent cases
     * where the list structure is not being modified by other threads. The
     * index is sensitive to concurrent modifications and may lead to unexpected
     * results if the list is modified between determining the index and calling
     * this method.
     *
     * @param entry
     *            the entry to move
     * @param toIndex
     *            the desired final index (0-based)
     * @throws IllegalArgumentException
     *             if the entry is not in the list
     * @throws IndexOutOfBoundsException
     *             if {@code toIndex} is negative or >= size
     */
    public void moveTo(ValueSignal<T> entry, int toIndex) {
        lock();
        try {
            checkPreconditions();
            List<ValueSignal<T>> entries = Objects
                    .requireNonNull(getSignalValue());
            int fromIndex = entries.indexOf(entry);
            if (fromIndex == -1) {
                throw new IllegalArgumentException("Entry is not in the list");
            }
            if (toIndex < 0 || toIndex >= entries.size()) {
                throw new IndexOutOfBoundsException(
                        "Index: " + toIndex + ", Size: " + entries.size());
            }
            if (fromIndex == toIndex) {
                return;
            }
            List<ValueSignal<T>> newEntries = new ArrayList<>(entries);
            newEntries.remove(fromIndex);
            newEntries.add(toIndex, entry);
            setSignalValue(Collections.unmodifiableList(newEntries));
        } finally {
            unlock();
        }
    }

    /**
     * Removes all entries from this list.
     */
    public void clear() {
        lock();
        try {
            checkPreconditions();
            if (!Objects.requireNonNull(getSignalValue()).isEmpty()) {
                setSignalValue(List.of());
            }
        } finally {
            unlock();
        }
    }

    @Override
    public String toString() {
        return peek().stream().map(ValueSignal::peek).map(Objects::toString)
                .collect(Collectors.joining(", ", "ListSignal[", "]"));
    }

}

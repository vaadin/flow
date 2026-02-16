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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
public class ListSignal<T> extends AbstractLocalSignal<List<ValueSignal<T>>> {

    /**
     * Creates a new empty list signal.
     */
    public ListSignal() {
        super(List.of());
    }

    /**
     * Inserts a value as the first entry in this list.
     *
     * @param value
     *            the value to insert
     * @return a signal for the inserted entry
     */
    public ValueSignal<T> insertFirst(T value) {
        return insertAt(0, value);
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
        ValueSignal<T> entry = new ValueSignal<>(value);
        List<ValueSignal<T>> newEntries = new ArrayList<>(
                Objects.requireNonNull(getSignalValue()));
        newEntries.add(index, entry);
        setSignalValue(Collections.unmodifiableList(newEntries));
        return entry;
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
     * Removes all entries from this list.
     */
    public void clear() {
        lock();
        try {
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

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
package com.vaadin.signals.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.signals.Signal;
import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;

/**
 * A local signal containing a list of values. Supports atomic updates to the
 * list structure. Each value in the list is accessed as a separate
 * {@link ValueSignal} instance which enables atomic updates to the value of
 * that list entry.
 * <p>
 * Local signals are non-serializable and intended for UI-local state only. They
 * do not participate in clustering and are simpler than shared signals.
 * <p>
 * Local list signals can't be used inside signal transactions.
 *
 * @param <T>
 *            the element type
 */
public class ListSignal<T> implements Signal<List<ValueSignal<T>>> {

    private final List<ValueSignal<T>> items = new ArrayList<>();
    private int version;

    private final List<TransientListener> listeners = new ArrayList<>();
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new empty list signal.
     */
    public ListSignal() {
    }

    private void checkPreconditions() {
        assert lock.isHeldByCurrentThread();

        if (Transaction.inTransaction()) {
            throw new IllegalStateException(
                    "Local signals cannot be used inside signal transactions.");
        }
    }

    @Override
    public List<ValueSignal<T>> value() {
        lock.lock();
        try {
            checkPreconditions();

            if (UsageTracker.isActive()) {
                UsageTracker.registerUsage(createUsage(version));
            }

            return Collections.unmodifiableList(new ArrayList<>(items));
        } finally {
            lock.unlock();
        }
    }

    private Usage createUsage(int originalVersion) {
        return new Usage() {
            @Override
            public boolean hasChanges() {
                lock.lock();
                boolean hasChanges = version != originalVersion;
                lock.unlock();

                return hasChanges;
            }

            @Override
            public CleanupCallback onNextChange(TransientListener listener) {
                lock.lock();
                try {
                    if (hasChanges()) {
                        boolean keep = listener.invoke(true);
                        if (!keep) {
                            return () -> {
                            };
                        }
                    }

                    listeners.add(listener);
                    return () -> {
                        lock.lock();
                        try {
                            listeners.remove(listener);
                        } finally {
                            lock.unlock();
                        }
                    };

                } finally {
                    lock.unlock();
                }
            }
        };
    }

    @Override
    public List<ValueSignal<T>> peek() {
        lock.lock();
        try {
            checkPreconditions();

            return Collections.unmodifiableList(new ArrayList<>(items));
        } finally {
            lock.unlock();
        }
    }

    private void notifyListeners() {
        assert lock.isHeldByCurrentThread();

        version++;

        List<TransientListener> copy = List.copyOf(listeners);
        listeners.clear();
        for (var listener : copy) {
            boolean keep = listener.invoke(false);

            if (keep) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Inserts a value at the beginning of this list.
     *
     * @param value
     *            the value to insert
     * @return the signal for the inserted entry
     */
    public ValueSignal<T> insertFirst(T value) {
        return insertAt(0, value);
    }

    /**
     * Inserts a value at the end of this list.
     *
     * @param value
     *            the value to insert
     * @return the signal for the inserted entry
     */
    public ValueSignal<T> insertLast(T value) {
        lock.lock();
        try {
            checkPreconditions();

            ValueSignal<T> signal = new ValueSignal<>(value);
            items.add(signal);
            notifyListeners();
            return signal;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts a value at the given index in this list.
     *
     * @param index
     *            the index at which to insert (0 = first position)
     * @param value
     *            the value to insert
     * @return the signal for the inserted entry
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;
     *             size())
     */
    public ValueSignal<T> insertAt(int index, T value) {
        lock.lock();
        try {
            checkPreconditions();

            ValueSignal<T> signal = new ValueSignal<>(value);
            items.add(index, signal);
            notifyListeners();
            return signal;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the given child signal from this list.
     *
     * @param child
     *            the child to remove, not <code>null</code>
     * @return <code>true</code> if the child was removed, <code>false</code> if
     *         it was not in the list
     */
    public boolean remove(ValueSignal<T> child) {
        Objects.requireNonNull(child);
        lock.lock();
        try {
            checkPreconditions();

            boolean removed = items.remove(child);
            if (removed) {
                notifyListeners();
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the entry at the given index from this list.
     *
     * @param index
     *            the index of the entry to remove
     * @return the signal that was removed
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;=
     *             size())
     */
    public ValueSignal<T> removeAt(int index) {
        lock.lock();
        try {
            checkPreconditions();

            ValueSignal<T> removed = items.remove(index);
            notifyListeners();
            return removed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all entries from this list.
     */
    public void clear() {
        lock.lock();
        try {
            checkPreconditions();

            if (!items.isEmpty()) {
                items.clear();
                notifyListeners();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of entries in this list.
     *
     * @return the size of the list
     */
    public int size() {
        lock.lock();
        try {
            checkPreconditions();

            return items.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the signal at the given index.
     *
     * @param index
     *            the index
     * @return the signal at the given index
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;=
     *             size())
     */
    public ValueSignal<T> get(int index) {
        lock.lock();
        try {
            checkPreconditions();

            return items.get(index);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Moves a child signal to a new position in this list.
     *
     * @param child
     *            the child to move, not <code>null</code>
     * @param toIndex
     *            the target index (after removal of the child from its current
     *            position)
     * @throws IllegalArgumentException
     *             if the child is not in this list
     * @throws IndexOutOfBoundsException
     *             if toIndex is out of range
     */
    public void moveTo(ValueSignal<T> child, int toIndex) {
        Objects.requireNonNull(child);
        lock.lock();
        try {
            checkPreconditions();

            int currentIndex = items.indexOf(child);
            if (currentIndex == -1) {
                throw new IllegalArgumentException(
                        "The given signal is not a child of this list");
            }

            if (currentIndex != toIndex) {
                items.remove(currentIndex);
                items.add(toIndex, child);
                notifyListeners();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the given signal is a child of this list.
     *
     * @param child
     *            the signal to check, not <code>null</code>
     * @return <code>true</code> if the signal is a child of this list,
     *         <code>false</code> otherwise
     */
    public boolean contains(ValueSignal<T> child) {
        Objects.requireNonNull(child);
        lock.lock();
        try {
            checkPreconditions();

            return items.contains(child);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the index of the given child signal in this list.
     *
     * @param child
     *            the signal to find, not <code>null</code>
     * @return the index of the child, or -1 if not found
     */
    public int indexOf(ValueSignal<T> child) {
        Objects.requireNonNull(child);
        lock.lock();
        try {
            checkPreconditions();

            return items.indexOf(child);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder("ListSignal[");
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(Objects.toString(items.get(i).peek()));
            }
            sb.append("]");
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }
}

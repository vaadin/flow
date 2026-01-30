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
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.signals.Signal;
import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;

/**
 * A local list signal that holds a list of {@link ValueSignal} entries.
 * <p>
 * Local signals are non-serializable and intended for UI-local state only. They
 * do not participate in clustering and are simpler than shared signals.
 * <p>
 * Each entry in the list is a {@link ValueSignal} that can be independently
 * observed and modified. This provides per-entry reactivity where changes to
 * one entry don't trigger notifications for other entries.
 * <p>
 * The list structure itself can be observed to detect when entries are added or
 * removed. Modifications to entry values do not trigger list-level change
 * notifications.
 * <p>
 * Local list signals cannot be used inside signal transactions.
 *
 * @param <T>
 *            the element type
 */
public class ListSignal<T> implements Signal<List<ValueSignal<T>>> {

    private final List<ValueSignal<T>> entries = new ArrayList<>();
    private int version;

    private final List<TransientListener> listeners = new ArrayList<>();
    // package-protected for testing
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
                    "ListSignal cannot be used inside signal transactions.");
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

            return Collections.unmodifiableList(new ArrayList<>(entries));
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

            return Collections.unmodifiableList(new ArrayList<>(entries));
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
     * Inserts a value at the beginning of the list.
     *
     * @param value
     *            the value to insert, may be <code>null</code>
     * @return a signal for the inserted entry, not <code>null</code>
     */
    public ValueSignal<T> insertFirst(T value) {
        lock.lock();
        try {
            checkPreconditions();

            ValueSignal<T> entry = new ValueSignal<>(value);
            entries.add(0, entry);
            notifyListeners();
            return entry;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts a value at the end of the list.
     *
     * @param value
     *            the value to insert, may be <code>null</code>
     * @return a signal for the inserted entry, not <code>null</code>
     */
    public ValueSignal<T> insertLast(T value) {
        lock.lock();
        try {
            checkPreconditions();

            ValueSignal<T> entry = new ValueSignal<>(value);
            entries.add(entry);
            notifyListeners();
            return entry;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts a value at the specified index in the list.
     *
     * @param index
     *            the index at which to insert the value, must be between 0 and
     *            the current size of the list (inclusive)
     * @param value
     *            the value to insert, may be <code>null</code>
     * @return a signal for the inserted entry, not <code>null</code>
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index &lt; 0 || index &gt;
     *             size())
     */
    public ValueSignal<T> insertAt(int index, T value) {
        lock.lock();
        try {
            checkPreconditions();

            if (index < 0 || index > entries.size()) {
                throw new IndexOutOfBoundsException(
                        "Index: " + index + ", Size: " + entries.size());
            }

            ValueSignal<T> entry = new ValueSignal<>(value);
            entries.add(index, entry);
            notifyListeners();
            return entry;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the specified entry from the list. If the entry is not in the
     * list, this method has no effect.
     *
     * @param entry
     *            the entry to remove, not <code>null</code>
     * @return <code>true</code> if the entry was found and removed,
     *         <code>false</code> if the entry was not in the list
     */
    public boolean remove(ValueSignal<T> entry) {
        lock.lock();
        try {
            checkPreconditions();

            boolean removed = entries.remove(entry);
            if (removed) {
                notifyListeners();
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all entries from the list.
     */
    public void clear() {
        lock.lock();
        try {
            checkPreconditions();

            if (!entries.isEmpty()) {
                entries.clear();
                notifyListeners();
            }
        } finally {
            lock.unlock();
        }
    }
}

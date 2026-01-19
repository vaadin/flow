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

import com.vaadin.signals.core.WritableSignal;
import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.function.SignalUpdater;
import com.vaadin.signals.function.ValueModifier;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A local list signal that wraps a list with change notifications.
 * <p>
 * Local list signals are non-serializable and use simple lock-based
 * synchronization. They are designed for UI-local state that doesn't need to be
 * synchronized across cluster nodes.
 * <p>
 * The signal exposes the list as a series of {@link ValueSignal} items, similar
 * to the shared ListSignal, but with simpler semantics and no transaction
 * support.
 * <p>
 * Local signals can't be used inside signal transactions.
 *
 * @param <T>
 *            the element type
 */
public class ListSignal<T> implements WritableSignal<List<ValueSignal<T>>> {

    private final List<ValueSignal<T>> items = new ArrayList<>();
    private int version;

    private final List<TransientListener> listeners = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new local list signal with no initial items.
     */
    public ListSignal() {
        this(Collections.emptyList());
    }

    /**
     * Creates a new local list signal with the given initial items.
     *
     * @param initialItems
     *            the initial items, not <code>null</code>
     */
    @SafeVarargs
    public ListSignal(T... initialItems) {
        for (T item : initialItems) {
            items.add(new ValueSignal<>(item));
        }
    }

    /**
     * Creates a new local list signal with the given initial items.
     *
     * @param initialItems
     *            the initial items, not <code>null</code>
     */
    public ListSignal(List<T> initialItems) {
        for (T item : initialItems) {
            items.add(new ValueSignal<>(item));
        }
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

            return Collections.unmodifiableList(items);
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

            return Collections.unmodifiableList(items);
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

    @Override
    public SignalOperation<List<ValueSignal<T>>> value(
            List<ValueSignal<T>> value) {
        lock.lock();
        try {
            checkPreconditions();

            List<ValueSignal<T>> oldValue = List.copyOf(items);

            items.clear();
            items.addAll(value);

            notifyListeners();

            return new SignalOperation<>(
                    new SignalOperation.Result<>(oldValue));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SignalOperation<Void> replace(List<ValueSignal<T>> expectedValue,
            List<ValueSignal<T>> newValue) {
        throw new UnsupportedOperationException(
                "Replace operation is not supported for local list signals");
    }

    @Override
    public CancelableOperation<List<ValueSignal<T>>> update(
            SignalUpdater<List<ValueSignal<T>>> updater) {
        throw new UnsupportedOperationException(
                "Update operation is not supported for local list signals. Use modify() instead.");
    }

    /**
     * Adds an item to the end of the list.
     *
     * @param item
     *            the item to add
     */
    public void add(T item) {
        lock.lock();
        try {
            checkPreconditions();

            items.add(new ValueSignal<>(item));
            notifyListeners();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds an item at the specified index.
     *
     * @param index
     *            the index to add at
     * @param item
     *            the item to add
     */
    public void add(int index, T item) {
        lock.lock();
        try {
            checkPreconditions();

            items.add(index, new ValueSignal<>(item));
            notifyListeners();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the item at the specified index.
     *
     * @param index
     *            the index to remove from
     * @return the removed signal
     */
    public ValueSignal<T> remove(int index) {
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
     * Gets the signal at the specified index.
     *
     * @param index
     *            the index to get from
     * @return the signal at the index
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
     * Gets the size of the list.
     *
     * @return the size
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
     * Clears all items from the list.
     */
    public void clear() {
        lock.lock();
        try {
            checkPreconditions();

            items.clear();
            notifyListeners();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the given callback to apply changes to the mutable list and then
     * notifies dependents.
     *
     * @param modifier
     *            a callback that receives the current list to modify, not
     *            <code>null</code>
     */
    public void modify(ValueModifier<List<ValueSignal<T>>> modifier) {
        lock.lock();
        try {
            checkPreconditions();

            modifier.modify(items);
            notifyListeners();
        } finally {
            lock.unlock();
        }
    }
}

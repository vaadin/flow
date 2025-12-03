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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.UsageTracker;
import com.vaadin.signals.impl.UsageTracker.Usage;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A writable signal that holds a reference to an object.
 * <p>
 * Changing the signal to reference another immutable value is an atomic
 * operation. It is safe to concurrently read and write the signal value from
 * multiple threads.
 * <p>
 * The signal can also be used with mutable values in which case no thread
 * safety is provided. Mutations must be done through {@link #modify(Consumer)}
 * to ensure dependents are informed after the modification is applied.
 * <p>
 * Reference signals can't be used inside signal transactions.
 * <p>
 * All operation objects returned from methods on this class are resolved
 * immediately.
 * 
 * @param <T>
 *            the signal value type
 */
public class ReferenceSignal<T> implements WritableSignal<T> {

    private T value;
    private int version;
    private boolean modifyRunning = false;

    private final List<TransientListener> listeners = new ArrayList<>();
    // package-protected for testing
    final ReentrantLock lock = new ReentrantLock();

    public ReferenceSignal(T initialValue) {
        this.value = initialValue;
    }

    public ReferenceSignal() {
        this(null);
    }

    private void checkPreconditions() {
        assert lock.isHeldByCurrentThread();

        if (Transaction.inTransaction()) {
            throw new IllegalStateException(
                    "ReferenceSignal cannot be used inside signal transactions.");
        }

        if (modifyRunning) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public T value() {
        lock.lock();
        try {
            checkPreconditions();

            if (UsageTracker.isActive()) {
                UsageTracker.registerUsage(createUsage(version));
            }

            return value;
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
            public Runnable onNextChange(TransientListener listener) {
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
    public T peek() {
        lock.lock();
        try {
            checkPreconditions();

            return value;
        } finally {
            lock.unlock();
        }
    }

    private void setAndNotify(T newValue) {
        assert lock.isHeldByCurrentThread();

        this.value = newValue;

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
    public SignalOperation<T> value(T value) {
        lock.lock();
        try {
            checkPreconditions();

            T oldValue = this.value;

            setAndNotify(value);

            return new SignalOperation<>(
                    new SignalOperation.Result<>(oldValue));
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Comparison between the expected value and the new value is performed
     * using {@link #equals(Object)}.
     */
    @Override
    public SignalOperation<Void> replace(T expectedValue, T newValue) {
        lock.lock();
        try {
            checkPreconditions();

            if (Objects.equals(expectedValue, value)) {
                setAndNotify(newValue);
                return new SignalOperation<>(
                        new SignalOperation.Result<>(null));
            } else {
                return new SignalOperation<>(
                        new SignalOperation.Error<>("Unexpected value"));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates the signal value based on the given callback. The callback
     * receives the current signal value and returns the new value to use. This
     * implementation acquires a lock while running the updater which means that
     * it's never necessary to run the callback again. This also means that
     * canceling the returned operation will never have any effect.
     * <p>
     * The result of the returned operation is resolved with the same value that
     * was passed to the updater callback.
     *
     * @param updater
     *            the value update callback, not <code>null</code>
     * @return an operation containing the result
     */
    @Override
    public synchronized CancelableOperation<T> update(
            UnaryOperator<T> updater) {
        Objects.requireNonNull(updater);
        lock.lock();
        try {
            checkPreconditions();

            T oldValue = this.value;
            T newValue = updater.apply(oldValue);
            if (newValue != oldValue) {
                setAndNotify(newValue);
            }

            CancelableOperation<T> operation = new CancelableOperation<>();
            operation.result().complete(new SignalOperation.Result<>(oldValue));
            return operation;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the given callback to apply changes to a mutable referenced value
     * and then notifies dependents.
     * <p>
     * This method is only intended for cases where concurrency is limited
     * through other means, such as Vaadin's session lock. Using this method
     * concurrently with any other methods on the same instance may, but is not
     * guaranteed to, cause an {@link ConcurrentModificationException}. The
     * exception can be thrown either from this method or from the other invoked
     * method. This can happen even if the other method is safe for concurrent
     * use.
     * 
     * @param modifier
     *            a callback that receives the current value to modify, not
     *            <code>null</code>
     */
    public void modify(Consumer<T> modifier) {
        Objects.requireNonNull(modifier);

        if (!lock.tryLock()) {
            throw new ConcurrentModificationException();
        }
        try {
            checkPreconditions();

            modifyRunning = true;
        } finally {
            lock.unlock();
        }

        boolean completed = false;
        try {
            modifier.accept(value);

            completed = true;
        } finally {
            lock.lock();
            try {
                modifyRunning = false;

                if (completed) {
                    setAndNotify(value);
                }

            } finally {
                lock.unlock();
            }
        }
    }
}

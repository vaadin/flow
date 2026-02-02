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

import java.util.ConcurrentModificationException;
import java.util.Objects;

import com.vaadin.signals.WritableSignal;
import com.vaadin.signals.function.SignalUpdater;
import com.vaadin.signals.function.ValueModifier;
import com.vaadin.signals.impl.Transaction;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A local writable signal that holds a reference to an object.
 * <p>
 * Local signals are non-serializable and intended for UI-local state only. They
 * do not participate in clustering and are simpler than shared signals.
 * <p>
 * Changing the signal to reference another immutable value is an atomic
 * operation. It is safe to concurrently read and write the signal value from
 * multiple threads.
 * <p>
 * The signal can also be used with mutable values in which case no thread
 * safety is provided. Mutations must be done through
 * {@link #modify(ValueModifier)} to ensure dependents are informed after the
 * modification is applied.
 * <p>
 * Local value signals can't be used inside signal transactions.
 * <p>
 * All operation objects returned from methods on this class are resolved
 * immediately.
 *
 * @param <T>
 *            the signal value type
 */
public class ValueSignal<T> extends AbstractLocalSignal<T>
        implements WritableSignal<T> {

    private boolean modifyRunning = false;

    /**
     * Creates a new value signal with the given initial value.
     *
     * @param initialValue
     *            the initial value, may be <code>null</code>
     */
    public ValueSignal(T initialValue) {
        super(initialValue);
    }

    /**
     * Creates a new value signal with an initial value of <code>null</code>.
     */
    public ValueSignal() {
        this(null);
    }

    @Override
    protected void checkPreconditions() {
        assertLockHeld();

        if (Transaction.inTransaction()) {
            throw new IllegalStateException(
                    "ValueSignal cannot be used inside signal transactions.");
        }

        if (modifyRunning) {
            throw new ConcurrentModificationException();
        }
    }

    private void setAndNotify(T newValue) {
        assertLockHeld();
        setSignalValue(newValue);
        notifyListeners();
    }

    @Override
    public SignalOperation<T> value(T value) {
        lock();
        try {
            checkPreconditions();

            T oldValue = getSignalValue();

            setAndNotify(value);

            return new SignalOperation<>(
                    new SignalOperation.Result<>(oldValue));
        } finally {
            unlock();
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
        lock();
        try {
            checkPreconditions();

            if (Objects.equals(expectedValue, getSignalValue())) {
                setAndNotify(newValue);
                return new SignalOperation<>(
                        new SignalOperation.Result<>(null));
            } else {
                return new SignalOperation<>(
                        new SignalOperation.Error<>("Unexpected value"));
            }
        } finally {
            unlock();
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
            SignalUpdater<T> updater) {
        Objects.requireNonNull(updater);
        lock();
        try {
            checkPreconditions();

            T oldValue = getSignalValue();
            T newValue = updater.update(oldValue);
            if (newValue != oldValue) {
                setAndNotify(newValue);
            }

            CancelableOperation<T> operation = new CancelableOperation<>();
            operation.result().complete(new SignalOperation.Result<>(oldValue));
            return operation;
        } finally {
            unlock();
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
    public void modify(ValueModifier<T> modifier) {
        Objects.requireNonNull(modifier);

        if (!tryLock()) {
            throw new ConcurrentModificationException();
        }
        try {
            checkPreconditions();

            modifyRunning = true;
        } finally {
            unlock();
        }

        boolean completed = false;
        try {
            // Access without lock since modifyRunning prevents concurrent
            // access
            modifier.modify(getSignalValueUnsafe());

            completed = true;
        } finally {
            lock();
            try {
                modifyRunning = false;

                if (completed) {
                    setAndNotify(getSignalValue());
                }

            } finally {
                unlock();
            }
        }
    }
}

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

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.signals.function.SignalUpdater;
import com.vaadin.signals.operations.CancelableOperation;
import com.vaadin.signals.operations.SignalOperation;

/**
 * A signal to which a new value can be directly written.
 * 
 * @param <T>
 *            the signal value type
 */
public interface WritableSignal<T> extends Signal<T> {
    /**
     * Sets the value of this signal. The result of the returned operation will
     * be resolved with the previous value at the time when this operation was
     * confirmed.
     *
     * @param value
     *            the value to set
     * @return an operation containing the eventual result
     */
    SignalOperation<T> value(T value);

    /**
     * Sets the value of this signal if and only if the signal has the expected
     * value at the time when the operation is confirmed. This is the signal
     * counterpart to {@link AtomicReference#compareAndSet(Object, Object)}. The
     * result of the returned operation will be resolved as successful if the
     * expected value was present and resolved as unsuccessful if any other
     * value was present when the operation is processed.
     *
     * @param expectedValue
     *            the expected value
     * @param newValue
     *            the new value
     * @return an operation containing the eventual result
     */
    SignalOperation<Void> replace(T expectedValue, T newValue);

    /**
     * Updates the signal value based on the given callback. The callback
     * receives the current signal value and returns the new value to use. If
     * the original value has changed by the time this change is confirmed, then
     * the returned value is ignored and the callback is run again with the new
     * value as input. This process is repeated until cancelled or until the
     * update succeeds without conflicting changes.
     * <p>
     * The process can be cancelled through the returned operation instance.
     * Note that canceling will only prevent further retries but the change will
     * still be made if the currently running attempt succeeds.
     * <p>
     * The result of the returned operation will be resolved with the previous
     * value at the time when a successful update operation was confirmed.
     * <p>
     * Update operations cannot participate in transactions since any retry
     * would occur after the original transaction has already been committed.
     * For this reason, the whole operation completely bypasses all transaction
     * handling.
     *
     * @param updater
     *            the value update callback, not <code>null</code>
     * @return an operation containing the eventual result
     */
    CancelableOperation<T> update(SignalUpdater<T> updater);

    /**
     * Wraps this signal to not accept changes.
     * <p>
     * This signal will keep its current configuration and changes applied
     * through this instance will be visible through the wrapped instance.
     *
     * @return the new readonly signal, not <code>null</code>
     */
    default Signal<T> asReadonly() {
        return () -> value();
    }
}

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
package com.vaadin.flow.signals;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.signals.function.SignalMapper;
import com.vaadin.flow.signals.function.SignalUpdater;
import com.vaadin.flow.signals.function.ValueMerger;
import com.vaadin.flow.signals.impl.MappedWritableSignal;
import com.vaadin.flow.signals.operations.CancelableOperation;
import com.vaadin.flow.signals.operations.SignalOperation;

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
    SignalOperation<T> set(T value);

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
        return () -> get();
    }

    /**
     * Creates a two-way mapped signal that provides a bidirectional view of
     * this signal. Reading the mapped signal applies the getter function to
     * extract a child value. Writing to the mapped signal uses the setter
     * function to update this signal with a new value derived from the current
     * value and the new child value.
     * <p>
     * This is useful for creating component bindings to properties of complex
     * objects. For example, to bind a checkbox to the "done" property of a Todo
     * record:
     *
     * <pre>
     * record Todo(String text, boolean done) {
     *     Todo withDone(boolean done) {
     *         return new Todo(this.text, done);
     *     }
     * }
     *
     * WritableSignal&lt;Todo&gt; todoSignal = new ValueSignal&lt;&gt;(
     *         new Todo("Buy milk", false));
     * WritableSignal&lt;Boolean&gt; doneSignal = todoSignal.map(Todo::done,
     *         Todo::withDone);
     *
     * checkbox.bindValue(doneSignal, doneSignal::set); // Two-way binding
     * </pre>
     *
     * @param <C>
     *            the child (mapped) signal type
     * @param getter
     *            the function to extract the child value from this signal's
     *            value, not <code>null</code>
     * @param merger
     *            the function to create a new value for this signal given the
     *            current value and a new child value, not <code>null</code>
     * @return a two-way mapped signal, not <code>null</code>
     */
    default <C> WritableSignal<C> map(SignalMapper<T, C> getter,
            ValueMerger<T, C> merger) {
        return new MappedWritableSignal<>(this, getter, merger);
    }
}

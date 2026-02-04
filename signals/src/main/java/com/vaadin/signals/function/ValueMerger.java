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
package com.vaadin.signals.function;

import com.vaadin.signals.WritableSignal;

/**
 * Creates a new outer value by merging a new inner value with the old outer
 * value. Used for creating two-way computed signals where changes to the mapped
 * signal propagate back to the parent signal.
 * <p>
 * This interface is used with immutable value patterns where changing the inner
 * value requires creating a new outer value instance.
 * <p>
 * Example usage with a record:
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
 * doneSignal.value(true); // Updates todoSignal to Todo("Buy milk", true)
 * </pre>
 *
 * @param <O>
 *            the outer (parent) signal value type
 * @param <I>
 *            the inner (mapped) signal value type
 * @see WritableSignal#map(SignalMapper, ValueMerger)
 */
@FunctionalInterface
public interface ValueMerger<O, I> {
    /**
     * Creates a new outer value by merging the new inner value with the old
     * outer value.
     *
     * @param outerValue
     *            the current outer signal value, may be <code>null</code>
     * @param newInnerValue
     *            the new inner value to merge, may be <code>null</code>
     * @return the new outer value, may be <code>null</code>
     */
    O merge(O outerValue, I newInnerValue);
}

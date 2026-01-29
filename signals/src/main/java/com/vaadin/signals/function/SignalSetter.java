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
 * Computes a new parent signal value based on the current parent value and a
 * new child value. Used for creating two-way computed signals where changes to
 * the mapped signal propagate back to the parent signal.
 * <p>
 * This interface is used with immutable value patterns where changing the child
 * value requires creating a new parent value instance.
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
 * WritableSignal&lt;Todo&gt; todoSignal = new ValueSignal&lt;&gt;(new Todo("Buy milk", false));
 * WritableSignal&lt;Boolean&gt; doneSignal = todoSignal.map(Todo::done, Todo::withDone);
 *
 * doneSignal.value(true); // Updates todoSignal to Todo("Buy milk", true)
 * </pre>
 *
 * @param <P>
 *            the parent signal value type
 * @param <C>
 *            the child (mapped) signal value type
 * @see WritableSignal#map(SignalMapper, SignalSetter)
 */
@FunctionalInterface
public interface SignalSetter<P, C> {
    /**
     * Computes a new parent value based on the current parent and a new child
     * value.
     *
     * @param parentValue
     *            the current parent signal value, may be <code>null</code>
     * @param newChildValue
     *            the new child value to apply, may be <code>null</code>
     * @return the new parent value, may be <code>null</code>
     */
    P set(P parentValue, C newChildValue);
}

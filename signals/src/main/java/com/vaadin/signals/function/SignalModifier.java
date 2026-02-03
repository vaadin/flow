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

import com.vaadin.signals.local.ValueSignal;

/**
 * Modifies the parent signal value in place based on a new child value. Used
 * for creating two-way computed signals with mutable parent values where
 * changes to the mapped signal propagate back to the parent signal.
 * <p>
 * This interface is used with mutable value patterns where changing the child
 * value directly modifies the parent value instance rather than creating a new
 * one.
 * <p>
 * Example usage with a mutable bean:
 *
 * <pre>
 * class Todo {
 *     private String text;
 *     private boolean done;
 *
 *     // getters and setters...
 * }
 *
 * ValueSignal&lt;Todo&gt; todoSignal = new ValueSignal&lt;&gt;(
 *         new Todo("Buy milk", false));
 * WritableSignal&lt;Boolean&gt; doneSignal = todoSignal.mapMutable(Todo::isDone,
 *         Todo::setDone);
 *
 * doneSignal.value(true); // Calls todoSignal.modify(t -&gt; t.setDone(true))
 * </pre>
 *
 * @param <P>
 *            the parent signal value type
 * @param <C>
 *            the child (mapped) signal value type
 * @see ValueSignal#mapMutable(SignalMapper, SignalModifier)
 */
@FunctionalInterface
public interface SignalModifier<P, C> {
    /**
     * Modifies the parent value in place with the new child value.
     *
     * @param parentValue
     *            the parent signal value to modify, may be <code>null</code>
     * @param newChildValue
     *            the new child value to apply, may be <code>null</code>
     */
    void modify(P parentValue, C newChildValue);
}

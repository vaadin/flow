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
package com.vaadin.flow.signals.function;

import java.io.Serializable;

import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Modifies the parent signal value in place based on a new child value. Used
 * with the {@link ValueSignal#modifier(SignalModifier)} helper method to create
 * write callbacks for mutable value patterns.
 * <p>
 * This interface is used with mutable value patterns where changing the child
 * value directly modifies the parent value instance rather than creating a new
 * one.
 * <p>
 * Example usage with a mutable bean:
 *
 * <pre>
 * class Person {
 *     private String name;
 *     private int age;
 *
 *     public String getName() {
 *         return name;
 *     }
 *
 *     public void setName(String name) {
 *         this.name = name;
 *     }
 * }
 *
 * ValueSignal&lt;Person&gt; personSignal = new ValueSignal&lt;&gt;(new Person());
 * textField.bindValue(personSignal.map(Person::getName),
 *         personSignal.modifier(Person::setName));
 * </pre>
 *
 * Example with a Todo class:
 *
 * <pre>
 * class Todo {
 *     private String task;
 *     private boolean done;
 *
 *     public String getTask() {
 *         return task;
 *     }
 *
 *     public void setTask(String task) {
 *         this.task = task;
 *     }
 *
 *     public boolean isDone() {
 *         return done;
 *     }
 *
 *     public void setDone(boolean done) {
 *         this.done = done;
 *     }
 * }
 *
 * ValueSignal&lt;Todo&gt; todoSignal = new ValueSignal&lt;&gt;(new Todo());
 * textField.bindValue(todoSignal.map(Todo::getTask),
 *         todoSignal.modifier(Todo::setTask));
 * checkbox.bindValue(todoSignal.map(Todo::isDone),
 *         todoSignal.modifier(Todo::setDone));
 * </pre>
 *
 * @param <P>
 *            the parent signal value type
 * @param <C>
 *            the child (mapped) signal value type
 * @see ValueSignal#modifier(SignalModifier)
 */
@FunctionalInterface
public interface SignalModifier<P, C> extends Serializable {
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

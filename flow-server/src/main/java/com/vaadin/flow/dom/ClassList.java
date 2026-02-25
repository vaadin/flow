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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.signals.Signal;

/**
 * Representation of the class names for an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ClassList extends Set<String>, Serializable {

    /**
     * Sets or removes the given class name, based on the {@code set} parameter.
     *
     * @param className
     *            the class name to set or remove
     * @param set
     *            true to set the class name, false to remove it
     * @return true if the class list was modified (class name added or
     *         removed), false otherwise
     */
    default boolean set(String className, boolean set) {
        if (set) {
            return add(className);
        } else {
            return remove(className);
        }
    }

    /**
     * Binds the presence of the given class name to the provided signal so that
     * the class is added when the signal value is {@code true} and removed when
     * the value is {@code false}.
     * <p>
     * While a binding for the given class name is active, manual calls to
     * {@link #add(Object)}, {@link #remove(Object)} or
     * {@link #set(String, boolean)} for that name will throw a
     * {@code com.vaadin.flow.dom.BindingActiveException}. Bindings are
     * lifecycle-aware and only active while the owning {@link Element} is in
     * attached state; they are deactivated while the element is in detached
     * state.
     * <p>
     * Bulk operations that indiscriminately replace or clear the class list
     * (for example {@link #clear()} or setting the {@code class} attribute via
     * {@link Element#setAttribute(String, String)}) clear all bindings.
     *
     * @param name
     *            the class name to bind, not {@code null} or blank
     * @param signal
     *            the boolean signal to bind to, not {@code null}
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @since 25.0
     */
    void bind(String name, Signal<Boolean> signal);

    /**
     * Binds the class names to the provided signal so that the class list is
     * dynamically updated to match the signal's value. Only one group binding
     * is allowed per class list.
     * <p>
     * The group binding coexists with static values and individual toggle
     * bindings. Names that appear in both sources are deduplicated by the
     * underlying classList (Set behavior).
     * <p>
     * Null or empty entries in the list and a {@code null} list value are
     * silently ignored.
     * <p>
     * Bulk operations that indiscriminately replace or clear the class list
     * (for example {@link #clear()} or setting the {@code class} attribute via
     * {@link Element#setAttribute(String, String)}) clear the group binding.
     *
     * @param names
     *            the signal providing the list of class names, not {@code null}
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing group binding
     * @since 25.1
     */
    void bind(Signal<List<String>> names);

}

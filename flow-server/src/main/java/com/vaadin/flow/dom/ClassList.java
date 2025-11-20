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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Set;

import com.vaadin.signals.Signal;

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
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the given class name. When unbinding, the current presence of the
     * class is left unchanged.
     * <p>
     * While a binding for the given class name is active, manual calls to
     * {@link #add(Object)}, {@link #remove(Object)} or
     * {@link #set(String, boolean)} for that name will throw a
     * {@code com.vaadin.flow.dom.BindingActiveException}. Bindings are
     * lifecycle-aware and only active while the owning {@link Element} is
     * in attached state; they are deactivated while the element is in detached state.
     * <p>
     * Bulk operations that indiscriminately replace or clear the class list
     * (for example {@link #clear()} or setting the {@code class} attribute via
     * {@link Element#setAttribute(String, String)}) clear all bindings.
     *
     * @param name
     *            the class name to bind, not {@code null} or blank
     * @param signal
     *            the boolean signal to bind to, or {@code null} to unbind
     * @since 25.0
     */
    void bind(String name, Signal<Boolean> signal);

}

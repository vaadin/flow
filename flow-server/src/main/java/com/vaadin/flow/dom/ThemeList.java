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
import java.util.Collection;
import java.util.Set;

import com.vaadin.signals.Signal;

/**
 * Representation of the theme names for an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ThemeList extends Set<String>, Serializable {

    /**
     * Sets or removes the given theme name, based on the {@code set} parameter.
     *
     * @param themeName
     *            the theme name to set or remove
     * @param set
     *            true to set the theme name, false to remove it
     * @return true if the theme list was modified (theme name added or
     *         removed), false otherwise
     */
    default boolean set(String themeName, boolean set) {
        return set ? add(themeName) : remove(themeName);
    }

    /**
     * Binds the presence of the given theme name to the provided signal so that
     * the theme name is added when the signal value is {@code true} and removed
     * when the value is {@code false}.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the given theme name. When unbinding, the current presence of the
     * theme name is left unchanged.
     * <p>
     * While a binding for the given theme name is active, manual calls to
     * {@link #add(Object)}, {@link #remove(Object)},
     * {@link #set(String, boolean)}, {@link #addAll(Collection)},
     * {@link #retainAll(Collection)} or {@link #removeAll(Collection)} for that
     * name will throw a {@code com.vaadin.flow.dom.BindingActiveException}.
     * Bindings are lifecycle-aware and only active while the owning
     * {@link Element} is in attached state; they are deactivated while the
     * element is in detached state.
     * <p>
     * Bulk operations that indiscriminately replace or clear the theme list
     * (for example {@link #clear()} or setting the {@code theme} attribute via
     * {@link com.vaadin.flow.component.HasTheme#setThemeName(String)}) clear
     * all bindings.
     *
     * @param name
     *            the theme name to bind, not {@code null} or blank
     * @param signal
     *            the boolean signal to bind to, or {@code null} to unbind
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @since 25.1
     */
    default void bind(String name, Signal<Boolean> signal) {
        // experimental API, do not force implementation
        throw new UnsupportedOperationException();
    }
}

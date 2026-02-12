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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.signals.Signal;

/**
 * A component which supports a placeholder.
 * <p>
 * A placeholder is a text that should be displayed in the input element, when
 * the user has not entered a value.
 * <p>
 * The default implementations sets the <code>placeholder</code> property for
 * this element. Override all methods in this interface if the placeholder
 * should be set in some other way.
 */
public interface HasPlaceholder extends HasElement {
    /**
     * Sets the placeholder text that should be displayed in the input element,
     * when the user has not entered a value
     *
     * @param placeholder
     *            the placeholder text, may be null.
     */
    default void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * The placeholder text that should be displayed in the input element, when
     * the user has not entered a value
     *
     * @return the {@code placeholder} property from the web component. May be
     *         null if not yet set.
     */
    default String getPlaceholder() {
        return getElement().getProperty("placeholder");
    }

    /**
     * Binds a signal's value to the component's placeholder so that the
     * placeholder is updated when the signal's value is updated.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the given placeholder. When unbinding, the current placeholder is
     * left unchanged.
     * <p>
     * While a binding for the placeholder is active, any attempt to set the
     * placeholder manually throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param placeholderSignal
     *            the signal to bind, not <code>null</code>
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setPlaceholder(String)
     * @see Element#bindProperty(String, Signal)
     *
     * @since 25.1
     */
    default void bindPlaceholder(Signal<String> placeholderSignal) {
        getElement().bindProperty("placeholder", placeholderSignal);
    }
}

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

import java.io.Serializable;

import com.vaadin.flow.signals.Signal;

/**
 * A component that supports input validation.
 * <p>
 * {@link HasValidation} is implemented by component when used with a Binder and
 * input is validated with a Binder.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface HasValidation extends Serializable {
    /**
     * Sets whether manual validation mode is enabled for the component.
     * <p>
     * When enabled, the component doesn't perform its built-in constraint
     * validation on value change, blur, and other events. This allows manually
     * controlling the invalid state and error messages using the
     * {@link #setInvalid(boolean)} and {@link #setErrorMessage(String)}
     * methods. Manual mode is helpful when there is a need for a totally custom
     * validation logic that cannot be achieved with Binder.
     * <p>
     * Example:
     *
     * <pre>
     * Field field = new Field();
     * field.setManualValidation(true);
     * field.addValueChangeListener(event -> {
     *     if (Objects.equal(event.getValue(), "")) {
     *         field.setInvalid(true);
     *         field.setErrorMessage("The field is required.");
     *     } else {
     *         field.setInvalid(false);
     *     }
     * });
     * </pre>
     *
     * <p>
     * For components that don't have built-in validation, the method has no
     * effect.
     *
     * @param enabled
     *            whether to enable manual validation mode.
     */
    default void setManualValidation(boolean enabled) {
    }

    /**
     * Sets an error message to the component.
     * <p>
     * The Web Component is responsible for deciding when to show the error
     * message to the user, and this is usually triggered by triggering the
     * invalid state for the Web Component. Which means that there is no need to
     * clean up the message when component becomes valid (otherwise it may lead
     * to undesired visual effects).
     * <p>
     * While a signal binding for the error message is active, calls to this
     * method throw a {@code com.vaadin.flow.signals.BindingActiveException}.
     *
     * @param errorMessage
     *            a new error message
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing signal binding
     */
    void setErrorMessage(String errorMessage);

    /**
     * Gets the current error message from the component.
     *
     * @return current error message
     */
    String getErrorMessage();

    /**
     * Binds the component's error message to the provided signal so that the
     * error message is kept in sync with the signal's current value.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding.
     * When unbinding, the current error message is left unchanged.
     * <p>
     * While a binding is active, manual calls to
     * {@link #setErrorMessage(String)} throw a
     * {@code com.vaadin.flow.signals.BindingActiveException}. Bindings are
     * lifecycle-aware and only active while the owning {@link Component} is in
     * attached state; they are deactivated while the component is in detached
     * state.
     *
     * @param signal
     *            the signal providing error messages, not {@code null}
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @since 25.1
     */
    default void bindErrorMessage(Signal<String> signal) {
        // experimental API, do not force implementation
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the validity of the component input.
     * <p>
     * When component becomes valid it hides the error message by itself, so
     * there is no need to clean up the error message via the
     * {@link #setErrorMessage(String)} call.
     * <p>
     * NOTE: If you need to manually control the invalid state, consider
     * enabling manual validation mode with
     * {@link #setManualValidation(boolean)} to avoid potential conflicts
     * between your custom validation and the component's built-in validation.
     * <p>
     * While a signal binding for the invalid state is active, calls to this
     * method throw a {@code com.vaadin.flow.signals.BindingActiveException}.
     *
     * @param invalid
     *            new value for component input validity
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing signal binding
     */
    void setInvalid(boolean invalid);

    /**
     * Returns {@code true} if component input is invalid, {@code false}
     * otherwise.
     *
     * @return whether the component input is valid
     */
    boolean isInvalid();

    /**
     * Binds the component's invalid state to the provided signal so that the
     * invalid flag is kept in sync with the signal's current value.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding.
     * When unbinding, the current invalid state is left unchanged.
     * <p>
     * While a binding is active, manual calls to {@link #setInvalid(boolean)}
     * throw a {@code com.vaadin.flow.signals.BindingActiveException}. Bindings
     * are lifecycle-aware and only active while the owning component is in the
     * attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param signal
     *            the signal providing invalid state flags, or {@code null} to
     *            unbind
     * @throws com.vaadin.flow.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @since 25.1
     */
    default void bindInvalid(Signal<Boolean> signal) {
        // experimental API, do not force implementation
        throw new UnsupportedOperationException();
    }
}

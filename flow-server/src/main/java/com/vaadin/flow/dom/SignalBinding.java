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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Represents an active binding between a signal and an element property (text,
 * attribute, visibility, etc.). Provides the ability to register callbacks that
 * are invoked whenever the binding updates, with rich context about the update.
 * <p>
 * Typical usage:
 *
 * <pre>
 * span.bindText(signal).onChange(ctx -&gt; {
 *     if (ctx.isBackgroundChange()) {
 *         ctx.getElement().flashClass("highlight");
 *     }
 * });
 * </pre>
 *
 * @param <T>
 *            the type of the bound signal value
 */
public class SignalBinding<T extends @Nullable Object> implements Serializable {

    private final List<SerializableConsumer<BindingContext<T>>> changeCallbacks = new ArrayList<>();
    private transient Registration effectRegistration;

    /**
     * Creates a new signal binding.
     */
    SignalBinding() {
    }

    /**
     * Sets the registration that controls the lifecycle of the underlying
     * effect.
     *
     * @param registration
     *            the registration to set
     */
    void setEffectRegistration(Registration registration) {
        this.effectRegistration = registration;
    }

    /**
     * Gets the registration that controls the lifecycle of the underlying
     * effect. Removing the registration stops the effect from running.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the effect registration, or {@code null} if not set
     */
    public Registration getEffectRegistration() {
        return effectRegistration;
    }

    /**
     * Registers a callback that is invoked every time the bound signal value
     * changes and the binding updates the element. The callback receives a
     * {@link BindingContext} that provides the old value, new value, target
     * element, nearest component, and whether the change was a background
     * update, initial render, or user-triggered.
     * <p>
     * Multiple callbacks can be registered by calling this method multiple
     * times. All registered callbacks are invoked in registration order.
     * <p>
     * Example:
     *
     * <pre>
     * span.bindText(signal).onChange(ctx -&gt; {
     *     if (ctx.isBackgroundChange()) {
     *         ctx.getElement().flashClass("highlight");
     *     }
     * });
     * </pre>
     *
     * @param action
     *            the callback to invoke on each update, not {@code null}
     * @return this binding for fluent chaining
     */
    public SignalBinding<T> onChange(
            SerializableConsumer<BindingContext<T>> action) {
        changeCallbacks.add(Objects.requireNonNull(action));
        return this;
    }

    /**
     * Fires all registered onChange callbacks with the given context. Called
     * internally by the binding effect after the setter has been applied.
     *
     * @param context
     *            the binding context for this execution
     */
    void fireOnChange(BindingContext<T> context) {
        for (SerializableConsumer<BindingContext<T>> callback : changeCallbacks) {
            callback.accept(context);
        }
    }
}

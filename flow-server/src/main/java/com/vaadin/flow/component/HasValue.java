/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.EventListener;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.shared.Registration;

/**
 * A generic interface for field components and other user interface objects
 * that have a user-editable value. Emits change events whenever the value is
 * changed, either by the user or programmatically.
 *
 * @param <E>
 *            the type of the value change event fired by this instance
 * @param <V>
 *            the value type
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface HasValue<E extends ValueChangeEvent<V>, V> extends Serializable {

    /**
     * An event fired when the value of a {@code HasValue} changes.
     *
     * @param <V>
     *            the value type
     */
    interface ValueChangeEvent<V> extends Serializable {
        HasValue<?, V> getHasValue();

        /**
         * Checks if this event originated from the client side.
         *
         * @return <code>true</code> if the event originated from the client
         *         side, <code>false</code> otherwise
         */
        boolean isFromClient();

        /**
         * Returns the value of the source before this value change event
         * occurred.
         *
         * @return the value previously held by the source of this event
         */
        V getOldValue();

        /**
         * Returns the new value that triggered this value change event.
         *
         * @return the new value
         */
        V getValue();
    }

    /**
     * A listener for value change events.
     *
     * @param <E>
     *            the value change event type
     * @see ValueChangeEvent
     * @see Registration
     */
    @FunctionalInterface
    interface ValueChangeListener<E extends ValueChangeEvent<?>>
            extends EventListener, Serializable {

        /**
         * Invoked when this listener receives a value change event from an
         * event source to which it has been added.
         *
         * @param event
         *            the received event, not null
         */
        void valueChanged(E event);
    }

    /**
     * Sets the value of this object. If the new value is not equal to
     * {@code getValue()}, fires a value change event. May throw
     * {@code IllegalArgumentException} if the value is not acceptable.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values are accepted or not, and override
     * {@link #getEmptyValue()} if the empty value is not {@code null}.
     *
     * @param value
     *            the new value
     * @throws IllegalArgumentException
     *             if the value is invalid
     */
    void setValue(V value);

    /**
     * Returns the current value of this object.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values may be returned or not, and override
     * {@link #getEmptyValue()} if the empty value is not {@code null}.
     *
     * @return the current value
     */
    V getValue();

    /**
     * Adds a value change listener. The listener is called when the value of
     * this {@code HasValue} is changed either by the user or programmatically.
     *
     * @param listener
     *            the value change listener, not null
     * @return a registration for the listener
     */
    Registration addValueChangeListener(
            ValueChangeListener<? super E> listener);

    /**
     * Returns the value that represents an empty value.
     * <p>
     * By default {@link HasValue} is expected to support {@code null} as empty
     * values. Specific implementations might not support this.
     *
     * @return empty value
     */
    default V getEmptyValue() {
        return null;
    }

    /**
     * Returns the current value of this object, wrapped in an {@code Optional}.
     * <p>
     * The {@code Optional} will be empty if the value is {@code null} or
     * {@code isEmpty()} returns {@code true}.
     *
     * @return the current value, wrapped in an {@code Optional}
     */
    default Optional<V> getOptionalValue() {
        return isEmpty() ? Optional.empty() : Optional.ofNullable(getValue());
    }

    /**
     * Returns whether this {@code HasValue} is considered to be empty.
     * <p>
     * By default this is an equality check between current value and empty
     * value.
     *
     * @return {@code true} if considered empty; {@code false} if not
     */
    default boolean isEmpty() {
        return Objects.equals(getValue(), getEmptyValue());
    }

    /**
     * Resets the value to the empty one.
     * <p>
     * This is just a shorthand for resetting the value, see the methods
     * {@link #setValue(Object)} and {@link #getEmptyValue()}.
     *
     * @see #setValue(Object)
     * @see #getEmptyValue()
     */
    default void clear() {
        setValue(getEmptyValue());
    }

    /**
     * Sets the read-only mode of this {@code HasValue} to given mode. The user
     * can't change the value when in read-only mode.
     * <p>
     * A {@code HasValue} with a visual component in read-only mode typically
     * looks visually different to signal to the user that the value cannot be
     * edited.
     *
     * @param readOnly
     *            a boolean value specifying whether the component is put
     *            read-only mode or not
     */
    void setReadOnly(boolean readOnly);

    /**
     * Returns whether this {@code HasValue} is in read-only mode or not.
     *
     * @return {@code false} if the user can modify the value, {@code true} if
     *         not.
     */
    boolean isReadOnly();

    /**
     * Sets the required indicator visible or not.
     * <p>
     * If set visible, it is visually indicated in the user interface.
     * <p>
     * The method is intended to be used with <code>Binder</code> which does
     * server-side validation. In case HTML element has its own (client-side)
     * validation it should be disabled when
     * <code>setRequiredIndicatorVisible(true)</code> is called and re-enabled
     * back on <code>setRequiredIndicatorVisible(false)</code>. It's
     * responsibility of each component implementation to follow the contract so
     * that the method call doesn't do anything else than show/hide the
     * "required" indication. Usually components provide their own
     * <code>setRequired</code> method which should be called in case the
     * client-side validation is required.
     *
     * @param requiredIndicatorVisible
     *            <code>true</code> to make the required indicator visible,
     *            <code>false</code> if not
     */
    void setRequiredIndicatorVisible(boolean requiredIndicatorVisible);

    /**
     * Checks whether the required indicator is visible.
     *
     * @return <code>true</code> if visible, <code>false</code> if not
     */
    boolean isRequiredIndicatorVisible();
}

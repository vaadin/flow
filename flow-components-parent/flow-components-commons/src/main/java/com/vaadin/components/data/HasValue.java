/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.components.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.ComponentSupplier;

/**
 * A generic interface for field components and other user interface objects
 * that have a user-editable value. Emits change events whenever the value is
 * changed, either by the user or programmatically.
 *
 * @author Vaadin Ltd.
 *
 * @param <C>
 *            the component type
 * @param <V>
 *            the value type
 */
public interface HasValue<C extends Component, V>
        extends ComponentSupplier<C>, Serializable {

    /**
     * An event fired when the value of a {@code HasValue} changes.
     *
     * @param <C>
     *            the component type
     * @param <V>
     *            the value type
     */
    public class ValueChangeEvent<C extends Component, V>
            extends ComponentEvent<C> {

        private final V oldValue;
        private final V value;

        /**
         * Creates a new {@code ValueChange} event containing the given value,
         * originating from the given source component.
         *
         * @param component
         *            the component, not null
         * @param hasValue
         *            the HasValue instance bearing the value, not null
         * @param oldValue
         *            the previous value held by the source of this event
         * @param fromClient
         *            {@code true} if this event originates from the client,
         *            {@code false} otherwise.
         */
        public ValueChangeEvent(C component,
                HasValue<C, V> hasValue,
                V oldValue, boolean fromClient) {
            super(component, fromClient);
            this.oldValue = oldValue;
            value = hasValue.getValue();
        }

        /**
         * Returns the value of the source before this value change event
         * occurred.
         *
         * @return the value previously held by the source of this event
         */
        public V getOldValue() {
            return oldValue;
        }

        /**
         * Returns the new value that triggered this value change event.
         *
         * @return the new value
         */
        public V getValue() {
            return value;
        }
    }

    /**
     * A listener for value change events.
     *
     * @param <V>
     *            the value type
     *
     * @see ValueChangeEvent
     * @see Registration
     */
    @FunctionalInterface
    public interface ValueChangeListener<C extends Component, V>
            extends ComponentEventListener<ValueChangeEvent<C, V>>,
            Serializable {

        /**
         * Invoked when this listener receives a value change event from an
         * event source to which it has been added.
         *
         * @param event
         *            the received event, not null
         */
        void onComponentEvent(ValueChangeEvent<C, V> event);
    }

    /**
     * Sets the value of this object. If the new value is not equal to
     * {@code getValue()}, fires a value change event. May throw
     * {@code IllegalArgumentException} if the value is not acceptable.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values are accepted or not.
     *
     * @param value
     *            the new value
     * @throws IllegalArgumentException
     *             if the value is invalid
     */
    C setValue(V value);

    /**
     * Returns the current value of this object.
     * <p>
     * <i>Implementation note:</i> the implementing class should document
     * whether null values may be returned or not.
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
            ValueChangeListener<C, V> listener);

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
}

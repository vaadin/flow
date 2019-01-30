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
package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.shared.Registration;

/**
 * Property value class for web components that will be published and synced on
 * client for the generated server side WebComponent.
 *
 * @param <T>
 *         property type
 */
public class WebComponentProperty<T> implements Serializable {

    private T value;
    private final Class<T> propertyClass;
    private Set<PropertyValueChangeListener> listeners;

    /**
     * Create a property with the given initial value to be added to the
     * generated file.
     *
     * @param initialValue
     *         initial value to generate file with
     * @param propertyClass
     *         property type class
     */
    public WebComponentProperty(T initialValue, Class<T> propertyClass) {
        Objects.requireNonNull(initialValue,
                "For a null initial value Class<T> needs to be given.");
        value = initialValue;
        this.propertyClass = propertyClass;
    }

    /**
     * Create a property with no initial value in the generated file.
     *
     * @param propertyClass
     *         property type class
     */
    public WebComponentProperty(Class<T> propertyClass) {
        value = null;
        this.propertyClass = propertyClass;
    }

    /**
     * Set the value for this property. Will fire a {@link
     * PropertyValueChangeEvent} if listeners have been registered.
     *
     * @param value
     *         value to be set
     */
    public void set(T value) {
        T oldValue = this.value;
        this.value = value;

        if (listeners != null) {
            PropertyValueChangeEvent<T> event = new PropertyValueChangeEvent<>(
                    this, oldValue, value);
            Set<PropertyValueChangeListener> listenerSet = new HashSet<>(
                    listeners);
            for (PropertyValueChangeListener listener : listenerSet) {
                listener.valueChange(event);
            }
        }

    }

    /**
     * Add a property value change listener to this property.
     *
     * @param listener
     *         listener to notify for value change
     * @return registration to remove listener with
     */
    public Registration addValueChangeListener(
            PropertyValueChangeListener<T> listener) {
        if (listeners == null) {
            listeners = new HashSet<>();
        }
        listeners.add(listener);

        return () -> listeners.remove(listener);
    }

    /**
     * Get the current property value.
     *
     * @return current value
     */
    public T get() {
        return value;
    }

    /**
     * Get the property class type.
     *
     * @return property class type
     */
    public Class<T> getValueType() {
        return propertyClass;
    }
}

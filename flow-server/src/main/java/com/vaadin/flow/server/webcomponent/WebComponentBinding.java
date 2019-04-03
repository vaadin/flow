/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * An internal representation of a web component instance bound to a
 * {@link Component} instance. Facilitates property updates from the client
 * to the {@code component}.
 *
 * @param <C>   type of the component being exported
 * @see WebComponentConfiguration#createWebComponentBinding(Instantiator, Element)
 *      to create {@code WebComponentBindings}
 */
public final class WebComponentBinding<C extends Component> implements Serializable {
    private C component;
    private HashMap<String, PropertyBinding<? extends Serializable>> properties = new HashMap<>();

    /**
     * Constructs a {@link WebComponentBinding} which consists of a
     * {@link Component} instance exposed as an embeddable web component and
     * set of {@link PropertyBinding PropertyBindindings} that the component
     * exposes as a web component.
     *
     * @param component     component which exposes {@code properties} as web
     *                      component. Not {@code null}
     */
    public WebComponentBinding(C component) {
        Objects.requireNonNull(component, "Parameter 'component' must not be " +
                "null!");

        this.component = component;
    }

    /**
     * Updates a property bound to the {@code component}. If the property has
     * an attached listener, the {@code value} is also delivered to the
     * listener. If the {@code value} is {@code null}, the property is set to
     * its default value (which could be {@code null}).
     *
     * @param propertyName  name of the property
     * @param value         new value to set for the property
     */
    public void updateProperty(String propertyName, Serializable value) {
        Objects.requireNonNull(propertyName, "Parameter 'propertyName' must " +
                "not be null!");

        PropertyBinding<?> propertyBinding = properties.get(propertyName);

        if (propertyBinding == null) {
            throw new IllegalArgumentException(
                    String.format("No %s found for propertyName '%s'!",
                            PropertyData.class.getSimpleName(), propertyName));
        }

        propertyBinding.updateValue(value);
    }

    /**
     * Retrieves the bound {@link Component} instance.
     *
     * @return {@code component} instance
     */
    public C getComponent() {
        return component;
    }

    /**
     * Retrieve the type of a property's value.
     *
     * @param propertyName  name of the property
     * @return property type
     */
    public Class<? extends Serializable> getPropertyType(String propertyName) {
        if (hasProperty(propertyName)) {
            return properties.get(propertyName).getType();
        }
        return null;
    }

    /**
     * Does the component binding have a property identified by given name.
     *
     * @param propertyName  name of the property
     * @return has property
     */
    public boolean hasProperty(String propertyName) {
        return properties.containsKey(propertyName);
    }

    /**
     * Calls the bound change handlers defined via
     * {@link PropertyConfigurationImpl#onChange(SerializableBiConsumer)} for
     * each bound property with the current value of the property.
     */
    public void updatePropertiesToComponent() {
        properties.forEach((key, value) -> value.notifyValueChange());
    }

    /**
     * Adds a property to this web component binding based on the
     * {@code propertyConfiguration}. If a property with an existing name is
     * bound, the previous binding is removed.
     *
     * @param propertyConfigurationImpl     property configuration
     */
    public void bindProperty(PropertyConfigurationImpl<C,
                    ? extends Serializable> propertyConfigurationImpl) {
        assert propertyConfigurationImpl != null : "propertyConfiguration cannot " +
                "be null!";

        SerializableBiConsumer<C, Serializable> consumer = propertyConfigurationImpl
                .getOnChangeHandler();
        PropertyBinding<? extends Serializable> binding =
                new PropertyBinding<>(propertyConfigurationImpl.getPropertyData(),
                        consumer == null ? null
                                : value -> consumer.accept(
                                        component, value));
        properties.put(propertyConfigurationImpl.getPropertyData().getName(), binding);
    }

    private static class PropertyBinding<P extends Serializable> implements Serializable {
        private PropertyData<P> data;
        private SerializableConsumer<P> listener;
        private P value;

        public PropertyBinding(PropertyData<P> data,
                        SerializableConsumer<P> listener) {
            Objects.requireNonNull(data, "Parameter 'data' must not be null!");
            this.data = data;
            this.listener = listener;
            this.value = data.getDefaultValue();
        }

        public void updateValue(Serializable newValue) {
            if (isReadOnly()) {
                LoggerFactory.getLogger(getClass())
                        .warn(String.format("An attempt was made to write to " +
                                        "a read-only property '%s' owned by exported " +
                                        "component %s", getName(),
                                getType().getCanonicalName()));
                return;
            }

            if (newValue != null && newValue.getClass() != getType()) {
                throw new IllegalArgumentException(String.format("Parameter " +
                                "'newValue' is of the wrong type: onChangeHandler" +
                                " of the property expected to receive %s but " +
                                "found %s instead.",
                        getType().getCanonicalName(),
                        newValue.getClass().getCanonicalName()));
            }

            P newTypedValue = (P)newValue;

            // null values are always set to default value (which might still be
            // null for some types)
            if (newTypedValue == null) {
                newTypedValue = data.getDefaultValue();
            }

            boolean updated = false;
            if (this.value != null && !this.value.equals(newTypedValue)) {
                updated = true;
            }
            else if (newValue != null && !newValue.equals(this.value)) {
                updated = true;
            }

            if (updated) {
                this.value = newTypedValue;
                notifyValueChange();
            }
        }

        public Class<P> getType() {
            return data.getType();
        }

        public String getName() {
            return data.getName();
        }

        public P getValue() {
            return value;
        }

        public boolean isReadOnly() {
            return data.isReadOnly();
        }

        public void notifyValueChange() {
            if (listener != null) {
                listener.accept(this.value);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PropertyBinding) {
                PropertyBinding other = (PropertyBinding) obj;
                boolean valuesAreNull = value == null && other.value == null;
                boolean valuesAreEqual = valuesAreNull ||
                        (value != null && value.equals(other.value));
                return data.equals(other.data) && valuesAreEqual;
            }
            return false;
        }
    }
}

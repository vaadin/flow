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

package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * Implements the {@link PropertyConfiguration} interface returned by any of the
 * {@link com.vaadin.flow.component.webcomponent.WebComponentDefinition}'s
 * {@code addProperty} methods. {@code PropertyConfigurationImpl} stores the
 * choices to be used by the web component generation process.
 *
 * @param <C>   type of the {@code component} being exported
 * @param <P>   type of the {@code property} owned by the component
 */
public class PropertyConfigurationImp<C extends Component, P extends Serializable> implements PropertyConfiguration<C, P> {
    private Class<C> componentClass;
    private PropertyData<P> data;
    private SerializableBiConsumer<C, Serializable> onChangeHandler = null;

    /**
     * Constructs a new {@code PropertyConfigurationImpl} tied to the
     * exported {@link Component} type given by {@code componentType}.
     *
     * @param componentType     type of the exported {@code component}
     * @param propertyName      name of the property
     * @param propertyType      type of the property
     * @param defaultValue      default value of the property. If the
     *                          property type has a primitive version, this
     *                          value is used when ever the property is being
     *                          set to a {@code null}.
     */
    public PropertyConfigurationImp(Class<C> componentType, String propertyName,
                                    Class<P> propertyType, P defaultValue) {

        data = new PropertyData<>(propertyName, propertyType, false,
                defaultValue);
        this.componentClass = componentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyConfiguration<C, P> onChange(SerializableBiConsumer<C, P> onChangeHandler) {
        Objects.requireNonNull(onChangeHandler, "Parameter 'onChangeHandler' " +
                "cannot be null!");
        if (this.onChangeHandler != null) {
            throw new IllegalStateException(String.format("onChangeHandler " +
                    "for property %s has already been set and cannot be " +
                    "overwritten!", data.getName()));
        }
        this.onChangeHandler = (c, o) -> onChangeHandler.accept(c, (P) o);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PropertyConfiguration<C, P> readOnly() {
        data = new PropertyData<>(data, true);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyConfigurationImp) {
            PropertyConfigurationImp other = (PropertyConfigurationImp) obj;
            return data.equals(other.data)
                    && componentClass.equals(other.componentClass);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentClass, data);
    }

    /**
     * Retrieves the {@code onChangeHandler} tied to this property, if on
     * exists.
     *
     * @return handler or {@code null}
     */
    SerializableBiConsumer<C, Serializable> getOnChangeHandler() {
        return onChangeHandler;
    }

    /**
     * Computed {@link PropertyData} based on the configuration details.
     *
     * @return {@code PropertyData} value object
     */
    PropertyData<P> getPropertyData() {
        return data;
    }
}

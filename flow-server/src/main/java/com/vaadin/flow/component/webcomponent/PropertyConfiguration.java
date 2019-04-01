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

package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableBiConsumer;

/**
 * Offers a fluent API for configuring the properties of embedded web
 * components produced by
 * {@link com.vaadin.flow.component.WebComponentExporter}.
 *
 * @param <C>   type of the {@code component} exported as a web component
 * @param <P>   type of the property exposed on the web component
 */
public abstract class PropertyConfiguration<C extends Component,
        P extends Serializable> {
    private Class<C> componentClass;
    private PropertyData<P> data;
    private SerializableBiConsumer<C, Serializable> onChangeHandler = null;

    private PropertyConfiguration() {}

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
    protected PropertyConfiguration(Class<C> componentType, String propertyName,
                                 Class<P> propertyType, P defaultValue) {

        data = new PropertyData<>(propertyName, propertyType, false,
                defaultValue);
        this.componentClass = componentType;
    }

    /**
     * Sets a Property change handler. {@code onChange} can only be called
     * once - multiple calls will throw an exception.
     * <p>
     * The {@code onChangeHandler} is called
     * when the property's value changes on the client-side. If the property
     * value is {@code null} for a property type which should not receive
     * null-values, such as {@code double}, the method will be called with
     * the property's default value. The default value is set by
     * {@link WebComponentDefinition} when {@code addProperty(propertyName,
     * defaultValue} is called.
     * <p>
     * In the following example we export {@code MyComponent} as a web
     * component. The {@code MyComponent} class has a method {@code setName}
     * which will be called in response to changes to the registered property
     * {@code "name"}.
     *
     * <pre>
     * &#064;Tag("my-component")
     * public class Exporter implements WebComponentExporter&lt;MyComponent&gt;() {
     *     // ... define the web component
     *     &#064;Override
     *     public void define(WebComponentDefinition&lt;MyComponent&gt;
     *     definition) {
     *         definition.addProperty("name", "John Doe")
     *                 .onChange(MyComponent::setName);
     *     }
     * }
     * </pre>
     *
     * @param onChangeHandler   {@code component}'s method which is called with
     *                          the property value
     * @return this {@code PropertyConfiguration}
     */
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
     * Mark the property as read-only. It cannot be written to by the client.
     *
     * @return this {@code PropertyConfiguration}
     */
    public PropertyConfiguration<C, P> readOnly() {
        data = new PropertyData<>(data, true);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyConfiguration) {
            PropertyConfiguration other = (PropertyConfiguration) obj;
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
    protected SerializableBiConsumer<C, Serializable> getOnChangeHandler() {
        return onChangeHandler;
    }

    /**
     * Computed {@link PropertyData} based on the configuration details.
     *
     * @return {@code PropertyData} value object
     */
    protected PropertyData<P> getPropertyData() {
        return data;
    }
}

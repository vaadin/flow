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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;

/**
 * @param <C>
 * @param <P>
 */
public interface PropertyConfiguration<C extends Component,
        P extends Serializable> extends Serializable {

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
    PropertyConfigurationImpl<C, P> onChange(SerializableBiConsumer<C, P> onChangeHandler);

    /**
     * Mark the property as read-only. It cannot be written to by the client.
     *
     * @return this {@code PropertyConfiguration}
     */
    PropertyConfigurationImpl<C, P> readOnly();
}

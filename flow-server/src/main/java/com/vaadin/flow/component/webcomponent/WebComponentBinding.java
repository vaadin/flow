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
import com.vaadin.flow.di.Instantiator;

/**
 * An internal representation of a web component instance bound to a
 * {@link Component} instance. Facilitates property updates from the client
 * to the {@code component}.
 *
 * @param <C> exported component type
 * @see WebComponentConfiguration#createBinding(Instantiator) for creating
 *      {@code WebComponentBindings}
 */
public interface WebComponentBinding<C extends Component> extends Serializable {

    /**
     * Updates a property bound to the {@code component}. If the property has
     * an attached listener, the {@code value} is also delivered to the
     * listener. If the {@code value} is {@code null}, the property is set to
     * its default value (which could be {@code null}.
     *
     * @param propertyName  name of the property
     * @param value         new value to set for the property
     */
    void updateProperty(String propertyName, Serializable value);

    /**
     * Retrieves the bound {@link Component} instance.
     *
     * @return {@code component} instance
     */
    C getComponent();

    /**
     * Retrieve the type of a property's value.
     *
     * @param propertyName  name of the property
     * @return property type
     */
    Class<? extends Serializable> getPropertyType(String propertyName);

    /**
     * Does the component binding have a property identified by given name.
     *
     * @param propertyName  name of the property
     * @return  has property
     */
    boolean hasProperty(String propertyName);
}

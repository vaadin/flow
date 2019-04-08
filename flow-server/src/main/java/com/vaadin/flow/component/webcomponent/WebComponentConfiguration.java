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
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.webcomponent.PropertyData;

/**
 * The configuration is used to construct the web component, and further-more
 * the {@link WebComponentBinding}.
 *
 * @param <C> type of the component being exported
 */
public interface WebComponentConfiguration<C extends Component>
        extends Serializable {

    /**
     * Check if the configuration has a property identified by the {@code
     * propertyName}.
     *
     * @param propertyName  name of the property, not null
     * @return has property
     */
    boolean hasProperty(String propertyName);

    /**
     * Retrieve the type of a property's value. If the property is not known,
     * returns {@code null}
     *
     * @param propertyName  name of the property, not null
     * @return property type or null
     */
    Class<? extends Serializable> getPropertyType(String propertyName);

    /**
     * Retrieve the type of the component.
     *
     * @return component type
     */
    Class<C> getComponentClass();

    /**
     * Set of all the {@link PropertyData} objects defining the web
     * component's properties.
     *
     * @return set of {@code PropertyData}
     */
    Set<PropertyData<? extends Serializable>> getPropertyDataSet();

    /**
     * Creates the component instance of type {@link C} using {@code
     * instantiator} and then constructs a {@link WebComponentBinding} which
     * allows for pushing property updates to the component {@code C} in a
     * fashion defined by the associated
     * {@link com.vaadin.flow.component.WebComponentExporter}.
     *
     * @param instantiator  {@link Instantiator} using to construct component
                            instance
     * @param el            element which acts as the root element for the
     *                      {@code component instance}
     * @return  web component binding which can be used by the web component
     *          host to communicate with the component it is hosting
     */
    WebComponentBinding<C> createWebComponentBinding(Instantiator instantiator,
    Element el);
}

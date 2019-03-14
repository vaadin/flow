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
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.di.Instantiator;
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
     * @param propertyName  name of the property
     * @return has property
     */
    boolean hasProperty(String propertyName);

    /**
     * Retrieve the type of a property's value.
     *
     * @param propertyName  name of the property
     * @return property type
     */
    Class<? extends Serializable> getPropertyType(String propertyName);

    /**
     * Retrieve the type of the component.
     *
     * @return component type
     */
    Class<C> getComponentClass();

    /**
     * Retrieve the type of the exporter's class.
     *
     * @return exporter type
     */
    Class<WebComponentExporter<C>> getExporterClass();

    /**
     * Set of all the {@link PropertyData} objects defining the web
     * component's properties.
     *
     * @return set of {@code PropertyData}
     */
    Set<PropertyData<? extends Serializable>> getPropertyDataSet();

    /**
     * Creates a {@link WebComponentBinding} which is a distinct web
     * component based on this configuration. Each {@code binding} has an
     * instance of the component type being exported. Each binding is mapped
     * to single element in the embedding context.
     *
     * @param instantiator  Vaadin {@link Instantiator}
     * @return web component binding
     */
    WebComponentBinding<C> createBinding(Instantiator instantiator);
}

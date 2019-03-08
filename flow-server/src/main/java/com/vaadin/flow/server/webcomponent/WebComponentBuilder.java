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

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.IWebComponent;
import com.vaadin.flow.component.webcomponent.InstanceConfigurator;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.ReflectTools;

public class WebComponentBuilder<C extends Component> implements WebComponentDefinition<C>, WebComponentConfiguration<C> {
    private Class<C> componentClass = null;
    private WebComponentExporter<C> exporter;
    private InstanceConfigurator<C> instanceConfigurator;
    private Map<String, PropertyConfigurationImp<C, ?>> propertyConfigurationMap =
            new HashMap<>();
    private C componentReference;

    public WebComponentBuilder(WebComponentExporter<C> exporter) {
        this.exporter = exporter;
        this.exporter.define(this);
    }

    @Override
    public <P> PropertyConfiguration<C, P> addProperty(String name, Class<P> type, P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' cannot be null!");
        Objects.requireNonNull(type, "Parameter 'type' cannot be null!");

        if (propertyConfigurationMap.containsKey(name)) {
            throw new InvalidParameterException(String.format( "Property '%s'" +
                    " has already been registered! WebComponent cannot have " +
                            "multiple properties with the same name.",
                    name));
        }

        PropertyConfigurationImp<C, P> configurationImp =
                new PropertyConfigurationImp<>(getComponentClass(), name,
                        type, defaultValue);

        propertyConfigurationMap.put(name, configurationImp);

        return configurationImp;
    }

    @Override
    public void setInstanceConfigurator(InstanceConfigurator<C> configurator) {
        this.instanceConfigurator = configurator;
    }

    @Override
    public <P> PropertyConfiguration<C, List<P>> addListProperty(String name, Class<P> entryClass) {
        return null;
    }

    @Override
    public <P> PropertyConfiguration<C, List<P>> addListProperty(String name, List<P> defaultValue) {
        return null;
    }

    public String getWebComponentTag() {
        return exporter.getTag();
    }

    @Override
    public Class<?> getPropertyType(String propertyName) {
        if (propertyConfigurationMap.containsKey(propertyName)) {
            return propertyConfigurationMap.get(propertyName).getPropertyType();
        } else {
            // TODO: or should we throw here?
            return null;
        }
    }

    /**
     * TODO: write this
     * {@link #getComponentInstance(Instantiator)} must be called before
     * calling this method
     *
     * @param propertyName
     * @param value
     *
     */
    @Override
    public void deliverPropertyUpdate(String propertyName, Object value) {
        Objects.requireNonNull(propertyName, "Parameter 'propertyName' must " +
                "not be null!");

        if (componentReference == null) {
            throw new IllegalStateException(String.format("%s" +
                    "::getComponentInstance must be called before calling " +
                    "this method!",
                    WebComponentConfiguration.class.getSimpleName()));
        }

        PropertyConfigurationImp<C, ?> propertyConfiguration =
                propertyConfigurationMap.get(propertyName);

        if (propertyConfiguration == null) {
            throw new InvalidParameterException(
                    String.format("No %s found for propertyName '%s'!",
                            PropertyConfiguration.class.getSimpleName(), propertyName));
        }

        if (propertyConfiguration.isReadOnly()) {
            throw new InvalidParameterException("Property '%s' is read-only!");
        }

        if (value != null && value.getClass() != propertyConfiguration.getPropertyType()) {
            // TODO: throw a specific exception here
            throw new RuntimeException(String.format("Parameter 'value' is of" +
                    " the wrong type: onChangeHandler of the property " +
                            "expected to receive %s but found %s instead.",
                    propertyConfiguration.getPropertyType().getName(),
                    value.getClass().getName()));
        }

        SerializableBiConsumer<C, Object> onChangeHandler =
                propertyConfiguration.getOnChangeHandler();

        if (onChangeHandler != null) {
            onChangeHandler.accept(componentReference, value);
        }
        // TODO: should we log about the missing handler?
    }

    @Override
    public Set<PropertyData2<?>> getPropertyDataSet() {
        return propertyConfigurationMap.values().stream().map(PropertyConfigurationImp::getPropertyData).collect(Collectors.toSet());
    }

    PropertyData2<?> getPropertyData(String propertyName) {
        return propertyConfigurationMap.get(propertyName).getPropertyData();
    }

    @Override
    public Class<C> getComponentClass() {
        // TODO: if this works, exporter.getComponentClass() can be removed
        if (componentClass == null) {
            componentClass = (Class<C>)ReflectTools.getGenericInterfaceType(
                    exporter.getClass(), WebComponentExporter.class);
        }
        return componentClass;
    }

    @Override
    public C getComponentInstance(Instantiator instantiator) {
        if (componentReference == null) {
            componentReference = instantiator.getOrCreate(this.getComponentClass());

            if (instanceConfigurator != null) {
                // TODO: real IWebComponent impl
                instanceConfigurator.accept(new DummyWebComponentInterfacer<>(),
                        componentReference);
            }
        }

        return componentReference;
    }

    @Override
    public Class<WebComponentExporter<C>> getExporterClass() {
        return (Class<WebComponentExporter<C>>) exporter.getClass();
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyConfigurationMap.containsKey(propertyName);
    }
}

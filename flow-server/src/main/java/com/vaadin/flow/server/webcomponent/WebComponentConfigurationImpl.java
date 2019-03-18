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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.InstanceConfigurator;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.ReflectTools;

import elemental.json.JsonValue;

/**
 * The implementation for {@link WebComponentDefinition} given to the
 * {@link WebComponentExporter}. {@link WebComponentConfigurationRegistry} exposes
 * the builder to the
 * {@link com.vaadin.flow.component.webcomponent.WebComponentUI} and
 * {@link com.vaadin.flow.component.webcomponent.WebComponentWrapper} through
 * the {@link WebComponentConfiguration} interface.
 *
 * @param <C> type of the component being exported
 */
public class WebComponentConfigurationImpl<C extends Component>
        extends WebComponentDefinition<C>
        implements WebComponentConfiguration<C> {
    // TODO: get rid of these and json (de)serialize on the spot
    private static final List<Class> SUPPORTED_TYPES = Arrays.asList(
            Boolean.class, String.class, Integer.class, Double.class,
            JsonValue.class);

    private final String tag;
    private final Class<C> componentClass;
    private InstanceConfigurator<C> instanceConfigurator;
    private Map<String, PropertyConfigurationImp<C, ? extends Serializable>> propertyConfigurationMap =
            new HashMap<>();

    /**
     * Constructs a new {@code WebComponentConfigurationImp} for a web
     * component identified by {@code tag}. The constructor calls
     * {@link WebComponentExporter#define(WebComponentDefinition)} using
     * itself as the {@link WebComponentDefinition}.
     *
     * @param tag       tag name of the web component being exported
     * @param exporter  exporter, which defines this configuration
     */
    public WebComponentConfigurationImpl(String tag, WebComponentExporter<C> exporter) {
        Objects.requireNonNull(tag, "Parameter 'tag' must not be null!");
        Objects.requireNonNull(exporter, "Parameter 'exporter' must not be null!");

        this.tag = tag;
        exporter.define(this);

        componentClass = (Class<C>) ReflectTools.getGenericInterfaceType(
                    exporter.getClass(), WebComponentExporter.class);
    }

    @Override
    public <P extends Serializable> PropertyConfiguration<C, P> addProperty(
            String name, Class<P> type, P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' cannot be null!");
        Objects.requireNonNull(type, "Parameter 'type' cannot be null!");

        if (!isSupportedType(type)) {
            throw new UnsupportedPropertyTypeException(String.format("PropertyConfiguration " +
                    "cannot handle type %s. Use any of %s instead.",
                    type.getCanonicalName(),
                    SUPPORTED_TYPES.stream().map(Class::getSimpleName)
                            .collect(Collectors.joining(", "))));
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

    public String getWebComponentTag() {
        return tag;
    }

    @Override
    public Class<? extends Serializable> getPropertyType(String propertyName) {
        if (propertyConfigurationMap.containsKey(propertyName)) {
            return propertyConfigurationMap.get(propertyName).getPropertyData().getType();
        } else {
            return null;
        }
    }

    @Override
    public Set<PropertyData<?>> getPropertyDataSet() {
        return propertyConfigurationMap.values().stream()
                .map(PropertyConfigurationImp::getPropertyData)
                .collect(Collectors.toSet());
    }

    @Override
    public WebComponentBinding<C> createBinding(Instantiator instantiator) {
        Objects.requireNonNull(instantiator, "Parameter 'instantiator' must not" +
                " be null!");

        final C componentReference =
                instantiator.getOrCreate(this.getComponentClass());

        if (componentReference == null) {
            throw new RuntimeException("Failed to instantiate a new " +
                    this.getComponentClass().getCanonicalName());
        }

        // TODO: real IWebComponent impl
        if (instanceConfigurator != null) {
            instanceConfigurator.accept(new WebComponentImpl<>(),
                    componentReference);
        }

        Set<PropertyBinding<? extends Serializable>> propertyBindings =
                propertyConfigurationMap.values().stream()
                        .map(propertyConfig -> {
                            SerializableBiConsumer<C, Serializable> consumer =
                                    propertyConfig.getOnChangeHandler();
                            return new PropertyBinding<>(
                                    propertyConfig.getPropertyData(),
                                    consumer == null ? null : value ->
                                            consumer.accept(componentReference,
                                                    value));
                        })
                        .collect(Collectors.toSet());

        WebComponentBindingImpl<C> binding =
                new WebComponentBindingImpl<>(componentReference,
                        propertyBindings);

        binding.updatePropertiesToComponent();

        return binding;
    }

    @Override
    public Class<C> getComponentClass() {
        return componentClass;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyConfigurationMap.containsKey(propertyName);
    }

    private static boolean isSupportedType(Class clazz) {
        return SUPPORTED_TYPES.contains(clazz);
    }
}

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
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.InstanceConfigurator;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.component.webcomponent.WebComponentProxy;
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

    private final String exporterName;
    private final String tag;
    private final Class<C> componentClass;
    private InstanceConfigurator<C> instanceConfigurator;
    private Map<String, PropertyConfigurationImp<C, ? extends Serializable>> propertyConfigurationMap =
            new HashMap<>();

    /**
     * Constructs a new {@code WebComponentConfigurationImp} based on the
     * {@link WebComponentExporter}.
     * <p>
     * The constructor calls
     * {@link WebComponentExporter#define(WebComponentDefinition)} using
     * itself as the {@link WebComponentDefinition}.
     *
     * @param exporter  exporter, which defines this configuration
     */
    public WebComponentConfigurationImpl(WebComponentExporter<C> exporter) {
        Objects.requireNonNull(exporter, "Parameter 'exporter' must not be null!");

        Tag tagAnnotation = exporter.getClass().getAnnotation(Tag.class);
        if (tagAnnotation == null) {
            throw new InvalidParameterException(String.format("'%s' is " +
                    "missing @%s annotation!",
                    exporter.getClass().getCanonicalName(),
                    Tag.class.getSimpleName()));
        }

        this.tag = tagAnnotation.value();
        this.exporterName = exporter.getClass().getCanonicalName();
        exporter.define(this);

        componentClass = (Class<C>) ReflectTools.getGenericInterfaceType(
                    exporter.getClass(), WebComponentExporter.class);

        if (componentClass == null) {
            throw new IllegalStateException(String.format("Could not find " +
                    "the Class of %s exported by '%s'!",
                    Component.class.getSimpleName(),
                    WebComponentExporter.class.getCanonicalName()));
        }
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
    public void bindProxy(Instantiator instantiator,
                          WebComponentProxy proxy) {
        Objects.requireNonNull(instantiator, "Parameter 'instantiator' must not" +
                " be null!");
        Objects.requireNonNull(proxy, "Parameter 'proxy' must not be null!");

        final C componentReference =
                instantiator.getOrCreate(this.getComponentClass());

        if (componentReference == null) {
            throw new RuntimeException("Failed to instantiate a new " +
                    this.getComponentClass().getCanonicalName());
        }

        /*
            The tag check cannot be done before the creation of the Component
             being exported, as the WebComponentConfigurationImpl itself is
             constructed only when the first request for a web component
             instance comes in. This is due to the unavailability of
             Instantiator before VaadinService has been initialized (which
             happens after collecting all the exporters.
         */
        String componentTag = componentReference.getElement().getTag();
        if (this.tag.equals(componentTag)) {
            throw new IllegalStateException(String.format(
                    "WebComponentExporter '%s' cannot share a tag with the " +
                            "%s instance being exported! Change the tag " +
                            "from '%s' to something else.",
                    this.exporterName,
                    componentReference.getClass().getCanonicalName(),
                    this.tag));
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

        proxy.setWebComponentBinding(binding);

        if (instanceConfigurator != null) {
            instanceConfigurator.accept(new WebComponentImpl<>(binding, proxy),
                    componentReference);
        }

        binding.updatePropertiesToComponent();
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

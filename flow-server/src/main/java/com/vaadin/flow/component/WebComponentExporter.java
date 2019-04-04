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

package com.vaadin.flow.component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.PropertyData;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.webcomponent.UnsupportedPropertyTypeException;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationFactory;

import elemental.json.JsonValue;

/**
 * Provides a way to exporter a class which extends {@link Component} as an
 * embeddable web component The tag of the exporter web component <b>must be</b>
 * defined using {@link Tag} annotation - otherwise, an exception will be
 * thrown.
 * <p>
 * Limitations regarding the tag are:
 * <ul>
 * <li>The tag must be a non-null, non-empty string with dash-separated words,
 * i.e. "dash-separated".</li>
 * <li>Exporter cannot share the tag with the component being exported. If they
 * do, an exception will be thrown during run-time.</li>
 * </ul>
 * <p>
 * The exported web components can be embedded into non-Vaadin applications.
 * <p>
 * Example of exporting {@code MyComponent} component:
 *
 * <pre>
 * &#064;Tag("my-component")
 * public class Exporter implements WebComponentExporter&lt;MyComponent&gt;() {
 *     &#064;Override
 *     public void define(WebComponentDefinition&lt;MyComponent&gt;
 *              definition) {
 *         definition.addProperty("name", "John Doe")
 *                 .onChange(MyComponent::setName);
 *     }
 *
 *     &#064;Override
 *     public void configure(WebComponent&lt;MyComponent&gt;
 *              webComponent, MyComponent component) {
 *          // add e.g. a listener to the {@code component}
 *          // and do something with {@code webComponent}
 *     }
 * }
 * </pre>
 *
 * @param <C>
 *            type of the component to export
 */
public abstract class WebComponentExporter<C extends Component>
        implements Serializable {

    private static final List<Class> SUPPORTED_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    Boolean.class, String.class, Integer.class, Double.class,
                    JsonValue.class));

    private final String tag;
    private final Class<C> componentClass;
    private Map<String, PropertyConfigurationImpl<C, ? extends Serializable>> propertyConfigurationMap = new HashMap<>();
    private WebComponentConfigurationImpl configuration;

    /**
     * <b>Do not implement this constructor unless this the extending class
     * is meant to be extended further!</b>
     * <p>
     * This constructor is meant to be called using {@code super(String)}
     * from a no-args constructor declared by the extending class.
     *
     * @param tag   tag name of the web component created by the exporter
     */
    protected WebComponentExporter(String tag) {
        Objects.requireNonNull(tag,
                "Parameter 'tag' must not be null!");
        this.tag = tag;

        componentClass = (Class<C>) ReflectTools.getGenericInterfaceType(
                this.getClass(), WebComponentExporter.class);

        assert componentClass != null : "Failed to determine component class "
                + "from WebComponentExporter's type parameter.";

        WebComponentConfigurationFactory.setConfiguration(
                new WebComponentConfigurationImpl());
    }

    private <P extends Serializable> PropertyConfiguration<C, P> addProperty(
            String name, Class<P> type, P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' cannot be null!");
        Objects.requireNonNull(type, "Parameter 'type' cannot be null!");

        if (!isSupportedType(type)) {
            throw new UnsupportedPropertyTypeException(String.format(
                    "PropertyConfiguration "
                            + "cannot handle type %s. Use any of %s instead.",
                    type.getCanonicalName(),
                    SUPPORTED_TYPES.stream().map(Class::getSimpleName)
                            .collect(Collectors.joining(", "))));
        }

        PropertyConfigurationImpl<C, P> propertyConfigurationImpl =
                new PropertyConfigurationImpl<>(
                componentClass, name, type, defaultValue);

        propertyConfigurationMap.put(name, propertyConfigurationImpl);

        return propertyConfigurationImpl;
    }

    /**
     * Add an {@code Integer} property to the exported web component identified
     * by {@code name}.
     *
     * @param name
     *            name of the property. While all formats are allowed, names in
     *            camelCase will be converted to dash-separated form, when
     *            property update events are generated, using form
     *            "property-name-changed", if the property is called
     *            "propertyName"
     * @param defaultValue
     *            default value of property.
     * @return fluent {@code PropertyConfiguration} for configuring the property
     */
    public final PropertyConfiguration<C, Integer> addProperty(
            String name, int defaultValue) {
        return addProperty(name, Integer.class, defaultValue);
    }

    /**
     * Add an {@code Double} property to the exported web component identified
     * by {@code name}.
     *
     * @param name
     *            name of the property. While all formats are allowed, names in
     *            camelCase will be converted to dash-separated form, when
     *            property update events are generated, using form
     *            "property-name-changed", if the property is called
     *            "propertyName"
     * @param defaultValue
     *            default value of property.
     * @return fluent {@code PropertyConfiguration} for configuring the property
     */
    public final PropertyConfiguration<C, Double> addProperty(
            String name, double defaultValue) {
        return addProperty(name, Double.class, defaultValue);
    }

    /**
     * Add an {@code String} property to the exported web component identified
     * by {@code name}.
     *
     * @param name
     *            name of the property. While all formats are allowed, names in
     *            camelCase will be converted to dash-separated form, when
     *            property update events are generated, using form
     *            "property-name-changed", if the property is called
     *            "propertyName"
     * @param defaultValue
     *            default value of property.
     * @return fluent {@code PropertyConfiguration} for configuring the property
     */
    public final PropertyConfiguration<C, String> addProperty(
            String name, String defaultValue) {
        return addProperty(name, String.class, defaultValue);
    }

    /**
     * Add an {@code Boolean} property to the exported web component identified
     * by {@code name}.
     *
     * @param name
     *            name of the property. While all formats are allowed, names in
     *            camelCase will be converted to dash-separated form, when
     *            property update events are generated, using form
     *            "property-name-changed", if the property is called
     *            "propertyName"
     * @param defaultValue
     *            default value of property.
     * @return fluent {@code PropertyConfiguration} for configuring the property
     */
    public final PropertyConfiguration<C, Boolean> addProperty(
            String name, boolean defaultValue) {
        return addProperty(name, Boolean.class, defaultValue);
    }

    /**
     * Add an {@code JsonValue} property to the exported web component
     * identified by {@code name}.
     *
     * @param name
     *            name of the property. While all formats are allowed, names in
     *            camelCase will be converted to dash-separated form, when
     *            property update events are generated, using form
     *            "property-name-changed", if the property is called
     *            "propertyName"
     * @param defaultValue
     *            default value of property.
     * @return fluent {@code PropertyConfiguration} for configuring the property
     */
    public final PropertyConfiguration<C, JsonValue> addProperty(
            String name, JsonValue defaultValue) {
        return addProperty(name, JsonValue.class, defaultValue);
    }

    /**
     * If custom initialization for the created {@link Component} instance is
     * needed, it can be done here. It is also possible to configure custom
     * communication between the {@code component} instance and client-side web
     * component using the {@link WebComponent} instance. The {@code
     * webComponent} and {@code component} are in 1-to-1 relation.
     *
     * @param webComponent
     *            instance representing the client-side web component instance
     *            matching the component
     * @param component
     *            exported component instance
     */
    public abstract void configureInstance(
            WebComponent<C> webComponent, C component);

    private static boolean isSupportedType(Class clazz) {
        return SUPPORTED_TYPES.contains(clazz);
    }

    private class WebComponentConfigurationImpl implements WebComponentConfiguration<C> {

        @Override
        public boolean hasProperty(String propertyName) {
            return propertyConfigurationMap.containsKey(propertyName);
        }

        @Override
        public Class<? extends Serializable> getPropertyType(String propertyName) {
            if (propertyConfigurationMap.containsKey(propertyName)) {
                return propertyConfigurationMap.get(propertyName).getPropertyData()
                        .getType();
            } else {
                return null;
            }
        }

        @Override
        public Class<C> getComponentClass() {
            return componentClass;
        }

        @Override
        public Set<PropertyData<? extends Serializable>> getPropertyDataSet() {
            return propertyConfigurationMap.values().stream()
                    .map(PropertyConfigurationImpl::getPropertyData)
                    .collect(Collectors.toSet());
        }

        @Override
        public WebComponentBinding<C> createWebComponentBinding(Instantiator instantiator, Element element) {
            assert (instantiator != null);

            final C componentReference =
                    instantiator.createComponent(this.getComponentClass()) ;

            if (componentReference == null) {
                throw new RuntimeException("Failed to instantiate a new "
                        + this.getComponentClass().getCanonicalName());
            }

            /*
             * The tag check cannot be done before the creation of the Component
             * being exported, as the WebComponentConfigurationImpl itself is
             * constructed only when the first request for a web component instance
             * comes in. This is due to the unavailability of Instantiator before
             * VaadinService has been initialized (which happens after collecting
             * all the exporters.
             */
            String componentTag = componentReference.getElement().getTag();
            if (tag.equals(componentTag)) {
                throw new IllegalStateException(String.format(
                        "WebComponentExporter '%s' cannot share a tag with the "
                                + "%s instance being exported! Change the tag "
                                + "from '%s' to something else.",
                        this.getClass().getCanonicalName(),
                        componentReference.getClass().getCanonicalName(),
                        tag));
            }

            WebComponentBinding<C> binding =
                    new WebComponentBinding<>(componentReference);

            propertyConfigurationMap
                    .values().forEach(binding::bindProperty);

            configureInstance(new WebComponent<>(binding, element),
                    binding.getComponent());

            binding.updatePropertiesToComponent();

            return binding;
        }

        @Override
        public String getTag() {
            return tag;
        }
    }
}

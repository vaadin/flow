/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.PropertyData;
import com.vaadin.flow.server.webcomponent.UnsupportedPropertyTypeException;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Exports a {@link Component} as a web component.
 * <p>
 * By extending this class you can export a server side {@link Component} with a
 * given tag name so that it can be included in any web page as
 * {@code <tag-name>}. You can add properties/attributes to the element, which
 * are synchronized with the server and you can fire events from the server,
 * which are available as custom events in the browser.
 * <p>
 * The tag name (must contain at least one dash and be unique on the target web
 * page) is provided through the super constructor. Note that the exporter tag
 * is not related to the tag used by the {@link Component} being exported and
 * they cannot be the same.
 * <p>
 * The component class to exported is determined by the parameter given to
 * {@code WebComponentExporter} when extending it, e.g.
 * {@code extends WebComponentExporter<MyComponent>}.
 * <p>
 * If you want to customize the {@link Component} instance after it has been
 * created, you should override
 * {@link #configureInstance(WebComponent, Component)} which is called for each
 * created instance.
 * <p>
 * Example of exporting {@code MyComponent} to be used as
 * {@code <my-component name="something">}:
 *
 * <pre>
 * public class Exporter extends WebComponentExporter&lt;MyComponent&gt;() {
 *     public Exporter() {
 *         super("my-component");
 *         addProperty("name", "John Doe").onChange(MyComponent::setName);
 *     }
 *     protected void configureInstance(WebComponent&lt;MyComponent&gt; webComponent, MyComponent component) {
 *         // Configuration for the component instance goes here
 *     }
 * }
 * </pre>
 *
 * @param <C>
 *            type of the component to export
 * @author Vaadin Ltd.
 * @since 2.0
 *
 * @see WebComponentExporterFactory
 */
public abstract class WebComponentExporter<C extends Component>
        implements Serializable {

    private static final List<Class> SUPPORTED_TYPES = Collections
            .unmodifiableList(
                    Arrays.asList(Boolean.class, String.class, Integer.class,
                            Double.class, JsonValue.class, JsonNode.class));

    private final String tag;
    private HashMap<String, PropertyConfigurationImpl<C, ? extends Serializable>> propertyConfigurationMap = new HashMap<>();

    private boolean isConfigureInstanceCall;

    /**
     * Creates a new {@code WebComponentExporter} instance and configures the
     * tag name of the web component created based on this exporter.
     * <p>
     * This constructor is not meant to be overridden unless the {@code
     * exporter} can be extended. Rather, create a non-args constructor and call
     * this constructor from it.
     *
     * @param tag
     *            tag name of the web component created by the exporter, cannot
     *            be {@code null}
     */
    @SuppressWarnings("unchecked")
    protected WebComponentExporter(String tag) {
        if (tag == null) {
            throw new NullTagException("Parameter 'tag' must not be null!");
        }
        this.tag = tag;
        if (getComponentClass() == null) {
            throw new IllegalStateException(String.format("Failed to "
                    + "determine component type for '%s'. Please "
                    + "provide a valid type for %s as a type parameter.",
                    getClass().getName(),
                    WebComponentExporter.class.getSimpleName()));
        }
    }

    private <P extends Serializable> PropertyConfiguration<C, P> addProperty(
            String name, Class<P> type, P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' cannot be null!");
        Objects.requireNonNull(type, "Parameter 'type' cannot be null!");

        if (isConfigureInstanceCall) {
            throw new IllegalStateException(
                    "The 'addProperty' method cannot be called within "
                            + "the 'configureInstance' method. All properties have "
                            + "to be configured in the " + getClass().getName()
                            + " class constructor.");
        }

        if (!isSupportedType(type)) {
            throw new UnsupportedPropertyTypeException(String.format(
                    "PropertyConfiguration "
                            + "cannot handle type %s. Use any of %s instead.",
                    type.getCanonicalName(),
                    SUPPORTED_TYPES.stream().map(Class::getSimpleName)
                            .collect(Collectors.joining(", "))));
        }

        PropertyConfigurationImpl<C, P> propertyConfigurationImpl = new PropertyConfigurationImpl<>(
                getComponentClass(), name, type, defaultValue);

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
    public final PropertyConfiguration<C, Integer> addProperty(String name,
            int defaultValue) {
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
    public final PropertyConfiguration<C, Double> addProperty(String name,
            double defaultValue) {
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
    public final PropertyConfiguration<C, String> addProperty(String name,
            String defaultValue) {
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
    public final PropertyConfiguration<C, Boolean> addProperty(String name,
            boolean defaultValue) {
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
    @Deprecated
    public final PropertyConfiguration<C, JsonValue> addProperty(String name,
            JsonValue defaultValue) {
        return addProperty(name, JsonValue.class, defaultValue);
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
    public final PropertyConfiguration<C, BaseJsonNode> addProperty(String name,
            BaseJsonNode defaultValue) {
        return addProperty(name, BaseJsonNode.class, defaultValue);
    }

    /**
     * If custom initialization for the created {@link Component} instance is
     * needed, it can be done here. It is also possible to configure custom
     * communication between the {@code component} instance and client-side web
     * component using the {@link WebComponent} instance. The {@code
     * webComponent} and {@code component} are in 1-to-1 relation.
     * <p>
     * Note that it's incorrect to call any {@code addProperty} method within
     * {@link #configureInstance(WebComponent, Component)} method. All
     * properties have to be configured inside the exporter's constructor.
     *
     * @param webComponent
     *            instance representing the client-side web component instance
     *            matching the component
     * @param component
     *            instance of the exported web component
     */
    protected abstract void configureInstance(WebComponent<C> webComponent,
            C component);

    /**
     * Always called before {@link #configureInstance(WebComponent, Component)}.
     */
    public final void preConfigure() {
        isConfigureInstanceCall = true;
    }

    /**
     * Always called after {@link #configureInstance(WebComponent, Component)}.
     */
    public final void postConfigure() {
        isConfigureInstanceCall = false;
    }

    /**
     * The tag associated with the exported component.
     *
     * @return the tag
     */
    public final String getTag() {
        return tag;
    }

    private static boolean isSupportedType(Class clazz) {
        return SUPPORTED_TYPES.contains(clazz);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final class WebComponentConfigurationImpl<C extends Component>
            implements WebComponentConfiguration<C> {
        private WebComponentExporter<C> exporter;
        private final Map<String, PropertyConfigurationImpl<C, ? extends Serializable>> immutablePropertyMap;

        private WebComponentConfigurationImpl(
                WebComponentExporter<C> exporter) {
            this.exporter = exporter;
            WebComponentExporter wcExporter = exporter;
            immutablePropertyMap = Collections
                    .unmodifiableMap(wcExporter.propertyConfigurationMap);
        }

        @Override
        public boolean hasProperty(String propertyName) {
            return immutablePropertyMap.containsKey(propertyName);
        }

        @Override
        public Class<? extends Serializable> getPropertyType(
                String propertyName) {
            if (hasProperty(propertyName)) {
                return immutablePropertyMap.get(propertyName).getPropertyData()
                        .getType();
            } else {
                return null;
            }
        }

        @Override
        public Class<C> getComponentClass() {
            return this.exporter.getComponentClass();
        }

        @Override
        public Set<PropertyData<? extends Serializable>> getPropertyDataSet() {
            return immutablePropertyMap.values().stream()
                    .map(PropertyConfigurationImpl::getPropertyData)
                    .collect(Collectors.toSet());
        }

        @Override
        public WebComponentBinding<C> createWebComponentBinding(
                Instantiator instantiator, Element element,
                JsonObject newAttributeDefaults) {
            return createWebComponentBinding(instantiator, element,
                    JacksonUtils.mapElemental(newAttributeDefaults));
        }

        @Override
        public WebComponentBinding<C> createWebComponentBinding(
                Instantiator instantiator, Element element,
                JsonNode newAttributeDefaults) {
            assert (instantiator != null);

            final C componentReference = instantiator
                    .createComponent(this.getComponentClass());

            if (componentReference == null) {
                throw new RuntimeException("Failed to instantiate a new "
                        + this.getComponentClass().getCanonicalName());
            }

            /*
             * The tag check cannot be done before the creation of the Component
             * being exported. This is due to the unavailability of Instantiator
             * before VaadinService has been initialized (which happens after
             * collecting all the exporters).
             */
            String componentTag = componentReference.getElement().getTag();
            if (this.exporter.getTag().equals(componentTag)) {
                throw new IllegalStateException(String.format(
                        "WebComponentExporter '%s' cannot share a tag with the "
                                + "%s instance being exported! Change the tag "
                                + "from '%s' to something else.",
                        this.getClass().getCanonicalName(),
                        componentReference.getClass().getCanonicalName(),
                        this.exporter.getTag()));
            }

            WebComponentBinding<C> binding = new WebComponentBinding<>(
                    componentReference);

            // collect possible new defaults from attributes as JsonValues
            final Map<String, JsonNode> newDefaultValues = JacksonUtils
                    .getKeys(newAttributeDefaults).stream().collect(Collectors
                            .toMap(key -> key, newAttributeDefaults::get));

            // bind properties onto the WebComponentBinding. Since
            // PropertyConfigurations are Exporter level constructs, we need
            // to create instance level bindings, hence PropertyBindings.
            // if newDefaultValues contains a new default value for the
            // component instance, that is used
            immutablePropertyMap.values().forEach(propertyConfiguration -> {
                String key = propertyConfiguration.getPropertyData().getName();
                binding.bindProperty(propertyConfiguration,
                        newDefaultValues.containsKey(key),
                        newDefaultValues.get(key));
            });

            exporter.preConfigure();
            try {
                this.exporter.configureInstance(
                        new WebComponent<>(binding, element),
                        binding.getComponent());
            } finally {
                exporter.postConfigure();
            }

            binding.updatePropertiesToComponent();

            return binding;
        }

        @Override
        public String getTag() {
            return this.exporter.getTag();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends WebComponentExporter<C>> getExporterClass() {
            return (Class<? extends WebComponentExporter<C>>) exporter
                    .getClass();
        }

        @Override
        public int hashCode() {
            Object[] objs = new Object[immutablePropertyMap.size() + 1];

            objs[0] = getTag();
            int place = 1;
            for (PropertyConfiguration configuration : immutablePropertyMap
                    .values()) {
                objs[place] = configuration;
                place++;
            }

            return Objects.hash(objs);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof WebComponentConfigurationImpl) {

                WebComponentConfigurationImpl<?> other = (WebComponentConfigurationImpl<?>) obj;

                boolean isSame = getTag().equals(other.getTag());
                isSame = isSame && (immutablePropertyMap
                        .size() == other.immutablePropertyMap.size());

                if (!isSame) {
                    return false;
                }

                PropertyConfiguration<?, ?> otherConf;
                for (String key : immutablePropertyMap.keySet()) {
                    otherConf = other.immutablePropertyMap.get(key);
                    if (!immutablePropertyMap.get(key).equals(otherConf)) {
                        return false;
                    }
                }
                return true;
            }

            return false;
        }
    }

    /**
     * The concrete component class object. By default creates an instance of
     * the actual type parameter.
     *
     * @return component class
     */
    protected Class<C> getComponentClass() {
        return (Class<C>) ReflectTools.getGenericInterfaceType(this.getClass(),
                WebComponentExporter.class);
    }

    /**
     * Produces {@link WebComponentConfiguration} instances from either
     * {@link WebComponentExporter} classes or instances.
     *
     * @author Vaadin Ltd
     */
    public static final class WebComponentConfigurationFactory
            implements Serializable {

        /**
         * Creates a {@link WebComponentConfiguration} for the provided
         * {@link WebComponentExporter} instances.
         *
         * @param exporter
         *            exporter instance, not {@code null}
         * @return a web component configuration matching the instance of
         *         received {@code exporter}
         * @throws NullPointerException
         *             when {@code exporter} is {@code null}
         *
         * @param <T>
         *            type of the component to export
         */
        public <T extends Component> WebComponentConfiguration<T> create(
                WebComponentExporter<T> exporter) {
            Objects.requireNonNull(exporter,
                    "Parameter 'exporter' cannot be " + "null!");

            return new WebComponentConfigurationImpl<>(exporter);
        }

    }

    static class NullTagException extends NullPointerException {
        NullTagException(String msg) {
            super(msg);
        }
    }
}

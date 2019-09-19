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
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Acts as a proxy to the web component root onto which the exporter {@link
 * Component} is added. Allows updating web component properties and firing
 * custom events on the client-side.
 *
 * @param <C>
 *         type of the {@code component} exported as web component
 * @author Vaadin Ltd.
 * @since 2.0
 */
public final class WebComponent<C extends Component> implements Serializable {
    private static final String UPDATE_PROPERTY = "this" +
            "._updatePropertyFromServer($0, $1);";
    private static final String UPDATE_PROPERTY_NULL = "this" +
            "._updatePropertyFromServer($0, null);";
    private static final String UPDATE_PROPERTY_FORMAT = "this" +
            "._updatePropertyFromServer($0, %s);";
    private static final String CUSTOM_EVENT = "this.dispatchEvent(new " +
            "CustomEvent($0, %s));";

    private static final EventOptions BASIC_OPTIONS = new EventOptions();

    private Element componentHost;
    private WebComponentBinding binding;

    private WebComponent() {
    }

    /**
     * Constructs a {@link WebComponent}. {@link WebComponentBinding} provides
     * the instance of the {@link Component} exported as a web component. {@code
     * Binding} also defines the properties the web component has. {@link
     * Element} is the host element which contains the exported {@code
     * component} instance (provided by the {@code binding}).
     *
     * @param binding
     *         binds web component configuration to {@code component X}
     * @param componentHost
     *         host {@code component X} on the embedding page
     * @see com.vaadin.flow.component.webcomponent.WebComponentWrapper for
     *         the web component host
     */
    public WebComponent(WebComponentBinding binding,
                        Element componentHost) {
        Objects.requireNonNull(binding, "Parameter 'binding' must not be " +
                "null!");
        Objects.requireNonNull(componentHost, "Parameter " +
                "'webComponentWrapper' must not be null!");
        this.binding = binding;
        this.componentHost = componentHost;
    }

    /**
     * Fires a custom event on the client-side originating from the web
     * component. This event does not bubble in the DOM hierarchy.
     *
     * @param eventName
     *         name of the event, not null
     * @see #fireEvent(String, JsonValue, EventOptions) for full set of
     *         options
     */
    public void fireEvent(String eventName) {
        fireEvent(eventName, Json.createNull(), BASIC_OPTIONS);
    }

    /**
     * Fires a custom event on the client-side originating from the web
     * component with custom event data. This event does not bubble in the DOM
     * hierarchy.
     *
     * @param eventName
     *         name of the event, not null
     * @param objectData
     *         data the event should carry. This data is placed as the {@code
     *         detail} property of the event, nullable
     * @see #fireEvent(String, JsonValue, EventOptions) for full set of
     *         options
     */
    public void fireEvent(String eventName, JsonValue objectData) {
        fireEvent(eventName, objectData, BASIC_OPTIONS);
    }

    /**
     * Fires a custom event on the client-side originating from the web
     * component with custom event data. Allows modifying the default event
     * behavior with {@link EventOptions}.
     *
     * @param eventName
     *         name of the event, not null
     * @param objectData
     *         data the event should carry. This data is placed as the {@code
     *         detail} property of the event, nullable
     * @param options
     *         event options for {@code bubbles}, {@code cancelable}, and {@code
     *         composed} flags, not null
     * @throws NullPointerException
     *         if either {@code eventName} or {@code options} is {@code null}
     */
    public void fireEvent(String eventName, JsonValue objectData, EventOptions options) {
        Objects.requireNonNull(eventName, "Parameter 'eventName' must not be " +
                "null!");
        Objects.requireNonNull(options, "Parameter 'options' must not be null");

        JsonObject object = Json.createObject();
        object.put("bubbles", options.isBubbles());
        object.put("cancelable", options.isCancelable());
        object.put("composed", options.isComposed());
        object.put("detail", objectData == null ?
                Json.createNull() : objectData);

        componentHost.executeJs(String.format(CUSTOM_EVENT,
                object.toJson()), eventName);
    }

    /**
     * Sets property value on the client-side to the given {@code value}. The
     * required {@link PropertyConfigurationImpl} is received from {@link
     * com.vaadin.flow.component.WebComponentExporter} when a new property is
     * added for the web component.
     *
     * @param propertyConfiguration
     *         identifies the property for which the value is being set, not
     *         {@code null}
     * @param value
     *         new value for the property, can be {@code null}
     * @param <P>
     *         type of the property value being set. If the type does not match
     *         the original property type, throws an exception
     * @throws NullPointerException
     *         if {@code propertyConfiguration} is {@code null}
     * @throws IllegalArgumentException
     *         if {@code PropertyConfiguration} is not a correct implementation
     * @throws IllegalArgumentException
     *         if the web component does not have a property identified by
     *         {@code propertyConfiguration}
     * @throws IllegalArgumentException
     *         the provided {@code value} is not of the type expected by the
     *         property
     */
    @SuppressWarnings("unchecked")
    public <P extends Serializable> void setProperty(PropertyConfiguration<C, P> propertyConfiguration, P value) {
        Objects.requireNonNull(propertyConfiguration, "Parameter " +
                "'propertyConfiguration' must not be null!");

        if (!(propertyConfiguration instanceof PropertyConfigurationImpl)) {
            throw new IllegalArgumentException(String.format("Parameter " +
                            "'propertyConfiguration' is not an implementation of %s",
                    PropertyConfigurationImpl.class));
        }
        PropertyConfigurationImpl<C, P> propertyConfigurationImpl =
                (PropertyConfigurationImpl<C, P>) propertyConfiguration;

        String propertyName = propertyConfigurationImpl.getPropertyData().getName();

        // does the binding actually have the property
        if (!binding.hasProperty(propertyName)) {
            throw new IllegalArgumentException(String.format("%s does not " +
                            "have a property identified by '%s'!",
                    WebComponent.class.getSimpleName(), propertyName));
        }

        // is the property's value type correct
        if (value != null && !binding.getPropertyType(propertyName).isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format("Property '%s' " +
                            "of type '%s' cannot be assigned value of type '%s'!",
                    propertyName,
                    binding.getPropertyType(propertyName).getName(),
                    value.getClass().getCanonicalName()));
        }

        setProperty(propertyName, value);
    }

    private void setProperty(String propertyName, Object value) {

        if (value == null) {
            componentHost.executeJs(UPDATE_PROPERTY_NULL, propertyName);
        }

        if (value instanceof Integer) {
            componentHost.executeJs(UPDATE_PROPERTY, propertyName,
                    (Integer) value);
        } else if (value instanceof Double) {
            componentHost.executeJs(UPDATE_PROPERTY, propertyName,
                    (Double) value);
        } else if (value instanceof String) {
            componentHost.executeJs(UPDATE_PROPERTY, propertyName,
                    (String) value);
        } else if (value instanceof Boolean) {
            componentHost.executeJs(UPDATE_PROPERTY, propertyName,
                    (Boolean) value);
        } else if (value instanceof JsonValue) {
            // this gets around executeJavaScript limitation.
            // Since properties can take JsonValues, this was needed to allow
            // that expected behavior.
            componentHost.executeJs(String.format(UPDATE_PROPERTY_FORMAT,
                    ((JsonValue) value).toJson()),
                    propertyName);
        }
    }
}

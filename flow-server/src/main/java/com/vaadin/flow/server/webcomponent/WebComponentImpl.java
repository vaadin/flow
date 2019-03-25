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
import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.dom.Element;

import elemental.json.JsonValue;

/**
 * {@inheritDoc}.
 * @param <C>   {@code component} being exported
 */
class WebComponentImpl<C extends Component> implements WebComponent<C> {
    private static final String UPDATE_PROPERTY = "this" +
            "._updatePropertyFromServer($0, $1);";
    private static final String UPDATE_PROPERTY_NULL = "this" +
            "._updatePropertyFromServer($0, null);";
    private static final String UPDATE_PROPERTY_FORMAT = "this" +
            "._updatePropertyFromServer($0, %s);";

    private Element componentHost;
    private WebComponentBinding binding;

    /**
     * Constructs an internal implementation for {@link WebComponent}. {@code
     * WebComponentImpl} uses {@link WebComponentBinding} to verify properties
     * and value types given as parameters to its methods. {@link Element} is
     * the host element which contains the exported {@code component}
     * instance (provided by the {@code binding}).
     *
     * @param binding   binds web component configuration to {@code component X}
     * @param componentHost     host {@code component X} on the embedding page
     * @see com.vaadin.flow.component.webcomponent.WebComponentWrapper for
     * the web component host
     */
    WebComponentImpl(WebComponentBinding binding,
                     Element componentHost) {
        Objects.requireNonNull(binding, "Parameter 'binding' must not be " +
                "null!");
        Objects.requireNonNull(componentHost, "Parameter " +
                "'webComponentWrapper' must not be null!");
        this.binding = binding;
        this.componentHost = componentHost;
    }

    @Override
    public void fireEvent(String eventName) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void fireEvent(String eventName, JsonValue objectData) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void fireEvent(String eventName, JsonValue objectData, EventOptions options) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public <P extends Serializable> void setProperty(PropertyConfiguration<C, P> propertyConfiguration, P value) {
        Objects.requireNonNull(propertyConfiguration, "Parameter " +
                "'propertyConfiguration' must not be null!");

        // if this fails, then the user attempted to use their own
        // implementation of PropertyConfiguration, which is nonsensical.
        PropertyConfigurationImp<C, P> propertyConfigurationImp;
        try {
            propertyConfigurationImp =
                    (PropertyConfigurationImp<C, P>) propertyConfiguration;
        } catch (ClassCastException e) {
            LoggerFactory.getLogger(WebComponentImpl.class).warn(String.format(
                    "Could not cast %s into %s.",
                    propertyConfiguration.getClass().getCanonicalName(),
                    PropertyConfiguration.class.getSimpleName()),
                    e);
            throw new InvalidParameterException(String.format("Parameter " +
                    "'propertyConfiguration' must be an implementation of " +
                    "'%s!", PropertyConfigurationImp.class.getCanonicalName()));
        }

        String propertyName = propertyConfigurationImp.getPropertyData().getName();
        if (value != null && !binding.getPropertyType(propertyName).isAssignableFrom(value.getClass())) {
            throw new InvalidParameterException(String.format("Property '%s' " +
                            "of type '%s' cannot be assigned value of type '%s'!",
                    propertyName,
                    binding.getPropertyType(propertyName).getCanonicalName(),
                    value.getClass().getCanonicalName()));
        }

        setProperty(propertyName, value);
    }

    @Override
    public <P extends Serializable> P getProperty(PropertyConfiguration<C, P> propertyConfiguration) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }


    private void setProperty(String propertyName, Object value) {

        if (value == null) {
            componentHost.executeJavaScript(UPDATE_PROPERTY_NULL, propertyName);
        }

        if (value instanceof Integer)  {
            componentHost.executeJavaScript(UPDATE_PROPERTY, propertyName,
                    (Integer)value);
        } else if (value instanceof Double) {
            componentHost.executeJavaScript(UPDATE_PROPERTY, propertyName,
                    (Double)value);
        } else if (value instanceof String) {
            componentHost.executeJavaScript(UPDATE_PROPERTY, propertyName,
                    (String)value);
        } else if (value instanceof JsonValue) {
            // this is a hack to get around executeJavaScript limitation.
            // Since properties can take JsonValues, this was needed to allow
            // that expected behavior.
            componentHost.executeJavaScript(String.format(UPDATE_PROPERTY_FORMAT,
                    ((JsonValue)value).toJson()),
                    propertyName);
        }
    }
}

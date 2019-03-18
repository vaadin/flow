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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.dom.Element;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * {@inheritDoc}.
 * @param <C>
 */
class WebComponentImpl<C extends Component> implements WebComponent<C> {
    private Component webComponentWrapper;
    private WebComponentBinding<C> binding;

    /**
     * @param binding
     * @param webComponentWrapper
     */
    public WebComponentImpl(WebComponentBinding<C> binding,
                            Component webComponentWrapper) {
        Objects.requireNonNull(binding, "Parameter 'binding' must not be " +
                "null!");
        Objects.requireNonNull(webComponentWrapper, "Parameter " +
                "'webComponentWrapper' must not be null!");
        this.binding = binding;
        this.webComponentWrapper = webComponentWrapper;
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
        }
        catch (ClassCastException e) {
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
        Element element = webComponentWrapper.getElement();
        if (value == null) {
            element.setPropertyJson(propertyName,
                    Json.createNull());
        }

        if (value instanceof Integer)  {
            element.setProperty(propertyName, ((Integer)value).doubleValue());
        } else if (value instanceof Double) {
            element.setProperty(propertyName, (Double)value);
        } else if (value instanceof String) {
            element.setProperty(propertyName, (String)value);
        } else if (value instanceof JsonValue) {
            element.setPropertyJson(propertyName, (JsonValue)value);
        }
    }
}

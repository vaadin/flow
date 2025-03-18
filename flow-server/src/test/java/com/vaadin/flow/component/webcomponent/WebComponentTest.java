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

package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;

import elemental.json.Json;
import elemental.json.JsonValue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WebComponentTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private WebComponent<Component> webComponent;

    @Before
    public void init() {
        WebComponentBinding<Component> componentBinding = new WebComponentBinding<>(
                mock(Component.class));
        webComponent = new WebComponent<>(componentBinding, new Element("tag"));
    }

    @Test
    public void fireEvent_throwsWhenNameIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("eventName");
        webComponent.fireEvent(null);
    }

    @Test
    public void fireEvent_doesNotThrowOnNullObjectData() {
        webComponent.fireEvent("name", (JsonNode) null);
    }

    @Test
    public void fireEvent_throwsWhenOptionsIsNull() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("options");
        webComponent.fireEvent("name", (JsonNode) null, null);
    }

    @Test
    public void setProperty_throwsOnNullPropertyConfiguration() {
        exception.expect(NullPointerException.class);
        exception.expectMessage("propertyConfiguration");
        webComponent.setProperty(null, "value");
    }

    @Test
    public void setProperty_throwsOnUnknownProperty() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage(
                "WebComponent does not have a property identified");

        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));

        WebComponent<Component> webComponent = new WebComponent<>(binding,
                new Element("tag"));

        PropertyConfigurationImpl<Component, String> configuration = new PropertyConfigurationImpl<>(
                Component.class, "property", String.class, "value");

        webComponent.setProperty(configuration, "newValue");
    }

    @Test
    public void setProperty_throwsWhenGivenWrongPropertyTypeAsParameter() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Property 'property' of type "
                + "'java.lang.Integer' cannot be assigned value of type "
                + "'java.lang.String'!");

        PropertyConfigurationImpl<Component, Integer> intConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "property", Integer.class, 0);

        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));
        binding.bindProperty(intConfiguration, false);

        WebComponent<Component> webComponent = new WebComponent<>(binding,
                new Element("tag"));

        PropertyConfigurationImpl<Component, String> stringConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "property", String.class, "value");

        webComponent.setProperty(stringConfiguration, "newValue");
    }

    @Test
    public void setProperty_attemptsToWriteSupportedTypes() {
        Element element = spy(new Element("tag"));

        // configurations
        PropertyConfigurationImpl<Component, Integer> intConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "int", Integer.class, 0);
        PropertyConfigurationImpl<Component, Double> doubleConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "double", Double.class, 0.0);
        PropertyConfigurationImpl<Component, String> stringConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "string", String.class, "");
        PropertyConfigurationImpl<Component, Boolean> booleanConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "boolean", Boolean.class, false);
        PropertyConfigurationImpl<Component, BaseJsonNode> jsonNodeConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "jsonNode", BaseJsonNode.class,
                JacksonUtils.nullNode());
        PropertyConfigurationImpl<Component, JsonValue> jsonConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "json", JsonValue.class, Json.createNull());

        // binding
        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));
        binding.bindProperty(intConfiguration, false);
        binding.bindProperty(doubleConfiguration, false);
        binding.bindProperty(stringConfiguration, false);
        binding.bindProperty(booleanConfiguration, false);
        binding.bindProperty(jsonNodeConfiguration, false);
        binding.bindProperty(jsonConfiguration, false);

        // test
        WebComponent<Component> webComponent = new WebComponent<>(binding,
                element);

        webComponent.setProperty(intConfiguration, 1);
        verify(element, Mockito.times(1)).executeJs(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
        webComponent.setProperty(doubleConfiguration, 1.0);
        verify(element, Mockito.times(2)).executeJs(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
        webComponent.setProperty(stringConfiguration, "asd");
        verify(element, Mockito.times(3)).executeJs(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
        webComponent.setProperty(booleanConfiguration, true);
        verify(element, Mockito.times(4)).executeJs(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(),
                ArgumentMatchers.any());
        webComponent.setProperty(jsonNodeConfiguration,
                (ValueNode) JacksonUtils.createNode(true));
        // JsonValue has a different number of arguments
        webComponent.setProperty(jsonConfiguration, Json.create(true));
        verify(element, Mockito.times(6)).executeJs(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(Serializable[].class));
    }
}

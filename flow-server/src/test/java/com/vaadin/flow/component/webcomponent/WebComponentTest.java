/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.ValueNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.webcomponent.PropertyConfigurationImpl;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class WebComponentTest {

    private WebComponent<Component> webComponent;

    @BeforeEach
    public void init() {
        WebComponentBinding<Component> componentBinding = new WebComponentBinding<>(
                mock(Component.class));
        webComponent = new WebComponent<>(componentBinding, new Element("tag"));
    }

    @Test
    void fireEvent_throwsWhenNameIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> webComponent.fireEvent(null));
        assertTrue(ex.getMessage().contains("eventName"));
    }

    @Test
    public void fireEvent_doesNotThrowOnNullObjectData() {
        webComponent.fireEvent("name", (JsonNode) null);
    }

    @Test
    void fireEvent_throwsWhenOptionsIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> webComponent.fireEvent("name", (JsonNode) null, null));
        assertTrue(ex.getMessage().contains("options"));
    }

    @Test
    void setProperty_throwsOnNullPropertyConfiguration() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> webComponent.setProperty(null, "value"));
        assertTrue(ex.getMessage().contains("propertyConfiguration"));
    }

    @Test
    void setProperty_throwsOnUnknownProperty() {
        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));

        WebComponent<Component> webComponent = new WebComponent<>(binding,
                new Element("tag"));

        PropertyConfigurationImpl<Component, String> configuration = new PropertyConfigurationImpl<>(
                Component.class, "property", String.class, "value");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> webComponent.setProperty(configuration, "newValue"));
        assertTrue(ex.getMessage()
                .contains("WebComponent does not have a property identified"));
    }

    @Test
    void setProperty_throwsWhenGivenWrongPropertyTypeAsParameter() {
        PropertyConfigurationImpl<Component, Integer> intConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "property", Integer.class, 0);

        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));
        binding.bindProperty(intConfiguration, false);

        WebComponent<Component> webComponent = new WebComponent<>(binding,
                new Element("tag"));

        PropertyConfigurationImpl<Component, String> stringConfiguration = new PropertyConfigurationImpl<>(
                Component.class, "property", String.class, "value");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> webComponent
                        .setProperty(stringConfiguration, "newValue"));
        assertTrue(ex.getMessage().contains("Property 'property' of type "
                + "'java.lang.Integer' cannot be assigned value of type "
                + "'java.lang.String'!"));
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

        // binding
        WebComponentBinding<Component> binding = new WebComponentBinding<>(
                mock(Component.class));
        binding.bindProperty(intConfiguration, false);
        binding.bindProperty(doubleConfiguration, false);
        binding.bindProperty(stringConfiguration, false);
        binding.bindProperty(booleanConfiguration, false);
        binding.bindProperty(jsonNodeConfiguration, false);

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
        verify(element, Mockito.times(5)).executeJs(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(Object[].class));
    }
}

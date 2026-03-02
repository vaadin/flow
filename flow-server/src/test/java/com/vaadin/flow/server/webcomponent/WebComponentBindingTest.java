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
package com.vaadin.flow.server.webcomponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebComponentBindingTest {

    private MyComponent component;
    private WebComponentBinding binding;

    @BeforeEach
    public void setUp() {
        component = new MyComponent();
        binding = new WebComponentBinding<>(component);
        PropertyConfigurationImpl<MyComponent, Integer> integerProperty = new PropertyConfigurationImpl<>(
                MyComponent.class, "int", Integer.class, 0);
        integerProperty.onChange(MyComponent::setInt);
        PropertyConfigurationImpl<MyComponent, ObjectNode> jsonProperty = new PropertyConfigurationImpl<>(
                MyComponent.class, "json", ObjectNode.class, null);
        jsonProperty.onChange(MyComponent::setJson);
        binding.bindProperty(integerProperty, false);
        binding.bindProperty(jsonProperty, false);
    }

    @Test
    public void getComponent() {
        assertEquals(component, binding.getComponent());
    }

    @Test
    public void getPropertyType() {
        assertEquals(Integer.class, binding.getPropertyType("int"));
        assertEquals(ObjectNode.class, binding.getPropertyType("json"));

        assertNull(binding.getPropertyType("not-a-property"));
    }

    @Test
    public void hasProperty() {
        assertTrue(binding.hasProperty("int"));
        assertTrue(binding.hasProperty("json"));

        assertFalse(binding.hasProperty("not-a-property"));
    }

    @Test
    public void updateValue() {
        binding.updateProperty("int", 5);
        assertEquals(5, component.integer);

        ObjectNode obj = JacksonUtils.createObjectNode();
        obj.put("String", "Value");

        binding.updateProperty("json", obj);
        assertEquals("{\"String\":\"Value\"}", component.jsonValue.toString());
    }

    @Test
    public void updateValueJackson() {
        binding.updateProperty("int", 5);
        assertEquals(5, component.integer);

        ObjectNode obj = JacksonUtils.createObjectNode();
        obj.put("String", "Value");

        binding.updateProperty("json", obj);
        assertEquals("{\"String\":\"Value\"}", component.jsonValue.toString());
    }

    @Tag("tag")
    private static class MyComponent extends Component {
        int integer;
        JsonNode jsonValue;

        public void setInt(int v) {
            integer = v;
        }

        public void setJson(JsonNode v) {
            jsonValue = v;
        }
    }
}

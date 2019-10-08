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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class WebComponentBindingTest {

    private MyComponent component;
    private WebComponentBinding binding;

    @Before
    public void setUp() {
        component = new MyComponent();
        binding = new WebComponentBinding<>(component);
        PropertyConfigurationImpl<MyComponent, Integer> integerProperty = new PropertyConfigurationImpl<>(
                MyComponent.class, "int", Integer.class, 0);
        integerProperty.onChange(MyComponent::setInt);
        PropertyConfigurationImpl<MyComponent, JsonValue> jsonProperty = new PropertyConfigurationImpl<>(
                MyComponent.class, "json", JsonValue.class, null);
        jsonProperty.onChange(MyComponent::setJson);
        binding.bindProperty(integerProperty, false, null);
        binding.bindProperty(jsonProperty, false, null);
    }

    @Test
    public void getComponent() {
        Assert.assertEquals(component, binding.getComponent());
    }

    @Test
    public void getPropertyType() {
        Assert.assertEquals(Integer.class, binding.getPropertyType("int"));
        Assert.assertEquals(JsonValue.class, binding.getPropertyType("json"));

        Assert.assertNull(binding.getPropertyType("not-a-property"));
    }

    @Test
    public void hasProperty() {
        Assert.assertTrue(binding.hasProperty("int"));
        Assert.assertTrue(binding.hasProperty("json"));

        Assert.assertFalse(binding.hasProperty("not-a-property"));
    }

    @Test
    public void updateValue() {
        binding.updateProperty("int", 5);
        Assert.assertEquals(5, component.integer);

        JsonObject obj = Json.createObject();
        obj.put("String", "Value");

        binding.updateProperty("json", obj);
        Assert.assertEquals("{\"String\":\"Value\"}",
                component.jsonValue.toJson());
    }

    @Tag("tag")
    private static class MyComponent extends Component {
        int integer;
        JsonValue jsonValue;

        public void setInt(int v) {
            integer = v;
        }

        public void setJson(JsonValue v) {
            jsonValue = v;
        }
    }
}

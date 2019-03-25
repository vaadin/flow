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

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

import elemental.json.JsonValue;

public class WebComponentBindingImplTest {

    private MyComponent component;
    private WebComponentBindingImpl<MyComponent> binding;

    @Before
    public void setUp() throws Exception {
        HashSet<PropertyBinding<?>> bindings = new HashSet<>();
        bindings.add(new PropertyBinding<>(new PropertyData<>("int",
                Integer.class, false, 0), null));
        bindings.add(new PropertyBinding<>(new PropertyData<>("json",
                JsonValue.class, false, null), null));
        component = new MyComponent();
        binding = new WebComponentBindingImpl<>(component, bindings);
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

    @Tag("tag")
    private static class MyComponent extends Component {

    }
}
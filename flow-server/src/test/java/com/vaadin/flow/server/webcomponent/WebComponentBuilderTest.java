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

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.server.MockInstantiator;

import elemental.json.JsonValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebComponentBuilderTest {

    private static final String TAG = "my-component";

    private MyComponentExporter myComponentExporter;
    private WebComponentBuilder<MyComponent> builder;

    @Before
    public void setUp() {
        myComponentExporter = new MyComponentExporter();

        builder = new WebComponentBuilder<>(TAG, myComponentExporter);
    }

    @Test
    public void addProperty_differentTypes() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        assertProperty(builder,"int", 1);
        assertProperty(builder, "string", "string");
        assertProperty(builder, "boolean", true);
        assertProperty(builder, "double", 1.0);

        // JsonValue
        Bean bean = new Bean();
        bean.setInteger(5);

        JsonValue value = JsonSerializer.toJson(bean);
        builder.addProperty("json", value);

        assertProperty(builder, "json", value);
    }

    @Test
    public void addProperty_propertyWithTheSameNameGetsOverwritten() {
        builder.addProperty("int", 1);

        assertTrue(builder.hasProperty("int"));

        builder.addProperty("int", 2);

        assertEquals("Builder should have one property", 1,
                builder.getPropertyDataSet().size());

        assertProperty(builder, "int", 2);
    }

    @Test
    public void getWebComponentTag() {
        assertEquals(TAG, builder.getWebComponentTag());
    }

    @Test
    public void getPropertyType_differentTypes() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        assertEquals(Integer.class, builder.getPropertyType("int"));
        assertEquals(String.class, builder.getPropertyType("string"));
        assertEquals(Boolean.class, builder.getPropertyType("boolean"));
        assertEquals(Double.class, builder.getPropertyType("double"));
    }

    @Test
    public void deliverPropertyUpdate() {
        Instantiator instantiator = new MockInstantiator();

        builder.addProperty("int", 0).onChange(MyComponent::update);

        WebComponentBinding<MyComponent> binding =
                builder.createBinding(instantiator);

        assertNotNull(binding);

        binding.updateProperty("int", 1);

        assertEquals("Component should have been updated", 1,
                binding.getComponent().getValue());
    }

    @Test
    public void getPropertyDataSet() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        Set<PropertyData<?>> set = builder.getPropertyDataSet();

        assertEquals(4, set.size());
    }

    @Test
    public void getComponentClass() {
        assertEquals("Component class should be MyComponent.class",
                MyComponent.class, builder.getComponentClass());
    }

    @Test
    public void getComponentInstance_withInstanceConfigurator() {
        builder.setInstanceConfigurator((webComponent, component) -> component.flop());

        MyComponent myComponent = new MyComponent();

        Instantiator instantiator = mock(Instantiator.class);
        when(instantiator.getOrCreate(MyComponent.class)).thenReturn(myComponent);

        WebComponentBinding<MyComponent> binding =
                builder.createBinding(instantiator);

        assertNotNull("Binding should not be null", binding);
        assertNotNull("Binding's component should not be null",
                binding.getComponent());
        assertTrue("InstanceConfigurator should have set 'flip' to true",
                binding.getComponent().getFlip());
    }

    @Test
    public void getComponentInstance_withoutInstanceConfigurator() {
        MyComponent myComponent = new MyComponent();

        Instantiator instantiator = mock(Instantiator.class);
        when(instantiator.getOrCreate(MyComponent.class)).thenReturn(myComponent);

        WebComponentBinding<MyComponent> binding =
                builder.createBinding(instantiator);

        assertNotNull("Binding should not be null", binding);
        assertNotNull("Binding's component should not be null",
                binding.getComponent());
        assertFalse("'flip' should have been false",
                binding.getComponent().getFlip());
    }

    @Test
    public void hasProperty() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        assertTrue(builder.hasProperty("int"));
        assertTrue(builder.hasProperty("string"));
        assertTrue(builder.hasProperty("boolean"));
        assertTrue(builder.hasProperty("double"));

        assertFalse(builder.hasProperty("does-not-exist"));
    }

    @Tag("test")
    public static class MyComponent extends Component {
        private boolean flip = false;
        private int value = 0;

        public void flop() {
            flip = true;
        }

        public void update(int i) {
            value = i;
        }

        public boolean getFlip() {
            return flip;
        }

        public int getValue() {
            return value;
        }
    }

    public static class Bean {
        protected int integer = 0;
        public Bean() {}

        public int getInteger() {
            return integer;
        }

        public void setInteger(int i) {
            integer = i;
        }

        @Override
        public int hashCode() {
            return integer;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Bean) {
                return integer == ((Bean)obj).integer;
            }
            return false;
        }
    }

    @Tag(TAG)
    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {
        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            // this is where WebComponentBuilder would be normally accessed
            // by the user but this tests uses its interfaces directly.
        }
    }

    private static void assertProperty(WebComponentBuilder<?> builder,
                                             String property, Object value) {
        PropertyData<?> data = builder.getPropertyDataSet().stream()
                .filter(d -> d.getName().equals(property))
                .findFirst().orElse(null);

        assertNotNull("Property " + property + " should not be null", data);
        assertEquals(value, data.getDefaultValue());
    }
}

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

import java.security.InvalidParameterException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.server.MockInstantiator;

import elemental.json.JsonValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebComponentConfigurationImplTest {

    private static final String TAG = "my-component";

    private MyComponentExporter myComponentExporter;
    private WebComponentConfigurationImpl<MyComponent> config;

    @Before
    public void setUp() {
        myComponentExporter = new MyComponentExporter();

        config = new WebComponentConfigurationImpl<>(myComponentExporter);
    }

    @Test
    public void addProperty_differentTypes() {
        config.addProperty("int", 1);
        config.addProperty("string", "string");
        config.addProperty("boolean", true);
        config.addProperty("double", 1.0);

        assertProperty(config,"int", 1);
        assertProperty(config, "string", "string");
        assertProperty(config, "boolean", true);
        assertProperty(config, "double", 1.0);

        // JsonValue
        Bean bean = new Bean();
        bean.setInteger(5);

        JsonValue value = JsonSerializer.toJson(bean);
        config.addProperty("json", value);

        assertProperty(config, "json", value);
    }

    @Test
    public void addProperty_propertyWithTheSameNameGetsOverwritten() {
        config.addProperty("int", 1);

        assertTrue(config.hasProperty("int"));

        config.addProperty("int", 2);

        assertEquals("Builder should have one property", 1,
                config.getPropertyDataSet().size());

        assertProperty(config, "int", 2);
    }

    @Test(expected = InvalidParameterException.class)
    public void addProperty_doesNotAllowNamesWithCapitalLetters() {
        config.addProperty("invalidPropertyName", 1);
    }

    @Test
    public void getWebComponentTag() {
        assertEquals(TAG, config.getWebComponentTag());
    }

    @Test
    public void getPropertyType_differentTypes() {
        config.addProperty("int", 1);
        config.addProperty("string", "string");
        config.addProperty("boolean", true);
        config.addProperty("double", 1.0);

        assertEquals(Integer.class, config.getPropertyType("int"));
        assertEquals(String.class, config.getPropertyType("string"));
        assertEquals(Boolean.class, config.getPropertyType("boolean"));
        assertEquals(Double.class, config.getPropertyType("double"));
    }

    @Test
    public void deliverPropertyUpdate() {
        Instantiator instantiator = new MockInstantiator();

        config.addProperty("int", 0).onChange(MyComponent::update);

        WebComponentBinding<MyComponent> binding =
                config.createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class));

        assertNotNull(binding);

        binding.updateProperty("int", 1);

        assertEquals("Component should have been updated", 1,
                binding.getComponent().getValue());
    }

    @Test
    public void getPropertyDataSet() {
        config.addProperty("int", 1);
        config.addProperty("string", "string");
        config.addProperty("boolean", true);
        config.addProperty("double", 1.0);

        Set<PropertyData<?>> set = config.getPropertyDataSet();

        assertEquals(4, set.size());
    }

    @Test
    public void getComponentClass() {
        assertEquals("Component class should be MyComponent.class",
                MyComponent.class, config.getComponentClass());
    }

    @Test
    public void bindProxy_withInstanceConfigurator() {
        config.setInstanceConfigurator((webComponent, component) -> component.flop());

        WebComponentBinding<MyComponent> binding =
                config.createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class));

        assertNotNull("Binding should not be null", binding);
        assertNotNull("Binding's component should not be null",
                binding.getComponent());
        assertTrue("InstanceConfigurator should have set 'flip' to true",
                binding.getComponent().getFlip());
    }

    @Test
    public void bindProxy_withoutInstanceConfigurator() {
        WebComponentBinding<MyComponent> binding =
                config.createWebComponentBinding(new MockInstantiator(),
                mock(Element.class));

        assertNotNull("Binding should not be null", binding);
        assertNotNull("Binding's component should not be null",
                binding.getComponent());
        assertFalse("'flip' should have been false",
                binding.getComponent().getFlip());
    }

    @Test(expected = IllegalStateException.class)
    public void bindProxy_throwsIfExporterSharesTagWithComponent() {
        WebComponentConfigurationImpl<SharedTagComponent> sharedConfig =
                new WebComponentConfigurationImpl<>(new SharedTagExporter());

        sharedConfig.createWebComponentBinding(new MockInstantiator(),
                mock(Element.class));
    }

    @Test
    public void hasProperty() {
        config.addProperty("int", 1);
        config.addProperty("string", "string");
        config.addProperty("boolean", true);
        config.addProperty("double", 1.0);

        assertTrue(config.hasProperty("int"));
        assertTrue(config.hasProperty("string"));
        assertTrue(config.hasProperty("boolean"));
        assertTrue(config.hasProperty("double"));

        assertFalse(config.hasProperty("does-not-exist"));
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

    @Tag("shared-tag")
    public static class SharedTagComponent extends Component {

    }

    @Tag("shared-tag")
    public static class SharedTagExporter implements WebComponentExporter<SharedTagComponent> {

        @Override
        public void define(WebComponentDefinition<SharedTagComponent> definition) {

        }
    }



    private static void assertProperty(WebComponentConfigurationImpl<?> builder,
                                       String property, Object value) {
        PropertyData<?> data = builder.getPropertyDataSet().stream()
                .filter(d -> d.getName().equals(property))
                .findFirst().orElse(null);

        assertNotNull("Property " + property + " should not be null", data);
        assertEquals(value, data.getDefaultValue());
    }
}

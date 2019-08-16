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

package com.vaadin.flow.component;

import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.webcomponent.PropertyData;
import com.vaadin.flow.server.webcomponent.WebComponentBinding;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class WebComponentExporterTest {

    private static final String TAG = "my-component";

    private MyComponentExporter exporter;
    private WebComponentConfiguration<MyComponent> config;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        exporter = new MyComponentExporter();
        config = (WebComponentConfiguration<MyComponent>) new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporter);
    }

    @Test
    public void addProperty_differentTypes() {
        exporter.addProperty("int", 1);
        exporter.addProperty("string", "string");
        exporter.addProperty("boolean", true);
        exporter.addProperty("double", 1.0);

        assertProperty(config, "int", 1);
        assertProperty(config, "string", "string");
        assertProperty(config, "boolean", true);
        assertProperty(config, "double", 1.0);

        // JsonValue
        Bean bean = new Bean();
        bean.setInteger(5);

        JsonValue value = JsonSerializer.toJson(bean);
        exporter.addProperty("json", value);

        assertProperty(config, "json", value);
    }

    @Test
    public void addProperty_propertyWithTheSameNameGetsOverwritten() {
        exporter.addProperty("int", 1);

        Assert.assertTrue(config.hasProperty("int"));

        exporter.addProperty("int", 2);

        Assert.assertEquals("Configuration should have one property", 1,
                config.getPropertyDataSet().size());

        assertProperty(config, "int", 2);
    }

    @Test
    public void configuration_getTag() {
        Assert.assertEquals(TAG, config.getTag());
    }

    @Test
    public void configuration_getPropertyType_differentTypes() {
        exporter.addProperty("int", 1);
        exporter.addProperty("string", "string");
        exporter.addProperty("boolean", true);
        exporter.addProperty("double", 1.0);

        Assert.assertEquals(Integer.class, config.getPropertyType("int"));
        Assert.assertEquals(String.class, config.getPropertyType("string"));
        Assert.assertEquals(Boolean.class, config.getPropertyType("boolean"));
        Assert.assertEquals(Double.class, config.getPropertyType("double"));
    }

    @Test
    public void configuration_deliverPropertyUpdate() {
        exporter.addProperty("int", 0).onChange(MyComponent::update);

        WebComponentBinding<MyComponent> binding = config
                .createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class), Json.createObject());

        Assert.assertNotNull(binding);

        binding.updateProperty("int", 1);

        Assert.assertEquals("Component should have been updated", 1,
                binding.getComponent().getValue());
    }

    @Test
    public void configuration_getPropertyDataSet() {
        exporter.addProperty("int", 1);
        exporter.addProperty("string", "string");
        exporter.addProperty("boolean", true);
        exporter.addProperty("double", 1.0);

        Set<PropertyData<?>> set = config.getPropertyDataSet();

        Assert.assertEquals(4, set.size());
    }

    @Test
    public void configuration_getComponentClass() {
        Assert.assertEquals("Component class should be MyComponent.class",
                MyComponent.class, config.getComponentClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void configuration_createWebComponentBinding() {
        exporter = new MyComponentExporter() {
            @Override
            public void configureInstance(
                    WebComponent<MyComponent> webComponent,
                    MyComponent component) {
                component.flop();
            }
        };
        exporter.addProperty("value", 1).onChange(MyComponent::update);

        config = (WebComponentConfiguration<MyComponent>) new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporter);

        WebComponentBinding<MyComponent> binding = config
                .createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class), Json.createObject());

        Assert.assertNotNull("Binding should not be null", binding);
        Assert.assertNotNull("Binding's component should not be null",
                binding.getComponent());
        Assert.assertTrue("configureInstance() should have set 'flip' to true",
                binding.getComponent().getFlip());
        Assert.assertEquals("value should be set to 1 by default",
                1, binding.getComponent().value);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void configuration_createWebComponentBinding_overridesDefaultValues() {
        exporter.addProperty("value", 1).onChange(MyComponent::update);

        config = (WebComponentConfiguration<MyComponent>) new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporter);

        // attribute: value=2
        WebComponentBinding<MyComponent> binding = config
                .createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class), Json.parse("{\"value\":2}"));

        Assert.assertEquals("attribute should have set default value to two",
                2, binding.getComponent().value);
    }

    @Test
    public void configuration_bindProxy_withoutInstanceConfigurator() {
        WebComponentBinding<MyComponent> binding = config
                .createWebComponentBinding(new MockInstantiator(),
                        mock(Element.class), Json.createObject());

        Assert.assertNotNull("Binding should not be null", binding);
        Assert.assertNotNull("Binding's component should not be null",
                binding.getComponent());
        Assert.assertFalse("'flip' should have been false",
                binding.getComponent().getFlip());
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void configuration_bindProxy_throwsIfExporterSharesTagWithComponent() {
        SharedTagExporter sharedTagExporter = new SharedTagExporter();
        WebComponentConfiguration<SharedTagComponent> sharedConfig = (WebComponentConfiguration<SharedTagComponent>) new WebComponentExporter.WebComponentConfigurationFactory()
                .create(sharedTagExporter);

        sharedConfig.createWebComponentBinding(new MockInstantiator(),
                mock(Element.class), Json.createObject());
    }

    @Test
    public void configuration_hasProperty() {
        exporter.addProperty("int", 1);
        exporter.addProperty("string", "string");
        exporter.addProperty("boolean", true);
        exporter.addProperty("double", 1.0);

        Assert.assertTrue(config.hasProperty("int"));
        Assert.assertTrue(config.hasProperty("string"));
        Assert.assertTrue(config.hasProperty("boolean"));
        Assert.assertTrue(config.hasProperty("double"));

        Assert.assertFalse(config.hasProperty("does-not-exist"));
    }

    @Test
    public void configuration_callAddProperty_throws() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("'addProperty'");

        AddPropertyInsideConfigureInstance exporter = new AddPropertyInsideConfigureInstance();
        WebComponentConfiguration<?> config = new WebComponentExporter.WebComponentConfigurationFactory()
                .create(exporter);

        config.createWebComponentBinding(new MockInstantiator(),
                mock(Element.class), Json.createObject());
    }

    @Test(expected = IllegalStateException.class)
    public void exporterConstructorThrowsIfNoComponentDefined() {
        NoComponentExporter exporter = new NoComponentExporter();
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

        public Bean() {
        }

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
                return integer == ((Bean) obj).integer;
            }
            return false;
        }
    }

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            super(TAG);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
        }
    }

    public static class AddPropertyInsideConfigureInstance
            extends WebComponentExporter<MyComponent> {

        public AddPropertyInsideConfigureInstance() {
            super("foo");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
            addProperty("bar", 1);
        }
    }

    @Tag("shared-tag")
    public static class SharedTagComponent extends Component {
    }

    private static class SharedTagExporter
            extends WebComponentExporter<SharedTagComponent> {

        public SharedTagExporter() {
            super("shared-tag");
        }

        @Override
        public void configureInstance(
                WebComponent<SharedTagComponent> webComponent,
                SharedTagComponent component) {

        }
    }

    public static class NoComponentExporter extends WebComponentExporter {
        public NoComponentExporter() {
            super("tag");
        }

        @Override
        public void configureInstance(WebComponent webComponent,
                Component component) {
        }
    }

    private static void assertProperty(WebComponentConfiguration<?> config,
            String property, Object value) {
        PropertyData<?> data = config.getPropertyDataSet().stream()
                .filter(d -> d.getName().equals(property)).findFirst()
                .orElse(null);

        Assert.assertNotNull("Property " + property + " should not be null",
                data);
        Assert.assertEquals(value, data.getDefaultValue());
    }
}

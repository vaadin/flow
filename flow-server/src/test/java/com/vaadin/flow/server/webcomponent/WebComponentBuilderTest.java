/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import org.junit.Ignore;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
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
    public void setUp() throws Exception {
        myComponentExporter = new MyComponentExporter();

        builder = new WebComponentBuilder<>(myComponentExporter);
    }

    @Test
    public void addProperty_differentTypes() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        assertEquals(builder.getPropertyData("int").getInitialValue(), 1);
        assertEquals(builder.getPropertyData("string").getInitialValue(),
                "string");
        assertEquals(builder.getPropertyData("boolean").getInitialValue(),
                true);
        assertEquals(builder.getPropertyData("double").getInitialValue(), 1.0);

        // complex types:

        Bean bean = new Bean();
        bean.setInteger(5);

        // 1) free types
        builder.addProperty("bean", Bean.class, bean);
        assertEquals(builder.getPropertyData("bean").getInitialValue(), bean);

        // 2) JsonValue
        JsonValue value = JsonSerializer.toJson(bean);
        builder.addProperty("json", value);

        assertEquals(builder.getPropertyData("json").getInitialValue(), value);
    }

    @Test(expected = RuntimeException.class)
    public void addProperty_samePropertyNameTwiceThrows() {
        builder.addProperty("int", 1);

        assertTrue(builder.hasProperty("int"));

        // should throw - TODO: better exception type
        builder.addProperty("int", 2);
    }

    @Ignore
    @Test
    public void addListProperty() {
        // TODO: these might be goner
    }

    @Test
    public void getWebComponentTag() {
        assertEquals(TAG, builder.getWebComponentTag());
    }

    @Ignore
    @Test
    public void getWebComponentTag_setByTagAnnotationOnTheComponent() {
        assertEquals(TAG, builder.getWebComponentTag());

        WebComponentExporter<MyComponent> exporter = new WebComponentExporter<MyComponent>() {
            @Override
            public String getTag() {
                // returning null should force the thing to use @Tag
                return null;
            }

            @Override
            public void define(WebComponentDefinition<MyComponent> configuration) { }
        };

        // TODO...
    }

    @Ignore
    @Test
    public void builderCreatingFailsIfNoTagAvailable() {
        // TODO: no getTag() override and no @Tag on Component
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

        MyComponent instantiatedComponent =
                builder.getComponentInstance(instantiator);

        assertNotNull(instantiatedComponent);

        builder.deliverPropertyUpdate("int", 1);

        assertEquals("Component should have been updated", 1,
                instantiatedComponent.getValue());
    }

    @Test
    public void getPropertyDataSet() {
        builder.addProperty("int", 1);
        builder.addProperty("string", "string");
        builder.addProperty("boolean", true);
        builder.addProperty("double", 1.0);

        Set<PropertyData2<?>> set = builder.getPropertyDataSet();

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

        MyComponent instantiatedComponent =
                builder.getComponentInstance(instantiator);

        assertNotNull(instantiatedComponent);
        assertTrue(instantiatedComponent.getFlip());
    }

    @Test
    public void getComponentInstance_withoutInstanceConfigurator() {
        MyComponent myComponent = new MyComponent();

        Instantiator instantiator = mock(Instantiator.class);
        when(instantiator.getOrCreate(MyComponent.class)).thenReturn(myComponent);

        MyComponent instantiatedComponent =
                builder.getComponentInstance(instantiator);

        assertNotNull(instantiatedComponent);
        assertFalse(instantiatedComponent.getFlip());
    }

    @Test
    public void getExporterClass() {
        assertEquals(myComponentExporter.getClass(), builder.getExporterClass());
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

    public class Bean {
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

    public class MyComponentExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return TAG;
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> configuration) {
            // this is where WebComponentBuilder would be normally accessed
            // by the user but this tests uses it's interfaces directly.
        }
    }
}
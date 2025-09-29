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
package com.vaadin.flow.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import tools.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;

public class BeanSerializationTest {

    @Tag("test-button")
    private static class TestButton extends Component {
        public TestButton(String text) {
            getElement().setText(text);
        }
    }

    @Test
    public void testSimpleRecord() {
        UI ui = new UI();
        TestButton button = new TestButton("Click");
        ui.add(button);

        record SimpleRecord(String text, int value, Component button) {
        }
        SimpleRecord record = new SimpleRecord("Test", 42, button);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(record);

        // Should be [5, {clean json with component ref}]
        Assert.assertTrue(encoded.isArray());
        Assert.assertEquals(5, encoded.get(0).asInt()); // BEAN_TYPE

        JsonNode bean = encoded.get(1);
        Assert.assertEquals("Test", bean.get("text").asText());
        Assert.assertEquals(42, bean.get("value").asInt());

        JsonNode buttonRef = bean.get("button");
        Assert.assertEquals("component",
                buttonRef.get("__vaadinType").asText());
        Assert.assertEquals(button.getElement().getNode().getId(),
                buttonRef.get("nodeId").asInt());
    }

    @Test
    public void testBeanWithComponentArray() {
        UI ui = new UI();
        Component[] components = new Component[] { new TestButton("Button1"),
                new TestButton("Button2"), null };
        ui.add(components[0], components[1]);

        class BeanWithArray {
            Component[] items = components;
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new BeanWithArray());

        JsonNode bean = encoded.get(1);
        JsonNode items = bean.get("items");
        Assert.assertTrue(items.isArray());
        Assert.assertEquals(3, items.size());

        Assert.assertEquals("component",
                items.get(0).get("__vaadinType").asText());
        Assert.assertEquals("component",
                items.get(1).get("__vaadinType").asText());
        Assert.assertTrue(items.get(2).isNull());
    }

    @Test
    public void testNestedBeans() {
        UI ui = new UI();
        TestButton button = new TestButton("Nested");
        ui.add(button);

        class InnerBean {
            String text = "inner";
            Component component = button;
        }

        class OuterBean {
            String id = "outer";
            InnerBean nested = new InnerBean();
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new OuterBean());

        JsonNode bean = encoded.get(1);
        Assert.assertEquals("outer", bean.get("id").asText());

        JsonNode nested = bean.get("nested");
        Assert.assertEquals("inner", nested.get("text").asText());
        Assert.assertEquals("component",
                nested.get("component").get("__vaadinType").asText());
    }

    @Test
    public void testPrimitiveTypes() {
        class PrimitiveBean {
            boolean bool = true;
            int integer = 123;
            long longVal = 456L;
            double doubleVal = 3.14;
            float floatVal = 2.5f;
            String string = "test";
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new PrimitiveBean());
        JsonNode bean = encoded.get(1);

        // All primitives should be direct values, not wrapped
        Assert.assertTrue(bean.get("bool").isBoolean());
        Assert.assertTrue(bean.get("integer").isNumber());
        Assert.assertTrue(bean.get("longVal").isNumber());
        Assert.assertTrue(bean.get("doubleVal").isNumber());
        Assert.assertTrue(bean.get("floatVal").isNumber());
        Assert.assertTrue(bean.get("string").isTextual());
    }

    @Test
    public void testNullHandling() {
        class BeanWithNulls {
            String text = null;
            Component component = null;
            Component[] array = new Component[] { null, null };
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new BeanWithNulls());
        JsonNode bean = encoded.get(1);

        Assert.assertTrue(bean.get("text").isNull());
        Assert.assertTrue(bean.get("component").isNull());
        Assert.assertTrue(bean.get("array").isArray());
        Assert.assertTrue(bean.get("array").get(0).isNull());
    }

    @Test
    public void testBeanWithMultipleComponentReferences() {
        UI ui = new UI();
        TestButton button1 = new TestButton("Button1");
        TestButton button2 = new TestButton("Button2");
        TestButton button3 = new TestButton("Button3");
        ui.add(button1, button2, button3);

        class FormBean {
            String formName = "TestForm";
            Component header = button1;
            Component footer = button2;
            Component[] actions = new Component[] { button3, button1 };
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new FormBean());
        JsonNode bean = encoded.get(1);

        Assert.assertEquals("TestForm", bean.get("formName").asText());

        JsonNode header = bean.get("header");
        Assert.assertEquals("component", header.get("__vaadinType").asText());
        Assert.assertEquals(button1.getElement().getNode().getId(),
                header.get("nodeId").asInt());

        JsonNode footer = bean.get("footer");
        Assert.assertEquals("component", footer.get("__vaadinType").asText());
        Assert.assertEquals(button2.getElement().getNode().getId(),
                footer.get("nodeId").asInt());

        JsonNode actions = bean.get("actions");
        Assert.assertTrue(actions.isArray());
        Assert.assertEquals(2, actions.size());
        Assert.assertEquals(button3.getElement().getNode().getId(),
                actions.get(0).get("nodeId").asInt());
        Assert.assertEquals(button1.getElement().getNode().getId(),
                actions.get(1).get("nodeId").asInt());
    }

    @Test
    public void testUnattachedComponent() {
        TestButton button = new TestButton("Unattached");

        class BeanWithUnattached {
            Component unattached = button;
        }

        JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(new BeanWithUnattached());
        JsonNode bean = encoded.get(1);

        JsonNode componentRef = bean.get("unattached");
        Assert.assertEquals("component",
                componentRef.get("__vaadinType").asText());
        Assert.assertTrue(componentRef.get("nodeId").isNull());
    }

    @Test
    public void testDeeplyNestedBeans() {
        UI ui = new UI();
        TestButton button = new TestButton("Deep");
        ui.add(button);

        class Level3 {
            String level = "three";
            Component button = BeanSerializationTest.this.getButton();

            private Component getButton() {
                return button;
            }
        }

        class Level2 {
            String level = "two";
            Level3 nested = new Level3();
        }

        class Level1 {
            String level = "one";
            Level2 nested = new Level2();
        }

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(new Level1());
        JsonNode bean = encoded.get(1);

        Assert.assertEquals("one", bean.get("level").asText());

        JsonNode level2 = bean.get("nested");
        Assert.assertEquals("two", level2.get("level").asText());

        JsonNode level3 = level2.get("nested");
        Assert.assertEquals("three", level3.get("level").asText());
        Assert.assertEquals("component",
                level3.get("button").get("__vaadinType").asText());
    }

    private Component getButton() {
        return new TestButton("Helper");
    }
}
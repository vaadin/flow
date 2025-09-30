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

        // Should be clean JSON with @v node references
        Assert.assertTrue(encoded.isObject());
        Assert.assertEquals("Test", encoded.get("text").asText());
        Assert.assertEquals(42, encoded.get("value").asInt());

        JsonNode buttonRef = encoded.get("button");
        Assert.assertEquals("node", buttonRef.get("@v").asText());
        Assert.assertEquals(button.getElement().getNode().getId(),
                buttonRef.get("id").asInt());
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

        // Bean is now encoded directly
        JsonNode items = encoded.get("items");
        Assert.assertTrue(items.isArray());
        Assert.assertEquals(3, items.size());

        Assert.assertEquals("node", items.get(0).get("@v").asText());
        Assert.assertEquals("node", items.get(1).get("@v").asText());
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

        // Bean is now encoded directly
        JsonNode bean = encoded;
        Assert.assertEquals("outer", bean.get("id").asText());

        JsonNode nested = bean.get("nested");
        Assert.assertEquals("inner", nested.get("text").asText());
        Assert.assertEquals("node",
                nested.get("component").get("@v").asText());
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
        // Bean is now encoded directly
        JsonNode bean = encoded;

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
        // Bean is now encoded directly
        JsonNode bean = encoded;

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
        // Bean is now encoded directly
        JsonNode bean = encoded;

        Assert.assertEquals("TestForm", bean.get("formName").asText());

        JsonNode header = bean.get("header");
        Assert.assertEquals("node", header.get("@v").asText());
        Assert.assertEquals(button1.getElement().getNode().getId(),
                header.get("id").asInt());

        JsonNode footer = bean.get("footer");
        Assert.assertEquals("node", footer.get("@v").asText());
        Assert.assertEquals(button2.getElement().getNode().getId(),
                footer.get("id").asInt());

        JsonNode actions = bean.get("actions");
        Assert.assertTrue(actions.isArray());
        Assert.assertEquals(2, actions.size());
        Assert.assertEquals(button3.getElement().getNode().getId(),
                actions.get(0).get("id").asInt());
        Assert.assertEquals(button1.getElement().getNode().getId(),
                actions.get(1).get("id").asInt());
    }

    @Test
    public void testUnattachedComponent() {
        TestButton button = new TestButton("Unattached");

        class BeanWithUnattached {
            Component unattached = button;
        }

        JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(new BeanWithUnattached());
        // Bean is now encoded directly
        JsonNode bean = encoded;

        JsonNode nodeRef = bean.get("unattached");
        Assert.assertEquals("node", nodeRef.get("@v").asText());
        Assert.assertTrue(nodeRef.get("id").isNull());
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
        // Bean is now encoded directly
        JsonNode bean = encoded;

        Assert.assertEquals("one", bean.get("level").asText());

        JsonNode level2 = bean.get("nested");
        Assert.assertEquals("two", level2.get("level").asText());

        JsonNode level3 = level2.get("nested");
        Assert.assertEquals("three", level3.get("level").asText());
        Assert.assertEquals("node",
                level3.get("button").get("@v").asText());
    }

    private Component getButton() {
        return new TestButton("Helper");
    }
}
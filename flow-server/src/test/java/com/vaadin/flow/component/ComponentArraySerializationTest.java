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
package com.vaadin.flow.component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.internal.JacksonCodec;

import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Test for serialization of Component arrays.
 */
public class ComponentArraySerializationTest {

    @Tag("test-div")
    public static class TestDiv extends Component {
        public TestDiv() {
            super();
        }

        public void setText(String text) {
            getElement().setText(text);
        }

        public String getText() {
            return getElement().getText();
        }
    }

    @Tag("test-span")
    public static class TestSpan extends Component {
        public TestSpan() {
            super();
        }

        public void setText(String text) {
            getElement().setText(text);
        }

        public String getText() {
            return getElement().getText();
        }
    }

    public static class ContainerWithComponentArray implements Serializable {
        private Component[] components;

        public Component[] getComponents() {
            return components;
        }

        public void setComponents(Component[] components) {
            this.components = components;
        }
    }

    @Test
    public void testComponentArrayJsonSerialization() {
        // Create test components
        TestDiv div = new TestDiv();
        div.setId("div1");
        div.setText("Hello");

        TestSpan span = new TestSpan();
        span.setId("span1");
        span.setText("World");

        Component[] components = new Component[] { div, span };

        // Test direct array serialization
        JsonValue jsonValue = JsonSerializer.toJson(components);

        Assert.assertNotNull("JsonSerializer should handle Component arrays",
                jsonValue);
        Assert.assertTrue("Result should be a JsonArray",
                jsonValue instanceof JsonArray);

        JsonArray jsonArray = (JsonArray) jsonValue;
        Assert.assertEquals("Array should have 2 elements", 2,
                jsonArray.length());
    }

    @Test
    public void testComponentArrayInObjectJsonSerialization() {
        // Create test components
        TestDiv div = new TestDiv();
        div.setId("div1");

        TestSpan span = new TestSpan();
        span.setId("span1");

        ContainerWithComponentArray container = new ContainerWithComponentArray();
        container.setComponents(new Component[] { div, span });

        // Test serialization of object containing component array
        JsonValue jsonValue = JsonSerializer.toJson(container);

        Assert.assertNotNull(
                "JsonSerializer should handle objects with Component arrays",
                jsonValue);
    }

    @Test
    public void testEmptyComponentArrayJsonSerialization() {
        Component[] emptyArray = new Component[0];

        JsonValue jsonValue = JsonSerializer.toJson(emptyArray);

        Assert.assertNotNull(
                "JsonSerializer should handle empty Component arrays",
                jsonValue);
        Assert.assertTrue("Result should be a JsonArray",
                jsonValue instanceof JsonArray);

        JsonArray jsonArray = (JsonArray) jsonValue;
        Assert.assertEquals("Array should be empty", 0, jsonArray.length());
    }

    @Test
    public void testComponentArrayWithNullsJsonSerialization() {
        Component[] arrayWithNulls = new Component[] { new TestDiv(), null,
                new TestSpan() };

        JsonValue jsonValue = JsonSerializer.toJson(arrayWithNulls);

        Assert.assertNotNull(
                "JsonSerializer should handle Component arrays with nulls",
                jsonValue);
        Assert.assertTrue("Result should be a JsonArray",
                jsonValue instanceof JsonArray);

        JsonArray jsonArray = (JsonArray) jsonValue;
        Assert.assertEquals("Array should have 3 elements", 3,
                jsonArray.length());
    }

    @Test
    public void testComponentArrayWithJacksonCodec() {
        // Create test components - need to attach them for encoding to work
        UI ui = new UI();
        TestDiv div = new TestDiv();
        div.setId("div1");
        ui.add(div);

        TestSpan span = new TestSpan();
        span.setId("span1");
        ui.add(span);

        Component[] components = new Component[] { div, span };

        // Test that canEncodeWithTypeInfo returns true for Component arrays
        Assert.assertTrue("JacksonCodec should support Component arrays",
                JacksonCodec.canEncodeWithTypeInfo(components.getClass()));

        // This should now work without throwing an exception
        com.fasterxml.jackson.databind.JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(components);
        Assert.assertNotNull("Encoded Component array should not be null",
                encoded);
        Assert.assertTrue("Result should be an array node",
                encoded instanceof com.fasterxml.jackson.databind.node.ArrayNode);

        // The result should be wrapped as NODE_ARRAY_TYPE (value 3)
        com.fasterxml.jackson.databind.node.ArrayNode arrayNode = (com.fasterxml.jackson.databind.node.ArrayNode) encoded;
        Assert.assertEquals("First element should be NODE_ARRAY_TYPE (3)", 
                JacksonCodec.NODE_ARRAY_TYPE, arrayNode.get(0).asInt());

        // Second element should be an array of node IDs
        Assert.assertTrue(
                "Second element should be an array of node IDs",
                arrayNode.get(
                        1) instanceof com.fasterxml.jackson.databind.node.ArrayNode);

        com.fasterxml.jackson.databind.node.ArrayNode idsArray = (com.fasterxml.jackson.databind.node.ArrayNode) arrayNode
                .get(1);
        Assert.assertEquals("Should have 2 node IDs", 2,
                idsArray.size());
        // Check that node IDs are numbers
        Assert.assertTrue("First element should be a node ID",
                idsArray.get(0).isNumber());
        Assert.assertTrue("Second element should be a node ID",
                idsArray.get(1).isNumber());
    }

    @Test
    public void testEmptyComponentArrayWithJacksonCodec() {
        Component[] emptyArray = new Component[0];

        // This should work for empty arrays too
        com.fasterxml.jackson.databind.JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(emptyArray);
        Assert.assertNotNull("Encoded empty Component array should not be null",
                encoded);
    }

    @Test
    public void testComponentArrayWithNullsJacksonCodec() {
        UI ui = new UI();
        TestDiv div = new TestDiv();
        ui.add(div);

        Component[] arrayWithNulls = new Component[] { div, null,
                new TestSpan() };

        // This should handle nulls properly
        com.fasterxml.jackson.databind.JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(arrayWithNulls);
        Assert.assertNotNull(
                "Encoded Component array with nulls should not be null",
                encoded);
    }

    @Test
    public void testComponentArrayJavaSerialization() throws Exception {
        // Create test components
        TestDiv div = new TestDiv();
        div.setId("div1");
        div.setText("Hello");

        TestSpan span = new TestSpan();
        span.setId("span1");
        span.setText("World");

        Component[] original = new Component[] { div, span };

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(
                baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Component[] deserialized = (Component[]) ois.readObject();
        ois.close();

        Assert.assertNotNull("Deserialized array should not be null",
                deserialized);
        Assert.assertEquals("Array length should be preserved", 2,
                deserialized.length);
        Assert.assertNotNull("First component should not be null",
                deserialized[0]);
        Assert.assertNotNull("Second component should not be null",
                deserialized[1]);
        Assert.assertTrue("First component should be a TestDiv",
                deserialized[0] instanceof TestDiv);
        Assert.assertTrue("Second component should be a TestSpan",
                deserialized[1] instanceof TestSpan);

        TestDiv deserializedDiv = (TestDiv) deserialized[0];
        TestSpan deserializedSpan = (TestSpan) deserialized[1];

        Assert.assertEquals("Div ID should be preserved", "div1",
                deserializedDiv.getId().orElse(null));
        Assert.assertEquals("Span ID should be preserved", "span1",
                deserializedSpan.getId().orElse(null));
        Assert.assertEquals("Div text should be preserved", "Hello",
                deserializedDiv.getText());
        Assert.assertEquals("Span text should be preserved", "World",
                deserializedSpan.getText());
    }
}
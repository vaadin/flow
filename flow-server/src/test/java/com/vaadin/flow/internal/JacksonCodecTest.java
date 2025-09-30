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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;

public class JacksonCodecTest {
    private static final List<Object> complexTypeValues = Arrays.asList(
            new Object(), new StateNode(), new Date(), new String[0],
            new ArrayList<>(), new HashSet<>(), new HashMap<>());

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void encodeWithoutTypeInfo_supportedTypes() {
        assertJsonEquals(objectMapper.valueToTree(true),
                JacksonCodec.encodeWithoutTypeInfo(Boolean.TRUE));
        assertJsonEquals(objectMapper.valueToTree("string"),
                JacksonCodec.encodeWithoutTypeInfo("string"));
        assertJsonEquals(objectMapper.valueToTree(3.14),
                JacksonCodec.encodeWithoutTypeInfo(Double.valueOf(3.14)));
        assertJsonEquals(objectMapper.valueToTree(42),
                JacksonCodec.encodeWithoutTypeInfo(Integer.valueOf(42)));
        assertJsonEquals(objectMapper.nullNode(),
                JacksonCodec.encodeWithoutTypeInfo(null));
        ObjectNode json = objectMapper.createObjectNode();
        json.put("foo", "bar");
        assertJsonEquals(json, JacksonCodec.encodeWithoutTypeInfo(json));

        assertJsonEquals(objectMapper.nullNode(), objectMapper.nullNode());
        assertJsonEquals(objectMapper.valueToTree(false),
                objectMapper.valueToTree(false));
        assertJsonEquals(objectMapper.valueToTree(234),
                objectMapper.valueToTree(234));
        assertJsonEquals(objectMapper.valueToTree("string"),
                objectMapper.valueToTree("string"));
        assertJsonEquals(json, json);
        assertJsonEquals(objectMapper.createArrayNode(),
                objectMapper.createArrayNode());

        // Test specific complex types - these are now handled via Jackson
        // serialization
        testComplexTypeSerialization();
    }

    @Test
    public void encodeWithoutTypeInfo_unsupportedTypes() {
        List<Object> unsupported = new ArrayList<>(complexTypeValues);
        unsupported.add(ElementFactory.createDiv());

        for (Object value : unsupported) {
            boolean thrown = false;
            try {
                JacksonCodec.encodeWithoutTypeInfo(value);

            } catch (AssertionError expected) {
                thrown = true;
            }
            if (!thrown) {
                Assert.fail("Should throw for " + value.getClass());
            }
        }
    }

    @Test
    public void encodeWithTypeInfo_basicTypes() {
        assertJsonEquals(objectMapper.valueToTree(true),
                JacksonCodec.encodeWithTypeInfo(Boolean.TRUE));
        assertJsonEquals(objectMapper.nullNode(),
                JacksonCodec.encodeWithTypeInfo(null));

        assertJsonEquals(objectMapper.valueToTree(234),
                JacksonCodec.encodeWithTypeInfo(objectMapper.valueToTree(234)));
        assertJsonEquals(objectMapper.valueToTree("string"), JacksonCodec
                .encodeWithTypeInfo(objectMapper.valueToTree("string")));
        assertJsonEquals(objectMapper.createObjectNode(), JacksonCodec
                .encodeWithTypeInfo(objectMapper.createObjectNode()));

        // Array is escaped
        assertJsonEquals(
                JacksonUtils.createArray(
                        objectMapper.valueToTree(JacksonCodec.ARRAY_TYPE),
                        objectMapper.createArrayNode()),
                JacksonCodec
                        .encodeWithTypeInfo(objectMapper.createArrayNode()));
    }

    @Test
    public void encodeWithTypeInfo_attachedElement() {
        Element element = ElementFactory.createDiv();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0,
                element.getNode());

        JsonNode json = JacksonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(
                JacksonUtils.createArray(
                        objectMapper.valueToTree(JacksonCodec.NODE_TYPE),
                        objectMapper.valueToTree(element.getNode().getId())),
                json);
    }

    @Test
    public void encodeWithTypeInfo_detachedElement() {
        Element element = ElementFactory.createDiv();

        JsonNode json = JacksonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(objectMapper.nullNode(), json);
    }

    private static void assertJsonEquals(JsonNode expected, JsonNode actual) {
        Assert.assertTrue(
                actual.toString() + " does not equal " + expected.toString(),
                JacksonUtils.jsonEquals(expected, actual));
    }

    @Test
    public void decodeAs_booleanJson() {
        JsonNode json = objectMapper.valueToTree(true);
        Assert.assertTrue(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertEquals("true", JacksonCodec.decodeAs(json, String.class));
        Assert.assertEquals(Integer.valueOf(0),
                JacksonCodec.decodeAs(json, Integer.class));
        Assert.assertEquals(Double.valueOf(0.0),
                JacksonCodec.decodeAs(json, Double.class));
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_stringJson() {
        JsonNode json = objectMapper.valueToTree("Test123 String\n !%");
        Assert.assertFalse(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertEquals("Test123 String\n !%",
                JacksonCodec.decodeAs(json, String.class));
        Assert.assertEquals(Integer.valueOf(0),
                JacksonCodec.decodeAs(json, Integer.class));
        Assert.assertFalse(JacksonCodec.decodeAs(json, Double.class).isNaN());
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_numberJson() {
        JsonNode json = objectMapper.valueToTree(15.7);
        Assert.assertFalse(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertEquals("15.7", JacksonCodec.decodeAs(json, String.class));
        Assert.assertEquals(Integer.valueOf(15),
                JacksonCodec.decodeAs(json, Integer.class));
        Assert.assertEquals(Double.valueOf(15.7),
                JacksonCodec.decodeAs(json, Double.class));
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_nullJson() {
        JsonNode json = objectMapper.nullNode();
        Assert.assertNull(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertNull(JacksonCodec.decodeAs(json, String.class));
        Assert.assertNull(JacksonCodec.decodeAs(json, Integer.class));
        Assert.assertNull(JacksonCodec.decodeAs(json, Double.class));
        Assert.assertNull(JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_jsonValue() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("foo", "bar");
        Assert.assertEquals("", JacksonCodec.decodeAs(json, String.class));
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
        // boolean
        Assert.assertFalse(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertNull(
                JacksonCodec.decodeAs(objectMapper.nullNode(), Boolean.class));
        Assert.assertFalse(JacksonCodec.decodeAs(json, boolean.class));
        Assert.assertFalse(
                JacksonCodec.decodeAs(objectMapper.nullNode(), boolean.class));
        // integer
        Assert.assertEquals(Integer.valueOf(0),
                JacksonCodec.decodeAs(json, Integer.class));
        Assert.assertNull(
                JacksonCodec.decodeAs(objectMapper.nullNode(), Integer.class));
        Assert.assertEquals(Integer.valueOf(0),
                JacksonCodec.decodeAs(json, int.class));
        Assert.assertEquals(Integer.valueOf(0),
                JacksonCodec.decodeAs(objectMapper.nullNode(), int.class));
        // double
        Assert.assertNull(
                JacksonCodec.decodeAs(objectMapper.nullNode(), Double.class));
        Assert.assertEquals(Double.valueOf(0.0),
                JacksonCodec.decodeAs(json, Double.class));
        Assert.assertEquals(Double.valueOf(0.0),
                JacksonCodec.decodeAs(json, double.class));
        Assert.assertEquals(0.0d,
                JacksonCodec.decodeAs(objectMapper.nullNode(), double.class),
                0.0001d);
    }

    @Test(expected = ClassCastException.class)
    public void decodeAs_jsonValueWrongType_classCastException() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("foo", "bar");
        JacksonCodec.decodeAs(json, NumericNode.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeAs_unsupportedType() {
        Assert.assertNull(JacksonCodec.decodeAs(objectMapper.valueToTree("foo"),
                float.class));
    }

    @Test
    public void testSimpleBeanSerialization() {
        SimpleBean bean = new SimpleBean("Test", 42);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(bean);

        // Should be directly encoded as JSON object
        Assert.assertTrue("Should be object", encoded.isObject());
        Assert.assertEquals("Test", encoded.get("text").asText());
        Assert.assertEquals(42, encoded.get("value").asInt());
    }

    @Test
    public void testNestedBeanSerialization() {
        NestedBean nested = new NestedBean("inner", 123);
        OuterBean outer = new OuterBean("outer", nested);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(outer);

        // Should be directly encoded as JSON object
        Assert.assertTrue("Should be object", encoded.isObject());
        Assert.assertEquals("outer", encoded.get("name").asText());

        JsonNode nestedJson = encoded.get("nested");
        Assert.assertEquals("inner", nestedJson.get("text").asText());
        Assert.assertEquals(123, nestedJson.get("number").asInt());
    }

    @Test
    public void testComplexTypeSerialization() {
        // Test Object - should serialize as empty object
        Object obj = new Object();
        JsonNode objEncoded = JacksonCodec.encodeWithTypeInfo(obj);
        Assert.assertTrue("Object should serialize as JSON object",
                objEncoded.isObject());
        Assert.assertEquals("Object should serialize as empty object", 0,
                objEncoded.size());

        // Test StateNode - should serialize as object with state properties
        StateNode stateNode = new StateNode();
        JsonNode stateNodeEncoded = JacksonCodec.encodeWithTypeInfo(stateNode);
        Assert.assertTrue("StateNode should serialize as JSON object",
                stateNodeEncoded.isObject());
        // StateNode should have some internal structure
        Assert.assertTrue("StateNode should have properties",
                stateNodeEncoded.size() > 0);

        // Test Date - should serialize as timestamp number or ISO string
        Date date = new Date(1234567890000L); // Fixed timestamp for consistent
                                              // testing
        JsonNode dateEncoded = JacksonCodec.encodeWithTypeInfo(date);
        Assert.assertTrue("Date should serialize as number or string",
                dateEncoded.isNumber() || dateEncoded.isTextual());
        if (dateEncoded.isNumber()) {
            Assert.assertEquals("Date should serialize to correct timestamp",
                    1234567890000L, dateEncoded.asLong());
        }

        // Test String array - should serialize as JSON array
        String[] stringArray = new String[] { "hello", "world" };
        JsonNode arrayEncoded = JacksonCodec.encodeWithTypeInfo(stringArray);
        Assert.assertTrue("String array should serialize as JSON array",
                arrayEncoded.isArray());
        Assert.assertEquals("Array should have correct length", 2,
                arrayEncoded.size());
        Assert.assertEquals("First element should be correct", "hello",
                arrayEncoded.get(0).asText());
        Assert.assertEquals("Second element should be correct", "world",
                arrayEncoded.get(1).asText());

        // Test ArrayList - should serialize as JSON array
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("item1");
        arrayList.add("item2");
        JsonNode listEncoded = JacksonCodec.encodeWithTypeInfo(arrayList);
        Assert.assertTrue("ArrayList should serialize as JSON array",
                listEncoded.isArray());
        Assert.assertEquals("List should have correct size", 2,
                listEncoded.size());
        Assert.assertEquals("First list item should be correct", "item1",
                listEncoded.get(0).asText());
        Assert.assertEquals("Second list item should be correct", "item2",
                listEncoded.get(1).asText());

        // Test HashSet - should serialize as JSON array (order may vary)
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add("value1");
        hashSet.add("value2");
        JsonNode setEncoded = JacksonCodec.encodeWithTypeInfo(hashSet);
        Assert.assertTrue("HashSet should serialize as JSON array",
                setEncoded.isArray());
        Assert.assertEquals("Set should have correct size", 2,
                setEncoded.size());
        // Verify both values are present (order not guaranteed with HashSet)
        boolean hasValue1 = false, hasValue2 = false;
        for (JsonNode node : setEncoded) {
            String value = node.asText();
            if ("value1".equals(value))
                hasValue1 = true;
            if ("value2".equals(value))
                hasValue2 = true;
        }
        Assert.assertTrue("Set should contain value1", hasValue1);
        Assert.assertTrue("Set should contain value2", hasValue2);

        // Test HashMap - should serialize as JSON object
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("key1", "stringValue");
        hashMap.put("key2", 42);
        hashMap.put("key3", true);
        JsonNode mapEncoded = JacksonCodec.encodeWithTypeInfo(hashMap);
        Assert.assertTrue("HashMap should serialize as JSON object",
                mapEncoded.isObject());
        Assert.assertEquals("Map should have correct size", 3,
                mapEncoded.size());
        Assert.assertEquals("String value should be correct", "stringValue",
                mapEncoded.get("key1").asText());
        Assert.assertEquals("Integer value should be correct", 42,
                mapEncoded.get("key2").asInt());
        Assert.assertEquals("Boolean value should be correct", true,
                mapEncoded.get("key3").asBoolean());
    }

    // Test classes
    private static class SimpleBean {
        public String text;
        public int value;

        public SimpleBean() {
        }

        public SimpleBean(String text, int value) {
            this.text = text;
            this.value = value;
        }
    }

    private static class NestedBean {
        public String text;
        public int number;

        public NestedBean() {
        }

        public NestedBean(String text, int number) {
            this.text = text;
            this.number = number;
        }
    }

    private static class OuterBean {
        public String name;
        public NestedBean nested;

        public OuterBean() {
        }

        public OuterBean(String name, NestedBean nested) {
            this.name = name;
            this.nested = nested;
        }
    }

    @Test
    public void testDecodeAsSimpleBean() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("text", "TestBean");
        json.put("value", 42);

        SimpleBean decoded = JacksonCodec.decodeAs(json, SimpleBean.class);

        Assert.assertEquals("TestBean", decoded.text);
        Assert.assertEquals(42, decoded.value);
    }

    @Test
    public void testDecodeAsNestedBean() {
        ObjectNode nestedJson = objectMapper.createObjectNode();
        nestedJson.put("text", "NestedTest");
        nestedJson.put("number", 456);

        ObjectNode outerJson = objectMapper.createObjectNode();
        outerJson.put("name", "OuterTest");
        outerJson.set("nested", nestedJson);

        OuterBean decoded = JacksonCodec.decodeAs(outerJson, OuterBean.class);

        Assert.assertEquals("OuterTest", decoded.name);
        Assert.assertEquals("NestedTest", decoded.nested.text);
        Assert.assertEquals(456, decoded.nested.number);
    }

    @Test
    public void testDecodeAsNullValue() {
        JsonNode nullNode = objectMapper.nullNode();

        SimpleBean decoded = JacksonCodec.decodeAs(nullNode, SimpleBean.class);
        Assert.assertNull(decoded);
    }

    @Test
    public void testDecodeAsInvalidJson() {
        JsonNode invalidJson = objectMapper.valueToTree("not an object");

        try {
            JacksonCodec.decodeAs(invalidJson, SimpleBean.class);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                    e.getMessage().contains("Cannot deserialize JSON to type"));
        }
    }

    @Test
    public void testDecodeAsPreservesExistingBehavior() {
        Assert.assertEquals("test", JacksonCodec
                .decodeAs(objectMapper.valueToTree("test"), String.class));
        Assert.assertEquals(Integer.valueOf(42), JacksonCodec
                .decodeAs(objectMapper.valueToTree(42), Integer.class));
        Assert.assertEquals(Boolean.TRUE, JacksonCodec
                .decodeAs(objectMapper.valueToTree(true), Boolean.class));
        Assert.assertEquals(Double.valueOf(3.14), JacksonCodec
                .decodeAs(objectMapper.valueToTree(3.14), Double.class));
    }
}

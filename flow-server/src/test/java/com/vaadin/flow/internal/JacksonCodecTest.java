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
import java.util.Set;

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

        for (Object value : complexTypeValues) {
            JsonNode encoded = JacksonCodec.encodeWithTypeInfo(value);
            Assert.assertNotNull(
                    "Bean JSON should not be null for " + value.getClass(),
                    encoded);
            Assert.assertTrue("Should be valid JSON for " + value.getClass(),
                    encoded.isObject() || encoded.isArray()
                            || encoded.isValueNode());
        }
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

    @Test
    public void testListOfBeansSerialization() {
        List<SimpleBean> beanList = Arrays.asList(new SimpleBean("First", 1),
                new SimpleBean("Second", 2), new SimpleBean("Third", 3));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanList);

        // Should be directly encoded as JSON array
        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 3 elements", 3, encoded.size());

        Assert.assertEquals("First", encoded.get(0).get("text").asText());
        Assert.assertEquals(1, encoded.get(0).get("value").asInt());
        Assert.assertEquals("Second", encoded.get(1).get("text").asText());
        Assert.assertEquals(2, encoded.get(1).get("value").asInt());
        Assert.assertEquals("Third", encoded.get(2).get("text").asText());
        Assert.assertEquals(3, encoded.get(2).get("value").asInt());
    }

    @Test
    public void testListOfBeansDeserialization() {
        // Create JSON array manually
        ObjectNode bean1 = objectMapper.createObjectNode();
        bean1.put("text", "FirstBean");
        bean1.put("value", 100);

        ObjectNode bean2 = objectMapper.createObjectNode();
        bean2.put("text", "SecondBean");
        bean2.put("value", 200);

        JsonNode arrayJson = objectMapper.createArrayNode().add(bean1)
                .add(bean2);

        // Test that Jackson can handle List<SimpleBean> deserialization
        List<SimpleBean> decoded = JacksonUtils.getMapper().convertValue(
                arrayJson, JacksonUtils.getMapper().getTypeFactory()
                        .constructCollectionType(List.class, SimpleBean.class));

        Assert.assertEquals("Should have 2 elements", 2, decoded.size());
        Assert.assertEquals("FirstBean", decoded.get(0).text);
        Assert.assertEquals(100, decoded.get(0).value);
        Assert.assertEquals("SecondBean", decoded.get(1).text);
        Assert.assertEquals(200, decoded.get(1).value);
    }

    @Test
    public void testSetOfBeansSerialization() {
        Set<SimpleBean> beanSet = new HashSet<>(Arrays.asList(
                new SimpleBean("Alpha", 10), new SimpleBean("Beta", 20)));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanSet);

        // Should be directly encoded as JSON array
        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 2 elements", 2, encoded.size());

        // Since Set order is not guaranteed, collect all texts and values
        Set<String> texts = new HashSet<>();
        Set<Integer> values = new HashSet<>();
        for (JsonNode node : encoded) {
            texts.add(node.get("text").asText());
            values.add(node.get("value").asInt());
        }

        Assert.assertTrue("Should contain Alpha", texts.contains("Alpha"));
        Assert.assertTrue("Should contain Beta", texts.contains("Beta"));
        Assert.assertTrue("Should contain value 10", values.contains(10));
        Assert.assertTrue("Should contain value 20", values.contains(20));
    }

    @Test
    public void testSetOfBeansDeserialization() {
        // Create JSON array manually
        ObjectNode bean1 = objectMapper.createObjectNode();
        bean1.put("text", "Gamma");
        bean1.put("value", 300);

        ObjectNode bean2 = objectMapper.createObjectNode();
        bean2.put("text", "Delta");
        bean2.put("value", 400);

        JsonNode arrayJson = objectMapper.createArrayNode().add(bean1)
                .add(bean2);

        // Test that Jackson can handle Set<SimpleBean> deserialization
        Set<SimpleBean> decoded = JacksonUtils.getMapper().convertValue(
                arrayJson, JacksonUtils.getMapper().getTypeFactory()
                        .constructCollectionType(Set.class, SimpleBean.class));

        Assert.assertEquals("Should have 2 elements", 2, decoded.size());

        // Since Set order is not guaranteed, collect all texts and values
        Set<String> texts = decoded.stream().map(b -> b.text)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> values = decoded.stream().map(b -> b.value)
                .collect(java.util.stream.Collectors.toSet());

        Assert.assertTrue("Should contain Gamma", texts.contains("Gamma"));
        Assert.assertTrue("Should contain Delta", texts.contains("Delta"));
        Assert.assertTrue("Should contain value 300", values.contains(300));
        Assert.assertTrue("Should contain value 400", values.contains(400));
    }
}

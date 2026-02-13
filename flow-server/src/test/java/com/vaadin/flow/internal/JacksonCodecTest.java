/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JacksonCodecTest {
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

        // Test specific complex types - handled via Jackson
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
                fail("Should throw for " + value.getClass());
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

        // Array is encoded directly (no wrapping needed)
        assertJsonEquals(objectMapper.createArrayNode(), JacksonCodec
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

        // Verify @v-node format is used for element encoding
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("@v-node", element.getNode().getId());

        assertJsonEquals(expected, json);
    }

    @Test
    public void encodeWithTypeInfo_detachedElement() {
        Element element = ElementFactory.createDiv();

        JsonNode json = JacksonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(objectMapper.nullNode(), json);
    }

    private static void assertJsonEquals(JsonNode expected, JsonNode actual) {
        assertTrue(JacksonUtils.jsonEquals(expected, actual),
                actual.toString() + " does not equal " + expected.toString());
    }

    @Test
    public void decodeAs_booleanJson() {
        JsonNode json = objectMapper.valueToTree(true);
        assertTrue(JacksonCodec.decodeAs(json, Boolean.class));
        assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_stringJson() {
        JsonNode json = objectMapper.valueToTree("Test123 String\n !%");
        assertEquals("Test123 String\n !%",
                JacksonCodec.decodeAs(json, String.class));
        assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_numberJson() {
        // Test integer
        JsonNode intJson = objectMapper.valueToTree(15);
        assertEquals(Integer.valueOf(15),
                JacksonCodec.decodeAs(intJson, Integer.class));
        assertEquals(Double.valueOf(15.0),
                JacksonCodec.decodeAs(intJson, Double.class));

        // Test double
        JsonNode doubleJson = objectMapper.valueToTree(15.7);
        assertEquals(Double.valueOf(15.7),
                JacksonCodec.decodeAs(doubleJson, Double.class));
        assertEquals(doubleJson,
                JacksonCodec.decodeAs(doubleJson, JsonNode.class));
    }

    @Test
    public void decodeAs_nullJson() {
        JsonNode json = objectMapper.nullNode();
        assertNull(JacksonCodec.decodeAs(json, Boolean.class));
        assertNull(JacksonCodec.decodeAs(json, String.class));
        assertNull(JacksonCodec.decodeAs(json, Integer.class));
        assertNull(JacksonCodec.decodeAs(json, Double.class));
        assertNull(JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_jsonValue() {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("foo", "bar");
        assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_jsonValueWrongType_classCastException() {
        assertThrows(ClassCastException.class, () -> {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("foo", "bar");
            JacksonCodec.decodeAs(json, NumericNode.class);
        });
    }

    @Test
    public void decodeAs_unsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> {
            assertNull(JacksonCodec.decodeAs(objectMapper.valueToTree("foo"),
                    float.class));
        });
    }

    @Test
    public void testSimpleBeanSerialization() {
        SimpleBean bean = new SimpleBean("Test", 42);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(bean);

        // Should be directly encoded as JSON object
        assertTrue(encoded.isObject(), "Should be object");
        assertEquals("Test", encoded.get("text").asString());
        assertEquals(42, encoded.get("value").asInt());
    }

    @Test
    public void testNestedBeanSerialization() {
        NestedBean nested = new NestedBean("inner", 123);
        OuterBean outer = new OuterBean("outer", nested);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(outer);

        // Should be directly encoded as JSON object
        assertTrue(encoded.isObject(), "Should be object");
        assertEquals("outer", encoded.get("name").asString());

        JsonNode nestedJson = encoded.get("nested");
        assertEquals("inner", nestedJson.get("text").asString());
        assertEquals(123, nestedJson.get("number").asInt());
    }

    @Test
    public void testComplexTypeSerialization() {
        // Test Object - should serialize as empty object
        Object obj = new Object();
        JsonNode objEncoded = JacksonCodec.encodeWithTypeInfo(obj);
        assertTrue(objEncoded.isObject(),
                "Object should serialize as JSON object");
        assertEquals(0, objEncoded.size(),
                "Object should serialize as empty object");

        // Test StateNode - should serialize as object with state properties
        StateNode stateNode = new StateNode();
        JsonNode stateNodeEncoded = JacksonCodec.encodeWithTypeInfo(stateNode);
        assertTrue(stateNodeEncoded.isObject(),
                "StateNode should serialize as JSON object");
        // StateNode should have some internal structure
        assertTrue(stateNodeEncoded.size() > 0,
                "StateNode should have properties");

        // Test Date - should serialize as timestamp number or ISO string
        Date date = new Date(1234567890000L); // Fixed timestamp for consistent
                                              // testing
        JsonNode dateEncoded = JacksonCodec.encodeWithTypeInfo(date);
        assertTrue(dateEncoded.isNumber() || dateEncoded.isTextual(),
                "Date should serialize as number or string");
        if (dateEncoded.isNumber()) {
            assertEquals(1234567890000L, dateEncoded.asLong(),
                    "Date should serialize to correct timestamp");
        }

        // Test String array - should serialize as JSON array
        String[] stringArray = new String[] { "hello", "world" };
        JsonNode arrayEncoded = JacksonCodec.encodeWithTypeInfo(stringArray);
        assertTrue(arrayEncoded.isArray(),
                "String array should serialize as JSON array");
        assertEquals(2, arrayEncoded.size(),
                "Array should have correct length");
        assertEquals("hello", arrayEncoded.get(0).asString(),
                "First element should be correct");
        assertEquals("world", arrayEncoded.get(1).asString(),
                "Second element should be correct");

        // Test ArrayList - should serialize as JSON array
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("item1");
        arrayList.add("item2");
        JsonNode listEncoded = JacksonCodec.encodeWithTypeInfo(arrayList);
        assertTrue(listEncoded.isArray(),
                "ArrayList should serialize as JSON array");
        assertEquals(2, listEncoded.size(), "List should have correct size");
        assertEquals("item1", listEncoded.get(0).asString(),
                "First list item should be correct");
        assertEquals("item2", listEncoded.get(1).asString(),
                "Second list item should be correct");

        // Test HashSet - should serialize as JSON array (order may vary)
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add("value1");
        hashSet.add("value2");
        JsonNode setEncoded = JacksonCodec.encodeWithTypeInfo(hashSet);
        assertTrue(setEncoded.isArray(),
                "HashSet should serialize as JSON array");
        assertEquals(2, setEncoded.size(), "Set should have correct size");
        // Verify both values are present (order not guaranteed with HashSet)
        boolean hasValue1 = false, hasValue2 = false;
        for (JsonNode node : setEncoded) {
            String value = node.asString();
            if ("value1".equals(value))
                hasValue1 = true;
            if ("value2".equals(value))
                hasValue2 = true;
        }
        assertTrue(hasValue1, "Set should contain value1");
        assertTrue(hasValue2, "Set should contain value2");

        // Test HashMap - should serialize as JSON object
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("key1", "stringValue");
        hashMap.put("key2", 42);
        hashMap.put("key3", true);
        JsonNode mapEncoded = JacksonCodec.encodeWithTypeInfo(hashMap);
        assertTrue(mapEncoded.isObject(),
                "HashMap should serialize as JSON object");
        assertEquals(3, mapEncoded.size(), "Map should have correct size");
        assertEquals("stringValue", mapEncoded.get("key1").asString(),
                "String value should be correct");
        assertEquals(42, mapEncoded.get("key2").asInt(),
                "Integer value should be correct");
        assertEquals(true, mapEncoded.get("key3").asBoolean(),
                "Boolean value should be correct");
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

        assertEquals("TestBean", decoded.text);
        assertEquals(42, decoded.value);
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

        assertEquals("OuterTest", decoded.name);
        assertEquals("NestedTest", decoded.nested.text);
        assertEquals(456, decoded.nested.number);
    }

    @Test
    public void testDecodeAsNullValue() {
        JsonNode nullNode = objectMapper.nullNode();

        SimpleBean decoded = JacksonCodec.decodeAs(nullNode, SimpleBean.class);
        assertNull(decoded);
    }

    @Test
    public void testDecodeAsInvalidJson() {
        JsonNode invalidJson = objectMapper.valueToTree("not an object");

        try {
            JacksonCodec.decodeAs(invalidJson, SimpleBean.class);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage().contains("Cannot deserialize JSON to type"));
        }
    }

    @Test
    public void testDecodeAsForPrimitiveTypes() {
        assertEquals("test", JacksonCodec
                .decodeAs(objectMapper.valueToTree("test"), String.class));
        assertEquals(Integer.valueOf(42), JacksonCodec
                .decodeAs(objectMapper.valueToTree(42), Integer.class));
        assertEquals(Boolean.TRUE, JacksonCodec
                .decodeAs(objectMapper.valueToTree(true), Boolean.class));
        assertEquals(Double.valueOf(3.14), JacksonCodec
                .decodeAs(objectMapper.valueToTree(3.14), Double.class));
    }

    @Test
    public void testListOfBeansSerialization() {
        List<SimpleBean> beanList = Arrays.asList(new SimpleBean("First", 1),
                new SimpleBean("Second", 2), new SimpleBean("Third", 3));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanList);

        // Should be direct array
        assertTrue(encoded.isArray(), "Should be array");
        assertEquals(3, encoded.size(), "Should have 3 beans");

        assertEquals("First", encoded.get(0).get("text").asString());
        assertEquals(1, encoded.get(0).get("value").asInt());
        assertEquals("Second", encoded.get(1).get("text").asString());
        assertEquals(2, encoded.get(1).get("value").asInt());
        assertEquals("Third", encoded.get(2).get("text").asString());
        assertEquals(3, encoded.get(2).get("value").asInt());
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

        assertEquals(2, decoded.size(), "Should have 2 elements");
        assertEquals("FirstBean", decoded.get(0).text);
        assertEquals(100, decoded.get(0).value);
        assertEquals("SecondBean", decoded.get(1).text);
        assertEquals(200, decoded.get(1).value);
    }

    @Test
    public void testSetOfBeansSerialization() {
        Set<SimpleBean> beanSet = new HashSet<>(Arrays.asList(
                new SimpleBean("Alpha", 10), new SimpleBean("Beta", 20)));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanSet);

        // With the new approach, sets are directly serialized as JSON arrays
        assertTrue(encoded.isArray(), "Should be array");
        assertEquals(2, encoded.size(), "Should have 2 elements");

        // Since Set order is not guaranteed, collect all texts and values
        Set<String> texts = new HashSet<>();
        Set<Integer> values = new HashSet<>();
        for (JsonNode node : encoded) {
            texts.add(node.get("text").asString());
            values.add(node.get("value").asInt());
        }

        assertTrue(texts.contains("Alpha"), "Should contain Alpha");
        assertTrue(texts.contains("Beta"), "Should contain Beta");
        assertTrue(values.contains(10), "Should contain value 10");
        assertTrue(values.contains(20), "Should contain value 20");
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

        assertEquals(2, decoded.size(), "Should have 2 elements");

        // Since Set order is not guaranteed, collect all texts and values
        Set<String> texts = decoded.stream().map(b -> b.text)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> values = decoded.stream().map(b -> b.value)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(texts.contains("Gamma"), "Should contain Gamma");
        assertTrue(texts.contains("Delta"), "Should contain Delta");
        assertTrue(values.contains(300), "Should contain value 300");
        assertTrue(values.contains(400), "Should contain value 400");
    }

    @Test
    public void testListOfIntegersSerialization() {
        List<Integer> integerList = List.of(1, 2, 3);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(integerList);

        // Should be direct array
        assertTrue(encoded.isArray(), "Should be array");
        assertEquals(3, encoded.size(), "Should have 3 integers");
        assertEquals(1, encoded.get(0).asInt());
        assertEquals(2, encoded.get(1).asInt());
        assertEquals(3, encoded.get(2).asInt());
    }

    @Test
    public void testMapOfBeansSerialization() {
        Map<String, SimpleBean> beanMap = new HashMap<>();
        beanMap.put("first", new SimpleBean("FirstBean", 100));
        beanMap.put("second", new SimpleBean("SecondBean", 200));
        beanMap.put("third", new SimpleBean("ThirdBean", 300));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanMap);

        // Should be JSON object
        assertTrue(encoded.isObject(), "Should be object");
        assertEquals(3, encoded.size(), "Should have 3 entries");

        assertEquals("FirstBean", encoded.get("first").get("text").asString());
        assertEquals(100, encoded.get("first").get("value").asInt());
        assertEquals("SecondBean",
                encoded.get("second").get("text").asString());
        assertEquals(200, encoded.get("second").get("value").asInt());
        assertEquals("ThirdBean", encoded.get("third").get("text").asString());
        assertEquals(300, encoded.get("third").get("value").asInt());
    }

    @Test
    public void testMapOfBeansDeserialization() {
        // Create JSON object manually
        ObjectNode bean1 = objectMapper.createObjectNode();
        bean1.put("text", "Alpha");
        bean1.put("value", 111);

        ObjectNode bean2 = objectMapper.createObjectNode();
        bean2.put("text", "Beta");
        bean2.put("value", 222);

        ObjectNode mapJson = objectMapper.createObjectNode();
        mapJson.set("keyA", bean1);
        mapJson.set("keyB", bean2);

        // Test that Jackson can handle Map<String, SimpleBean> deserialization
        Map<String, SimpleBean> decoded = JacksonUtils.getMapper().convertValue(
                mapJson,
                JacksonUtils.getMapper().getTypeFactory().constructMapType(
                        Map.class, String.class, SimpleBean.class));

        assertEquals(2, decoded.size(), "Should have 2 entries");
        assertNotNull(decoded.get("keyA"), "Should have keyA");
        assertEquals("Alpha", decoded.get("keyA").text);
        assertEquals(111, decoded.get("keyA").value);
        assertNotNull(decoded.get("keyB"), "Should have keyB");
        assertEquals("Beta", decoded.get("keyB").text);
        assertEquals(222, decoded.get("keyB").value);
    }

    @Test
    public void testNestedMapSerialization() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("bean", new SimpleBean("NestedBean", 999));
        nestedMap.put("number", 42);
        nestedMap.put("text", "Hello");

        Map<String, Object> outerMap = new HashMap<>();
        outerMap.put("nested", nestedMap);
        outerMap.put("simple", "value");

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(outerMap);

        // Should be JSON object
        assertTrue(encoded.isObject(), "Should be object");
        assertEquals(2, encoded.size(), "Should have 2 entries");
        assertEquals("value", encoded.get("simple").asString());

        JsonNode nestedJson = encoded.get("nested");
        assertTrue(nestedJson.isObject(), "Nested should be object");
        assertEquals(42, nestedJson.get("number").asInt());
        assertEquals("Hello", nestedJson.get("text").asString());
        assertEquals("NestedBean",
                nestedJson.get("bean").get("text").asString());
        assertEquals(999, nestedJson.get("bean").get("value").asInt());
    }

    @Test
    public void testListOfComponentElementsSerialization() {
        // Create elements directly instead of through components to avoid
        // attachment issues
        Element element1 = ElementFactory.createDiv();
        Element element2 = ElementFactory.createDiv();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0,
                element1.getNode());
        tree.getRootNode().getFeature(ElementChildrenList.class).add(1,
                element2.getNode());

        // Test list of elements (the proper way)
        List<Element> elementList = Arrays.asList(element1, element2);
        JsonNode listEncoded = JacksonCodec.encodeWithTypeInfo(elementList);
        assertTrue(listEncoded.isArray(), "Should be array");
        assertEquals(2, listEncoded.size(), "Should have 2 elements");

        // Elements should serialize as @v-node references
        JsonNode element1Json = listEncoded.get(0);
        assertTrue(element1Json.isObject(), "First element should be object");
        assertTrue(element1Json.has("@v-node"),
                "First element should have @v-node");
        assertEquals(element1.getNode().getId(),
                element1Json.get("@v-node").asInt());

        JsonNode element2Json = listEncoded.get(1);
        assertTrue(element2Json.isObject(), "Second element should be object");
        assertTrue(element2Json.has("@v-node"),
                "Second element should have @v-node");
        assertEquals(element2.getNode().getId(),
                element2Json.get("@v-node").asInt());
    }

    @Test
    public void testBeanWithComponentSerialization() {
        TestComponent component = new TestComponent();
        BeanWithComponent bean = new BeanWithComponent("TestComponent",
                component, 42);

        // Test bean list containing detached components
        List<BeanWithComponent> beanList = Arrays.asList(bean);
        JsonNode listEncoded = JacksonCodec.encodeWithTypeInfo(beanList);
        assertTrue(listEncoded.isArray(), "Should be array");
        assertEquals(1, listEncoded.size(), "Should have 1 bean");

        JsonNode beanJson = listEncoded.get(0);
        assertTrue(beanJson.isObject(), "Bean should serialize as object");
        assertEquals("TestComponent", beanJson.get("name").asString());
        assertEquals(42, beanJson.get("value").asInt());
        assertTrue(beanJson.has("component"),
                "Bean should have component field");

        JsonNode componentJson = beanJson.get("component");
        assertTrue(componentJson.isNull(),
                "Detached component should serialize as null");
    }

    @Test
    public void testArrayWithComponentsSerialization() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        TestComponent[] componentArray = new TestComponent[] { component1,
                component2 };

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(componentArray);

        assertTrue(encoded.isArray(), "Should be array");
        assertEquals(2, encoded.size(), "Should have 2 components");

        // Detached components should serialize as null
        JsonNode first = encoded.get(0);
        assertTrue(first.isNull(), "First detached component should be null");

        JsonNode second = encoded.get(1);
        assertTrue(second.isNull(), "Second detached component should be null");
    }

    @Test
    public void testListOfBeansWithComponentsSerialization() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        List<BeanWithComponent> beanList = Arrays.asList(
                new BeanWithComponent("First", component1, 10),
                new BeanWithComponent("Second", component2, 20));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanList);

        assertTrue(encoded.isArray(), "Should be array");
        assertEquals(2, encoded.size(), "Should have 2 beans");

        // First bean
        JsonNode firstBean = encoded.get(0);
        assertEquals("First", firstBean.get("name").asString());
        assertEquals(10, firstBean.get("value").asInt());
        assertTrue(firstBean.has("component"),
                "First bean should have component");
        JsonNode firstComponent = firstBean.get("component");
        assertTrue(firstComponent.isNull(),
                "First detached component should be null");

        // Second bean
        JsonNode secondBean = encoded.get(1);
        assertEquals("Second", secondBean.get("name").asString());
        assertEquals(20, secondBean.get("value").asInt());
        assertTrue(secondBean.has("component"),
                "Second bean should have component");
        JsonNode secondComponent = secondBean.get("component");
        assertTrue(secondComponent.isNull(),
                "Second detached component should be null");
    }

    public static class TestComponent extends Component {
        public TestComponent() {
            super(ElementFactory.createDiv());
        }
    }

    public static class BeanWithComponent {
        public String name;
        public TestComponent component;
        public int value;

        public BeanWithComponent() {
        }

        public BeanWithComponent(String name, TestComponent component,
                int value) {
            this.name = name;
            this.component = component;
            this.value = value;
        }
    }

    @Test
    public void testTypeReferenceListDeserialization() {
        // Create JSON array with bean objects
        ObjectNode bean1 = objectMapper.createObjectNode();
        bean1.put("text", "FirstBean");
        bean1.put("value", 100);

        ObjectNode bean2 = objectMapper.createObjectNode();
        bean2.put("text", "SecondBean");
        bean2.put("value", 200);

        JsonNode arrayJson = objectMapper.createArrayNode().add(bean1)
                .add(bean2);

        // Deserialize using TypeReference
        TypeReference<List<SimpleBean>> typeRef = new TypeReference<List<SimpleBean>>() {
        };
        List<SimpleBean> result = JacksonCodec.decodeAs(arrayJson, typeRef);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should have 2 elements");
        assertEquals("FirstBean", result.get(0).text);
        assertEquals(100, result.get(0).value);
        assertEquals("SecondBean", result.get(1).text);
        assertEquals(200, result.get(1).value);
    }

    @Test
    public void testTypeReferenceMapDeserialization() {
        // Create JSON object with bean values
        ObjectNode bean1 = objectMapper.createObjectNode();
        bean1.put("text", "Alpha");
        bean1.put("value", 111);

        ObjectNode bean2 = objectMapper.createObjectNode();
        bean2.put("text", "Beta");
        bean2.put("value", 222);

        ObjectNode mapJson = objectMapper.createObjectNode();
        mapJson.set("keyA", bean1);
        mapJson.set("keyB", bean2);

        // Deserialize using TypeReference
        TypeReference<Map<String, SimpleBean>> typeRef = new TypeReference<Map<String, SimpleBean>>() {
        };
        Map<String, SimpleBean> result = JacksonCodec.decodeAs(mapJson,
                typeRef);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should have 2 entries");
        assertEquals("Alpha", result.get("keyA").text);
        assertEquals(111, result.get("keyA").value);
        assertEquals("Beta", result.get("keyB").text);
        assertEquals(222, result.get("keyB").value);
    }

    @Test
    public void testTypeReferenceNestedList() {
        // Create nested structure: List<Map<String, SimpleBean>>
        ObjectNode innerBean = objectMapper.createObjectNode();
        innerBean.put("text", "Nested");
        innerBean.put("value", 999);

        ObjectNode innerMap = objectMapper.createObjectNode();
        innerMap.set("item", innerBean);

        JsonNode outerArray = objectMapper.createArrayNode().add(innerMap);

        // Deserialize using TypeReference
        TypeReference<List<Map<String, SimpleBean>>> typeRef = new TypeReference<List<Map<String, SimpleBean>>>() {
        };
        List<Map<String, SimpleBean>> result = JacksonCodec.decodeAs(outerArray,
                typeRef);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should have 1 element");
        assertTrue(result.get(0).containsKey("item"),
                "First element should have 'item' key");
        assertEquals("Nested", result.get(0).get("item").text);
        assertEquals(999, result.get(0).get("item").value);
    }

    @Test
    public void testTypeReferenceNullHandling() {
        JsonNode nullJson = objectMapper.nullNode();

        TypeReference<List<SimpleBean>> typeRef = new TypeReference<List<SimpleBean>>() {
        };
        List<SimpleBean> result = JacksonCodec.decodeAs(nullJson, typeRef);

        assertNull(result, "Null JSON should deserialize to null");
    }

    @Test
    public void testTypeReferenceListOfPrimitives() {
        JsonNode arrayJson = objectMapper.createArrayNode().add(10).add(20)
                .add(30);

        TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {
        };
        List<Integer> result = JacksonCodec.decodeAs(arrayJson, typeRef);

        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should have 3 elements");
        assertEquals(Integer.valueOf(10), result.get(0));
        assertEquals(Integer.valueOf(20), result.get(1));
        assertEquals(Integer.valueOf(30), result.get(2));
    }

    @Test
    public void testNestedRecordDeserialization() {
        // Test that nested records work for event data pattern
        record EventDetails(int button, int clientX, int clientY) {
        }
        record MouseEventData(EventDetails event, String type) {
        }

        // Create JSON matching the structure
        ObjectNode eventNode = objectMapper.createObjectNode();
        eventNode.put("button", 0);
        eventNode.put("clientX", 150);
        eventNode.put("clientY", 200);

        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.set("event", eventNode);
        rootNode.put("type", "click");

        // Deserialize using Class
        MouseEventData result = JacksonCodec.decodeAs(rootNode,
                MouseEventData.class);

        assertNotNull(result, "Result should not be null");
        assertEquals("click", result.type(), "Type should match");
        assertNotNull(result.event(), "Event should not be null");
        assertEquals(0, result.event().button(), "Button should be 0");
        assertEquals(150, result.event().clientX(), "ClientX should be 150");
        assertEquals(200, result.event().clientY(), "ClientY should be 200");
    }

    @Test
    public void testNestedRecordWithTypeReference() {
        // Test List<Record> deserialization
        record Point(int x, int y) {
        }

        ObjectNode point1 = objectMapper.createObjectNode();
        point1.put("x", 10);
        point1.put("y", 20);

        ObjectNode point2 = objectMapper.createObjectNode();
        point2.put("x", 30);
        point2.put("y", 40);

        JsonNode arrayJson = objectMapper.createArrayNode().add(point1)
                .add(point2);

        TypeReference<List<Point>> typeRef = new TypeReference<List<Point>>() {
        };
        List<Point> result = JacksonCodec.decodeAs(arrayJson, typeRef);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should have 2 points");
        assertEquals(10, result.get(0).x());
        assertEquals(20, result.get(0).y());
        assertEquals(30, result.get(1).x());
        assertEquals(40, result.get(1).y());
    }
}

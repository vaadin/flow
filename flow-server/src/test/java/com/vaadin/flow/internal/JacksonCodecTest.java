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

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NumericNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
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
        Assert.assertTrue(
                actual.toString() + " does not equal " + expected.toString(),
                JacksonUtils.jsonEquals(expected, actual));
    }

    @Test
    public void decodeAs_booleanJson() {
        JsonNode json = objectMapper.valueToTree(true);
        Assert.assertTrue(JacksonCodec.decodeAs(json, Boolean.class));
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_stringJson() {
        JsonNode json = objectMapper.valueToTree("Test123 String\n !%");
        Assert.assertEquals("Test123 String\n !%",
                JacksonCodec.decodeAs(json, String.class));
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
    }

    @Test
    public void decodeAs_numberJson() {
        // Test integer
        JsonNode intJson = objectMapper.valueToTree(15);
        Assert.assertEquals(Integer.valueOf(15),
                JacksonCodec.decodeAs(intJson, Integer.class));
        Assert.assertEquals(Double.valueOf(15.0),
                JacksonCodec.decodeAs(intJson, Double.class));

        // Test double
        JsonNode doubleJson = objectMapper.valueToTree(15.7);
        Assert.assertEquals(Double.valueOf(15.7),
                JacksonCodec.decodeAs(doubleJson, Double.class));
        Assert.assertEquals(doubleJson,
                JacksonCodec.decodeAs(doubleJson, JsonNode.class));
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
        Assert.assertEquals(json, JacksonCodec.decodeAs(json, JsonNode.class));
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
    public void testDecodeAsForPrimitiveTypes() {
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

        // Should be direct array
        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 3 beans", 3, encoded.size());

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

        // With the new approach, sets are directly serialized as JSON arrays
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

    @Test
    public void testListOfIntegersSerialization() {
        List<Integer> integerList = List.of(1, 2, 3);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(integerList);

        // Should be direct array
        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 3 integers", 3, encoded.size());
        Assert.assertEquals(1, encoded.get(0).asInt());
        Assert.assertEquals(2, encoded.get(1).asInt());
        Assert.assertEquals(3, encoded.get(2).asInt());
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
        Assert.assertTrue("Should be array", listEncoded.isArray());
        Assert.assertEquals("Should have 2 elements", 2, listEncoded.size());

        // Elements should serialize as @v-node references
        JsonNode element1Json = listEncoded.get(0);
        Assert.assertTrue("First element should be object",
                element1Json.isObject());
        Assert.assertTrue("First element should have @v-node",
                element1Json.has("@v-node"));
        Assert.assertEquals(element1.getNode().getId(),
                element1Json.get("@v-node").asInt());

        JsonNode element2Json = listEncoded.get(1);
        Assert.assertTrue("Second element should be object",
                element2Json.isObject());
        Assert.assertTrue("Second element should have @v-node",
                element2Json.has("@v-node"));
        Assert.assertEquals(element2.getNode().getId(),
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
        Assert.assertTrue("Should be array", listEncoded.isArray());
        Assert.assertEquals("Should have 1 bean", 1, listEncoded.size());

        JsonNode beanJson = listEncoded.get(0);
        Assert.assertTrue("Bean should serialize as object",
                beanJson.isObject());
        Assert.assertEquals("TestComponent", beanJson.get("name").asText());
        Assert.assertEquals(42, beanJson.get("value").asInt());
        Assert.assertTrue("Bean should have component field",
                beanJson.has("component"));

        JsonNode componentJson = beanJson.get("component");
        Assert.assertTrue("Detached component should serialize as null",
                componentJson.isNull());
    }

    @Test
    public void testArrayWithComponentsSerialization() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        TestComponent[] componentArray = new TestComponent[] { component1,
                component2 };

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(componentArray);

        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 2 components", 2, encoded.size());

        // Detached components should serialize as null
        JsonNode first = encoded.get(0);
        Assert.assertTrue("First detached component should be null",
                first.isNull());

        JsonNode second = encoded.get(1);
        Assert.assertTrue("Second detached component should be null",
                second.isNull());
    }

    @Test
    public void testListOfBeansWithComponentsSerialization() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        List<BeanWithComponent> beanList = Arrays.asList(
                new BeanWithComponent("First", component1, 10),
                new BeanWithComponent("Second", component2, 20));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(beanList);

        Assert.assertTrue("Should be array", encoded.isArray());
        Assert.assertEquals("Should have 2 beans", 2, encoded.size());

        // First bean
        JsonNode firstBean = encoded.get(0);
        Assert.assertEquals("First", firstBean.get("name").asText());
        Assert.assertEquals(10, firstBean.get("value").asInt());
        Assert.assertTrue("First bean should have component",
                firstBean.has("component"));
        JsonNode firstComponent = firstBean.get("component");
        Assert.assertTrue("First detached component should be null",
                firstComponent.isNull());

        // Second bean
        JsonNode secondBean = encoded.get(1);
        Assert.assertEquals("Second", secondBean.get("name").asText());
        Assert.assertEquals(20, secondBean.get("value").asInt());
        Assert.assertTrue("Second bean should have component",
                secondBean.has("component"));
        JsonNode secondComponent = secondBean.get("component");
        Assert.assertTrue("Second detached component should be null",
                secondComponent.isNull());
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

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should have 2 elements", 2, result.size());
        Assert.assertEquals("FirstBean", result.get(0).text);
        Assert.assertEquals(100, result.get(0).value);
        Assert.assertEquals("SecondBean", result.get(1).text);
        Assert.assertEquals(200, result.get(1).value);
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

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should have 2 entries", 2, result.size());
        Assert.assertEquals("Alpha", result.get("keyA").text);
        Assert.assertEquals(111, result.get("keyA").value);
        Assert.assertEquals("Beta", result.get("keyB").text);
        Assert.assertEquals(222, result.get("keyB").value);
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

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should have 1 element", 1, result.size());
        Assert.assertTrue("First element should have 'item' key",
                result.get(0).containsKey("item"));
        Assert.assertEquals("Nested", result.get(0).get("item").text);
        Assert.assertEquals(999, result.get(0).get("item").value);
    }

    @Test
    public void testTypeReferenceNullHandling() {
        JsonNode nullJson = objectMapper.nullNode();

        TypeReference<List<SimpleBean>> typeRef = new TypeReference<List<SimpleBean>>() {
        };
        List<SimpleBean> result = JacksonCodec.decodeAs(nullJson, typeRef);

        Assert.assertNull("Null JSON should deserialize to null", result);
    }

    @Test
    public void testTypeReferenceListOfPrimitives() {
        JsonNode arrayJson = objectMapper.createArrayNode().add(10).add(20)
                .add(30);

        TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {
        };
        List<Integer> result = JacksonCodec.decodeAs(arrayJson, typeRef);

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Should have 3 elements", 3, result.size());
        Assert.assertEquals(Integer.valueOf(10), result.get(0));
        Assert.assertEquals(Integer.valueOf(20), result.get(1));
        Assert.assertEquals(Integer.valueOf(30), result.get(2));
    }
}

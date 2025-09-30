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

import java.lang.reflect.InaccessibleObjectException;
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
    private static final List<Object> withTypeInfoUnsupportedValues = Arrays
            .asList(new Object(), new StateNode(), new Date(), new String[0],
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
    }

    @Test
    public void encodeWithoutTypeInfo_unsupportedTypes() {
        List<Object> unsupported = new ArrayList<>(
                withTypeInfoUnsupportedValues);
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
        // Boolean and null are encoded directly without type info since they
        // can be serialized
        assertJsonEquals(objectMapper.valueToTree(true),
                JacksonCodec.encodeWithTypeInfo(Boolean.TRUE));
        assertJsonEquals(objectMapper.nullNode(),
                JacksonCodec.encodeWithTypeInfo(null));

        // JsonNode values are handled as primitive types
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

        // Should be a direct object without array wrapper
        Assert.assertTrue("Should be an object", json.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                json.get("@vaadin").asText());
        Assert.assertNotNull("Should have nodeId", json.get("nodeId"));
        Assert.assertEquals("Should have nodeId matching element",
                element.getNode().getId(), json.get("nodeId").intValue());
    }

    @Test
    public void encodeWithTypeInfo_detachedElement() {
        Element element = ElementFactory.createDiv();

        JsonNode json = JacksonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(objectMapper.nullNode(), json);
    }

    @Test
    public void encodeWithTypeInfo_unsupportedTypes() {
        // All types are now supported through bean encoding, but some may fail
        // due to Java module access restrictions
        for (Object value : withTypeInfoUnsupportedValues) {
            try {
                JsonNode result = JacksonCodec.encodeWithTypeInfo(value);
                Assert.assertNotNull("Should encode " + value.getClass(),
                        result);
                // Complex objects should be wrapped with BEAN_TYPE
                if (!(value instanceof String[])
                        && !(value instanceof ArrayList)
                        && !(value instanceof HashSet)
                        && !(value instanceof HashMap)) {
                    Assert.assertTrue("Should be wrapped as BEAN_TYPE",
                            result.isArray());
                    if (result.isArray() && result.size() >= 1) {
                        Assert.assertEquals("Should have BEAN_TYPE", 5,
                                result.get(0).asInt());
                    }
                }
            } catch (Exception e) {
                // Some objects might fail due to Java module access
                // restrictions - this is expected
                Assert.assertTrue(
                        "Should be an access-related exception for "
                                + value.getClass(),
                        e.getMessage().contains("accessible") || e
                                .getCause() instanceof InaccessibleObjectException);
            }
        }
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
    public void encodeWithTypeInfo_singleComponent() {
        UI ui = new UI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(element);

        // Should be a direct object without array wrapper
        Assert.assertTrue("Should be an object", encoded.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                encoded.get("@vaadin").asText());
        Assert.assertNotNull("Should have nodeId", encoded.get("nodeId"));
        Assert.assertEquals("Should have nodeId matching element",
                element.getNode().getId(), encoded.get("nodeId").intValue());
    }

    @Test
    public void encodeWithTypeInfo_listOfPrimitives() {
        List<String> stringList = Arrays.asList("one", "two", "three");
        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(stringList);

        // Should be encoded as ARRAY_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.ARRAY_TYPE,
                encoded.get(0).intValue());

        JsonNode arrayContent = encoded.get(1);
        Assert.assertTrue("Content should be an array", arrayContent.isArray());
        Assert.assertEquals("Should have 3 elements", 3, arrayContent.size());
        Assert.assertEquals("First element", "one",
                arrayContent.get(0).asText());
        Assert.assertEquals("Second element", "two",
                arrayContent.get(1).asText());
        Assert.assertEquals("Third element", "three",
                arrayContent.get(2).asText());
    }

    @Test
    public void encodeWithTypeInfo_listOfComponents() {
        UI ui = new UI();
        Element elem1 = ElementFactory.createDiv();
        Element elem2 = ElementFactory.createSpan();
        ui.getElement().appendChild(elem1, elem2);

        List<Element> elements = Arrays.asList(elem1, elem2);
        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(elements);

        // Should be encoded as ARRAY_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.ARRAY_TYPE,
                encoded.get(0).intValue());

        JsonNode arrayContent = encoded.get(1);
        Assert.assertTrue("Content should be an array", arrayContent.isArray());
        Assert.assertEquals("Should have 2 elements", 2, arrayContent.size());

        // First element should be a component reference
        JsonNode firstComponent = arrayContent.get(0);
        Assert.assertTrue("Should be an object", firstComponent.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                firstComponent.get("@vaadin").asText());
        Assert.assertEquals("Should have correct nodeId",
                elem1.getNode().getId(),
                firstComponent.get("nodeId").intValue());

        // Second element should be a component reference
        JsonNode secondComponent = arrayContent.get(1);
        Assert.assertTrue("Should be an object", secondComponent.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                secondComponent.get("@vaadin").asText());
        Assert.assertEquals("Should have correct nodeId",
                elem2.getNode().getId(),
                secondComponent.get("nodeId").intValue());
    }

    @Test
    public void encodeWithTypeInfo_listMixed() {
        UI ui = new UI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        List<Object> mixed = Arrays.asList("text", 42, element, null);
        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(mixed);

        // Should be encoded as ARRAY_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.ARRAY_TYPE,
                encoded.get(0).intValue());

        JsonNode arrayContent = encoded.get(1);
        Assert.assertTrue("Content should be an array", arrayContent.isArray());
        Assert.assertEquals("Should have 4 elements", 4, arrayContent.size());

        // Check each element
        Assert.assertEquals("First element should be string", "text",
                arrayContent.get(0).asText());
        Assert.assertEquals("Second element should be number", 42,
                arrayContent.get(1).intValue());

        JsonNode componentRef = arrayContent.get(2);
        Assert.assertTrue("Third element should be object",
                componentRef.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                componentRef.get("@vaadin").asText());

        Assert.assertTrue("Fourth element should be null",
                arrayContent.get(3).isNull());
    }

    public static class BeanWithList {
        public String name;
        public List<String> items;

        public BeanWithList() {
        }

        public BeanWithList(String name, List<String> items) {
            this.name = name;
            this.items = items;
        }
    }

    @Test
    public void encodeWithTypeInfo_beanWithList() {
        List<String> items = Arrays.asList("item1", "item2", "item3");
        BeanWithList bean = new BeanWithList("TestBean", items);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(bean);

        // Should be encoded as BEAN_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.BEAN_TYPE,
                encoded.get(0).intValue());

        JsonNode beanContent = encoded.get(1);
        Assert.assertTrue("Content should be an object",
                beanContent.isObject());
        Assert.assertEquals("Should have name field", "TestBean",
                beanContent.get("name").asText());

        // The items field should be an array (not wrapped with ARRAY_TYPE
        // inside bean)
        JsonNode itemsField = beanContent.get("items");
        Assert.assertTrue("items field should be an array",
                itemsField.isArray());
        Assert.assertEquals("Should have 3 items", 3, itemsField.size());
        Assert.assertEquals("First item", "item1", itemsField.get(0).asText());
    }

    @Test
    public void encodeWithTypeInfo_mapOfPrimitives() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(map);

        // Should be encoded as MAP_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.MAP_TYPE,
                encoded.get(0).intValue());

        JsonNode mapContent = encoded.get(1);
        Assert.assertTrue("Content should be an object", mapContent.isObject());
        Assert.assertEquals("Should have value for 'one'", 1,
                mapContent.get("one").intValue());
        Assert.assertEquals("Should have value for 'two'", 2,
                mapContent.get("two").intValue());
        Assert.assertEquals("Should have value for 'three'", 3,
                mapContent.get("three").intValue());
    }

    @Test
    public void encodeWithTypeInfo_mapWithComponents() {
        UI ui = new UI();
        Element button = ElementFactory.createButton("Click");
        Element input = ElementFactory.createInput();
        ui.getElement().appendChild(button, input);

        java.util.Map<String, Element> map = new java.util.HashMap<>();
        map.put("button", button);
        map.put("input", input);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(map);

        // Should be encoded as MAP_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.MAP_TYPE,
                encoded.get(0).intValue());

        JsonNode mapContent = encoded.get(1);
        Assert.assertTrue("Content should be an object", mapContent.isObject());

        // Check button value
        JsonNode buttonValue = mapContent.get("button");
        Assert.assertTrue("Button value should be an object",
                buttonValue.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                buttonValue.get("@vaadin").asText());
        Assert.assertEquals("Should have correct nodeId", button.getNode().getId(),
                buttonValue.get("nodeId").intValue());

        // Check input value
        JsonNode inputValue = mapContent.get("input");
        Assert.assertTrue("Input value should be an object",
                inputValue.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                inputValue.get("@vaadin").asText());
        Assert.assertEquals("Should have correct nodeId", input.getNode().getId(),
                inputValue.get("nodeId").intValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void encodeWithTypeInfo_mapWithNonStringKeys_throwsException() {
        java.util.Map<Integer, String> map = new java.util.HashMap<>();
        map.put(42, "value");

        // Should throw IllegalArgumentException for non-String key
        JacksonCodec.encodeWithTypeInfo(map);
    }

    @Test(expected = IllegalArgumentException.class)
    public void encodeWithTypeInfo_mapWithComponentKeys_throwsException() {
        UI ui = new UI();
        Element button = ElementFactory.createButton("Button");
        ui.getElement().appendChild(button);

        java.util.Map<Element, String> map = new java.util.HashMap<>();
        map.put(button, "Button Component");

        // Should throw IllegalArgumentException for non-String key
        JacksonCodec.encodeWithTypeInfo(map);
    }

    @Test
    public void encodeWithTypeInfo_mapMixed() {
        UI ui = new UI();
        Element element = ElementFactory.createDiv();
        ui.getElement().appendChild(element);

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("string", "value");
        map.put("component", element);
        map.put("nullValue", null);
        map.put("nested", Arrays.asList("a", "b"));

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(map);

        // Should be encoded as MAP_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.MAP_TYPE,
                encoded.get(0).intValue());

        JsonNode mapContent = encoded.get(1);
        Assert.assertTrue("Content should be an object", mapContent.isObject());

        // Check string value
        Assert.assertEquals("String value", "value",
                mapContent.get("string").asText());

        // Check component value
        JsonNode componentValue = mapContent.get("component");
        Assert.assertTrue("Component value should be an object",
                componentValue.isObject());
        Assert.assertEquals("Should have @vaadin=component", "component",
                componentValue.get("@vaadin").asText());

        // Check null value
        Assert.assertTrue("Null value should be null",
                mapContent.get("nullValue").isNull());

        // Check nested list value
        JsonNode nestedValue = mapContent.get("nested");
        Assert.assertTrue("Nested value should be an array", nestedValue.isArray());
        Assert.assertEquals("Should have type marker for array",
                JacksonCodec.ARRAY_TYPE, nestedValue.get(0).intValue());
    }

    public static class BeanWithMap {
        public String id;
        public java.util.Map<String, Object> properties;

        public BeanWithMap() {
        }

        public BeanWithMap(String id, java.util.Map<String, Object> properties) {
            this.id = id;
            this.properties = properties;
        }
    }

    @Test
    public void encodeWithTypeInfo_beanWithMap() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("name", "Test");
        props.put("count", 42);
        props.put("enabled", true);

        BeanWithMap bean = new BeanWithMap("bean1", props);

        JsonNode encoded = JacksonCodec.encodeWithTypeInfo(bean);

        // Should be encoded as BEAN_TYPE
        Assert.assertTrue("Should be an array", encoded.isArray());
        Assert.assertEquals("Should have type marker", JacksonCodec.BEAN_TYPE,
                encoded.get(0).intValue());

        JsonNode beanContent = encoded.get(1);
        Assert.assertTrue("Content should be an object",
                beanContent.isObject());
        Assert.assertEquals("Should have id field", "bean1",
                beanContent.get("id").asText());

        // The properties field should be a plain object (not wrapped with
        // MAP_TYPE inside bean)
        JsonNode propsField = beanContent.get("properties");
        Assert.assertTrue("properties field should be an object",
                propsField.isObject());
        Assert.assertEquals("name property", "Test",
                propsField.get("name").asText());
        Assert.assertEquals("count property", 42,
                propsField.get("count").intValue());
        Assert.assertTrue("enabled property",
                propsField.get("enabled").booleanValue());
    }
}

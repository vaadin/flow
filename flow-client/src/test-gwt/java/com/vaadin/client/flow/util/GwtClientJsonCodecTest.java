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
package com.vaadin.client.flow.util;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;

import elemental.js.dom.JsElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

public class GwtClientJsonCodecTest {

    /**
     * Helper method to get property from a decoded object. The decoded object
     * is now a Map, so we can access properties directly.
     */
    private static Object getObjectProperty(Object obj, String key) {
        if (obj instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) obj).get(key);
        } else {
            throw new RuntimeException(
                    "Expected Map but got " + obj.getClass());
        }
    }

    @Test
    public void decodeWithoutTypeInfo() {
        decodePrimitiveValues(ClientJsonCodec::decodeWithoutTypeInfo);
    }

    private static void decodePrimitiveValues(
            Function<JsonValue, Object> decoder) {
        Assert.assertEquals("string", decoder.apply(Json.create("string")));

        Assert.assertEquals(Double.valueOf(3.14),
                decoder.apply(Json.create(3.14)));

        Assert.assertEquals(Boolean.TRUE, decoder.apply(Json.create(true)));

        Assert.assertNull(decoder.apply(Json.createNull()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWithoutTypeInfo_arrayUnsupported() {
        ClientJsonCodec.decodeWithoutTypeInfo(Json.createArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecodeWithoutTypeInfo_objectUnsupported() {
        ClientJsonCodec.decodeWithoutTypeInfo(Json.createObject());
    }

    @Test
    public void decodeWithTypeInfo_primitives() {
        decodePrimitiveValues(
                json -> ClientJsonCodec.decodeWithTypeInfo(null, json));
    }

    @Test
    public void decodeWithTypeInfo_array() {
        JsonValue json = JsonCodec.encodeWithTypeInfo(JsonUtils
                .createArray(Json.create("string"), Json.create(true)));

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, json);

        Assert.assertTrue(decoded instanceof JsArray);
        JsArray<?> decodedArray = (JsArray<?>) decoded;

        Assert.assertEquals(2, decodedArray.length());
        Assert.assertEquals("string", decodedArray.get(0));
        Assert.assertEquals(Boolean.TRUE, decodedArray.get(1));
    }

    @Test
    public void decodeWithTypeInfo_element() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(42, tree);
        tree.registerNode(node);

        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        // Create @v-node format
        elemental.json.JsonObject jsonObject = Json.createObject();
        jsonObject.put("@v-node", node.getId());

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonObject);

        Assert.assertSame(element, decoded);
    }

    @Test
    public void encodeWithoutTypeInfo() {
        encodePrimitiveValues(ClientJsonCodec::encodeWithoutTypeInfo);
    }

    @Test
    public void decodeStateNode_node() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(43, tree);
        tree.registerNode(node);

        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        // Create @v-node format
        elemental.json.JsonObject jsonObject = Json.createObject();
        jsonObject.put("@v-node", node.getId());

        StateNode decoded = ClientJsonCodec.decodeStateNode(tree, jsonObject);

        Assert.assertSame(node, decoded);
    }

    @Test
    public void decodeStateNode_array() {
        JsonValue json = JsonCodec.encodeWithTypeInfo(JsonUtils
                .createArray(Json.create("string"), Json.create(true)));

        Assert.assertNull(ClientJsonCodec.decodeStateNode(null, json));
    }

    @Test
    public void decodeStateNode_primitive() {
        Assert.assertNull(
                ClientJsonCodec.decodeStateNode(null, Json.create("string")));
    }

    private static void encodePrimitiveValues(
            Function<Object, JsonValue> encoder) {

        assertJsonEquals(Json.create("string"), encoder.apply("string"));

        assertJsonEquals(Json.create(3.14),
                encoder.apply(Double.valueOf(3.14)));

        assertJsonEquals(Json.create(true), encoder.apply(Boolean.TRUE));

        assertJsonEquals(Json.createNull(), encoder.apply(null));
    }

    private static void assertJsonEquals(JsonValue expected, JsonValue actual) {
        Assert.assertTrue(
                actual.toJson() + " does not equal " + expected.toJson(),
                JsonUtils.jsonEquals(expected, actual));
    }

    @Test
    public void decodeWithTypeInfo_unknownVType_throwsException() {
        // Test that unknown @v- prefixed types are rejected for forward
        // compatibility
        elemental.json.JsonObject unknownType = Json.createObject();
        unknownType.put("@v-unknown", "someValue");

        try {
            ClientJsonCodec.decodeWithTypeInfo(null, unknownType);
            Assert.fail(
                    "Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                    "Exception message should mention the unknown key",
                    e.getMessage().contains("@v-unknown"));
        }
    }

    @Test
    public void decodeStateNode_unknownVType_throwsException() {
        // Test that unknown @v- prefixed types are rejected in decodeStateNode
        // too
        elemental.json.JsonObject unknownType = Json.createObject();
        unknownType.put("@v-future", 42);

        try {
            ClientJsonCodec.decodeStateNode(null, unknownType);
            Assert.fail(
                    "Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                    "Exception message should mention the unknown key",
                    e.getMessage().contains("@v-future"));
        }
    }

    @Test
    public void decodeWithTypeInfo_nestedObjectWithVNodeReference() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(100, tree);
        tree.registerNode(node);

        JsElement element = new JsElement() {
        };
        node.setDomNode(element);

        // Create nested object containing @v-node reference
        elemental.json.JsonObject nestedObject = Json.createObject();
        nestedObject.put("@v-node", node.getId());

        elemental.json.JsonObject outerObject = Json.createObject();
        outerObject.put("data", "test");
        outerObject.put("element", nestedObject);
        outerObject.put("count", 42);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, outerObject);

        // Should return a native JS object that has properties accessible by
        // name
        Assert.assertEquals("test", getObjectProperty(decoded, "data"));
        Assert.assertEquals(Double.valueOf(42.0),
                getObjectProperty(decoded, "count"));
        Assert.assertSame("Nested element should be decoded to DOM node",
                element, getObjectProperty(decoded, "element"));
    }

    @Test
    public void decodeWithTypeInfo_arrayContainingVNodeReferences() {
        StateTree tree = new StateTree(null);
        StateNode node1 = new StateNode(200, tree);
        StateNode node2 = new StateNode(201, tree);
        tree.registerNode(node1);
        tree.registerNode(node2);

        JsElement element1 = new JsElement() {
        };
        JsElement element2 = new JsElement() {
        };
        node1.setDomNode(element1);
        node2.setDomNode(element2);

        // Create array containing @v-node references and primitives
        JsonArray jsonArray = Json.createArray();
        jsonArray.set(0, "first");

        elemental.json.JsonObject nodeRef1 = Json.createObject();
        nodeRef1.put("@v-node", node1.getId());
        jsonArray.set(1, nodeRef1);

        jsonArray.set(2, 42);

        elemental.json.JsonObject nodeRef2 = Json.createObject();
        nodeRef2.put("@v-node", node2.getId());
        jsonArray.set(3, nodeRef2);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonArray);

        Assert.assertTrue("Should decode to JsArray",
                decoded instanceof JsArray);
        JsArray<?> result = (JsArray<?>) decoded;

        Assert.assertEquals(4, result.length());
        Assert.assertEquals("first", result.get(0));
        Assert.assertSame("First element should be decoded to DOM node",
                element1, result.get(1));
        Assert.assertEquals(Double.valueOf(42.0), result.get(2));
        Assert.assertSame("Second element should be decoded to DOM node",
                element2, result.get(3));
    }

    @Test
    public void decodeWithTypeInfo_complexNestedStructureWithMultipleVTypes() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(300, tree);
        tree.registerNode(node);

        JsElement element = new JsElement() {
        };
        node.setDomNode(element);

        // Create complex nested structure with both @v-node and @v-return
        // references
        elemental.json.JsonObject nodeRef = Json.createObject();
        nodeRef.put("@v-node", node.getId());

        elemental.json.JsonObject returnRef = Json.createObject();
        JsonArray returnArray = Json.createArray();
        returnArray.set(0, 123);
        returnArray.set(1, 456);
        returnRef.put("@v-return", returnArray);

        // Nested array containing both types
        JsonArray mixedArray = Json.createArray();
        mixedArray.set(0, nodeRef);
        mixedArray.set(1, "middle");
        mixedArray.set(2, returnRef);

        // Outer object containing the mixed array and other properties
        elemental.json.JsonObject outerObject = Json.createObject();
        outerObject.put("title", "Complex Structure");
        outerObject.put("items", mixedArray);
        outerObject.put("metadata", Json.createObject());

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, outerObject);

        // Should return a native JS object that has properties accessible by
        // name
        Assert.assertEquals("Complex Structure",
                getObjectProperty(decoded, "title"));

        Object itemsObj = getObjectProperty(decoded, "items");
        Assert.assertTrue("Items should be JsArray",
                itemsObj instanceof JsArray);
        JsArray<?> items = (JsArray<?>) itemsObj;

        Assert.assertEquals(3, items.length());
        Assert.assertSame("First item should be decoded DOM node", element,
                items.get(0));
        Assert.assertEquals("middle", items.get(1));
        Assert.assertNotNull("Return channel should create callback",
                items.get(2));
    }

    @Test
    public void decodeWithTypeInfo_deeplyNestedRecursiveDecoding() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(400, tree);
        tree.registerNode(node);

        JsElement element = new JsElement() {
        };
        node.setDomNode(element);

        // Create deeply nested structure: object -> array -> object -> array ->
        // @v-node
        elemental.json.JsonObject nodeRef = Json.createObject();
        nodeRef.put("@v-node", node.getId());

        JsonArray deepArray = Json.createArray();
        deepArray.set(0, "deep");
        deepArray.set(1, nodeRef);

        elemental.json.JsonObject deepObject = Json.createObject();
        deepObject.put("level", 3);
        deepObject.put("content", deepArray);

        JsonArray midArray = Json.createArray();
        midArray.set(0, deepObject);
        midArray.set(1, true);

        elemental.json.JsonObject midObject = Json.createObject();
        midObject.put("level", 2);
        midObject.put("data", midArray);

        JsonArray topArray = Json.createArray();
        topArray.set(0, "top");
        topArray.set(1, midObject);

        elemental.json.JsonObject topObject = Json.createObject();
        topObject.put("level", 1);
        topObject.put("nested", topArray);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, topObject);

        // Should return a native JS object with recursively decoded nested
        // structures
        Assert.assertEquals(Double.valueOf(1.0),
                getObjectProperty(decoded, "level"));

        JsArray<?> nestedArray = (JsArray<?>) getObjectProperty(decoded,
                "nested");
        Assert.assertEquals("top", nestedArray.get(0));

        Object midResult = nestedArray.get(1);
        Assert.assertEquals(Double.valueOf(2.0),
                getObjectProperty(midResult, "level"));

        JsArray<?> dataArray = (JsArray<?>) getObjectProperty(midResult,
                "data");
        Assert.assertEquals(Boolean.TRUE, dataArray.get(1));

        Object deepResult = dataArray.get(0);
        Assert.assertEquals(Double.valueOf(3.0),
                getObjectProperty(deepResult, "level"));

        JsArray<?> contentArray = (JsArray<?>) getObjectProperty(deepResult,
                "content");
        Assert.assertEquals("deep", contentArray.get(0));
        Assert.assertSame("Deeply nested element should be decoded to DOM node",
                element, contentArray.get(1));
    }

}

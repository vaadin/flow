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

import org.junit.Test;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;

import elemental.js.dom.JsElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

public class GwtClientJsonCodecTest extends ClientEngineTestBase {

    /**
     * Helper method to get property from a decoded object. The decoded object
     * is a native JS object, so we need to use native access.
     */
    private static native Object getObjectProperty(Object obj, String key)
    /*-{
        return obj[key];
    }-*/;

    @Test
    public void decodeWithoutTypeInfo() {
        decodePrimitiveValues(ClientJsonCodec::decodeWithoutTypeInfo);
    }

    private static void decodePrimitiveValues(
            Function<JsonValue, Object> decoder) {
        assertEquals("string", decoder.apply(Json.create("string")));

        assertEquals(Double.valueOf(3.14),
                decoder.apply(Json.create(3.14)));

        assertEquals(Boolean.TRUE, decoder.apply(Json.create(true)));

        assertNull(decoder.apply(Json.createNull()));
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
        // Create a simple JSON array directly
        JsonArray json = Json.createArray();
        json.set(0, "string");
        json.set(1, true);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, json);

        assertTrue(decoded instanceof JsArray);
        JsArray<?> decodedArray = (JsArray<?>) decoded;

        assertEquals(2, decodedArray.length());
        assertEquals("string", decodedArray.get(0));
        assertEquals(Boolean.TRUE, decodedArray.get(1));
    }

    @Test
    public void decodeWithTypeInfo_element() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(42, tree);
        tree.registerNode(node);

        JsElement element = createTestElement();
        node.setDomNode(element);

        // Parse @v-node format from JSON string
        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree,
                Json.parse("{\"@v-node\": 42}"));

        assertSame(element, decoded);
    }

    @Test
    public void decodeWithTypeInfo_plainObject() {
        // When server sends a BaseJsonNode, it arrives as a plain JSON object
        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null,
                Json.parse("{\"name\": \"test\", \"value\": 42, \"active\": true}"));
        assertNotNull(decoded);
        assertEquals("test", getObjectProperty(decoded, "name"));
        assertEquals(Double.valueOf(42), getObjectProperty(decoded, "value"));
        assertEquals(Boolean.TRUE, getObjectProperty(decoded, "active"));
    }

    @Test
    public void encodeWithoutTypeInfo() {
        encodePrimitiveValues(ClientJsonCodec::encodeWithoutTypeInfo);
    }

    private static native JsElement createTestElement()
    /*-{
        return document.createElement('div');
    }-*/;

    @Test
    public void decodeStateNode_node() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(43, tree);
        tree.registerNode(node);

        JsElement element = createTestElement();
        node.setDomNode(element);

        // Parse @v-node format from JSON string
        StateNode decoded = ClientJsonCodec.decodeStateNode(tree,
                Json.parse("{\"@v-node\": 43}"));

        assertSame(node, decoded);
    }

    @Test
    public void decodeStateNode_array() {
        // Create a simple JSON array directly
        JsonArray json = Json.createArray();
        json.set(0, "string");
        json.set(1, true);

        assertNull(ClientJsonCodec.decodeStateNode(null, json));
    }

    @Test
    public void decodeStateNode_primitive() {
        assertNull(
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
        assertEquals(
                "JSON values do not match",
                expected.toJson(), actual.toJson());
    }

    @Test
    public void decodeWithTypeInfo_unknownVType_throwsException() {
        // Test that unknown @v- prefixed types are rejected for forward
        // compatibility
        elemental.json.JsonObject unknownType = Json.createObject();
        unknownType.put("@v-unknown", "someValue");

        try {
            ClientJsonCodec.decodeWithTypeInfo(null, unknownType);
            fail(
                    "Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            assertTrue(
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
            fail(
                    "Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            assertTrue(
                    "Exception message should mention the unknown key",
                    e.getMessage().contains("@v-future"));
        }
    }

    @Test
    public void decodeWithTypeInfo_nestedObjectWithVNodeReference() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(100, tree);
        tree.registerNode(node);

        JsElement element = createTestElement();
        node.setDomNode(element);

        // Parse nested object containing @v-node reference from JSON string
        String json = "" //
                + "{" //
                + "    \"data\": \"test\"," //
                + "    \"element\": {\"@v-node\": 100}," //
                + "    \"count\": 42" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonValue);

        // Should return a native JS object that has properties accessible by
        // name
        assertEquals("test", getObjectProperty(decoded, "data"));
        assertEquals(Double.valueOf(42.0),
                getObjectProperty(decoded, "count"));
        assertSame("Nested element should be decoded to DOM node",
                element, getObjectProperty(decoded, "element"));
    }

    @Test
    public void decodeWithTypeInfo_arrayContainingVNodeReferences() {
        StateTree tree = new StateTree(null);
        StateNode node1 = new StateNode(200, tree);
        StateNode node2 = new StateNode(201, tree);
        tree.registerNode(node1);
        tree.registerNode(node2);

        JsElement element1 = createTestElement();
        JsElement element2 = createTestElement();
        node1.setDomNode(element1);
        node2.setDomNode(element2);

        // Parse array containing @v-node references and primitives from JSON string
        String json = "" //
                + "[" //
                + "    \"first\"," //
                + "    {\"@v-node\": 200}," //
                + "    42," //
                + "    {\"@v-node\": 201}" //
                + "]";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonValue);

        assertTrue("Should decode to JsArray",
                decoded instanceof JsArray);
        JsArray<?> result = (JsArray<?>) decoded;

        assertEquals(4, result.length());
        assertEquals("first", result.get(0));
        assertSame("First element should be decoded to DOM node",
                element1, result.get(1));
        assertEquals(Double.valueOf(42.0), result.get(2));
        assertSame("Second element should be decoded to DOM node",
                element2, result.get(3));
    }

    @Test
    public void decodeWithTypeInfo_complexNestedStructureWithMultipleVTypes() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(300, tree);
        tree.registerNode(node);

        JsElement element = createTestElement();
        node.setDomNode(element);

        // Parse complex nested structure with both @v-node and @v-return references
        // from JSON string
        String json = "" //
                + "{" //
                + "    \"title\": \"Complex Structure\"," //
                + "    \"items\": [" //
                + "        {\"@v-node\": 300}," //
                + "        \"middle\"," //
                + "        {\"@v-return\": [123, 456]}" //
                + "    ]," //
                + "    \"metadata\": {}" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonValue);

        // Should return a native JS object that has properties accessible by
        // name
        assertEquals("Complex Structure",
                getObjectProperty(decoded, "title"));

        Object itemsObj = getObjectProperty(decoded, "items");
        assertTrue("Items should be JsArray",
                itemsObj instanceof JsArray);
        JsArray<?> items = (JsArray<?>) itemsObj;

        assertEquals(3, items.length());
        assertSame("First item should be decoded DOM node", element,
                items.get(0));
        assertEquals("middle", items.get(1));
        assertNotNull("Return channel should create callback",
                items.get(2));
    }

    @Test
    public void decodeWithTypeInfo_deeplyNestedRecursiveDecoding() {
        StateTree tree = new StateTree(null);
        StateNode node = new StateNode(400, tree);
        tree.registerNode(node);

        JsElement element = createTestElement();
        node.setDomNode(element);

        // Parse deeply nested structure from JSON string: object -> array -> object ->
        // array -> @v-node
        String json = "" //
                + "{" //
                + "    \"level\": 1," //
                + "    \"nested\": [" //
                + "        \"top\"," //
                + "        {" //
                + "            \"level\": 2," //
                + "            \"data\": [" //
                + "                {" //
                + "                    \"level\": 3," //
                + "                    \"content\": [" //
                + "                        \"deep\"," //
                + "                        {\"@v-node\": 400}" //
                + "                    ]" //
                + "                }," //
                + "                true" //
                + "            ]" //
                + "        }" //
                + "    ]" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, jsonValue);

        // Should return a native JS object with recursively decoded nested
        // structures
        assertEquals(Double.valueOf(1.0),
                getObjectProperty(decoded, "level"));

        JsArray<?> nestedArray = (JsArray<?>) getObjectProperty(decoded,
                "nested");
        assertEquals("top", nestedArray.get(0));

        Object midResult = nestedArray.get(1);
        assertEquals(Double.valueOf(2.0),
                getObjectProperty(midResult, "level"));

        JsArray<?> dataArray = (JsArray<?>) getObjectProperty(midResult,
                "data");
        assertEquals(Boolean.TRUE, dataArray.get(1));

        Object deepResult = dataArray.get(0);
        assertEquals(3.0,
                getObjectProperty(deepResult, "level"));

        JsArray<?> contentArray = (JsArray<?>) getObjectProperty(deepResult,
                "content");
        assertEquals("deep", contentArray.get(0));
        assertSame("Deeply nested element should be decoded to DOM node",
                element, contentArray.get(1));
    }

    @Test
    public void testNestedJsonObjectPassthrough() {
        // Test nested objects - parse from JSON string
        String json = "" //
                + "{" //
                + "    \"outer\": \"parent\"," //
                + "    \"nested\": {" //
                + "        \"innerName\": \"nested\"," //
                + "        \"innerValue\": 100" //
                + "    }" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, jsonValue);

        assertNotNull(decoded);
        assertEquals("parent", getObjectProperty(decoded, "outer"));

        // The nested object should also be properly decoded
        Object decodedInnerObject = getObjectProperty(decoded, "nested");
        assertNotNull(decodedInnerObject);
        Object decodedInnerObjectValue = getObjectProperty(decodedInnerObject, "innerValue");
        assertEquals("nested", getObjectProperty(decodedInnerObject, "innerName"));
        assertEquals(Double.valueOf(100), decodedInnerObjectValue);
    }

    @Test
    public void testJsonArrayInObjectPassthrough() {
        // Test array inside object - parse from JSON string
        String json = "" //
                + "{" //
                + "    \"items\": [\"first\", \"second\", 42]," //
                + "    \"count\": 3" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, jsonValue);

        assertNotNull(decoded);
        assertEquals(Double.valueOf(3), getObjectProperty(decoded, "count"));

        // The array should be decoded as JsArray
        Object itemsValue = getObjectProperty(decoded, "items");
        assertNotNull(itemsValue);
        // After decoding, arrays become JsArray
        assertTrue("Items should be JsArray",
                itemsValue instanceof JsArray);
    }

    @Test
    public void testComplexJsonObjectWithPrimitivesPassthrough() {
        // Test complex object with various primitive types - parse from JSON string
        String json = "" //
                + "{" //
                + "    \"string\": \"text value\"," //
                + "    \"number\": 3.14159," //
                + "    \"integer\": 42," //
                + "    \"boolean\": false," //
                + "    \"nullValue\": null" //
                + "}";
        JsonValue jsonValue = Json.parse(json);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, jsonValue);

        assertNotNull(decoded);
        assertEquals("text value", getObjectProperty(decoded, "string"));
        assertEquals(Double.valueOf(3.14159), getObjectProperty(decoded, "number"));
        assertEquals(Double.valueOf(42), getObjectProperty(decoded, "integer"));
        assertEquals(Boolean.FALSE, getObjectProperty(decoded, "boolean"));
        assertNull(getObjectProperty(decoded, "nullValue"));
    }

    @Test
    public void testEmptyJsonObjectPassthrough() {
        // Test empty object - parse from JSON string
        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, Json.parse("{}"));

        assertNotNull(decoded);
        // Empty object should decode to an empty native JS object
    }

}

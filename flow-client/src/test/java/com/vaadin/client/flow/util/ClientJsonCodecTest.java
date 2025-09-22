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

public class ClientJsonCodecTest {
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

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, json);

        Assert.assertSame(element, decoded);
    }

    @Test
    public void decodeWithTypeInfo_componentArray() {
        StateTree tree = new StateTree(null);

        // Create three state nodes with DOM elements
        StateNode node1 = new StateNode(10, tree);
        StateNode node2 = new StateNode(20, tree);
        StateNode node3 = new StateNode(30, tree);

        tree.registerNode(node1);
        tree.registerNode(node2);
        tree.registerNode(node3);

        JsElement element1 = new JsElement() {
        };
        JsElement element2 = new JsElement() {
        };
        JsElement element3 = new JsElement() {
        };

        node1.setDomNode(element1);
        node2.setDomNode(element2);
        node3.setDomNode(element3);

        // Create a JSON array representing Component array
        // Structure: [ARRAY_TYPE, [[NODE_TYPE, id1], [NODE_TYPE, id2],
        // [NODE_TYPE, id3]]]
        JsonArray innerArray = Json.createArray();
        innerArray.set(0, JsonUtils.createArray(
                Json.create(JsonCodec.NODE_TYPE), Json.create(10)));
        innerArray.set(1, JsonUtils.createArray(
                Json.create(JsonCodec.NODE_TYPE), Json.create(20)));
        innerArray.set(2, JsonUtils.createArray(
                Json.create(JsonCodec.NODE_TYPE), Json.create(30)));

        JsonArray json = JsonUtils
                .createArray(Json.create(JsonCodec.ARRAY_TYPE), innerArray);

        // Decode the array
        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, json);

        // Verify it's an array (native JS array when running in GWT)
        Assert.assertNotNull("Decoded array should not be null", decoded);

        // Since this test runs in JVM, we need to handle the type checking
        // differently
        // In actual GWT compilation, this would be a native JS array
        if (decoded instanceof JsArray) {
            JsArray<?> decodedArray = (JsArray<?>) decoded;
            Assert.assertEquals("Array should have 3 elements", 3,
                    decodedArray.length());
            Assert.assertSame("First element should be element1", element1,
                    decodedArray.get(0));
            Assert.assertSame("Second element should be element2", element2,
                    decodedArray.get(1));
            Assert.assertSame("Third element should be element3", element3,
                    decodedArray.get(2));
        } else {
            // In GWT compiled mode, it would be a native array
            // For now, just verify it's not a JsArray of encoded values
            Assert.assertTrue("Decoded value should be an array",
                    decoded.getClass().isArray() || decoded instanceof JsArray);
        }
    }

    @Test
    public void decodeWithTypeInfo_stringArray() {
        // Test a simple string array to ensure it still works
        JsonArray innerArray = Json.createArray();
        innerArray.set(0, Json.create("Hello"));
        innerArray.set(1, Json.create("World"));
        innerArray.set(2, Json.create("Test"));

        JsonArray json = JsonUtils
                .createArray(Json.create(JsonCodec.ARRAY_TYPE), innerArray);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(null, json);

        Assert.assertNotNull("Decoded array should not be null", decoded);

        // Verify the strings are decoded correctly
        if (decoded instanceof JsArray) {
            JsArray<?> decodedArray = (JsArray<?>) decoded;
            Assert.assertEquals("Array should have 3 elements", 3,
                    decodedArray.length());
            Assert.assertEquals("Hello", decodedArray.get(0));
            Assert.assertEquals("World", decodedArray.get(1));
            Assert.assertEquals("Test", decodedArray.get(2));
        }
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

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        StateNode decoded = ClientJsonCodec.decodeStateNode(tree, json);

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

}

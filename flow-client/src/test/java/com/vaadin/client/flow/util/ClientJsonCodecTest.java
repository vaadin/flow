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

        // Create compact @v-node format
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

        // Create compact @v-node format
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
        // Test that unknown @v- prefixed types are rejected for forward compatibility
        elemental.json.JsonObject unknownType = Json.createObject();
        unknownType.put("@v-unknown", "someValue");
        
        try {
            ClientJsonCodec.decodeWithTypeInfo(null, unknownType);
            Assert.fail("Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Exception message should mention the unknown key",
                    e.getMessage().contains("@v-unknown"));
        }
    }

    @Test
    public void decodeStateNode_unknownVType_throwsException() {
        // Test that unknown @v- prefixed types are rejected in decodeStateNode too
        elemental.json.JsonObject unknownType = Json.createObject();
        unknownType.put("@v-future", 42);
        
        try {
            ClientJsonCodec.decodeStateNode(null, unknownType);
            Assert.fail("Expected IllegalArgumentException for unknown @v- type");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Exception message should mention the unknown key",
                    e.getMessage().contains("@v-future"));
        }
    }

}

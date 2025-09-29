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

    @Test
    public void decodeWithTypeInfo_beanWithComponent() {
        StateTree tree = new StateTree(null);
        StateNode componentNode = new StateNode(44, tree);
        tree.registerNode(componentNode);

        JsElement element = new JsElement() {
        };
        componentNode.setDomNode(element);

        // Create a bean JSON with a component reference
        elemental.json.JsonObject beanJson = Json.createObject();
        beanJson.put("name", "TestBean");
        beanJson.put("value", 42);

        elemental.json.JsonObject componentRef = Json.createObject();
        componentRef.put("@vaadin", "component");
        componentRef.put("nodeId", componentNode.getId());
        beanJson.put("button", componentRef);

        // Wrap as BEAN_TYPE
        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, beanJson);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, wrappedBean);

        Assert.assertNotNull("Decoded bean should not be null", decoded);

        // In JVM tests, this returns the raw JsonValue
        // In real GWT, it would be a JavaScript object with component
        // resolution
        Assert.assertTrue("Should decode to JsonValue in JVM tests",
                decoded instanceof JsonValue);
    }

    @Test
    public void decodeWithTypeInfo_beanWithComponentArray() {
        StateTree tree = new StateTree(null);

        // Create multiple component nodes
        StateNode node1 = new StateNode(45, tree);
        StateNode node2 = new StateNode(46, tree);
        tree.registerNode(node1);
        tree.registerNode(node2);

        JsElement element1 = new JsElement() {
        };
        JsElement element2 = new JsElement() {
        };
        node1.setDomNode(element1);
        node2.setDomNode(element2);

        // Create a bean with component array
        elemental.json.JsonObject beanJson = Json.createObject();
        beanJson.put("title", "Form");

        JsonArray componentsArray = Json.createArray();

        elemental.json.JsonObject ref1 = Json.createObject();
        ref1.put("__vaadinType", "component");
        ref1.put("nodeId", node1.getId());
        componentsArray.set(0, ref1);

        elemental.json.JsonObject ref2 = Json.createObject();
        ref2.put("__vaadinType", "component");
        ref2.put("nodeId", node2.getId());
        componentsArray.set(1, ref2);

        componentsArray.set(2, Json.createNull());

        beanJson.put("components", componentsArray);

        // Wrap as BEAN_TYPE
        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, beanJson);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, wrappedBean);

        Assert.assertNotNull("Decoded bean should not be null", decoded);
    }

    @Test
    public void decodeWithTypeInfo_nestedBeansWithComponents() {
        StateTree tree = new StateTree(null);
        StateNode componentNode = new StateNode(47, tree);
        tree.registerNode(componentNode);

        JsElement element = new JsElement() {
        };
        componentNode.setDomNode(element);

        // Create nested bean structure
        elemental.json.JsonObject innerBean = Json.createObject();
        innerBean.put("text", "inner");

        elemental.json.JsonObject componentRef = Json.createObject();
        componentRef.put("@vaadin", "component");
        componentRef.put("nodeId", componentNode.getId());
        innerBean.put("component", componentRef);

        elemental.json.JsonObject outerBean = Json.createObject();
        outerBean.put("id", "outer");
        outerBean.put("nested", innerBean);

        // Wrap as BEAN_TYPE
        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, outerBean);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, wrappedBean);

        Assert.assertNotNull("Decoded nested bean should not be null", decoded);
    }

    @Test
    public void decodeWithTypeInfo_beanWithNullComponent() {
        StateTree tree = new StateTree(null);

        // Create a bean with null component reference
        elemental.json.JsonObject beanJson = Json.createObject();
        beanJson.put("text", "TestBean");
        beanJson.put("component", Json.createNull());

        // Wrap as BEAN_TYPE
        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, beanJson);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, wrappedBean);

        Assert.assertNotNull("Decoded bean should not be null", decoded);
    }

    @Test
    public void decodeWithTypeInfo_beanWithUnattachedComponent() {
        StateTree tree = new StateTree(null);

        // Create a bean with component reference that has null nodeId
        elemental.json.JsonObject beanJson = Json.createObject();
        beanJson.put("name", "TestBean");

        elemental.json.JsonObject componentRef = Json.createObject();
        componentRef.put("@vaadin", "component");
        componentRef.put("nodeId", Json.createNull());
        beanJson.put("unattached", componentRef);

        // Wrap as BEAN_TYPE
        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, beanJson);

        Object decoded = ClientJsonCodec.decodeWithTypeInfo(tree, wrappedBean);

        Assert.assertNotNull(
                "Decoded bean with unattached component should not be null",
                decoded);
    }

    @Test
    public void decodeStateNode_bean() {
        // Test that decodeStateNode returns null for BEAN_TYPE
        elemental.json.JsonObject beanJson = Json.createObject();
        beanJson.put("field", "value");

        JsonArray wrappedBean = Json.createArray();
        wrappedBean.set(0, ClientJsonCodec.BEAN_TYPE);
        wrappedBean.set(1, beanJson);

        StateNode decoded = ClientJsonCodec.decodeStateNode(null, wrappedBean);

        Assert.assertNull("decodeStateNode should return null for BEAN_TYPE",
                decoded);
    }

}

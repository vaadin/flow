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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Test
    public void encodeWithTypeInfo_unsupportedTypes() {
        for (Object value : withTypeInfoUnsupportedValues) {
            boolean thrown = false;
            try {
                JacksonCodec.encodeWithTypeInfo(value);

            } catch (AssertionError expected) {
                thrown = true;
            }
            if (!thrown) {
                Assert.fail("Should throw for " + value.getClass());
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
}

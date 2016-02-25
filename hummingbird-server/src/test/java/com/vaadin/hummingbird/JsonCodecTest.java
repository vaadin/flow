/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;
import elemental.json.JsonValue;

public class JsonCodecTest {
    private static List<Object> withTypeInfoUnsupportedValues = Arrays.asList(
            new Object(), new StateNode(), new Date(), new String[0],
            new ArrayList<>(), new HashSet<>(), new HashMap<>());

    @Test
    public void encodeWithoutTypeInfo_supportedTypes() {
        assertJsonEquals(Json.create(true),
                JsonCodec.encodeWithoutTypeInfo(Boolean.TRUE));
        assertJsonEquals(Json.create("string"),
                JsonCodec.encodeWithoutTypeInfo("string"));
        assertJsonEquals(Json.create(3.14),
                JsonCodec.encodeWithoutTypeInfo(Double.valueOf(3.14)));
        assertJsonEquals(Json.create(42),
                JsonCodec.encodeWithoutTypeInfo(Integer.valueOf(42)));
        assertJsonEquals(Json.createNull(),
                JsonCodec.encodeWithoutTypeInfo(null));

        assertJsonEquals(Json.createNull(), Json.createNull());
        assertJsonEquals(Json.create(false), Json.create(false));
        assertJsonEquals(Json.create(234), Json.create(234));
        assertJsonEquals(Json.create("string"), Json.create("string"));
        assertJsonEquals(Json.createObject(), Json.createObject());
        assertJsonEquals(Json.createArray(), Json.createArray());
    }

    @Test
    public void encodeWithoutTypeInfo_unsupportedTypes() {
        List<Object> unsupported = new ArrayList<>(
                withTypeInfoUnsupportedValues);
        unsupported.add(new Element("div"));

        for (Object value : unsupported) {
            try {
                JsonCodec.encodeWithoutTypeInfo(value);

                Assert.fail("Should throw for " + value.getClass());
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void encodeWithTypeInfo_basicTypes() {
        assertJsonEquals(Json.create(true),
                JsonCodec.encodeWithTypeInfo(Boolean.TRUE));
        assertJsonEquals(Json.createNull(), JsonCodec.encodeWithTypeInfo(null));

        assertJsonEquals(Json.create(234),
                JsonCodec.encodeWithTypeInfo(Json.create(234)));
        assertJsonEquals(Json.create("string"),
                JsonCodec.encodeWithTypeInfo(Json.create("string")));
        assertJsonEquals(Json.createObject(),
                JsonCodec.encodeWithTypeInfo(Json.createObject()));

        // Array is escaped
        assertJsonEquals(
                JsonUtil.createArray(Json.create(JsonCodec.ARRAY_TYPE),
                        Json.createArray()),
                JsonCodec.encodeWithTypeInfo(Json.createArray()));
    }

    @Test
    public void encodeWithTypeInfo_attachedElement() {
        Element element = new Element("div");

        StateTree tree = new StateTree(ElementChildrenNamespace.class);
        tree.getRootNode().getNamespace(ElementChildrenNamespace.class).add(0,
                element.getNode());

        JsonValue json = JsonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(
                JsonUtil.createArray(Json.create(JsonCodec.ELEMENT_TYPE),
                        Json.create(element.getNode().getId())),
                json);
    }

    @Test
    public void encodeWithTypeInfo_detachedElement() {
        Element element = new Element("div");

        JsonValue json = JsonCodec.encodeWithTypeInfo(element);

        assertJsonEquals(Json.createNull(), json);
    }

    @Test
    public void encodeWithTypeInfo_unsupportedTypes() {
        for (Object value : withTypeInfoUnsupportedValues) {
            try {
                JsonCodec.encodeWithTypeInfo(value);

                Assert.fail("Should throw for " + value.getClass());
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    private static void assertJsonEquals(JsonValue expected, JsonValue actual) {
        Assert.assertTrue(
                actual.toJson() + " does not equal " + expected.toJson(),
                JsonUtil.jsonEquals(expected, actual));
    }
}

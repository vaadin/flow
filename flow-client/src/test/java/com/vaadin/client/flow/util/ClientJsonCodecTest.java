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
package com.vaadin.client.flow.util;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.Registry;
import com.vaadin.client.flow.StateTree;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Tests for the parts of {@link ClientJsonCodec} that do not need a browser.
 * <p>
 * The values that decode to something only a browser can provide - DOM nodes,
 * functions and JS module imports - are covered by integration tests instead;
 * what is checked here is that a malformed value is rejected with a message
 * naming the offending type rather than failing later on.
 */
public class ClientJsonCodecTest {

    private final StateTree tree = new StateTree(new Registry() {
        {
            set(StateTree.class, new StateTree(this));
        }
    });

    @Test
    public void decodeWithTypeInfo_primitivesDecodedAsThemselves() {
        Assert.assertEquals("foo",
                ClientJsonCodec.decodeWithTypeInfo(tree, Json.create("foo")));
        Assert.assertEquals(Double.valueOf(3),
                ClientJsonCodec.decodeWithTypeInfo(tree, Json.create(3)));
        Assert.assertEquals(Boolean.TRUE,
                ClientJsonCodec.decodeWithTypeInfo(tree, Json.create(true)));
        Assert.assertNull(
                ClientJsonCodec.decodeWithTypeInfo(tree, Json.createNull()));
    }

    @Test
    public void decodeWithTypeInfo_jsImportsWithNonStringKey_throws() {
        JsonObject json = Json.createObject();
        json.put("@v-imports", 42);

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ClientJsonCodec.decodeWithTypeInfo(tree, json));

        Assert.assertTrue(
                "The message should name the attribute, was: "
                        + exception.getMessage(),
                exception.getMessage().contains("@v-imports"));
    }

    @Test
    public void decodeWithTypeInfo_unknownVaadinType_throws() {
        JsonObject json = Json.createObject();
        json.put("@v-nonsense", "whatever");

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ClientJsonCodec.decodeWithTypeInfo(tree, json));

        Assert.assertTrue(
                "The message should name the unsupported type, was: "
                        + exception.getMessage(),
                exception.getMessage().contains("@v-nonsense"));
    }
}

/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.change;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateNodeTest;
import com.vaadin.flow.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.nodefeature.NodeMap;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class MapPutChangeTest {
    private NodeMap feature = AbstractNodeFeatureTest
            .createFeature(ElementPropertyMap.class);

    @Test
    public void testJson() {
        MapPutChange change = new MapPutChange(feature, "some", "string");

        JsonObject json = change.toJson(null);

        assertEquals(change.getNode().getId(),
                (int) json.getNumber(JsonConstants.CHANGE_NODE));
        assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                (int) json.getNumber(JsonConstants.CHANGE_FEATURE));
        assertEquals(JsonConstants.CHANGE_TYPE_PUT,
                json.getString(JsonConstants.CHANGE_TYPE));
        assertEquals("some",
                json.getString(JsonConstants.CHANGE_MAP_KEY));
        assertEquals("string",
                json.getString(JsonConstants.CHANGE_PUT_VALUE));
    }

    @Test
    public void testJsonValueTypes() {
        JsonValue stringValue = getValue("string");
        assertSame(JsonType.STRING, stringValue.getType());
        assertEquals("string", stringValue.asString());

        JsonValue numberValue = getValue(Integer.valueOf(1));
        assertSame(JsonType.NUMBER, numberValue.getType());
        assertEquals(1, numberValue.asNumber(), 0);

        JsonValue booleanValue = getValue(Boolean.TRUE);
        assertSame(JsonType.BOOLEAN, booleanValue.getType());
        assertTrue(booleanValue.asBoolean());

        JsonObject jsonInput = Json.createObject();
        JsonValue jsonValue = getValue(jsonInput);
        assertSame(JsonType.OBJECT, jsonValue.getType());
        assertSame(jsonInput, jsonValue);
    }

    @Test
    public void testNodeValueType() {
        StateNode value = StateNodeTest.createEmptyNode("value");
        MapPutChange change = new MapPutChange(feature, "myKey", value);

        JsonObject json = change.toJson(null);
        assertFalse(json.hasKey(JsonConstants.CHANGE_PUT_VALUE));

        JsonValue nodeValue = json.get(JsonConstants.CHANGE_PUT_NODE_VALUE);
        assertSame(JsonType.NUMBER, nodeValue.getType());
        assertEquals(value.getId(), (int) nodeValue.asNumber());
    }

    private JsonValue getValue(Object input) {
        MapPutChange change = new MapPutChange(feature, "myKey", input);
        JsonObject json = change.toJson(null);
        return json.get(JsonConstants.CHANGE_PUT_VALUE);
    }

}

/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.internal.change;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

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

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber(JsonConstants.CHANGE_NODE));
        Assert.assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                (int) json.getNumber(JsonConstants.CHANGE_FEATURE));
        Assert.assertEquals(JsonConstants.CHANGE_TYPE_PUT,
                json.getString(JsonConstants.CHANGE_TYPE));
        Assert.assertEquals("some",
                json.getString(JsonConstants.CHANGE_MAP_KEY));
        Assert.assertEquals("string",
                json.getString(JsonConstants.CHANGE_PUT_VALUE));
    }

    @Test
    public void testJsonValueTypes() {
        JsonValue stringValue = getValue("string");
        Assert.assertSame(JsonType.STRING, stringValue.getType());
        Assert.assertEquals("string", stringValue.asString());

        JsonValue numberValue = getValue(Integer.valueOf(1));
        Assert.assertSame(JsonType.NUMBER, numberValue.getType());
        Assert.assertEquals(1, numberValue.asNumber(), 0);

        JsonValue booleanValue = getValue(Boolean.TRUE);
        Assert.assertSame(JsonType.BOOLEAN, booleanValue.getType());
        Assert.assertTrue(booleanValue.asBoolean());

        JsonObject jsonInput = Json.createObject();
        JsonValue jsonValue = getValue(jsonInput);
        Assert.assertSame(JsonType.OBJECT, jsonValue.getType());
        Assert.assertSame(jsonInput, jsonValue);
    }

    @Test
    public void testNodeValueType() {
        StateNode value = StateNodeTest.createEmptyNode("value");
        MapPutChange change = new MapPutChange(feature, "myKey", value);

        JsonObject json = change.toJson(null);
        Assert.assertFalse(json.hasKey(JsonConstants.CHANGE_PUT_VALUE));

        JsonValue nodeValue = json.get(JsonConstants.CHANGE_PUT_NODE_VALUE);
        Assert.assertSame(JsonType.NUMBER, nodeValue.getType());
        Assert.assertEquals(value.getId(), (int) nodeValue.asNumber());
    }

    private JsonValue getValue(Object input) {
        MapPutChange change = new MapPutChange(feature, "myKey", input);
        JsonObject json = change.toJson(null);
        return json.get(JsonConstants.CHANGE_PUT_VALUE);
    }

}

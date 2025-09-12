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

package com.vaadin.flow.internal.change;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

public class MapPutChangeTest {
    private NodeMap feature = AbstractNodeFeatureTest
            .createFeature(ElementPropertyMap.class);

    @Test
    public void testJson() {
        MapPutChange change = new MapPutChange(feature, "some", "string");

        ObjectNode json = change.toJson(null);

        Assert.assertEquals(change.getNode().getId(),
                json.get(JsonConstants.CHANGE_NODE).intValue());
        Assert.assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                json.get(JsonConstants.CHANGE_FEATURE).intValue());
        Assert.assertEquals(JsonConstants.CHANGE_TYPE_PUT,
                json.get(JsonConstants.CHANGE_TYPE).textValue());
        Assert.assertEquals("some",
                json.get(JsonConstants.CHANGE_MAP_KEY).textValue());
        Assert.assertEquals("string",
                json.get(JsonConstants.CHANGE_PUT_VALUE).textValue());
    }

    @Test
    public void testJsonValueTypes() {
        JsonNode stringValue = getValue("string");
        Assert.assertSame(JsonNodeType.STRING, stringValue.getNodeType());
        Assert.assertEquals("string", stringValue.textValue());

        JsonNode numberValue = getValue(Integer.valueOf(1));
        Assert.assertSame(JsonNodeType.NUMBER, numberValue.getNodeType());
        Assert.assertEquals(1, numberValue.intValue(), 0);

        JsonNode booleanValue = getValue(Boolean.TRUE);
        Assert.assertSame(JsonNodeType.BOOLEAN, booleanValue.getNodeType());
        Assert.assertTrue(booleanValue.asBoolean());

        ObjectNode jsonInput = JacksonUtils.createObjectNode();
        JsonNode jsonValue = getValue(jsonInput);
        Assert.assertSame(JsonNodeType.OBJECT, jsonValue.getNodeType());
        // Should use same when all *Change classes populateJson updated
        Assert.assertEquals(jsonInput, jsonValue);
    }

    @Test
    public void testNodeValueType() {
        StateNode value = StateNodeTest.createEmptyNode("value");
        MapPutChange change = new MapPutChange(feature, "myKey", value);

        ObjectNode json = change.toJson(null);
        Assert.assertFalse(json.has(JsonConstants.CHANGE_PUT_VALUE));

        JsonNode nodeValue = json.get(JsonConstants.CHANGE_PUT_NODE_VALUE);
        Assert.assertSame(JsonNodeType.NUMBER, nodeValue.getNodeType());
        Assert.assertEquals(value.getId(), nodeValue.intValue());
    }

    private JsonNode getValue(Object input) {
        MapPutChange change = new MapPutChange(feature, "myKey", input);
        ObjectNode json = change.toJson(null);
        return json.get(JsonConstants.CHANGE_PUT_VALUE);
    }

}

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
package com.vaadin.flow.internal.change;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPutChangeTest {
    private NodeMap feature = AbstractNodeFeatureTest
            .createFeature(ElementPropertyMap.class);

    @Test
    void testJson() {
        MapPutChange change = new MapPutChange(feature, "some", "string");

        ObjectNode json = change.toJson(null);

        assertEquals(change.getNode().getId(),
                json.get(JsonConstants.CHANGE_NODE).intValue());
        assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                json.get(JsonConstants.CHANGE_FEATURE).intValue());
        assertEquals(JsonConstants.CHANGE_TYPE_PUT,
                json.get(JsonConstants.CHANGE_TYPE).textValue());
        assertEquals("some",
                json.get(JsonConstants.CHANGE_MAP_KEY).textValue());
        assertEquals("string",
                json.get(JsonConstants.CHANGE_PUT_VALUE).textValue());
    }

    @Test
    void testJsonValueTypes() {
        JsonNode stringValue = getValue("string");
        assertSame(JsonNodeType.STRING, stringValue.getNodeType());
        assertEquals("string", stringValue.textValue());

        JsonNode numberValue = getValue(Integer.valueOf(1));
        assertSame(JsonNodeType.NUMBER, numberValue.getNodeType());
        assertEquals(1, numberValue.intValue(), 0);

        JsonNode booleanValue = getValue(Boolean.TRUE);
        assertSame(JsonNodeType.BOOLEAN, booleanValue.getNodeType());
        assertTrue(booleanValue.asBoolean());

        ObjectNode jsonInput = JacksonUtils.createObjectNode();
        JsonNode jsonValue = getValue(jsonInput);
        assertSame(JsonNodeType.OBJECT, jsonValue.getNodeType());
        // Should use same when all *Change classes populateJson updated
        assertEquals(jsonInput, jsonValue);
    }

    @Test
    void testNodeValueType() {
        StateNode value = StateNodeTest.createEmptyNode("value");
        MapPutChange change = new MapPutChange(feature, "myKey", value);

        ObjectNode json = change.toJson(null);
        assertFalse(json.has(JsonConstants.CHANGE_PUT_VALUE));

        JsonNode nodeValue = json.get(JsonConstants.CHANGE_PUT_NODE_VALUE);
        assertSame(JsonNodeType.NUMBER, nodeValue.getNodeType());
        assertEquals(value.getId(), nodeValue.intValue());
    }

    private JsonNode getValue(Object input) {
        MapPutChange change = new MapPutChange(feature, "myKey", input);
        ObjectNode json = change.toJson(null);
        return json.get(JsonConstants.CHANGE_PUT_VALUE);
    }

}

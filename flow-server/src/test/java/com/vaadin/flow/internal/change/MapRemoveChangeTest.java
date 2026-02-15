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
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeMap;
import com.vaadin.flow.shared.JsonConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapRemoveChangeTest {
    private NodeMap feature = AbstractNodeFeatureTest
            .createFeature(ElementPropertyMap.class);

    @Test
    public void testJson() {
        MapRemoveChange change = new MapRemoveChange(feature, "some");

        ObjectNode json = change.toJson(null);

        assertEquals(change.getNode().getId(),
                json.get(JsonConstants.CHANGE_NODE).intValue());
        assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                json.get(JsonConstants.CHANGE_FEATURE).intValue());
        assertEquals(JsonConstants.CHANGE_TYPE_REMOVE,
                json.get(JsonConstants.CHANGE_TYPE).textValue());
        assertEquals("some",
                json.get(JsonConstants.CHANGE_MAP_KEY).textValue());
    }

}

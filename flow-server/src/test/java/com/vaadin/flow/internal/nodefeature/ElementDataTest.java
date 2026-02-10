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
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;

class ElementDataTest extends AbstractNodeFeatureTest<ElementData> {
    private final ElementData elementData = new StateNode(
            Collections.singletonList(ElementData.class))
            .getFeature(ElementData.class);

    @Test
    public void setGetTag() {
        Assertions.assertNull(elementData.getTag(),
                "Tag should initially be null");

        elementData.setTag("myTag");

        Assertions.assertEquals("myTag", elementData.getTag());
    }

    @Test
    public void setGetPayload() {
        Assertions.assertNull(elementData.getPayload(),
                "Tag should initially be null");

        ObjectNode object = JacksonUtils.createObjectNode();
        elementData.setPayload(object);

        Assertions.assertEquals(object, elementData.getPayload());
    }

    @Test
    public void collectChanges_setTagOnly_onlyOneChanges() {
        elementData.setTag("foo");
        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assertions.assertEquals(1, changes.size());
        Assertions.assertTrue(changes.get(0) instanceof MapPutChange);

        MapPutChange change = (MapPutChange) changes.get(0);

        Assertions.assertEquals(NodeProperties.TAG, change.getKey());
        Assertions.assertEquals(elementData.getNode(), change.getNode());
        Assertions.assertEquals("foo", change.getValue());
    }

    @Test
    public void collectChanges_setPayloadOnly_onlyOneChanges() {
        ObjectNode object = JacksonUtils.createObjectNode();
        elementData.setPayload(object);
        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assertions.assertEquals(1, changes.size());
        Assertions.assertTrue(changes.get(0) instanceof MapPutChange);
        MapPutChange change = (MapPutChange) changes.get(0);

        Assertions.assertEquals(NodeProperties.PAYLOAD, change.getKey());
        Assertions.assertEquals(elementData.getNode(), change.getNode());
        Assertions.assertEquals(object, change.getValue());
    }

    @Test
    public void collectChanges_setBothTagAndPayload_twoChanges() {
        ObjectNode object = JacksonUtils.createObjectNode();
        elementData.setPayload(object);
        elementData.setTag("foo");

        List<NodeChange> changes = new ArrayList<>();
        elementData.collectChanges(changes::add);

        Assertions.assertEquals(2, changes.size());
        Assertions.assertTrue(changes.get(0) instanceof MapPutChange);
        Assertions.assertTrue(changes.get(1) instanceof MapPutChange);

        MapPutChange change = getChange(changes, NodeProperties.TAG);
        Assertions.assertEquals(NodeProperties.TAG, change.getKey());
        Assertions.assertEquals(elementData.getNode(), change.getNode());
        Assertions.assertEquals("foo", change.getValue());

        change = getChange(changes, NodeProperties.PAYLOAD);
        Assertions.assertEquals(NodeProperties.PAYLOAD, change.getKey());
        Assertions.assertEquals(elementData.getNode(), change.getNode());
        Assertions.assertEquals(object, change.getValue());
    }

    private MapPutChange getChange(List<NodeChange> changes, String key) {
        Optional<MapPutChange> keyFound = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(chang -> chang.getKey().equals(key)).findFirst();
        Assertions.assertTrue(keyFound.isPresent(),
                "No " + key + " change found");
        return keyFound.get();
    }
}

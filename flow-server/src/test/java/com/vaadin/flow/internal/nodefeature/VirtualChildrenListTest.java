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

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.internal.StateNode;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualChildrenListTest {

    private StateNode node = new StateNode(VirtualChildrenList.class);
    private VirtualChildrenList list = node
            .getFeature(VirtualChildrenList.class);

    private StateNode child = new StateNode(ElementData.class);

    @Test
    public void insert_atIndexWithType_payloadIsSetAndElementIsInserted() {
        list.add(0, child, "foo", (String) null);

        Assertions.assertEquals(child, list.get(0));

        JsonNode payload = (JsonNode) child.getFeature(ElementData.class)
                .getPayload();
        Assertions.assertNotNull(payload);

        Assertions.assertEquals("foo",
                payload.get(NodeProperties.TYPE).asString());

        StateNode anotherChild = new StateNode(ElementData.class);
        list.add(0, anotherChild, "bar", (String) null);

        Assertions.assertEquals(anotherChild, list.get(0));

        payload = (JsonNode) anotherChild.getFeature(ElementData.class)
                .getPayload();
        Assertions.assertNotNull(payload);

        Assertions.assertEquals("bar",
                payload.get(NodeProperties.TYPE).asString());
    }

    @Test
    public void insert_atIndexWithPayload_payloadIsSetAndElementIsInserted() {
        list.add(0, child, "foo", "bar");

        Assertions.assertEquals(child, list.get(0));

        JsonNode payload = (JsonNode) child.getFeature(ElementData.class)
                .getPayload();
        Assertions.assertNotNull(payload);

        Assertions.assertEquals("foo",
                payload.get(NodeProperties.TYPE).asString());
        Assertions.assertEquals("bar",
                payload.get(NodeProperties.PAYLOAD).asString());
    }

    @Test
    public void iteratorAndSize_addTwoItems_methodsReturnCorrectValues() {
        list.append(child, "foo");
        StateNode anotherChild = new StateNode(ElementData.class);
        list.append(anotherChild, "bar");

        Assertions.assertEquals(2, list.size());

        Set<StateNode> set = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(list.iterator(),
                        Spliterator.ORDERED), false)
                .collect(Collectors.toSet());
        Assertions.assertEquals(2, set.size());

        set.remove(child);
        set.remove(anotherChild);

        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void remove_withIndex_removesNodeAndPayload() {
        list.append(child, "foo");

        Assertions.assertEquals(child, list.get(0));

        list.remove(0);

        Assertions.assertEquals(0, list.size());
        Assertions.assertEquals(-1, list.indexOf(child));

        JsonNode payload = (JsonNode) child.getFeature(ElementData.class)
                .getPayload();
        Assertions.assertNull(payload);
    }

    @Test
    public void clear_throw() {
        assertThrows(UnsupportedOperationException.class, () -> {
            list.append(child, "foo");
            list.clear();
        });
    }

}

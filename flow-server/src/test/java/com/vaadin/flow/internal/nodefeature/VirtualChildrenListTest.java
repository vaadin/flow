/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.internal.StateNode;
import org.junit.Assert;
import org.junit.Test;

import elemental.json.JsonObject;

public class VirtualChildrenListTest {

    private StateNode node = new StateNode(VirtualChildrenList.class);
    private VirtualChildrenList list = node
            .getFeature(VirtualChildrenList.class);

    private StateNode child = new StateNode(ElementData.class);

    @Test
    public void insert_atIndexWithType_payloadIsSetAndElementIsInserted() {
        list.add(0, child, "foo", (String) null);

        Assert.assertEquals(child, list.get(0));

        JsonObject payload = (JsonObject) child.getFeature(ElementData.class)
                .getPayload();
        Assert.assertNotNull(payload);

        Assert.assertEquals("foo", payload.get(NodeProperties.TYPE).asString());

        StateNode anotherChild = new StateNode(ElementData.class);
        list.add(0, anotherChild, "bar", (String) null);

        Assert.assertEquals(anotherChild, list.get(0));

        payload = (JsonObject) anotherChild.getFeature(ElementData.class)
                .getPayload();
        Assert.assertNotNull(payload);

        Assert.assertEquals("bar", payload.get(NodeProperties.TYPE).asString());
    }

    @Test
    public void insert_atIndexWithPayload_payloadIsSetAndElementIsInserted() {
        list.add(0, child, "foo", "bar");

        Assert.assertEquals(child, list.get(0));

        JsonObject payload = (JsonObject) child.getFeature(ElementData.class)
                .getPayload();
        Assert.assertNotNull(payload);

        Assert.assertEquals("foo", payload.get(NodeProperties.TYPE).asString());
        Assert.assertEquals("bar",
                payload.get(NodeProperties.PAYLOAD).asString());
    }

    @Test
    public void iteratorAndSize_addTwoItems_methodsReturnCorrectValues() {
        list.append(child, "foo");
        StateNode anotherChild = new StateNode(ElementData.class);
        list.append(anotherChild, "bar");

        Assert.assertEquals(2, list.size());

        Set<StateNode> set = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(list.iterator(),
                        Spliterator.ORDERED), false)
                .collect(Collectors.toSet());
        Assert.assertEquals(2, set.size());

        set.remove(child);
        set.remove(anotherChild);

        Assert.assertEquals(0, set.size());
    }

    @Test
    public void remove_withIndex_removesNodeAndPayload() {
        list.append(child, "foo");

        Assert.assertEquals(child, list.get(0));

        list.remove(0);

        Assert.assertEquals(0, list.size());
        Assert.assertEquals(-1, list.indexOf(child));

        JsonObject payload = (JsonObject) child.getFeature(ElementData.class)
                .getPayload();
        Assert.assertNull(payload);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void clear_throw() {
        list.append(child, "foo");
        list.clear();
    }

}

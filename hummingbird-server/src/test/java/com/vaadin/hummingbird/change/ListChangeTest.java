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

package com.vaadin.hummingbird.change;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.namespace.AbstractNamespaceTest;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ListNamespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;
import com.vaadin.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ListChangeTest {
    private ListNamespace namespace = AbstractNamespaceTest
            .createNamespace(ElementChildrenNamespace.class);

    @Test
    public void testBasicJson() {
        StateNode child1 = StateNodeTest.createEmptyNode("child1");
        StateNode child2 = StateNodeTest.createEmptyNode("child2");
        ListSpliceChange change = new ListSpliceChange(namespace, 0, 1,
                Arrays.asList(child1, child2));

        JsonObject json = change.toJson();

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber(JsonConstants.CHANGE_NODE));
        Assert.assertEquals(NamespaceRegistry.getId(namespace.getClass()),
                (int) json.getNumber(JsonConstants.CHANGE_NAMESPACE));
        Assert.assertEquals(JsonConstants.CHANGE_TYPE_SPLICE,
                json.getString(JsonConstants.CHANGE_TYPE));
        Assert.assertEquals(0,
                (int) json.getNumber(JsonConstants.CHANGE_SPLICE_INDEX));
        Assert.assertEquals(1,
                (int) json.getNumber(JsonConstants.CHANGE_SPLICE_REMOVE));

        JsonArray addNodes = json
                .getArray(JsonConstants.CHANGE_SPLICE_ADD_NODES);
        Assert.assertEquals(2, addNodes.length());

        Assert.assertEquals(child1.getId(), (int) addNodes.getNumber(0));
        Assert.assertEquals(child2.getId(), (int) addNodes.getNumber(1));
    }

    @Test
    public void testZeroRemoveNotInJson() {
        ListSpliceChange change = new ListSpliceChange(namespace, 1, 0,
                Arrays.asList());

        JsonObject json = change.toJson();

        Assert.assertFalse(json.hasKey(JsonConstants.CHANGE_SPLICE_REMOVE));
    }

    @Test
    public void testEmptyAddNotInJson() {
        ListSpliceChange change = new ListSpliceChange(namespace, 1, 0,
                Arrays.asList());

        JsonObject json = change.toJson();

        Assert.assertFalse(json.hasKey(JsonConstants.CHANGE_SPLICE_ADD_NODES));
    }
}

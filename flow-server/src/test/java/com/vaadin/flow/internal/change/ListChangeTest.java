/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.change;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.nodefeature.AbstractNodeFeatureTest;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.NodeFeatureRegistry;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ListChangeTest {
    private NodeList<StateNode> feature = AbstractNodeFeatureTest
            .createFeature(ElementChildrenList.class);

    @Test
    public void testBasicJson() {
        StateNode child1 = StateNodeTest.createEmptyNode("child1");
        StateNode child2 = StateNodeTest.createEmptyNode("child2");
        ListAddChange<StateNode> change = new ListAddChange<>(feature, true, 0,
                Arrays.asList(child1, child2));

        JsonObject json = change.toJson(null);

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber(JsonConstants.CHANGE_NODE));
        Assert.assertEquals(NodeFeatureRegistry.getId(feature.getClass()),
                (int) json.getNumber(JsonConstants.CHANGE_FEATURE));
        Assert.assertEquals(JsonConstants.CHANGE_TYPE_SPLICE,
                json.getString(JsonConstants.CHANGE_TYPE));
        Assert.assertEquals(0,
                (int) json.getNumber(JsonConstants.CHANGE_SPLICE_INDEX));

        JsonArray addNodes = json
                .getArray(JsonConstants.CHANGE_SPLICE_ADD_NODES);
        Assert.assertEquals(2, addNodes.length());

        Assert.assertEquals(child1.getId(), (int) addNodes.getNumber(0));
        Assert.assertEquals(child2.getId(), (int) addNodes.getNumber(1));
    }

    @Test
    public void testZeroRemoveNotInJson() {
        ListAddChange<StateNode> change = new ListAddChange<>(feature, false, 1,
                Arrays.asList());

        JsonObject json = change.toJson(null);

        Assert.assertFalse(json.hasKey(JsonConstants.CHANGE_SPLICE_REMOVE));
    }

    @Test
    public void testEmptyAddNotInJson() {
        ListAddChange<StateNode> change = new ListAddChange<>(feature, false, 1,
                Arrays.asList());

        JsonObject json = change.toJson(null);

        Assert.assertFalse(json.hasKey(JsonConstants.CHANGE_SPLICE_ADD_NODES));
    }
}

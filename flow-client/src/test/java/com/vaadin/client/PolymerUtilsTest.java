package com.vaadin.client;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.StateNode;

public class PolymerUtilsTest {

    @Test
    public void stateNodeWithNoFeatures_serializedAsNull() {
        StateNode emptyNode = new StateNode(-1, null);
        Assert.assertNull(
                "StateNode with no node features should be serialized as null",
                PolymerUtils.createModelTree(emptyNode));
    }

}

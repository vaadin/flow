/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.NodeChange;

public class NodeListEmptyRequiredFeatureTest {

    private StateNode node;

    private NodeList<StateNode> nodeList;

    @Before
    public void setUp() {
        node = new StateNode(Arrays.asList(ElementChildrenList.class)) {
            @Override
            public boolean isAttached() {
                return true;
            }
        };
        nodeList = node.getFeature(ElementChildrenList.class);
    }

    @Test
    public void generateChangesFromEmpty_featureHasChangesToCollect() {
        nodeList.generateChangesFromEmpty();

        AtomicReference<NodeChange> change = new AtomicReference<>();
        node.collectChanges(change::set);

        Assert.assertTrue(change.get() instanceof EmptyChange);

        nodeList.generateChangesFromEmpty();
        change.set(null);
        node.collectChanges(change::set);
        Assert.assertNull(change.get());
    }

    @Test
    public void generateChangesFromEmpty_noEmptyChange() {
        nodeList.add(new StateNode());
        node.clearChanges();
        nodeList.generateChangesFromEmpty();

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }

    @Test
    public void collectChanges_featureHasEmptyChange() {
        AtomicReference<NodeChange> change = new AtomicReference<>();
        nodeList.collectChanges(change::set);

        Assert.assertTrue(change.get() instanceof EmptyChange);

        change.set(null);
        nodeList.collectChanges(change::set);
        Assert.assertNull(change.get());
    }

    @Test
    public void collectChanges_noEmptyChange() {
        nodeList.add(new StateNode());

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }
}

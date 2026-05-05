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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.EmptyChange;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.NodeChange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeListEmptyRequiredFeatureTest {

    private StateNode node;

    private NodeList<StateNode> nodeList;

    @BeforeEach
    void setUp() {
        node = new StateNode(Arrays.asList(ElementChildrenList.class)) {
            @Override
            public boolean isAttached() {
                return true;
            }
        };
        nodeList = node.getFeature(ElementChildrenList.class);
    }

    @Test
    void generateChangesFromEmpty_featureHasChangesToCollect() {
        nodeList.generateChangesFromEmpty();

        AtomicReference<NodeChange> change = new AtomicReference<>();
        node.collectChanges(change::set);

        assertTrue(change.get() instanceof EmptyChange);

        nodeList.generateChangesFromEmpty();
        change.set(null);
        node.collectChanges(change::set);
        assertNull(change.get());
    }

    @Test
    void generateChangesFromEmpty_noEmptyChange() {
        nodeList.add(new StateNode());
        node.clearChanges();
        nodeList.generateChangesFromEmpty();

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }

    @Test
    void collectChanges_featureHasEmptyChange() {
        AtomicReference<NodeChange> change = new AtomicReference<>();
        nodeList.collectChanges(change::set);

        assertTrue(change.get() instanceof EmptyChange);

        change.set(null);
        nodeList.collectChanges(change::set);
        assertNull(change.get());
    }

    @Test
    void collectChanges_noEmptyChange() {
        nodeList.add(new StateNode());

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }
}

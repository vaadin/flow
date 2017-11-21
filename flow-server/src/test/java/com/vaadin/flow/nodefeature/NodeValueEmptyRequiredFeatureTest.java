/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.change.EmptyChange;
import com.vaadin.flow.change.MapPutChange;
import com.vaadin.flow.change.NodeChange;

public class NodeValueEmptyRequiredFeatureTest {

    private StateNode node;

    private NodeValue<String> nodeValue;

    @Before
    public void setUp() {
        node = new StateNode(Arrays.asList(ElementData.class)) {
            @Override
            public boolean isAttached() {
                return true;
            }
        };
        nodeValue = node.getFeature(ElementData.class);
    }

    @Test
    public void generateChangesFromEmpty_featureHasChangesToCollect() {
        nodeValue.generateChangesFromEmpty();

        AtomicReference<NodeChange> change = new AtomicReference<>();
        node.collectChanges(change::set);

        assertTrue(change.get() instanceof EmptyChange);

        nodeValue.generateChangesFromEmpty();
        change.set(null);
        node.collectChanges(change::set);
        assertNull(change.get());
    }

    @Test
    public void generateChangesFromEmpty_noEmptyChange() {
        nodeValue.setValue("foo");
        node.clearChanges();
        nodeValue.generateChangesFromEmpty();

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof MapPutChange);
    }

    @Test
    public void collectChanges_featureHasEmptyChange() {
        nodeValue.generateChangesFromEmpty();

        AtomicReference<NodeChange> change = new AtomicReference<>();
        nodeValue.collectChanges(change::set);

        assertTrue(change.get() instanceof EmptyChange);

        change.set(null);
        nodeValue.collectChanges(change::set);
        assertNull(change.get());
    }

    @Test
    public void collectChanges_noEmptyChange() {
        nodeValue.setValue("foo");

        List<NodeChange> changes = new ArrayList<>();
        node.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof MapPutChange);
    }
}

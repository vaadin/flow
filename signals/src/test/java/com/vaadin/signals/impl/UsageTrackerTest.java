/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalTestBase;
import com.vaadin.signals.TestUtil;
import com.vaadin.signals.ValueSignal;
import com.vaadin.signals.impl.UsageTracker.NodeUsage;
import com.vaadin.signals.impl.UsageTracker.UsageType;

public class UsageTrackerTest extends SignalTestBase {
    @Test
    void valueType_nodeWithValue_valueIsExtracted() {
        TextNode value = TextNode.valueOf("value");
        Data data = new Node.Data(null, Id.random(), null, value, null, null);

        Object extracted = UsageType.VALUE.extract(data);

        assertSame(value, extracted);
    }

    @Test
    void listType_nodeWithListChildren_childrenIsExtracted() {
        List<Id> children = List.of(Id.random());
        Data data = new Node.Data(null, Id.random(), null, null, children,
                null);

        Object extracted = UsageType.LIST.extract(data);

        assertSame(children, extracted);
    }

    @Test
    void mapType_nodeWithMapChildren_childrenIsExtracted() {
        Map<String, Id> children = Map.of("key", Id.random());
        Data data = new Node.Data(null, Id.random(), null, null, null,
                children);

        Object extracted = UsageType.MAP.extract(data);

        assertSame(children, extracted);
    }

    /*
     * No test for the computed type since it's non-trivial to create a valid
     * node. That type is tested indirectly through various other tests.
     */

    @Test
    void allType_nodeWithLastUpdate_lastUpdateIsExtracted() {
        Id lastUpdate = Id.random();
        Data data = new Node.Data(null, lastUpdate, null, null, null, null);

        Object extracted = UsageType.ALL.extract(data);

        assertSame(lastUpdate, extracted);
    }

    @Test
    void hasChanges_sameValue_noChanges() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        NodeUsage usage = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("initial"));

        assertFalse(usage.hasChanges());
    }

    @Test
    void hasChanges_differenValue_hasChanges() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        NodeUsage usage = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("other"));

        assertTrue(usage.hasChanges());
    }

    @Test
    void hasChanges_missingNode_hasChanges() {
        SynchronousSignalTree tree = new SynchronousSignalTree(false);

        NodeUsage usage = new NodeUsage(tree, Id.random(), UsageType.VALUE,
                null);

        assertTrue(usage.hasChanges());
    }

    @Test
    void hasChanges_emptyCollection_noChanges() {
        assertFalse(NodeUsage.hasChanges(List.of()));
    }

    @Test
    void hasChanges_twoWithoutChanges_noChanges() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        NodeUsage usage1 = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("initial"));
        NodeUsage usage2 = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("initial"));

        assertFalse(NodeUsage.hasChanges(List.of(usage1, usage2)));
    }

    @Test
    void hasChanges_oneWithChangesOneWithout_hasChanges() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        NodeUsage usage1 = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("initial"));
        NodeUsage usage2 = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("other"));

        assertTrue(NodeUsage.hasChanges(List.of(usage1, usage2)));
    }

    @Test
    void hasChanges_runInTransaction_readsFromTransaction() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        NodeUsage usage = new NodeUsage(TestUtil.tree(signal), signal.id(),
                UsageType.VALUE, TextNode.valueOf("initial"));

        Signal.runInTransaction(() -> {
            signal.value("changed");

            assertTrue(usage.hasChanges());

            Signal.runWithoutTransaction(() -> {
                assertFalse(usage.hasChanges());
            });
        });
    }

    @Test
    void track_noUsage_emptyResult() {
        Set<NodeUsage> result = UsageTracker.track(() -> {
        });
        assertEquals(0, result.size());
    }

    @Test
    void track_singleRegister_singleResult() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> result = UsageTracker.track(() -> {
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.VALUE);
        });

        assertEquals(1, result.size());
        NodeUsage usage = result.iterator().next();

        assertEquals(TestUtil.tree(signal), usage.tree());
        assertEquals(signal.id(), usage.nodeId());
        assertEquals(UsageType.VALUE, usage.type());
        assertEquals(TextNode.valueOf("initial"), usage.referenceValue());
    }

    @Test
    void track_multipleRegister_multipleResults() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> result = UsageTracker.track(() -> {
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.VALUE);
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.ALL);
        });

        assertEquals(2, result.size());
    }

    @Test
    void track_duplicateRegister_singleResult() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> result = UsageTracker.track(() -> {
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.VALUE);
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.VALUE);
        });

        assertEquals(1, result.size());
    }

    @Test
    void track_readValueInCallback_tracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> trackUsage = UsageTracker.track(() -> {
            signal.value();
        });

        assertEquals(1, trackUsage.size());
    }

    @Test
    void track_peekInCallback_notTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> trackUsage = UsageTracker.track(() -> {
            signal.peek();
        });

        assertEquals(0, trackUsage.size());
    }

    @Test
    void track_peekConfirmedInCallback_notTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> trackUsage = UsageTracker.track(() -> {
            signal.peekConfirmed();
        });

        assertEquals(0, trackUsage.size());
    }

    @Test
    void track_writeInCallback_notAllowedNoUsageTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> trackUsage = UsageTracker.track(() -> {
            assertThrows(IllegalStateException.class, () -> {
                signal.value("update");
            });
        });

        assertEquals(0, trackUsage.size());
    }

    @Test
    void untracked_registerDependency_notRegistered() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> result = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                        UsageType.VALUE);
                return null;
            });
        });

        assertEquals(0, result.size());
    }

    @Test
    void untracked_writeInCallback_allowedNoUsageTracked() {
        ValueSignal<String> signal = new ValueSignal<>("initial");

        Set<NodeUsage> result = UsageTracker.track(() -> {
            Signal.untracked(() -> {
                signal.value("update");
                return null;
            });
        });

        assertEquals(0, result.size());
    }

    @Test
    void registerUsage_outsideTrack_noEffect() {
        ValueSignal<String> signal = new ValueSignal<>("initial");
        Signal.untracked(() -> {
            UsageTracker.registerUsage(TestUtil.tree(signal), signal.id(),
                    UsageType.VALUE);
            return null;
        });

        /*
         * Empty trackUsage to show that the registered usage wasn't just stored
         * for later
         */
        Set<NodeUsage> result = UsageTracker.track(() -> {
        });

        assertEquals(0, result.size());
    }

    @Test
    void isActive_activeInsideTrackerInactiveOutsdide() {
        UsageTracker.track(() -> {
            assertTrue(UsageTracker.isActive());

            Signal.untracked(() -> {
                assertFalse(UsageTracker.isActive());
                return null;
            });
        });

        assertFalse(UsageTracker.isActive());
    }

}

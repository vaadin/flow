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

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnapshotTest {

    @Test
    void emptyConstructor_withoutMaxNode_hasOnlyZeroNode() {
        Id id = Id.random();

        Snapshot snapshot = new Snapshot(id, false);

        assertEquals(id, snapshot.ownerId());

        assertEquals(Set.of(Id.ZERO), snapshot.nodes().keySet());
    }

    @Test
    void emptyConstructor_withMaxNode_hasZeroAndMaxNodes() {
        Id id = Id.random();

        Snapshot snapshot = new Snapshot(id, true);

        assertEquals(id, snapshot.ownerId());

        assertEquals(Set.of(Id.ZERO, Id.MAX), snapshot.nodes().keySet());
    }

    @Test
    void copyingConstructor_baseUpdated_snapshotFrozen() {
        MutableTreeRevision mutable = new MutableTreeRevision(
                new Snapshot(Id.random(), false));

        Snapshot snapshot = new Snapshot(mutable);

        mutable.nodes().put(Id.random(), Node.EMPTY);
        mutable.originalInserts().put(Id.random(), null);

        assertEquals(Set.of(Id.ZERO), snapshot.nodes().keySet());
        assertEquals(Map.of(), snapshot.originalInserts());
    }

}

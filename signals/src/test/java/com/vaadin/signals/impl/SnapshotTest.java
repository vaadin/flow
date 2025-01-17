package com.vaadin.signals.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;

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

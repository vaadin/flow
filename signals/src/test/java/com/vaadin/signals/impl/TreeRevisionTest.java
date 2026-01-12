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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node;
import com.vaadin.signals.SignalCommand;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TreeRevisionTest {
    private static class MutableTestRevision extends TreeRevision {
        public MutableTestRevision() {
            super(Id.ZERO,
                    new HashMap<>(
                            Map.of(Id.ZERO,
                                    new Node.Data(null, Id.ZERO, null, null,
                                            List.of(), Map.of()))),
                    new HashMap<>());
        }
    }

    @Test
    void assertValidTree_emptyTree_passes() {
        MutableTestRevision revision = new MutableTestRevision();

        assertTrue(revision.assertValidTree());
    }

    @Test
    void assertValidTree_validTree_passes() {
        MutableTestRevision revision = new MutableTestRevision();

        Id listChildId = Id.random();
        Id mapChildId = Id.random();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(listChildId), Map.of("key", mapChildId)));
        revision.nodes().put(listChildId, new Node.Data(Id.ZERO, Id.ZERO,
                Id.ZERO, null, List.of(), Map.of()));
        revision.nodes().put(mapChildId, new Node.Data(Id.ZERO, Id.ZERO,
                Id.random(), null, List.of(), Map.of()));
        revision.nodes().put(Id.random(), new Node.Alias(listChildId));

        revision.originalInserts().put(listChildId,
                new SignalCommand.InsertCommand(listChildId, Id.ZERO, Id.ZERO,
                        null, ListPosition.first()));

        assertTrue(revision.assertValidTree());
    }

    @Test
    void assertValidTree_rootMissing_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().remove(Id.ZERO);

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_rootHasParent_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.ZERO, createSimpleNode(Id.random()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_detachedNode_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.random(), createSimpleNode(Id.random()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_missingListChild_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(Id.random()), Map.of()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_missingMapChild_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(), Map.of("key", Id.random())));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_aliasChild_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        Id childId = Id.random();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(childId), Map.of()));
        revision.nodes().put(childId, new Node.Alias(Id.ZERO));

        assertThrows(AssertionError.class, revision::assertValidTree);

    }

    @Test
    void assertValidTree_childWithoutParentPointer_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        Id childId = Id.random();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(childId), Map.of()));
        revision.nodes().put(childId, createSimpleNode(null));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_childWithoutMissingParent_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        Id childId = Id.random();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(childId), Map.of()));
        revision.nodes().put(childId, createSimpleNode(Id.random()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_childWithWrongParent_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        Id childId = Id.random();
        Id otherChildId = Id.random();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, null, null,
                List.of(childId, otherChildId), Map.of()));
        revision.nodes().put(childId, createSimpleNode(Id.ZERO));
        revision.nodes().put(otherChildId, createSimpleNode(childId));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_aliasTargetMissing_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.random(), new Node.Alias(Id.random()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_aliasTargetOtherAlias_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        Id aliasId = Id.random();

        revision.nodes().put(aliasId, new Node.Alias(Id.ZERO));
        revision.nodes().put(Id.random(), new Node.Alias(aliasId));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_missingOriginalInsert_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.nodes().put(Id.ZERO, new Node.Data(null, Id.ZERO, Id.ZERO,
                null, List.of(), Map.of()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    @Test
    void assertValidTree_redundantOriginalInsert_fails() {
        MutableTestRevision revision = new MutableTestRevision();

        revision.originalInserts().put(Id.ZERO, new SignalCommand.InsertCommand(
                Id.ZERO, Id.ZERO, Id.ZERO, null, ListPosition.first()));

        assertThrows(AssertionError.class, revision::assertValidTree);
    }

    private static Node.Data createSimpleNode(Id parent) {
        return new Node.Data(parent, Id.ZERO, null, null, List.of(), Map.of());
    }
}

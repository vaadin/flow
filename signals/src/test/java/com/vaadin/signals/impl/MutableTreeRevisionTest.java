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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Alias;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.impl.OperationResult.Accept;
import com.vaadin.signals.impl.OperationResult.TreeModification;

public class MutableTreeRevisionTest {
    private final MutableTreeRevision revision = new MutableTreeRevision(
            new Snapshot(Id.random(), false));
    private final Id commandId = Id.random();

    @Test
    void constuctor_modifyBase_copyNotUpdated() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        revision.nodes().put(Id.random(), Node.EMPTY);
        revision.originalInserts().put(Id.random(), null);

        assertEquals(Map.of(Id.ZERO, Node.EMPTY), copy.nodes());
        assertEquals(Map.of(), copy.originalInserts());
    }

    @Test
    void constuctor_modifyCopy_baseNotUpdated() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        copy.nodes().put(Id.random(), Node.EMPTY);
        copy.originalInserts().put(Id.random(), null);

        assertEquals(Map.of(Id.ZERO, Node.EMPTY), revision.nodes());
        assertEquals(Map.of(), revision.originalInserts());
    }

    @Test
    void setCommand_existingTarget_valueIsSet() {
        OperationResult result = applySingle(new SignalCommand.SetCommand(
                commandId, Id.ZERO, new TextNode("value")));

        // Check result object
        Accept accept = assertAccepted(result);
        TreeModification modification = accept.onlyUpdate();
        assertEquals(Node.EMPTY, modification.oldNode());
        Data newDataNode = (Data) modification.newNode();
        assertEquals(new TextNode("value"), newDataNode.value());

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void setCommand_missingTarget_rejected() {
        OperationResult result = applySingle(new SignalCommand.SetCommand(
                commandId, Id.random(), new TextNode("value")));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertNullValue(Id.ZERO);
    }

    @Test
    void setCommand_aliasTarget_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(new SignalCommand.SetCommand(
                commandId, alias, new TextNode("value")));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(Set.of(Id.ZERO), accept.updates().keySet());
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertValue(Id.ZERO, "value");
        assertValue(alias, "value");
    }

    @Test
    void incrementCommand_nullValue_incrementsFromZero() {
        OperationResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        Data newData = assertSingleDataChange(result);
        assertEquals(3, newData.value().asInt());

        // Check revision state
        assertValue(Id.ZERO, 3);
    }

    @Test
    void incrementCommand_numericValue_incremented() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(2)));

        OperationResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        Data newData = assertSingleDataChange(result);
        assertEquals(5, newData.value().asInt());

        // Check revision state
        assertValue(Id.ZERO, 5);
    }

    @Test
    void incrementCommand_textValue_reject() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new TextNode("value")));

        OperationResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void incrementCommand_alias_dataUpdated() {
        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, alias, 3));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(Set.of(Id.ZERO), accept.updates().keySet());
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertValue(Id.ZERO, 3);
    }

    @Test
    void insertCommand_emptyNode_onlyChild() {
        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, new TextNode("value"),
                ListSignal.ListPosition.first()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        TreeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertEquals(List.of(commandId),
                ((Data) parentUpdate.newNode()).listChildren());
        TreeModification childUpdate = accept.updates().get(commandId);
        assertNull(childUpdate.oldNode());
        assertEquals("value",
                ((Data) childUpdate.newNode()).value().textValue());

        // Check revision state
        assertListChildren(Id.ZERO, commandId);
        assertValue(commandId, "value");
    }

    @Test
    void insertCommandFirst_otherEntry_insertedFirst() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.first()));

        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        ListSignal.ListPosition.first()));

        // Check result object
        Accept accept = assertAccepted(result);
        TreeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertEquals(List.of(other),
                ((Data) parentUpdate.oldNode()).listChildren());
        assertEquals(List.of(inserted, other),
                ((Data) parentUpdate.newNode()).listChildren());

        // Check revision state
        assertListChildren(Id.ZERO, inserted, other);
    }

    @Test
    void insertCommandLast_otherEntry_insertedLast() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.first()));

        Id inserted = Id.random();
        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                inserted, Id.ZERO, null, null, ListSignal.ListPosition.last()));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, other, inserted);
    }

    @Test
    void insertCommand_afterOther_insertedAfter() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.first()));

        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(other, null)));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, other, inserted);
    }

    @Test
    void insertCommand_beforeOther_insertedBefore() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.first()));

        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(null, other)));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, inserted, other);
    }

    @Test
    void insertCommand_beforeMissing_reject() {
        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(null, Id.random())));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void insertCommand_afterMissing_reject() {
        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(Id.random(), null)));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void insertCommand_betweenAdjacent_insertedBetween() {
        Id other1 = Id.random();
        applySingle(new SignalCommand.InsertCommand(other1, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id other2 = Id.random();
        applySingle(new SignalCommand.InsertCommand(other2, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(other1, other2)));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, other1, inserted, other2);
    }

    @Test
    void insertCommand_betweenNonAdjacent_reject() {
        Id other1 = Id.random();
        applySingle(new SignalCommand.InsertCommand(other1, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id other2 = Id.random();
        applySingle(new SignalCommand.InsertCommand(other2, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id inserted = Id.random();
        OperationResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        new ListSignal.ListPosition(other2, other1)));
        // they are technically adjacent, but not in the expected order

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, other1, other2);
    }

    @Test
    void insertCommand_emptyPosition_reject() {
        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, null, new ListPosition(null, null)));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void insertCommand_parentAlias_dataUpdated() {
        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, alias, null, null, ListPosition.first()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO, commandId);
    }

    @Test
    void insertCommand_afterAlias_afterAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);
        Id alias = createAlias(child);

        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, null, new ListPosition(alias, null)));

        // Check result object
        assertAccepted(result);

        // Check revision state
        assertListChildren(Id.ZERO, child, commandId);
    }

    @Test
    void insertCommand_beforeAlias_beforeAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);
        Id alias = createAlias(child);

        OperationResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, null, new ListPosition(null, alias)));

        // Check result object
        assertAccepted(result);

        // Check revision state
        assertListChildren(Id.ZERO, commandId, child);
    }

    @Test
    void putCommand_emptyTarget_nodeInserted() {
        OperationResult result = applySingle(new SignalCommand.PutCommand(
                commandId, Id.ZERO, "key", new TextNode("value")));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        TreeModification rootModification = accept.updates().get(Id.ZERO);
        assertEquals(Map.of("key", commandId),
                ((Data) rootModification.newNode()).mapChildren());
        TreeModification childModification = accept.updates().get(commandId);
        assertNull(childModification.oldNode());
        assertEquals("value",
                ((Data) childModification.newNode()).value().textValue());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
        assertValue(commandId, "value");
    }

    @Test
    void putCommand_replaceExisting_nodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key",
                new TextNode("1")));

        OperationResult result = applySingle(new SignalCommand.PutCommand(
                commandId, Id.ZERO, "key", new TextNode("2")));

        // Check result object
        Accept accept = assertAccepted(result);
        TreeModification childUpdate = accept.onlyUpdate();
        assertEquals("1", ((Data) childUpdate.oldNode()).value().textValue());
        assertEquals("2", ((Data) childUpdate.newNode()).value().textValue());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", child));
        assertValue(child, "2");
    }

    @Test
    void putCommand_aliasTarget_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(
                new SignalCommand.PutCommand(commandId, alias, "key", null));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
    }

    @Test
    void putIfAbsentCommand_absent_nodeCreated() {
        OperationResult result = applySingle(
                new SignalCommand.PutIfAbsentCommand(commandId, Id.ZERO, null,
                        "key", new TextNode("value")));
        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
        assertInstanceOf(Node.Data.class, revision.nodes().get(commandId));
        assertValue(commandId, "value");
    }

    @Test
    void putIfAbsentCommand_present_aliasCreated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key",
                new TextNode("1")));

        OperationResult result = applySingle(
                new SignalCommand.PutIfAbsentCommand(commandId, Id.ZERO, null,
                        "key", new TextNode("2")));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals("Only alias is updated", 1, accept.updates().size());
        TreeModification modification = accept.updates().get(commandId);
        assertNull(modification.oldNode());
        assertInstanceOf(Node.Alias.class, modification.newNode());
        assertEquals(child, ((Alias) modification.newNode()).target());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", child));
        assertValue(child, "1");
        assertValue(commandId, "1");
        assertInstanceOf(Node.Data.class, revision.nodes().get(child));
        assertInstanceOf(Node.Alias.class, revision.nodes().get(commandId));
    }

    @Test
    void putIfAbsentCommand_aliasTarget_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(
                new SignalCommand.PutIfAbsentCommand(commandId, alias, null,
                        "key", null));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
    }

    @Test
    void adoptAtCommand_childMissing_reject() {
        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, Id.ZERO, Id.random(), ListPosition.last()));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void adoptAtCommand_childAdoptsItsParent_reject() {
        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, child, Id.ZERO, ListPosition.last()));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, child);
    }

    @Test
    void adoptAtCommand_childAlreadyInParent_orderChanged() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, Id.ZERO, child, ListPosition.first()));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, child, other);
    }

    @Test
    void adoptAtCommand_childInAnotherParent_adopted() {
        Id target = Id.random();
        applySingle(new SignalCommand.InsertCommand(target, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListSignal.ListPosition.last()));

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, target, child, ListPosition.first()));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, target);
        assertListChildren(target, child);
    }

    @Test
    void adoptAtCommand_mapChild_removedFromMap() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, Id.ZERO, child, ListPosition.first()));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, child);
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void adoptAtCommand_aliasParent_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, alias, children.get(0), ListPosition.last()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO, children.get(1), children.get(0));
    }

    @Test
    void adoptAtCommand_moveAlias_dataNodeMoved() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        Id alias = createAlias(children.get(0));

        OperationResult result = applySingle(new SignalCommand.AdoptAtCommand(
                commandId, Id.ZERO, alias, ListPosition.last()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(children.get(0)));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO, children.get(1), children.get(0));
    }

    @Test
    void adoptAsCommand_childMissing_reject() {
        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, Id.random(), "key"));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void adoptAsCommand_childAdoptsItsParent_reject() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, child, Id.ZERO, "key"));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", child));
    }

    @Test
    void adoptAsCommand_childAlreadyInParent_keyChanged() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, child, "key2"));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key2", child));
    }

    @Test
    void adoptAsCommand_childInAnotherParent_adopted() {
        Id target = Id.random();
        applySingle(new SignalCommand.PutCommand(target, Id.ZERO, "key", null));

        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key2", null));

        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, target, child, "key"));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", target));
        assertMapChildren(target, Map.of("key", child));
    }

    @Test
    void adoptAsCommand_listChild_removedFromList() {
        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListPosition.last()));

        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, child, "key"));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO);
        assertMapChildren(Id.ZERO, Map.of("key", child));
    }

    @Test
    void adoptAsCommand_existingKey_reject() {
        Id other = Id.random();
        applySingle(new SignalCommand.PutCommand(other, Id.ZERO, "key", null));

        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key2", null));

        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, child, "key"));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", other, "key2", child));
    }

    @Test
    void adoptAsCommand_aliasParent_dataNodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, alias, child, "key2"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key2", child));
    }

    @Test
    void adoptAsCommand_moveAlias_dataNodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(child);
        OperationResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, alias, "key2"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(child));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key2", child));
    }

    @Test
    void removeCommand_rootNode_reject() {
        OperationResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, Id.ZERO, null));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void removeCommand_childWithGrandchild_recursivelyRemoved() {
        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListPosition.last()));

        Id grandChild = Id.random();
        applySingle(new SignalCommand.InsertCommand(grandChild, child, null,
                null, ListPosition.last()));

        OperationResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, child, null));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(3, accept.updates().size());
        Data newRootData = (Data) accept.updates().get(Id.ZERO).newNode();
        assertEquals(List.of(), newRootData.listChildren());
        TreeModification childUpdate = accept.updates().get(child);
        assertNull(childUpdate.newNode());
        TreeModification grandchildUpdate = accept.updates().get(grandChild);
        assertNull(grandchildUpdate.newNode());

        // Check revision state
        assertListChildren(Id.ZERO);
        assertEquals(1, revision.nodes().size());
    }

    @Test
    void removeCommand_expectedParentNotParent_reject() {
        Id expectedParent = Id.random();
        applySingle(new SignalCommand.InsertCommand(expectedParent, Id.ZERO,
                null, null, ListPosition.last()));

        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListPosition.last()));

        OperationResult result = applySingle(new SignalCommand.RemoveCommand(
                commandId, child, expectedParent));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO, expectedParent, child);
    }

    @Test
    void removeCommand_expectedParentIsParent_childRemoved() {
        Id child = Id.random();
        applySingle(new SignalCommand.InsertCommand(child, Id.ZERO, null, null,
                ListPosition.last()));

        OperationResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, child, Id.ZERO));

        // Check result object
        assertTrue(result.accepted());

        // Check revision state
        assertListChildren(Id.ZERO);
    }

    @Test
    void removeCommand_parentAlias_dataNodeUpdated() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, child, alias));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO);
    }

    @Test
    void removeCommand_childAlias_dataNodeUpdated() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(child);
        OperationResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, alias, Id.ZERO));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(child));
        assertTrue("Alias was also removed",
                accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO);
        assertFalse(revision.nodes().containsKey(alias));
    }

    @Test
    void removeByKeyCommand_missingKey_reject() {
        OperationResult result = applySingle(
                new SignalCommand.RemoveByKeyCommand(commandId, Id.ZERO,
                        "key"));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void removeByKeyCommand_existingKey_removed() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(
                new SignalCommand.RemoveByKeyCommand(commandId, Id.ZERO,
                        "key"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        TreeModification childUpdate = accept.updates().get(child);
        assertNull(childUpdate.newNode());
        TreeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertEquals(Map.of(), ((Data) parentUpdate.newNode()).mapChildren());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void removeByKeyCommand_parentAlias_dataNodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(
                new SignalCommand.RemoveByKeyCommand(commandId, alias, "key"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void clearCommand_nodeWithChildren_childrenRemoved() {
        Id listChild = Id.random();
        applySingle(new SignalCommand.InsertCommand(listChild, Id.ZERO, null,
                null, ListPosition.last()));

        Id mapChild = Id.random();
        applySingle(
                new SignalCommand.PutCommand(mapChild, Id.ZERO, "key", null));

        OperationResult result = applySingle(
                new SignalCommand.ClearCommand(commandId, Id.ZERO));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(3, accept.updates().size());
        TreeModification listChildUpdate = accept.updates().get(listChild);
        assertNull(listChildUpdate.newNode());
        TreeModification mapChildUpdate = accept.updates().get(mapChild);
        assertNull(mapChildUpdate.newNode());
        TreeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertEquals(Map.of(), ((Data) parentUpdate.newNode()).mapChildren());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void clearCommand_emptyNode_noChange() {
        OperationResult result = applySingle(
                new SignalCommand.ClearCommand(commandId, Id.ZERO));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(Map.of(), accept.updates());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void clearCommand_alias_dataNodeUpdated() {
        insertChildren(Id.ZERO, 1);

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(
                new SignalCommand.ClearCommand(alias, Id.ZERO));

        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO);
    }

    @Test
    void positionTestNoPosition_listChild_accepted() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, child, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionTestNoPosition_missingChild_rejected() {
        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, Id.random(), new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionTestNoPosition_mapChild_rejected() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, child, new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionTestNoPosition_otherParent_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(
                new SignalCommand.PositionTest(commandId, children.get(0),
                        children.get(1), new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionTestFirst_isFirst_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0), ListPosition.first()));

        assertTestResult(true, result);
    }

    @Test
    void positionTestFirst_isNotFirst_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1), ListPosition.first()));

        assertTestResult(false, result);
    }

    @Test
    void positionTestLast_isNotLast_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0), ListPosition.last()));

        assertTestResult(false, result);
    }

    @Test
    void positionTestLast_isLast_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1), ListPosition.last()));

        assertTestResult(true, result);
    }

    @Test
    void positionTestAfter_isAfter_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(0), null)));

        assertTestResult(true, result);
    }

    @Test
    void positionTestAfter_itself_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(children.get(0), null)));

        assertTestResult(false, result);
    }

    @Test
    void positionTestAfter_isNotAfter_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(children.get(1), null)));

        assertTestResult(false, result);
    }

    @Test
    void positionTestBefore_isBefore_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(null, children.get(1))));

        assertTestResult(true, result);
    }

    @Test
    void positionTestBefore_itself_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(null, children.get(0))));

        assertTestResult(false, result);
    }

    @Test
    void positionTestBefore_isNotBefore_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(null, children.get(0))));

        assertTestResult(false, result);
    }

    @Test
    void positionTestBetween_isBetween_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 3);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(0), children.get(2))));

        assertTestResult(true, result);
    }

    @Test
    void positionTestBetween_notBetween_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 3);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(2), children.get(1))));

        assertTestResult(false, result);
    }

    @Test
    void positionTest_parentAlias_checksAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, alias, child, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionTest_childAlias_checkAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(child);

        OperationResult result = applySingle(new SignalCommand.PositionTest(
                commandId, Id.ZERO, alias, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionTest_aliasSiblings_checkAliasTargets() {
        List<Id> children = insertChildren(Id.ZERO, 3);
        Id first = createAlias(children.get(0));
        Id last = createAlias(children.get(2));

        OperationResult result = applySingle(
                new SignalCommand.PositionTest(commandId, Id.ZERO,
                        children.get(1), new ListPosition(first, last)));

        assertTestResult(true, result);
    }

    @Test
    void valueTest_sameValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new TextNode("value")));

        OperationResult result = applySingle(new SignalCommand.ValueTest(
                commandId, Id.ZERO, new TextNode("value")));

        assertTestResult(true, result);
    }

    @Test
    void valueTest_alias_dataNodeChecked() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new TextNode("value")));

        Id alias = createAlias(Id.ZERO);

        OperationResult result = applySingle(new SignalCommand.ValueTest(
                commandId, alias, new TextNode("value")));

        assertTestResult(true, result);
    }

    @Test
    void valueTest_otherValue_rejected() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new TextNode("other")));

        OperationResult result = applySingle(new SignalCommand.ValueTest(
                commandId, Id.ZERO, new TextNode("value")));

        assertTestResult(false, result);
    }

    @Test
    void valueTestNull_jsonNullValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                NullNode.getInstance()));

        OperationResult result = applySingle(
                new SignalCommand.ValueTest(commandId, Id.ZERO, null));

        assertTestResult(true, result);
    }

    @Test
    void valueTestJsonNull_nullValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO, null));

        OperationResult result = applySingle(new SignalCommand.ValueTest(
                commandId, Id.ZERO, NullNode.getInstance()));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateTest_sameValue_accepted() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        OperationResult result = applySingle(
                new SignalCommand.LastUpdateTest(commandId, Id.ZERO, update));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateTest_alias_targetNodeChecked() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(
                new SignalCommand.LastUpdateTest(commandId, alias, update));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateTest_differentValue_rejected() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        OperationResult result = applySingle(new SignalCommand.LastUpdateTest(
                commandId, Id.ZERO, Id.random()));

        assertTestResult(false, result);
    }

    @Test
    void keyTestNoKey_noKey_accepted() {
        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", Id.ZERO));

        assertTestResult(true, result);
    }

    @Test
    void keyTestNoKey_keyPresent_rejected() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", Id.ZERO));

        assertTestResult(false, result);
    }

    @Test
    void keyTestKeyPresent_keyPresent_accepted() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", null));

        assertTestResult(true, result);
    }

    @Test
    void keyTestKeyPresent_noKey_rejected() {
        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", null));

        assertTestResult(false, result);
    }

    @Test
    void keyTestSpecificNode_nodePresent_accepted() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", child));

        assertTestResult(true, result);
    }

    @Test
    void keyTestSpecificNode_noNode_rejected() {
        Id child = Id.random();

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyTestSpecificNode_otherKey_rejected() {
        Id child = Id.random();
        applySingle(
                new SignalCommand.PutCommand(child, Id.ZERO, "other", null));

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyTestSpecificNode_otherNode_rejected() {
        Id child = Id.random();
        applySingle(
                new SignalCommand.PutCommand(child, Id.ZERO, "other", null));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyTest_aliasParent_targetChecked() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(Id.ZERO);
        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, alias, "key", child));

        assertTestResult(true, result);
    }

    @Test
    void keyTest_aliasChild_targetChecked() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(child);
        OperationResult result = applySingle(
                new SignalCommand.KeyTest(commandId, Id.ZERO, "key", alias));

        assertTestResult(true, result);
    }

    @Test
    void transactionCommand_empty_noChange() {
        OperationResult result = applySingle(
                new SignalCommand.TransactionCommand(commandId, List.of()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(Map.of(), accept.updates());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void transactionCommand_allAccepted_changesApplied() {
        Id command1 = Id.random();
        Id command2 = Id.random();

        Map<Id, OperationResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.SetCommand(command1, Id.ZERO,
                                new TextNode("value")),
                                new SignalCommand.PutCommand(command2, Id.ZERO,
                                        "key", null)))));

        // Check result objects
        Accept transaction = assertAccepted(results.get(commandId));
        assertEquals(2, transaction.updates().size());
        // Verify that modifications from both commands are merged
        TreeModification rootModification = transaction.updates().get(Id.ZERO);
        assertEquals(Node.EMPTY, rootModification.oldNode());
        assertEquals(revision.nodes().get(Id.ZERO), rootModification.newNode());

        Accept set = assertAccepted(results.get(command1));
        Data setModificationNode = (Data) set.onlyUpdate().newNode();
        assertEquals("value", setModificationNode.value().textValue());
        assertEquals(Map.of(), setModificationNode.mapChildren());

        // Check revision state
        assertValue(Id.ZERO, "value");
        assertMapChildren(Id.ZERO, Map.of("key", command2));
    }

    @Test
    void transactionCommand_lastRejected_allRejected() {
        Id command1 = Id.random();
        Id command2 = Id.random();

        Map<Id, OperationResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.SetCommand(command1, Id.ZERO,
                                new TextNode("value")),
                                new SignalCommand.ValueTest(command2, Id.ZERO,
                                        null)))));

        // Check result objects
        assertEquals(Set.of(command1, command2, commandId), results.keySet());
        for (OperationResult subResult : results.values()) {
            assertFalse(subResult.accepted());
        }

        // Check revision state
        assertUnchanged();
    }

    @Test
    void transactionCommand_nestedTransactions_allApplied() {
        Id set = Id.random();
        Id innerTransaction = Id.random();

        Map<Id, OperationResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.TransactionCommand(
                                innerTransaction,
                                List.of(new SignalCommand.SetCommand(set,
                                        Id.ZERO, new TextNode("value"))))))));

        // Check result objects
        assertEquals(3, results.size());
        assertAccepted(results.get(commandId));
        assertAccepted(results.get(innerTransaction));
        assertAccepted(results.get(set));

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void snapshotEvent_withNodes_loaded() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        Id child = Id.random();
        copy.apply(new SignalCommand.PutCommand(child, Id.ZERO, "key", null),
                null);

        OperationResult result = applySingle(
                new SignalCommand.SnapshotCommand(commandId, copy.nodes()));

        // Check result objects
        Accept accept = assertAccepted(result);
        // This is a special case
        assertEquals(2, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", child));
    }

    @Test
    void clearOwnerEvent_ownedListNode_removed() {
        Id node = Id.random();
        applySingle(new SignalCommand.InsertCommand(node, Id.ZERO,
                revision.ownerId(), null, ListPosition.last()));

        assertEquals(node, revision.originalInserts().get(node).commandId());

        Id childOfOwned = Id.random();
        applySingle(
                new SignalCommand.PutCommand(childOfOwned, node, "key", null));

        OperationResult result = applySingle(
                new SignalCommand.ClearOwnerCommand(commandId,
                        revision.ownerId()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(3, accept.updates().size());

        // Check revision state
        assertListChildren(Id.ZERO);
        assertEquals(Set.of(Id.ZERO), revision.nodes().keySet());
        assertEquals(Map.of(), revision.originalInserts());
    }

    @Test
    void clearOwnerEvent_ownedMapNode_removed() {
        Id node = Id.random();
        applySingle(new SignalCommand.PutIfAbsentCommand(node, Id.ZERO,
                revision.ownerId(), "key", null));

        assertEquals(node, revision.originalInserts().get(node).commandId());

        OperationResult result = applySingle(
                new SignalCommand.ClearOwnerCommand(commandId,
                        revision.ownerId()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
        assertEquals(Map.of(), revision.originalInserts());
    }

    @Test
    void clearOwnerEvent_ownedByOther_retained() {
        Id node = Id.random();
        applySingle(new SignalCommand.PutIfAbsentCommand(node, Id.ZERO,
                Id.random(), "key", null));

        assertEquals(Map.of(), revision.originalInserts());

        OperationResult result = applySingle(
                new SignalCommand.ClearOwnerCommand(commandId,
                        revision.ownerId()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(0, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", node));
    }

    @Test
    void clearOwnerEvent_ownedByNone_retained() {
        Id node = Id.random();
        applySingle(new SignalCommand.PutIfAbsentCommand(node, Id.ZERO, null,
                "key", null));

        assertEquals(Map.of(), revision.originalInserts());

        OperationResult result = applySingle(
                new SignalCommand.ClearOwnerCommand(commandId,
                        revision.ownerId()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(0, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", node));
    }

    private OperationResult applySingle(SignalCommand command) {
        Map<Id, OperationResult> results = revision
                .applyAndGetResults(List.of(command));
        assertEquals(1, results.size());

        return results.get(command.commandId());
    }

    private void assertNullValue(Id nodeId) {
        assertNull(revision.data(nodeId).get().value());
    }

    private void assertValue(Id nodeId, String expectedValue) {
        assertEquals(new TextNode(expectedValue),
                revision.data(nodeId).get().value());
    }

    private void assertValue(Id nodeId, double expectedValue) {
        assertEquals(new DoubleNode(expectedValue),
                revision.data(nodeId).get().value());
    }

    private void assertListChildren(Id nodeId, Id... expectedChildren) {
        assertEquals(List.of(expectedChildren),
                revision.data(nodeId).get().listChildren());

        for (Id child : expectedChildren) {
            assertEquals(nodeId, revision.data(child).get().parent());
        }
    }

    private void assertMapChildren(Id nodeId,
            Map<String, Id> expectedChildren) {
        assertEquals(expectedChildren,
                revision.data(nodeId).get().mapChildren());

        for (Id child : expectedChildren.values()) {
            assertEquals(nodeId, revision.data(child).get().parent());
        }
    }

    private void assertUnchanged() {
        assertEquals(Map.of(Id.ZERO, Node.EMPTY), revision.nodes());
    }

    private static void assertTestResult(boolean expectedResult,
            OperationResult result) {
        assertEquals(expectedResult, result.accepted());
        if (result.accepted()) {
            assertEquals(0, ((Accept) result).updates().size());
        }
    }

    private static Accept assertAccepted(OperationResult result) {
        assertTrue(result.accepted());
        return (Accept) result;
    }

    private static Data assertSingleDataChange(OperationResult result) {
        Accept accept = assertAccepted(result);
        TreeModification onlyUpdate = accept.onlyUpdate();
        return (Data) onlyUpdate.newNode();
    }

    private List<Id> insertChildren(Id parent, int count) {
        return IntStream.range(0, count).mapToObj(ignore -> Id.random())
                .peek(id -> applySingle(new SignalCommand.InsertCommand(id,
                        parent, null, null, ListPosition.last())))
                .toList();
    }

    private Id createAlias(Id target) {
        Id alias = Id.random();
        revision.nodes().put(alias, new Node.Alias(target));
        return alias;
    }
}

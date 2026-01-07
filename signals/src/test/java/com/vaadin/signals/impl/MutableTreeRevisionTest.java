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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

import com.vaadin.signals.Id;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.ListSignal.ListPosition;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Alias;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.impl.CommandResult.Accept;
import com.vaadin.signals.impl.CommandResult.NodeModification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MutableTreeRevisionTest {
    private final MutableTreeRevision revision = new MutableTreeRevision(
            new Snapshot(Id.random(), false));
    private final Id commandId = Id.random();

    @Test
    void constructor_modifyBase_copyNotUpdated() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        revision.nodes().put(Id.random(), Node.EMPTY);
        revision.originalInserts().put(Id.random(), null);

        assertEquals(Map.of(Id.ZERO, Node.EMPTY), copy.nodes());
        assertEquals(Map.of(), copy.originalInserts());
    }

    @Test
    void constructor_modifyCopy_baseNotUpdated() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        copy.nodes().put(Id.random(), Node.EMPTY);
        copy.originalInserts().put(Id.random(), null);

        assertEquals(Map.of(Id.ZERO, Node.EMPTY), revision.nodes());
        assertEquals(Map.of(), revision.originalInserts());
    }

    @Test
    void setCommand_existingTarget_valueIsSet() {
        CommandResult result = applySingle(new SignalCommand.SetCommand(
                commandId, Id.ZERO, new StringNode("value")));

        // Check result object
        Accept accept = assertAccepted(result);
        NodeModification modification = accept.onlyUpdate();
        assertEquals(Node.EMPTY, modification.oldNode());
        Data newDataNode = (Data) modification.newNode();
        assertNotNull(newDataNode);
        assertEquals(new StringNode("value"), newDataNode.value());

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void setCommand_missingTarget_rejected() {
        CommandResult result = applySingle(new SignalCommand.SetCommand(
                commandId, Id.random(), new StringNode("value")));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertNullValue(Id.ZERO);
    }

    @Test
    void setCommand_aliasTarget_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(new SignalCommand.SetCommand(
                commandId, alias, new StringNode("value")));

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
        CommandResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        Data newData = assertSingleDataChange(result);
        var newValue = newData.value();
        assertNotNull(newValue);
        assertEquals(3, newValue.asInt());

        // Check revision state
        assertValue(Id.ZERO, 3);
    }

    @Test
    void incrementCommand_numericValue_incremented() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new DoubleNode(2)));

        CommandResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        Data newData = assertSingleDataChange(result);
        var newValue = newData.value();
        assertNotNull(newValue);
        assertEquals(5, newValue.asInt());

        // Check revision state
        assertValue(Id.ZERO, 5);
    }

    @Test
    void incrementCommand_textValue_reject() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new StringNode("value")));

        CommandResult result = applySingle(
                new SignalCommand.IncrementCommand(commandId, Id.ZERO, 3));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void incrementCommand_alias_dataUpdated() {
        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(
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
        CommandResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, new StringNode("value"),
                ListSignal.ListPosition.first()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        NodeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertNotNull(parentUpdate);
        Data parentNewNode = (Data) parentUpdate.newNode();
        assertNotNull(parentNewNode);
        assertEquals(List.of(commandId), parentNewNode.listChildren());
        NodeModification childUpdate = accept.updates().get(commandId);
        assertNotNull(childUpdate);
        assertNull(childUpdate.oldNode());
        Data childNewNode = (Data) childUpdate.newNode();
        assertNotNull(childNewNode);
        var childValue = childNewNode.value();
        assertNotNull(childValue);
        assertEquals("value", childValue.textValue());

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
        CommandResult result = applySingle(
                new SignalCommand.InsertCommand(inserted, Id.ZERO, null, null,
                        ListSignal.ListPosition.first()));

        // Check result object
        Accept accept = assertAccepted(result);
        NodeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertNotNull(parentUpdate);
        Data parentOldNode = (Data) parentUpdate.oldNode();
        assertNotNull(parentOldNode);
        assertEquals(List.of(other), parentOldNode.listChildren());
        Data parentNewNode = (Data) parentUpdate.newNode();
        assertNotNull(parentNewNode);
        assertEquals(List.of(inserted, other), parentNewNode.listChildren());

        // Check revision state
        assertListChildren(Id.ZERO, inserted, other);
    }

    @Test
    void insertCommandLast_otherEntry_insertedLast() {
        Id other = Id.random();
        applySingle(new SignalCommand.InsertCommand(other, Id.ZERO, null, null,
                ListSignal.ListPosition.first()));

        Id inserted = Id.random();
        CommandResult result = applySingle(new SignalCommand.InsertCommand(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, null, new ListPosition(null, null)));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void insertCommand_parentAlias_dataUpdated() {
        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(new SignalCommand.InsertCommand(
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

        CommandResult result = applySingle(new SignalCommand.InsertCommand(
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

        CommandResult result = applySingle(new SignalCommand.InsertCommand(
                commandId, Id.ZERO, null, null, new ListPosition(null, alias)));

        // Check result object
        assertAccepted(result);

        // Check revision state
        assertListChildren(Id.ZERO, commandId, child);
    }

    @Test
    void putCommand_emptyTarget_nodeInserted() {
        CommandResult result = applySingle(new SignalCommand.PutCommand(
                commandId, Id.ZERO, "key", new StringNode("value")));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        NodeModification rootModification = accept.updates().get(Id.ZERO);
        assertNotNull(rootModification);
        Data rootNewNode = (Data) rootModification.newNode();
        assertNotNull(rootNewNode);
        assertEquals(Map.of("key", commandId), rootNewNode.mapChildren());
        NodeModification childModification = accept.updates().get(commandId);
        assertNotNull(childModification);
        assertNull(childModification.oldNode());
        Data childNewNode = (Data) childModification.newNode();
        assertNotNull(childNewNode);
        var childValue = childNewNode.value();
        assertNotNull(childValue);
        assertEquals("value", childValue.textValue());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
        assertValue(commandId, "value");
    }

    @Test
    void putCommand_multiplePuts_orderPreserved() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "firstKey", new StringNode("first")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "secondKey", new StringNode("second")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "thirdKey", new StringNode("third")));

        assertMapKeys(Id.ZERO, "firstKey", "secondKey", "thirdKey");
    }

    @Test
    void putCommand_replaceExisting_nodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key",
                new StringNode("1")));

        CommandResult result = applySingle(new SignalCommand.PutCommand(
                commandId, Id.ZERO, "key", new StringNode("2")));

        // Check result object
        Accept accept = assertAccepted(result);
        NodeModification childUpdate = accept.onlyUpdate();
        Data childOldNode = (Data) childUpdate.oldNode();
        assertNotNull(childOldNode);
        var childOldValue = childOldNode.value();
        assertNotNull(childOldValue);
        assertEquals("1", childOldValue.textValue());
        Data childNewNode = (Data) childUpdate.newNode();
        assertNotNull(childNewNode);
        var childNewValue = childNewNode.value();
        assertNotNull(childNewValue);
        assertEquals("2", childNewValue.textValue());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", child));
        assertValue(child, "2");
    }

    @Test
    void putCommand_aliasTarget_dataNodeUpdated() {
        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(
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
        CommandResult result = applySingle(new SignalCommand.PutIfAbsentCommand(
                commandId, Id.ZERO, null, "key", new StringNode("value")));
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
                new StringNode("1")));

        CommandResult result = applySingle(new SignalCommand.PutIfAbsentCommand(
                commandId, Id.ZERO, null, "key", new StringNode("2")));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(1, accept.updates().size(), "Only alias is updated");
        NodeModification modification = accept.updates().get(commandId);
        assertNotNull(modification);
        assertNull(modification.oldNode());
        var modificationNewNode = modification.newNode();
        assertInstanceOf(Node.Alias.class, modificationNewNode);
        assertNotNull(modificationNewNode);
        assertEquals(child, ((Alias) modificationNewNode).target());

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

        CommandResult result = applySingle(new SignalCommand.PutIfAbsentCommand(
                commandId, alias, null, "key", null));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", commandId));
    }

    @Test
    void putIfAbsentCommand_multiplePuts_orderPreserved() {
        applySingle(new SignalCommand.PutIfAbsentCommand(Id.random(), Id.ZERO,
                null, "firstKey", new StringNode("first")));
        applySingle(new SignalCommand.PutIfAbsentCommand(Id.random(), Id.ZERO,
                null, "secondKey", new StringNode("second")));
        applySingle(new SignalCommand.PutIfAbsentCommand(Id.random(), Id.ZERO,
                null, "thirdKey", new StringNode("third")));

        assertMapKeys(Id.ZERO, "firstKey", "secondKey", "thirdKey");
    }

    @Test
    void adoptAtCommand_childMissing_reject() {
        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAtCommand(
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
        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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

        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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
        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
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
        CommandResult result = applySingle(new SignalCommand.AdoptAsCommand(
                commandId, Id.ZERO, alias, "key2"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(child));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key2", child));
    }

    @Test
    void adoptAsCommand_existingKeys_orderPreserved() {
        Id firstId = Id.random();
        applySingle(new SignalCommand.PutCommand(firstId, Id.ZERO, "firstKey",
                new StringNode("first")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "secondKey", new StringNode("second")));
        applySingle(new SignalCommand.PutCommand(commandId, firstId, "key",
                new StringNode("value")));

        applySingle(new SignalCommand.AdoptAsCommand(Id.random(), Id.ZERO,
                commandId, "thirdKey"));

        assertMapKeys(Id.ZERO, "firstKey", "secondKey", "thirdKey");
    }

    @Test
    void removeCommand_rootNode_reject() {
        CommandResult result = applySingle(
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

        CommandResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, child, null));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(3, accept.updates().size());
        NodeModification rootUpdate = accept.updates().get(Id.ZERO);
        assertNotNull(rootUpdate);
        Data newRootData = (Data) rootUpdate.newNode();
        assertNotNull(newRootData);
        assertEquals(List.of(), newRootData.listChildren());
        NodeModification childUpdate = accept.updates().get(child);
        assertNotNull(childUpdate);
        assertNull(childUpdate.newNode());
        NodeModification grandchildUpdate = accept.updates().get(grandChild);
        assertNotNull(grandchildUpdate);
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

        CommandResult result = applySingle(new SignalCommand.RemoveCommand(
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

        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
                new SignalCommand.RemoveCommand(commandId, alias, Id.ZERO));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(child));
        assertTrue(accept.updates().containsKey(alias),
                "Alias was also removed");

        // Check revision state
        assertListChildren(Id.ZERO);
        assertFalse(revision.nodes().containsKey(alias));
    }

    @Test
    void removeCommand_multipleMapChildren_orderPreserved() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "firstKey", new StringNode("first")));
        applySingle(new SignalCommand.PutCommand(commandId, Id.ZERO,
                "secondKey", new StringNode("second")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "thirdKey", new StringNode("third")));

        applySingle(
                new SignalCommand.RemoveCommand(Id.random(), commandId, null));

        assertMapKeys(Id.ZERO, "firstKey", "thirdKey");

    }

    @Test
    void removeByKeyCommand_missingKey_reject() {
        CommandResult result = applySingle(new SignalCommand.RemoveByKeyCommand(
                commandId, Id.ZERO, "key"));

        // Check result object
        assertFalse(result.accepted());

        // Check revision state
        assertUnchanged();
    }

    @Test
    void removeByKeyCommand_existingKey_removed() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        CommandResult result = applySingle(new SignalCommand.RemoveByKeyCommand(
                commandId, Id.ZERO, "key"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(2, accept.updates().size());
        NodeModification childUpdate = accept.updates().get(child);
        assertNotNull(childUpdate);
        assertNull(childUpdate.newNode());
        NodeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertNotNull(parentUpdate);
        Data parentNewNode = (Data) parentUpdate.newNode();
        assertNotNull(parentNewNode);
        assertEquals(Map.of(), parentNewNode.mapChildren());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void removeByKeyCommand_parentAlias_dataNodeUpdated() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(Id.ZERO);
        CommandResult result = applySingle(
                new SignalCommand.RemoveByKeyCommand(commandId, alias, "key"));

        // Check result object
        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void removeByKeyCommand_multipleMapChildren_orderPreserved() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "firstKey", new StringNode("first")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "secondKey", new StringNode("second")));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO,
                "thirdKey", new StringNode("third")));

        applySingle(new SignalCommand.RemoveByKeyCommand(Id.random(), Id.ZERO,
                "secondKey"));

        assertMapKeys(Id.ZERO, "firstKey", "thirdKey");

    }

    @Test
    void clearCommand_nodeWithChildren_childrenRemoved() {
        Id listChild = Id.random();
        applySingle(new SignalCommand.InsertCommand(listChild, Id.ZERO, null,
                null, ListPosition.last()));

        Id mapChild = Id.random();
        applySingle(
                new SignalCommand.PutCommand(mapChild, Id.ZERO, "key", null));

        CommandResult result = applySingle(
                new SignalCommand.ClearCommand(commandId, Id.ZERO));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(3, accept.updates().size());
        NodeModification listChildUpdate = accept.updates().get(listChild);
        assertNotNull(listChildUpdate);
        assertNull(listChildUpdate.newNode());
        NodeModification mapChildUpdate = accept.updates().get(mapChild);
        assertNotNull(mapChildUpdate);
        assertNull(mapChildUpdate.newNode());
        NodeModification parentUpdate = accept.updates().get(Id.ZERO);
        assertNotNull(parentUpdate);
        Data parentNewNode = (Data) parentUpdate.newNode();
        assertNotNull(parentNewNode);
        assertEquals(Map.of(), parentNewNode.mapChildren());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of());
    }

    @Test
    void clearCommand_emptyNode_noChange() {
        CommandResult result = applySingle(
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
        CommandResult result = applySingle(
                new SignalCommand.ClearCommand(alias, Id.ZERO));

        Accept accept = assertAccepted(result);
        assertTrue(accept.updates().containsKey(Id.ZERO));
        assertFalse(accept.updates().containsKey(alias));

        // Check revision state
        assertListChildren(Id.ZERO);
    }

    @Test
    void positionConditionNoPosition_listChild_accepted() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, child, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionNoPosition_missingChild_rejected() {
        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, Id.random(), new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionNoPosition_mapChild_rejected() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, child, new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionNoPosition_otherParent_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(
                new SignalCommand.PositionCondition(commandId, children.get(0),
                        children.get(1), new ListPosition(null, null)));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionFirst_isFirst_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0), ListPosition.first()));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionFirst_isNotFirst_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1), ListPosition.first()));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionLast_isNotLast_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0), ListPosition.last()));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionLast_isLast_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1), ListPosition.last()));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionAfter_isAfter_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(0), null)));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionAfter_itself_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(children.get(0), null)));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionAfter_isNotAfter_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(children.get(1), null)));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionBefore_isBefore_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(null, children.get(1))));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionBefore_itself_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(0),
                new ListPosition(null, children.get(0))));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionBefore_isNotBefore_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 2);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(null, children.get(0))));

        assertTestResult(false, result);
    }

    @Test
    void positionConditionBetween_isBetween_accepted() {
        List<Id> children = insertChildren(Id.ZERO, 3);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(0), children.get(2))));

        assertTestResult(true, result);
    }

    @Test
    void positionConditionBetween_notBetween_rejected() {
        List<Id> children = insertChildren(Id.ZERO, 3);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, children.get(1),
                new ListPosition(children.get(2), children.get(1))));

        assertTestResult(false, result);
    }

    @Test
    void positionCondition_parentAlias_checksAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, alias, child, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionCondition_childAlias_checkAliasTarget() {
        Id child = insertChildren(Id.ZERO, 1).get(0);

        Id alias = createAlias(child);

        CommandResult result = applySingle(new SignalCommand.PositionCondition(
                commandId, Id.ZERO, alias, new ListPosition(null, null)));

        assertTestResult(true, result);
    }

    @Test
    void positionCondition_aliasSiblings_checkAliasTargets() {
        List<Id> children = insertChildren(Id.ZERO, 3);
        Id first = createAlias(children.get(0));
        Id last = createAlias(children.get(2));

        CommandResult result = applySingle(
                new SignalCommand.PositionCondition(commandId, Id.ZERO,
                        children.get(1), new ListPosition(first, last)));

        assertTestResult(true, result);
    }

    @Test
    void valueCondition_sameValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new StringNode("value")));

        CommandResult result = applySingle(new SignalCommand.ValueCondition(
                commandId, Id.ZERO, new StringNode("value")));

        assertTestResult(true, result);
    }

    @Test
    void valueCondition_alias_dataNodeChecked() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new StringNode("value")));

        Id alias = createAlias(Id.ZERO);

        CommandResult result = applySingle(new SignalCommand.ValueCondition(
                commandId, alias, new StringNode("value")));

        assertTestResult(true, result);
    }

    @Test
    void valueCondition_otherValue_rejected() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                new StringNode("other")));

        CommandResult result = applySingle(new SignalCommand.ValueCondition(
                commandId, Id.ZERO, new StringNode("value")));

        assertTestResult(false, result);
    }

    @Test
    void valueConditiontNull_jsonNullValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO,
                NullNode.getInstance()));

        CommandResult result = applySingle(
                new SignalCommand.ValueCondition(commandId, Id.ZERO, null));

        assertTestResult(true, result);
    }

    @Test
    void valueConditionJsonNull_nullValue_accepted() {
        applySingle(new SignalCommand.SetCommand(Id.random(), Id.ZERO, null));

        CommandResult result = applySingle(new SignalCommand.ValueCondition(
                commandId, Id.ZERO, NullNode.getInstance()));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateCondition_sameValue_accepted() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        CommandResult result = applySingle(
                new SignalCommand.LastUpdateCondition(commandId, Id.ZERO,
                        update));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateCondition_alias_targetNodeChecked() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        Id alias = createAlias(Id.ZERO);
        CommandResult result = applySingle(
                new SignalCommand.LastUpdateCondition(commandId, alias,
                        update));

        assertTestResult(true, result);
    }

    @Test
    void lastUpdateCondition_differentValue_rejected() {
        Id update = Id.random();
        applySingle(new SignalCommand.SetCommand(update, Id.ZERO, null));

        CommandResult result = applySingle(
                new SignalCommand.LastUpdateCondition(commandId, Id.ZERO,
                        Id.random()));

        assertTestResult(false, result);
    }

    @Test
    void keyConditionNoKey_noKey_accepted() {
        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", Id.ZERO));

        assertTestResult(true, result);
    }

    @Test
    void keyConditionNoKey_keyPresent_rejected() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", Id.ZERO));

        assertTestResult(false, result);
    }

    @Test
    void keyConditionKeyPresent_keyPresent_accepted() {
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", null));

        assertTestResult(true, result);
    }

    @Test
    void keyConditionKeyPresent_noKey_rejected() {
        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", null));

        assertTestResult(false, result);
    }

    @Test
    void keyConditionSpecificNode_nodePresent_accepted() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", child));

        assertTestResult(true, result);
    }

    @Test
    void keyConditionSpecificNode_noNode_rejected() {
        Id child = Id.random();

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyConditionSpecificNode_otherKey_rejected() {
        Id child = Id.random();
        applySingle(
                new SignalCommand.PutCommand(child, Id.ZERO, "other", null));

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyConditionSpecificNode_otherNode_rejected() {
        Id child = Id.random();
        applySingle(
                new SignalCommand.PutCommand(child, Id.ZERO, "other", null));
        applySingle(new SignalCommand.PutCommand(Id.random(), Id.ZERO, "key",
                null));

        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", child));

        assertTestResult(false, result);
    }

    @Test
    void keyCondition_aliasParent_targetChecked() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(Id.ZERO);
        CommandResult result = applySingle(
                new SignalCommand.KeyCondition(commandId, alias, "key", child));

        assertTestResult(true, result);
    }

    @Test
    void keyCondition_aliasChild_targetChecked() {
        Id child = Id.random();
        applySingle(new SignalCommand.PutCommand(child, Id.ZERO, "key", null));

        Id alias = createAlias(child);
        CommandResult result = applySingle(new SignalCommand.KeyCondition(
                commandId, Id.ZERO, "key", alias));

        assertTestResult(true, result);
    }

    @Test
    void transactionCommand_empty_noChange() {
        CommandResult result = applySingle(
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

        Map<Id, CommandResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.SetCommand(command1, Id.ZERO,
                                new StringNode("value")),
                                new SignalCommand.PutCommand(command2, Id.ZERO,
                                        "key", null)))));

        // Check result objects
        var resultId = results.get(commandId);
        assertNotNull(resultId);
        Accept transaction = assertAccepted(resultId);
        assertEquals(2, transaction.updates().size());
        // Verify that modifications from both commands are merged
        NodeModification rootModification = transaction.updates().get(Id.ZERO);
        assertNotNull(rootModification);
        assertEquals(Node.EMPTY, rootModification.oldNode());
        assertEquals(revision.nodes().get(Id.ZERO), rootModification.newNode());

        var result1 = results.get(command1);
        assertNotNull(result1);
        Accept set = assertAccepted(result1);
        Data setModificationNode = (Data) set.onlyUpdate().newNode();
        assertNotNull(setModificationNode);
        var setModificationValue = setModificationNode.value();
        assertNotNull(setModificationValue);
        assertEquals("value", setModificationValue.textValue());
        assertEquals(Map.of(), setModificationNode.mapChildren());

        // Check revision state
        assertValue(Id.ZERO, "value");
        assertMapChildren(Id.ZERO, Map.of("key", command2));
    }

    @Test
    void transactionCommand_lastRejected_allRejected() {
        Id command1 = Id.random();
        Id command2 = Id.random();

        Map<Id, CommandResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.SetCommand(command1, Id.ZERO,
                                new StringNode("value")),
                                new SignalCommand.ValueCondition(command2,
                                        Id.ZERO, null)))));

        // Check result objects
        assertEquals(Set.of(command1, command2, commandId), results.keySet());
        for (CommandResult subResult : results.values()) {
            assertFalse(subResult.accepted());
        }

        // Check revision state
        assertUnchanged();
    }

    @Test
    void transactionCommand_nestedTransactions_allApplied() {
        Id set = Id.random();
        Id innerTransaction = Id.random();

        Map<Id, CommandResult> results = revision.applyAndGetResults(
                List.of(new SignalCommand.TransactionCommand(commandId,
                        List.of(new SignalCommand.TransactionCommand(
                                innerTransaction,
                                List.of(new SignalCommand.SetCommand(set,
                                        Id.ZERO, new StringNode("value"))))))));

        // Check result objects
        var resultId = results.get(commandId);
        assertNotNull(resultId);
        assertAccepted(resultId);
        var resultInner = results.get(innerTransaction);
        assertNotNull(resultInner);
        assertAccepted(resultInner);
        var resultSet = results.get(set);
        assertNotNull(resultSet);
        assertAccepted(resultSet);

        // Check revision state
        assertValue(Id.ZERO, "value");
    }

    @Test
    void snapshotEvent_withNodes_loaded() {
        MutableTreeRevision copy = new MutableTreeRevision(revision);

        Id child = Id.random();
        copy.apply(new SignalCommand.PutCommand(child, Id.ZERO, "key", null),
                null);

        CommandResult result = applySingle(
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

        var originalInsert = revision.originalInserts().get(node);
        assertNotNull(originalInsert);
        assertEquals(node, originalInsert.commandId());

        Id childOfOwned = Id.random();
        applySingle(
                new SignalCommand.PutCommand(childOfOwned, node, "key", null));

        CommandResult result = applySingle(new SignalCommand.ClearOwnerCommand(
                commandId, revision.ownerId()));

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

        var originalInsert = revision.originalInserts().get(node);
        assertNotNull(originalInsert);
        assertEquals(node, originalInsert.commandId());

        CommandResult result = applySingle(new SignalCommand.ClearOwnerCommand(
                commandId, revision.ownerId()));

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

        CommandResult result = applySingle(new SignalCommand.ClearOwnerCommand(
                commandId, revision.ownerId()));

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

        CommandResult result = applySingle(new SignalCommand.ClearOwnerCommand(
                commandId, revision.ownerId()));

        // Check result object
        Accept accept = assertAccepted(result);
        assertEquals(0, accept.updates().size());

        // Check revision state
        assertMapChildren(Id.ZERO, Map.of("key", node));
    }

    @Test
    void apply_listOfCommands_appliesAllCommands() {
        SignalCommand.SetCommand setA = new SignalCommand.SetCommand(
                Id.random(), Id.ZERO, new StringNode("a"));
        // Failed condition to test that subsequent commands are still applied
        SignalCommand.ValueCondition testB = new SignalCommand.ValueCondition(
                Id.random(), Id.ZERO, new StringNode("b"));
        SignalCommand.InsertCommand insertC = new SignalCommand.InsertCommand(
                Id.random(), Id.ZERO, null, new StringNode("c"),
                ListPosition.first());

        revision.apply(List.of(setA, testB, insertC));

        assertValue(Id.ZERO, "a");
        assertListChildren(Id.ZERO, insertC.commandId());
    }

    private CommandResult applySingle(SignalCommand command) {
        Map<Id, CommandResult> results = revision
                .applyAndGetResults(List.of(command));
        assertEquals(1, results.size());

        var result = results.get(command.commandId());
        assertNotNull(result);
        return result;
    }

    private void assertNullValue(Id nodeId) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertNull(data.value());
    }

    private void assertValue(Id nodeId, String expectedValue) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertEquals(new StringNode(expectedValue), data.value());
    }

    private void assertValue(Id nodeId, double expectedValue) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertEquals(new DoubleNode(expectedValue), data.value());
    }

    private void assertListChildren(Id nodeId, Id... expectedChildren) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertEquals(List.of(expectedChildren), data.listChildren());

        for (Id child : expectedChildren) {
            var childData = revision.data(child).get();
            assertNotNull(childData);
            assertEquals(nodeId, childData.parent());
        }
    }

    private void assertMapChildren(Id nodeId,
            Map<String, Id> expectedChildren) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertEquals(expectedChildren, data.mapChildren());

        for (Id child : expectedChildren.values()) {
            var childData = revision.data(child).get();
            assertNotNull(childData);
            assertEquals(nodeId, childData.parent());
        }
    }

    private void assertMapKeys(Id nodeId, String... expectedKeys) {
        var data = revision.data(nodeId).get();
        assertNotNull(data);
        assertEquals(List.of(expectedKeys),
                List.copyOf(data.mapChildren().keySet()));
    }

    private void assertUnchanged() {
        assertEquals(Map.of(Id.ZERO, Node.EMPTY), revision.nodes());
    }

    private static void assertTestResult(boolean expectedResult,
            CommandResult result) {
        assertEquals(expectedResult, result.accepted());
        if (result.accepted()) {
            assertEquals(0, ((Accept) result).updates().size());
        }
    }

    private static Accept assertAccepted(CommandResult result) {
        assertTrue(result.accepted());
        return (Accept) result;
    }

    private static Data assertSingleDataChange(CommandResult result) {
        Accept accept = assertAccepted(result);
        NodeModification onlyUpdate = accept.onlyUpdate();
        var newNode = onlyUpdate.newNode();
        assertNotNull(newNode);
        return (Data) newNode;
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

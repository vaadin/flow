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
package com.vaadin.signals;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.signals.ListSignal.ListPosition;

/**
 * A command triggered from a signal.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
@JsonSubTypes(value = {

        @Type(SignalCommand.ValueCondition.class),
        @Type(SignalCommand.PositionCondition.class),
        @Type(SignalCommand.KeyCondition.class),
        @Type(SignalCommand.LastUpdateCondition.class),

        @Type(SignalCommand.AdoptAtCommand.class),
        @Type(SignalCommand.AdoptAsCommand.class),

        @Type(SignalCommand.IncrementCommand.class),
        @Type(SignalCommand.ClearCommand.class),

        @Type(SignalCommand.RemoveByKeyCommand.class),
        @Type(SignalCommand.PutCommand.class),
        @Type(SignalCommand.PutIfAbsentCommand.class),

        @Type(SignalCommand.InsertCommand.class),
        @Type(SignalCommand.SetCommand.class),
        @Type(SignalCommand.RemoveCommand.class),
        @Type(SignalCommand.ClearOwnerCommand.class),

        @Type(SignalCommand.TransactionCommand.class),
        @Type(SignalCommand.SnapshotCommand.class),

})
public sealed interface SignalCommand {
    /**
     * A signal command that sets the value of a signal.
     */
    sealed interface ValueCommand extends SignalCommand {
        /**
         * Gets the JSON node with the value to set.
         *
         * @return the JSON value, or <code>null</code>
         */
        JsonNode value();
    }

    /**
     * A signal command that targets a map entry by key.
     */
    sealed interface KeyCommand extends SignalCommand {
        /**
         * Gets the targeted map key.
         *
         * @return the map key, not <code>null</code>
         */
        String key();
    }

    /**
     * A signal command that doesn't apply any change but only performs a test
     * that will be part of determining whether a transaction passes.
     */
    sealed interface ConditionCommand extends SignalCommand {
    }

    /**
     * A signal command that creates a new signal node that might have an owner.
     * The created node will be automatically removed if the owner is
     * disconnected.
     */
    sealed interface ScopeOwnerCommand extends SignalCommand {
        /**
         * The owner id.
         *
         * @return the owner id, or <code>null</code> if the created signal has
         *         no scope owner
         */
        Id scopeOwner();
    }

    /**
     * A signal command that doesn't target a specific node.
     */
    sealed interface GlobalCommand extends SignalCommand {
        @Override
        default Id targetNodeId() {
            return Id.ZERO;
        }
    }

    /**
     * Tests whether the given node has the expected value, based on JSON
     * equality.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to check, not <code>null</code>
     * @param expectedValue
     *            the expected value
     */
    public record ValueCondition(Id commandId, Id targetNodeId,
            JsonNode expectedValue) implements ConditionCommand {
    }

    /**
     * Tests whether the given node has a given child at a given position.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the parent node to check, not <code>null</code>
     * @param childId
     *            the id of the child to check for, not <code>null</code>
     * @param position
     *            the list position to use for optionally checking whether the
     *            child has the expected siblings
     */
    public record PositionCondition(Id commandId, Id targetNodeId, Id childId,
            ListPosition position) implements ConditionCommand {
    }

    /**
     * Tests whether the given node has the expected child for a specific map
     * key.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the parent node to check, not <code>null</code>
     * @param key
     *            the key to check, not <code>null</code>
     * @param expectedChild
     *            the child id to test for, or <code>null</code> to check that
     *            any child is present, or <code>Id.ZERO</code> to test that no
     *            child is present
     */
    public record KeyCondition(Id commandId, Id targetNodeId, String key,
            Id expectedChild) implements ConditionCommand {
    }

    /**
     * Tests that the given node was last updated by the command with the given
     * id.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to check, not <code>null</code>
     * @param expectedLastUpdate
     *            the expected id of the command that last updated this node,
     *            not <code>null</code>
     */
    public record LastUpdateCondition(Id commandId, Id targetNodeId,
            Id expectedLastUpdate) implements ConditionCommand {
    }

    /**
     * Adopts the given node as a child with the given key. The child must
     * already be attached somewhere in the same tree and it cannot be an
     * ancestor of its new parent. There cannot be an existing child with the
     * same key. The child is detached from its previous parent.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the parent node to adopt to, not <code>null</code>
     * @param childId
     *            id of the child node to adopt, not <code>null</code>
     * @param key
     *            key to adopt the node as, not <code>null</code>
     */
    public record AdoptAsCommand(Id commandId, Id targetNodeId, Id childId,
            String key) implements KeyCommand {
    }

    /**
     * Adopts the given node as a child at the given insertion position. The
     * child must already be attached somewhere in the same tree and it cannot
     * be an ancestor of its new parent. The child is detached from its previous
     * parent.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the parent node to adopt to, not <code>null</code>
     * @param childId
     *            id of the child node to adopt, not <code>null</code>
     * @param position
     *            the list insert position to insert into, not <code>null</code>
     */
    public record AdoptAtCommand(Id commandId, Id targetNodeId, Id childId,
            ListPosition position) implements SignalCommand {
    }

    /**
     * Increments the value of the given node by the given delta. The node must
     * have a numerical value. If the node has no value at all, then 0 is used
     * as the previous value. A negative delta value leads to decrementing the
     * value.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to update, not <code>null</code>
     * @param delta
     *            a double value to increment by
     */
    public record IncrementCommand(Id commandId, Id targetNodeId,
            double delta) implements SignalCommand {
    }

    /**
     * Removes all children from the target node.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to update, not <code>null</code>
     */
    public record ClearCommand(Id commandId,
            Id targetNodeId) implements SignalCommand {
    }

    /**
     * Removes the child with the given key, if present.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to update, not <code>null</code>
     * @param key
     *            the key to remove, not <code>null</code>
     */
    public record RemoveByKeyCommand(Id commandId, Id targetNodeId,
            String key) implements KeyCommand {
    }

    /**
     * Stores the given value in a child node with the given key. If a node
     * already exists, then its value is updated. If no node exists, then a new
     * node is created.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the parent node to update, not <code>null</code>
     * @param key
     *            the key to update, not <code>null</code>
     * @param value
     *            the value to set
     */
    public record PutCommand(Id commandId, Id targetNodeId, String key,
            JsonNode value) implements ValueCommand, KeyCommand {
    }

    /**
     * Stores the given value in a child node with the given key if it doesn't
     * already exist. If the key exists, then the value is not updated but a new
     * alias is created to reference the existing entry. If the key doesn't
     * exist, then a new node is created to hold the value.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>. Also used as the node id of
     *            the newly created node.
     * @param targetNodeId
     *            id of the parent node to update, not <code>null</code>
     * @param key
     *            the key to update, not <code>null</code>
     * @param value
     *            the value to set if a mapping didn't already exist
     */
    public record PutIfAbsentCommand(Id commandId, Id targetNodeId,
            Id scopeOwner, String key, JsonNode value)
            implements
                ValueCommand,
                KeyCommand,
                ScopeOwnerCommand {
    }

    /**
     * Inserts a new node with the given value at the given list insert
     * position.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>. Also used as the node id of
     *            the newly created node.
     * @param targetNodeId
     *            id of the parent node to update, not <code>null</code>
     * @param value
     *            the value to set if a mapping didn't already exist
     * @param position
     *            the list insert position, not <code>null</code>
     */
    record InsertCommand(Id commandId, Id targetNodeId, Id scopeOwner,
            JsonNode value, ListSignal.ListPosition position)
            implements
                ValueCommand,
                ScopeOwnerCommand {
    }

    /**
     * Sets the value of the given node.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to update, not <code>null</code>
     * @param value
     *            the value to set
     */
    record SetCommand(Id commandId, Id targetNodeId,
            JsonNode value) implements ValueCommand {
    }

    /**
     * Removes the given node from its parent, optionally verifying that the
     * parent is as expected.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param targetNodeId
     *            id of the node to remove, not <code>null</code>
     * @param expectedParentId
     *            the expected parent node id, or <code>null</code> to not
     *            verify the parent
     */
    record RemoveCommand(Id commandId, Id targetNodeId,
            Id expectedParentId) implements SignalCommand {
    }

    /**
     * Removes all nodes that have its scope owner set as the given id.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param ownerId
     *            the scope owner id to look for, not <code>null</code>
     */
    record ClearOwnerCommand(Id commandId,
            Id ownerId) implements GlobalCommand {
    }

    /**
     * A sequence of commands that should be applied atomically and only if all
     * commands are individually accepted.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param commands
     *            the list of commands to apply, not <code>null</code>
     */
    record TransactionCommand(Id commandId,
            List<SignalCommand> commands) implements GlobalCommand {
    }

    /**
     * Initializes a tree based on a collection of pre-existing nodes.
     *
     * @param commandId
     *            the unique command id used to track the status of this command
     *            instance, not <code>null</code>
     * @param nodes
     *            a map from node id to nodes to use, not <code>null</code>
     */
    record SnapshotCommand(Id commandId,
            Map<Id, Node> nodes) implements GlobalCommand {
    }

    /**
     * Gets the unique command id used to track the status of this command. For
     * commands that creates a new node, the command id is also used as the node
     * id of the created node.
     *
     * @return the unique command id used to track the status of this command,
     *         not <code>null</code>
     */
    Id commandId();

    /**
     * Gets the id of the signal node that is targeted by this command. Some
     * commands might target multiple nodes e.g. in a parent-child relationship
     * and in that case this node is the primary node. Some commands,
     * implementing {@link GlobalCommand} do not target any specific node and
     * for those commands, {@link Id#ZERO} is used as the node id.
     *
     * @return id of the primary node targeted by this command, not
     *         <code>null</code>
     */
    Id targetNodeId();
}

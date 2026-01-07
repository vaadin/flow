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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.vaadin.signals.Id;
import com.vaadin.signals.Node;
import com.vaadin.signals.Node.Alias;
import com.vaadin.signals.Node.Data;
import com.vaadin.signals.SignalCommand;
import com.vaadin.signals.SignalCommand.ScopeOwnerCommand;

/**
 * A revision of a signal tree. The revision keeps track of the nodes that make
 * up the tree and any insert commands that are owned by this revision.
 *
 * @see MutableTreeRevision
 */
public abstract class TreeRevision {
    private final Map<Id, Node> nodes;
    private final Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts;

    private final Id ownerId;

    /**
     * Creates a new revision based on the given owner id, map of signal nodes
     * and map of original inserts.
     *
     * @param ownerId
     *            the id of the tree owner, not <code>null</code>
     * @param nodes
     *            the map of state nodes in this revision, not <code>null</code>
     * @param originalInserts
     *            the map of insert commands that created any nodes owned by
     *            this tree, not <code>null</code>
     */
    public TreeRevision(Id ownerId, Map<Id, Node> nodes,
            Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts) {
        this.ownerId = ownerId;
        this.nodes = nodes;
        this.originalInserts = originalInserts;

        assert assertValidTree();
    }

    /**
     * Gets the id of the tree that this revision belongs to.
     *
     * @see #originalInserts()
     *
     * @return the owner id, not <code>null</code>
     */
    public Id ownerId() {
        return ownerId;
    }

    /**
     * Gets the nodes that make up this revision.
     *
     * @return a map from node id to node, not <code>null</code>
     */
    public Map<Id, Node> nodes() {
        return nodes;
    }

    /**
     * Gets a map of signal commands for creating any nodes owned by this tree.
     * Any signal node with a matching {@link Data#scopeOwner()} is considered
     * to be owned by that tree and such nodes should be removed if the tree is
     * disconnected. The revision keeps track of the original insert operations
     * so that the nodes can be inserted back again in the appropriate way if
     * the tree is connected back again.
     *
     * @return a map from node id to signal command, not <code>null</code>
     */
    public Map<Id, SignalCommand.ScopeOwnerCommand> originalInserts() {
        return originalInserts;
    }

    /**
     * Get the data node for the given node id, if present. If the given id
     * corresponds to an alias node, then alias is resolved and the data node
     * for the alias target is returned instead.
     *
     * @param id
     *            the id for which to get a data node, not <code>null</code>
     * @return an optional containing the corresponding data node, or an empty
     *         optional of there is no node with the given id
     */
    public Optional<Data> data(@Nullable Id id) {
        Node node = nodes.get(id);
        if (node instanceof Data data) {
            return Optional.of(data);
        } else if (node instanceof Alias alias) {
            return data(alias.target());
        } else {
            // Guard against an unexpected Node subclass
            assert node == null;

            return Optional.empty();
        }
    }

    /**
     * Asserts that the nodes in this revision are internally consistent.
     *
     * <ul>
     * <li>All nodes are attached to the root
     * <li>All parent-child relationships are consistent in both directions
     * <li>No node is attached in multiple places
     * <li>All aliases target an existing data node
     * <li>All nodes with a matching scope owner has a matching original insert
     * <li>All original insert entries correspond to a node with a matching
     * scope owner
     * </ul>
     *
     * This method is intended to be invoked with the <code>assert</code>
     * keyword. While the return type is <code>boolean</code>, it will never
     * return <code>false</code> but instead throws an assertion error from the
     * appropriate check to make it easier to pinpoint the source of any error.
     *
     * @return <code>true</code> if the tree is valid, otherwise an assertion
     *         error is thrown
     */
    protected boolean assertValidTree() {

        Set<Id> visited = new HashSet<>();
        Set<Id> checkedScopeOwners = new HashSet<>();

        LinkedList<Id> toCheck = new LinkedList<>();
        assert nodes.containsKey(Id.ZERO);
        assert nodes.get(Id.ZERO) instanceof Data root && root.parent() == null;

        toCheck.add(Id.ZERO);
        if (nodes.containsKey(Id.MAX)) {
            assert nodes.get(Id.MAX) instanceof Data root
                    && root.parent() == null;

            toCheck.add(Id.MAX);
        }

        // Traverse to check parent-child relationships
        while (!toCheck.isEmpty()) {
            Id id = toCheck.poll();
            if (!visited.add(id)) {
                assert false : "Already visited";
            }
            Data node = data(id).get();

            if (ownerId.equals(node.scopeOwner())) {
                checkedScopeOwners.add(id);
                ScopeOwnerCommand scopeOwnerCommand = originalInserts.get(id);
                assert scopeOwnerCommand != null;
                assert scopeOwnerCommand.scopeOwner().equals(ownerId);
            }

            Stream<Id> allChildren = Stream.concat(node.listChildren().stream(),
                    node.mapChildren().values().stream());
            allChildren.forEach(childId -> {
                toCheck.add(childId);

                assert nodes.containsKey(childId);

                Data child = data(childId).get();
                assert child.parent() != null;
                assert child.parent().equals(id);
            });
        }

        List<Id> unattached = nodes.keySet().stream().filter(
                id -> nodes.get(id) instanceof Data && !visited.contains(id))
                .toList();
        assert unattached.isEmpty();

        nodes().values().forEach(node -> {
            if (node instanceof Alias alias) {
                assert nodes.get(alias.target()) instanceof Data;
            }
        });

        assert checkedScopeOwners.equals(originalInserts.keySet());

        return true;
    }
}

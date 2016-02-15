/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.hummingbird;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.namespace.Namespace;

/**
 * The state tree that is synchronized with the client-side.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StateTree implements NodeOwner {

    private final class RootNode extends StateNode {
        private RootNode(Class<? extends Namespace>[] namespaces) {
            super(namespaces);

            // Bootstrap
            setTree(StateTree.this);

            // Assign id
            onAttach();
        }

        @Override
        public void setParent(StateNode parent) {
            throw new IllegalStateException(
                    "Can't set the parent of the tree root");
        }

        @Override
        public boolean isAttached() {
            // Root is always attached
            return true;
        }
    }

    private Set<StateNode> dirtyNodes = new HashSet<>();

    private final Map<Integer, StateNode> idToNode = new HashMap<>();
    private int nextId = 1;

    private final StateNode rootNode;

    /**
     * Creates a new state tree with a set of namespaces defined for the root
     * node.
     *
     * @param namespaces
     *            the namespaces of the root node
     */
    @SafeVarargs
    public StateTree(Class<? extends Namespace>... namespaces) {
        rootNode = new RootNode(namespaces);
    }

    /**
     * Gets the root node of this state tree. The root node is created together
     * with the tree and can't be detached.
     *
     * @return the root node
     */
    public StateNode getRootNode() {
        return rootNode;
    }

    @Override
    public int register(StateNode node) {
        assert node.getOwner() == this;

        int id = node.getId();

        int nodeId;
        if (id > 0 && !idToNode.containsKey(Integer.valueOf(id))) {
            // Node already had an id, continue using it

            // Don't accept an id that we haven't yet handed out
            assert id < nextId;

            nodeId = id;
        } else {
            nodeId = nextId++;
        }

        idToNode.put(Integer.valueOf(nodeId), node);
        return nodeId;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;

        Integer id = Integer.valueOf(node.getId());

        StateNode removedNode = idToNode.remove(id);

        if (removedNode != node) {
            // Remove by id didn't remove the expected node
            if (removedNode != null) {
                // Put the old node back
                idToNode.put(Integer.valueOf(removedNode.getId()), removedNode);
            }
            throw new IllegalStateException(
                    "Unregistered node was not found based on its id. The tree is most likely corrupted.");
        }
    }

    /**
     * Finds a node with the given id.
     *
     * @see StateNode#getId()
     * @param id
     *            the node id to look for
     * @return the node with the given id; <code>null</code> if the id is not
     *         registered with this tree
     */
    public StateNode getNodeById(int id) {
        return idToNode.get(Integer.valueOf(id));
    }

    /**
     * Collects all changes made to this tree since the last time
     * {@link #collectChanges(Consumer)} has been called.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public void collectChanges(Consumer<NodeChange> collector) {
        // TODO fire preCollect events

        collectDirtyNodes().forEach(n -> n.collectChanges(collector));
    }

    @Override
    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;

        dirtyNodes.add(node);
    }

    /**
     * Gets all the nodes that have been marked as dirty since the last time
     * this method was invoked.
     *
     * @return a set of dirty nodes
     */
    public Set<StateNode> collectDirtyNodes() {
        Set<StateNode> collectedNodes = dirtyNodes;
        dirtyNodes = new HashSet<>();
        return collectedNodes;
    }

    /**
     * Checks if there are nodes that have been marked as dirty since the last
     * time {@link #collectDirtyNodes()} was invoked.
     *
     * @return true if there are dirty nodes, false otherwise
     */
    public boolean hasDirtyNodes() {
        return !dirtyNodes.isEmpty();
    }
}

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

package com.vaadin.hummingbird;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.change.ListAddChange;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.nodefeature.NodeFeature;
import com.vaadin.ui.UI;

/**
 * The state tree that is synchronized with the client-side.
 *
 * @author Vaadin Ltd
 */
public class StateTree implements NodeOwner {

    private final class RootNode extends StateNode {
        private RootNode(Class<? extends NodeFeature>[] features) {
            super(features);

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

    private LinkedHashSet<StateNode> dirtyNodes = new LinkedHashSet<>();

    private final Map<Integer, StateNode> idToNode = new HashMap<>();
    private int nextId = 1;

    private final StateNode rootNode;

    private final UI ui;

    /**
     * Creates a new state tree with a set of features defined for the root
     * node.
     *
     * @param features
     *            the features of the root node
     * @param ui
     *            the UI that this tree belongs to
     */
    @SafeVarargs
    public StateTree(UI ui, Class<? extends NodeFeature>... features) {
        rootNode = new RootNode(features);
        this.ui = ui;
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
        if (id > 0 && !idToNode.containsKey(id)) {
            // Node already had an id, continue using it

            // Don't accept an id that we haven't yet handed out
            assert id < nextId;

            nodeId = id;
        } else {
            nodeId = nextId++;
        }

        idToNode.put(nodeId, node);
        return nodeId;
    }

    @Override
    public void unregister(StateNode node) {
        assert node.getOwner() == this;

        StateNode removedNode = idToNode.remove(node.getId());
        if (removedNode != node) {
            // Remove by id didn't remove the expected node
            if (removedNode != null) {
                // Put the old node back
                idToNode.put(removedNode.getId(), removedNode);
            }
            throw new IllegalStateException(
                    "Unregistered node was not found based on its id. The tree is most likely corrupted.");
        }
    }

    /**
     * Finds a node with the given id.
     *
     * @param id
     *            the node id to look for
     * @return the node with the given id; <code>null</code> if the id is not
     *         registered with this tree
     * @see StateNode#getId()
     */
    public StateNode getNodeById(int id) {
        return idToNode.get(id);
    }

    /**
     * Collects all changes made to this tree since the last time this method
     * has been called.
     *
     * @return all changes made to this tree since the last method call
     */
    public Collection<NodeChange> collectChanges() {
        // TODO fire preCollect events
        return sortChanges(collectDirtyNodes().stream()
                .flatMap(node -> node.collectChanges().stream())
                .collect(Collectors.toList()));
    }

    /**
     * Forces all dependent changes to come after all their dependencies.
     *
     * In current implementation, every time we add element into a model list,
     * two events are propagated: node attach event and list splice event. If we
     * won't make sure that latter comes after former, we won't be able to
     * process list splice event normally, since there will be no data on
     * changes.
     *
     * Also puts node attach changes to go first, since we can not operate nodes
     * on the client otherwise.
     *
     * @param originalChanges
     *            not yet sorted changes
     * @return sorted changes
     */
    private Collection<NodeChange> sortChanges(
            List<NodeChange> originalChanges) {
        Set<NodeChange> sortedChanges = new LinkedHashSet<>(
                originalChanges.size(), 1);
        Set<NodeChange> notSortedChanges = new LinkedHashSet<>(originalChanges);

        Map<NodeChange, Collection<Integer>> nodesToDependencyIds = new HashMap<>(
                originalChanges.size(), 1);
        Map<Integer, Collection<NodeChange>> dependencyNodeIdToNodes = new HashMap<>(
                originalChanges.size(), 1);

        for (NodeChange change : originalChanges) {
            if (change instanceof NodeAttachChange) {
                sortedChanges.add(change);
            } else {
                notSortedChanges.add(change);

                if (change instanceof ListAddChange
                        && ((ListAddChange<?>) change).isNodeValues()) {
                    List<StateNode> stateNodes = ((ListAddChange<StateNode>) change)
                            .getNewItems();
                    nodesToDependencyIds.put(change,
                            stateNodes.stream().map(StateNode::getId)
                                    .collect(Collectors.toList()));
                } else if (change instanceof MapPutChange) {
                    dependencyNodeIdToNodes.merge(change.getNode().getId(),
                            Collections.singletonList(change),
                            (list1, list2) -> {
                                List<NodeChange> newList = new ArrayList<>(
                                        list1);
                                newList.addAll(list2);
                                return newList;
                            });
                }
            }
        }

        notSortedChanges.forEach(change -> {
            Collection<Integer> dependencyIds = nodesToDependencyIds
                    .remove(change);
            if (dependencyIds != null) {
                dependencyIds.stream().map(dependencyNodeIdToNodes::get)
                        .filter(Objects::nonNull).flatMap(Collection::stream)
                        .forEach(sortedChanges::add);
            }

            sortedChanges.add(change);
        });
        return sortedChanges;
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
     * @return a set of dirty nodes, in the order they were marked dirty
     */
    public LinkedHashSet<StateNode> collectDirtyNodes() {
        LinkedHashSet<StateNode> collectedNodes = dirtyNodes;
        dirtyNodes = new LinkedHashSet<>();
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

    /**
     * Gets the {@link UI} that this tree belongs to.
     *
     * @return the UI that this tree belongs to
     */
    public UI getUI() {
        return ui;
    }
}

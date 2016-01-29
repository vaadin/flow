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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract root of a state node tree. A node always belongs to one specific
 * owner. The owner keeps track of metadata for its nodes.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class NodeOwner implements Serializable {
    private Set<StateNode> dirtyNodes = new HashSet<>();

    /**
     * Adopts all nodes from another node owner, causing all nodes to be owned
     * by this owner instead.
     *
     * @param oldOwner
     *            the node owner from which to adopt nodes
     */
    public void adoptNodes(NodeOwner oldOwner) {
        assert oldOwner != this;
        ArrayList<StateNode> copy = new ArrayList<>(oldOwner.getNodes());
        copy.forEach(node -> node.setOwner(this));

        dirtyNodes.addAll(oldOwner.dirtyNodes);
    }

    /**
     * Registers a node with this node owner. The new node should already be set
     * to be owned by this instance.
     *
     * @param node
     *            the node to register
     * @return the id of the registered node
     */
    public int register(StateNode node) {
        assert node.getOwner() == this;

        markAsDirty(node);

        return doRegister(node);
    }

    /**
     * Overridden by subclasses to register a new node.
     *
     * @param node
     *            the node to register
     * @return the id of the registered node
     */
    protected abstract int doRegister(StateNode node);

    /**
     * Gets a collection of all nodes currently registered to this owner.
     *
     * @return a collection of owned nodes
     */
    public abstract Collection<StateNode> getNodes();

    /**
     * Unregisters a node from this owner. This must be done before the node is
     * set to not be owned by this instance.
     *
     * @param node
     *            the node to unregister
     */
    public void unregister(StateNode node) {
        assert node.getOwner() == this;
        dirtyNodes.remove(node);

        doUnregister(node);
    }

    /**
     * Overridden by subclasses to unregister a node.
     *
     * @param node
     *            the node to unregister
     */
    public abstract void doUnregister(StateNode node);

    /**
     * Marks a node owned by this instance as dirty. Dirty nodes are collected
     * from an owner using {@link #collectDirtyNodes()}.
     *
     * @param node
     *            the node to be marked as dirty
     */
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
}

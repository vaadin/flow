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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.namespace.Namespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;

/**
 * A node in the state tree that is synchronized with the client-side. Data
 * stored in nodes is structured into different namespaces to provide isolation.
 * The namespaces available for a node are defined when the node is created.
 *
 * @see StateTree
 * @since
 * @author Vaadin Ltd
 */
public class StateNode implements Serializable {
    private final Map<Class<? extends Namespace>, Namespace> namespaces = new HashMap<>();

    private NodeOwner owner = NullOwner.get();

    private StateNode parent;

    private int id = -1;

    // Only the root node is attached at this point
    private boolean wasAttached = isAttached();

    /**
     * Creates a state node with the given namespace types.
     *
     * @param namespaces
     *            a collection of namespace classes that the node should support
     */
    @SafeVarargs
    public StateNode(Class<? extends Namespace>... namespaces) {
        for (Class<? extends Namespace> namespaceType : namespaces) {
            Namespace namespace = NamespaceRegistry.create(namespaceType, this);
            this.namespaces.put(namespaceType, namespace);
        }

    }

    /**
     * Gets the node owner that this node currently belongs to.
     *
     * @return the node owner
     */
    public NodeOwner getOwner() {
        return owner;
    }

    /**
     * Gets the parent node that this node belongs to.
     *
     * @return the current parent node; <code>null</code> if the node is not
     *         attached to a parent node, or if this node is the root of a state
     *         tree.
     */
    public StateNode getParent() {
        return parent;
    }

    /**
     * Sets the parent node that this node belongs to. This node is set to
     * belong to the node owner of the parent node. The node still retains its
     * owner when the parent is set to <code>null</code>.
     *
     * @param parent
     *            the new parent of this node; or <code>null</code> if this node
     *            is not attached to another node
     */
    public void setParent(StateNode parent) {
        boolean attachedBefore = isAttached();
        boolean attachedAfter = false;

        if (parent != null) {
            assert this.parent == null : "Node is already attached to a parent: "
                    + this.parent;
            assert parent.hasChildAssert(this);

            if (isAncestorOf(parent)) {
                throw new IllegalStateException(
                        "Can't set own child as parent");
            }

            attachedAfter = parent.isAttached();

            NodeOwner parentOwner = parent.getOwner();
            if (parentOwner != owner && parentOwner instanceof StateTree) {
                setTree((StateTree) parentOwner);
            }
        }

        this.parent = parent;

        if (!attachedBefore && attachedAfter) {
            onAttach();
        } else if (attachedBefore && !attachedAfter) {
            onDetach();
        }
    }

    private boolean isAncestorOf(StateNode node) {
        while (node != null) {
            if (node == this) {
                return true;
            }
            node = node.getParent();
        }

        return false;
    }

    // Should only be used for debugging
    private boolean hasChildAssert(StateNode child) {
        AtomicBoolean found = new AtomicBoolean(false);
        forEachChild(c -> {
            if (c == child) {
                found.set(true);
            }
        });
        return found.get();
    }

    /**
     * Called when this node has been attached to a state tree.
     */
    // protected only to get the root node attached
    protected void onAttach() {
        assert isAttached();

        int newId = owner.register(this);

        if (newId != -1) {
            if (id == -1) {
                // Didn't have an id previously, set one now
                id = newId;
            } else if (newId != id) {
                throw new IllegalStateException(
                        "Can't change id once it has been assigned");
            }
        }

        // Ensure attach change is sent
        markAsDirty();

        forEachChild(StateNode::onAttach);
    }

    /**
     * Called when this node has been detached from its state tree.
     */
    private void onDetach() {
        assert !isAttached();

        // Ensure detach change is sent
        markAsDirty();

        forEachChild(StateNode::onDetach);
        owner.unregister(this);
    }

    private void forEachChild(Consumer<StateNode> action) {
        namespaces.values().forEach(n -> n.forEachChild(action));
    }

    /**
     * Sets the state tree that this node belongs to.
     *
     * @param tree
     *            the state tree
     */
    // protected only to get the root node attached
    protected void setTree(StateTree tree) {
        if (tree == owner) {
            return;
        }

        if (owner instanceof StateTree) {
            throw new IllegalStateException(
                    "Can't move a node from one state tree to another");
        }

        owner = tree;

        forEachChild(c -> c.setTree(tree));
    }

    /**
     * Gets the namespace of the given type. This method throws
     * {@link IllegalStateException} if this node does not contain the desired
     * namespace. Use {@link #hasNamespace(Class)} to check whether a node
     * contains a specific namespace.
     *
     * @param namespaceType
     *            the desired namespace type, not <code>null</code>
     * @return a namespace instance, not <code>null</code>
     */
    public <T extends Namespace> T getNamespace(Class<T> namespaceType) {
        assert namespaceType != null;

        Namespace namespace = namespaces.get(namespaceType);
        if (namespace == null) {
            throw new IllegalStateException(
                    "Node does not have the namespace " + namespaceType);
        }

        return namespaceType.cast(namespace);
    }

    /**
     * Checks whether this node contains a namespace.
     *
     * @param namespaceType
     *            the namespace type to check for
     * @return <code>true</code> if this node contains the namespace; otherwise
     *         <code>false</code>
     */
    public boolean hasNamespace(Class<? extends Namespace> namespaceType) {
        assert namespaceType != null;

        return namespaces.containsKey(namespaceType);
    }

    /**
     * Gets the id of this node. The id is unique within the state tree that the
     * node belongs to. The id is 0 if the node does not belong to any state
     * tree.
     *
     * @see StateTree#getNodeById(int)
     *
     * @return the node id
     */
    public int getId() {
        return id;
    }

    /**
     * Marks this node as dirty.
     *
     * @see StateTree#collectDirtyNodes()
     */
    public void markAsDirty() {
        owner.markAsDirty(this);
    }

    /**
     * Checks whether this node is attached to a state tree.
     *
     * @return <code>true</code> if this node is attached; <code>false</code> if
     *         this node is not atatched
     */
    public boolean isAttached() {
        return parent != null && parent.isAttached();
    }

    /**
     * Collects all changes made to this node since the last time
     * {@link #collectChanges(Consumer)} has been called. If the node is
     * recently attached, then the reported changes will be relative to a newly
     * created node.
     *
     * @param collector
     *            a consumer accepting node changes
     */
    public void collectChanges(Consumer<NodeChange> collector) {
        boolean isAttached = isAttached();
        if (isAttached != wasAttached) {
            if (isAttached) {
                collector.accept(new NodeAttachChange(this));

                // Make all changes show up as if the node was recently attached
                namespaces.values().forEach(Namespace::resetChanges);
            } else {
                collector.accept(new NodeDetachChange(this));
            }

            wasAttached = isAttached;
        }

        if (isAttached) {
            namespaces.values().forEach(n -> n.collectChanges(collector));
        }
    }
}

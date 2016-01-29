package com.vaadin.hummingbird;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.namespace.Namespace;

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

    private NodeOwner owner;

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
    public StateNode(Collection<Class<? extends Namespace>> namespaces) {
        for (Class<? extends Namespace> namespaceType : namespaces) {
            Namespace namespace = Namespace.create(namespaceType, this);
            this.namespaces.put(namespaceType, namespace);
        }

        setOwner(new TemporaryOwner());
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
        if (parent != null) {
            assert this.parent == null : "Node is already attached to a: "
                    + parent;

            NodeOwner parentOwner = parent.getOwner();
            if (parentOwner != owner) {
                setOwner(parentOwner);
            }
        }

        markAsDirty();
        this.parent = parent;
    }

    /**
     * Sets the node owner that this node belongs to.
     *
     * @param owner
     *            the new node owner
     */
    public void setOwner(NodeOwner owner) {
        assert owner != null;

        if (this.owner != null) {
            this.owner.unregister(this);
        }

        this.owner = owner;

        int newId = owner.register(this);
        if (newId != -1 && id != -1) {
            throw new IllegalStateException(
                    "Can't change id once it has been assigned");
        }
        id = newId;
    }

    /**
     * Gets the namespace of the given type.
     *
     * @param namespaceType
     *            the desired namespace type
     * @return a namespace instance; <code>null</code> if the node was not
     *         created to support a namespace of the given type
     */
    public <T extends Namespace> T getNamespace(Class<T> namespaceType) {
        assert namespaceType != null;

        Namespace namespace = namespaces.get(namespaceType);
        if (namespace == null) {
            return null;
        } else {
            return namespaceType.cast(namespace);
        }
    }

    /**
     * Gets the node of this node. The id is unique within the state tree that
     * the node belongs to. The id is 0 if the node does not belong to any state
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
     * @see NodeOwner#collectDirtyNodes()
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

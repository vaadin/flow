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

public class StateNode implements Serializable {
    private final Map<Class<? extends Namespace>, Namespace> namespaces = new HashMap<>();

    private NodeOwner owner;

    private StateNode parent;

    private int id = -1;

    // Only the root node is attached at this point
    private boolean wasAttached = isAttached();

    public StateNode(Collection<Class<? extends Namespace>> namespaces) {
        for (Class<? extends Namespace> namespaceType : namespaces) {
            Namespace namespace = Namespace.create(namespaceType, this);
            this.namespaces.put(namespaceType, namespace);
        }

        setOwner(new TemporaryOwner());
    }

    public NodeOwner getOwner() {
        return owner;
    }

    public StateNode getParent() {
        return parent;
    }

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

    public <T extends Namespace> T getNamespace(Class<T> namespaceType) {
        assert namespaceType != null;

        Namespace namespace = namespaces.get(namespaceType);
        if (namespace == null) {
            return null;
        } else {
            return namespaceType.cast(namespace);
        }
    }

    public int getId() {
        return id;
    }

    public void markAsDirty() {
        owner.markAsDirty(this);
    }

    public boolean isAttached() {
        return parent != null && parent.isAttached();
    }

    public void collectChanges(Consumer<NodeChange> collector) {
        boolean isAttached = isAttached();
        if (isAttached != wasAttached) {
            if (isAttached) {
                collector.accept(new NodeAttachChange(this));

                // Make all changes show up if we were recently attached
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

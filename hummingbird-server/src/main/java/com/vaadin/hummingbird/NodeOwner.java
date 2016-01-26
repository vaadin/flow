package com.vaadin.hummingbird;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeOwner implements Serializable {
    private Set<StateNode> dirtyNodes = new HashSet<>();

    public void adoptNodes(NodeOwner childOwner) {
        ArrayList<StateNode> copy = new ArrayList<>(childOwner.getNodes());
        copy.forEach(node -> node.setOwner(this));

        dirtyNodes.addAll(childOwner.dirtyNodes);
    }

    public int register(StateNode node) {
        assert node.getOwner() == this;

        markAsDirty(node);

        return doRegister(node);
    }

    protected abstract int doRegister(StateNode node);

    public abstract Collection<StateNode> getNodes();

    public void unregister(StateNode node) {
        assert node.getOwner() == this;
        dirtyNodes.remove(node);

        doUnregister(node);
    }

    public abstract void doUnregister(StateNode node);

    public void markAsDirty(StateNode node) {
        assert node.getOwner() == this;

        dirtyNodes.add(node);
    }

    public Set<StateNode> collectDirtyNodes() {
        Set<StateNode> collectedNodes = dirtyNodes;
        dirtyNodes = new HashSet<>();
        return collectedNodes;
    }
}

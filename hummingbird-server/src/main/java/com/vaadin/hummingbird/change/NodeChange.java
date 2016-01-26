package com.vaadin.hummingbird.change;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

public abstract class NodeChange implements Serializable {
    private final StateNode node;

    public NodeChange(StateNode node) {
        this.node = node;
    }

    public StateNode getNode() {
        return node;
    }
}

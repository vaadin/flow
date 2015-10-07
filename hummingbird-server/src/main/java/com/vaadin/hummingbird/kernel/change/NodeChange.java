package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public abstract class NodeChange {

    public abstract void accept(StateNode node, NodeChangeVisitor visitor);
}

package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class PutChange extends NodeDataChange {

    public PutChange(Object key, Object value) {
        super(key, value);
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitPutChange(node, this);
    }
}

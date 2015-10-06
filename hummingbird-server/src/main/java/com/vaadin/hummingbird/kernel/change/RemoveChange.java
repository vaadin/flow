package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class RemoveChange extends NodeDataChange {

    public RemoveChange(Object key, Object value) {
        super(key, value);
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitRemoveChange(node, this);
    }

}

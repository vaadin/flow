package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListInsertChange extends NodeListChange {

    public ListInsertChange(int index, Object key, Object value) {
        super(index, key, value);
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListInsertChange(node, this);
    }

}

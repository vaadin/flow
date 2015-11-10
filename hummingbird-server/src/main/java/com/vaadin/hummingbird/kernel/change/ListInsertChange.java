package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListInsertChange extends ListChange {

    public ListInsertChange(int index, Object value) {
        super(index, value);
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListInsertChange(node, this);
    }

}

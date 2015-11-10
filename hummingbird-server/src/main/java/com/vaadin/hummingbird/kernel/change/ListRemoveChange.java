package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListRemoveChange extends ListChange {

    public ListRemoveChange(int index, Object value) {
        super(index, value);
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListRemoveChange(node, this);
    }

}

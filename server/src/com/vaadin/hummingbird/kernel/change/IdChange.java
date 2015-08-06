package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class IdChange extends NodeChange {

    private int oldId;
    private int newId;

    public IdChange(int oldId, int newId) {
        this.oldId = oldId;
        this.newId = newId;
    }

    public int getOldId() {
        return oldId;
    }

    public int getNewId() {
        return newId;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitIdChange(node, this);
    }

}

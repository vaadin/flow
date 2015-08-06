package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ParentChange extends NodeChange {

    private StateNode oldParent;
    private StateNode newParent;

    public ParentChange(StateNode oldParent, StateNode newParent) {
        this.oldParent = oldParent;
        this.newParent = newParent;
    }

    public StateNode getOldParent() {
        return oldParent;
    }

    public StateNode getNewParent() {
        return newParent;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitParentChange(node, this);
    }

}

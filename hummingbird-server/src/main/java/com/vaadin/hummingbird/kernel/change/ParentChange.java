package com.vaadin.hummingbird.kernel.change;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else {
            ParentChange that = (ParentChange) obj;
            return Objects.equals(oldParent, that.oldParent)
                    && Objects.equals(newParent, that.newParent);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldParent, newParent);
    }

}

package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class RangeStartChange extends NodeChange {

    private int rangeStart;

    public RangeStartChange(int rangeStart) {
        super();
        this.rangeStart = rangeStart;
    }

    public int getRangeStart() {
        return rangeStart;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.rangeStartChange(node, this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [rangeStart=" + rangeStart + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else {
            return rangeStart == ((RangeStartChange) obj).rangeStart;
        }
    }

    @Override
    public int hashCode() {
        return rangeStart;
    }

}

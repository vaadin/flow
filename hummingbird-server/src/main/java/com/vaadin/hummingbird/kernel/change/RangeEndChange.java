package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class RangeEndChange extends NodeChange {

    private int rangeEnd;

    public RangeEndChange(int rangeEnd) {
        super();
        this.rangeEnd = rangeEnd;
    }

    public int getRangeEnd() {
        return rangeEnd;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.rangeEndChange(node, this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [rangeEnd=" + rangeEnd + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else {
            return rangeEnd == ((RangeEndChange) obj).rangeEnd;
        }
    }

    @Override
    public int hashCode() {
        return rangeEnd;
    }

}

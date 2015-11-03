package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class RangeStartChange extends NodeContentsChange {

    private int rangeStart;

    public RangeStartChange(Object key, int rangeStart) {
        super(key);
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
        return getClass().getSimpleName() + " [key=" + getKey()
                + ", rangeStart=" + rangeStart + "]";
    }

}

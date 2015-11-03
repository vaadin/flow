package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class RangeEndChange extends NodeContentsChange {

    private int rangeEnd;

    public RangeEndChange(Object key, int rangeEnd) {
        super(key);
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
        return getClass().getSimpleName() + " [key=" + getKey() + ", rangeEnd="
                + rangeEnd + "]";
    }

}

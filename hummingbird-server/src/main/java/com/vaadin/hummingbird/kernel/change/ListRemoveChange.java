package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListRemoveChange extends NodeChange {

    private int index;
    private Object value;
    private Object key;

    public ListRemoveChange(int index, Object key, Object value) {
        this.index = index;
        this.key = key;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListRemoveChange(node, this);
    }

}

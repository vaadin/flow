package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListInsertChange extends NodeChange {

    private int index;
    private Object value;
    private Object key;

    public ListInsertChange(int index, Object key, Object value) {
        this.index = index;
        this.key = key;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public Object getValue() {
        return value;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListInsertChange(node, this);
    }

}

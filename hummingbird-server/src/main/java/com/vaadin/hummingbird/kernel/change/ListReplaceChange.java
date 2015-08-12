package com.vaadin.hummingbird.kernel.change;

import com.vaadin.hummingbird.kernel.StateNode;

public class ListReplaceChange extends NodeChange {

    private int index;
    private Object oldValue;
    private Object newValue;
    private Object key;

    public ListReplaceChange(int index, Object key, Object oldValue,
            Object newValue) {
        this.index = index;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public int getIndex() {
        return index;
    }

    public Object getKey() {
        return key;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    @Override
    public void accept(StateNode node, NodeChangeVisitor visitor) {
        visitor.visitListReplaceChange(node, this);
    }

}

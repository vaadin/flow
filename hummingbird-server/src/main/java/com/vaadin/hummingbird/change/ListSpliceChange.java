package com.vaadin.hummingbird.change;

import java.util.List;

import com.vaadin.hummingbird.namespace.ListNamespace;

public class ListSpliceChange extends NamespaceChange {
    private final int index;
    private final int removeCount;
    private final List<?> newItems;

    public ListSpliceChange(ListNamespace namespace, int index, int removeCount,
            List<?> newItems) {
        super(namespace);
        this.index = index;
        this.removeCount = removeCount;
        this.newItems = newItems;
    }

    public int getIndex() {
        return index;
    }

    public int getRemoveCount() {
        return removeCount;
    }

    public List<?> getNewItems() {
        return newItems;
    }
}

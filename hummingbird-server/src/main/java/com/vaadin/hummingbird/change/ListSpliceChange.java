package com.vaadin.hummingbird.change;

import java.util.List;

import com.vaadin.hummingbird.namespace.ListNamespace;

/**
 * Change describing a splice operation on a list namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ListSpliceChange extends NamespaceChange {
    private final int index;
    private final int removeCount;
    private final List<?> newItems;

    /**
     * Creates a new splice change.
     *
     * @param namespace
     *            the changed namespace
     * @param index
     *            the index of the splice operation
     * @param removeCount
     *            the number of removed items
     * @param newItems
     *            a list of new items
     */
    public ListSpliceChange(ListNamespace namespace, int index, int removeCount,
            List<?> newItems) {
        super(namespace);
        this.index = index;
        this.removeCount = removeCount;
        this.newItems = newItems;
    }

    /**
     * Gets the index of the change
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the number of removed items
     *
     * @return the number of removed items
     */
    public int getRemoveCount() {
        return removeCount;
    }

    /**
     * Gets the newly added items
     *
     * @return a list of added items
     */
    public List<?> getNewItems() {
        return newItems;
    }
}

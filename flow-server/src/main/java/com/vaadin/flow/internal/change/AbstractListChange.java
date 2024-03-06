/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.change;

import java.io.Serializable;

import com.vaadin.flow.internal.nodefeature.NodeList;

/**
 * Change describing an operation (add/remove) in a {@link NodeList list} node
 * feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public abstract class AbstractListChange<T extends Serializable>
        extends NodeFeatureChange {

    private int index;
    private final NodeList<T> list;

    /**
     * Creates a new list change.
     *
     * @param list
     *            the changed list
     * @param index
     *            the index of the add operations
     */
    protected AbstractListChange(NodeList<T> list, int index) {
        super(list);
        this.list = list;
        this.index = index;
    }

    /**
     * Gets the index of the change.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets a changed list.
     *
     * @return the changed list
     */
    protected NodeList<T> getNodeList() {
        return list;
    }

    /**
     * Gets a copy of the change with the same data except {@code index}.
     *
     * @param index
     *            the new index of the change
     * @return a copy of the change based on new index
     */
    public abstract AbstractListChange<T> copy(int index);

    /**
     * Sets the index of this change in the change list.
     * <p>
     * Note: This should be used only when list of changes is being re-indexed
     * after adding a new change.
     *
     * @param index
     *            Integer value.
     */
    public void setIndex(int index) {
        assert (index > -1) : "Index can't be negative.";
        this.index = index;
    }
}

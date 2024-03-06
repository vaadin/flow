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

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;

/**
 * Change describing a remove operation in a {@link NodeList list} node feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public class ListRemoveChange<T extends Serializable>
        extends AbstractListChange<T> {

    private final T removedItem;

    /**
     * Creates a new list remove change.
     *
     *
     * @param list
     *            the changed list
     * @param index
     *            the index of the remove operation
     * @param removedItem
     *            a removed item
     */
    public ListRemoveChange(NodeList<T> list, int index, T removedItem) {
        super(list, index);
        this.removedItem = removedItem;
    }

    /**
     * Gets the removed item.
     * <p>
     * This method is public only for testing reasons.
     *
     * @return the removed item
     */
    public T getRemovedItem() {
        return removedItem;
    }

    @Override
    public AbstractListChange<T> copy(int indx) {
        return new ListRemoveChange<>(getNodeList(), indx, getRemovedItem());
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_SPLICE);

        super.populateJson(json, constantPool);

        json.put(JsonConstants.CHANGE_SPLICE_INDEX, getIndex());
        json.put(JsonConstants.CHANGE_SPLICE_REMOVE, 1);
    }
}

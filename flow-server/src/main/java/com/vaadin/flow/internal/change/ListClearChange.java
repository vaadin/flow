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
 * Change describing a clear operation in a {@link NodeList list} node feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the items in the node list
 */
public class ListClearChange<T extends Serializable>
        extends AbstractListChange<T> {

    /**
     * Creates a new list clear change.
     *
     *
     * @param list
     *            the changed list
     */
    public ListClearChange(NodeList<T> list) {
        super(list, -1);
    }

    @Override
    public AbstractListChange<T> copy(int indx) {
        return new ListClearChange<>(getNodeList());
    }

    @Override
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_CLEAR);
        super.populateJson(json, constantPool);
    }

}

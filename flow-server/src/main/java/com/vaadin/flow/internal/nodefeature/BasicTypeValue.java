/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;

import com.vaadin.flow.internal.StateNode;

/**
 * The feature contains a value of the basic type.
 * <p>
 * The value is wrapped into a {@link StateNode} and this feature instead being
 * sent directly. It allows to use basic types in lists.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class BasicTypeValue extends NodeValue<Serializable> {

    /**
     * Creates a new value map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public BasicTypeValue(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.VALUE;
    }

    /**
     * Sets the value of a basic type.
     *
     * @param value
     *            the value to set
     */
    @Override
    public void setValue(Serializable value) {
        super.setValue(value);
    }

    /**
     * Gets the value of a basic type.
     *
     * @return the value
     */
    @Override
    public Serializable getValue() {
        return super.getValue();
    }
}

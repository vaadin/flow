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

import elemental.json.JsonValue;

/**
 * Map of basic element information.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementData extends NodeMap {

    /**
     * Creates a new element data map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ElementData(StateNode node) {
        super(node);
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        put(NodeProperties.TAG, tag);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name
     */
    public String getTag() {
        return getOrDefault(NodeProperties.TAG, null);
    }

    /**
     * Sets the payload data of the element.
     *
     * @param payload
     *            the payload data
     */
    public void setPayload(JsonValue payload) {
        put(NodeProperties.PAYLOAD, payload);
    }

    /**
     * Set the visibility of the element.
     *
     * @param visible
     *            is the element visible or hidden
     */
    public void setVisible(boolean visible) {
        put(NodeProperties.VISIBLE, visible);
    }

    /**
     * Get element visibility.
     *
     * @return Element is visible by default
     */
    public boolean isVisible() {
        return !Boolean.FALSE.equals(get(NodeProperties.VISIBLE));
    }

    /**
     * Gets the payload data of the element.
     *
     * @return the payload data of the element
     */
    public JsonValue getPayload() {
        Serializable value = get(NodeProperties.PAYLOAD);
        return value == null ? null : (JsonValue) value;
    }

    @Override
    public boolean allowsChanges() {
        return isVisible();
    }
}

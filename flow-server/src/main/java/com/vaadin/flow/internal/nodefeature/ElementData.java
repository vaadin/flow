/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;

import com.vaadin.flow.internal.StateNode;

import elemental.json.JsonValue;

/**
 * Map of basic element information.
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

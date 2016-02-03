/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom;

import java.io.Serializable;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;

/**
 * Handles storing and retrieval of the state information for an element using a
 * state node.
 *
 * @author Vaadin
 * @since
 */
public interface ElementStateProvider extends Serializable {

    /**
     * Checks if the element state provider supports the given state node.
     *
     * @param node
     *            the state node to check
     * @return true if the element state provider is compatible with the given
     *         state node, false otherwise
     */
    boolean supports(StateNode node);

    /**
     * Gets the tag name for the given node.
     *
     * @param node
     *            the node containing the data
     * @return the tag name
     */
    String getTag(StateNode node);

    /**
     * Sets the given attribute to the given value.
     *
     * @param node
     *            the node containing the data
     * @param attribute
     *            the attribute name
     * @param value
     *            the attribute value
     */
    void setAttribute(StateNode node, String attribute, String value);

    /**
     * Gets the value of the given attribute.
     *
     * @param node
     *            the node containing the data
     * @param attribute
     *            the attribute name
     * @return the attribute value or null if the attribute has not been set
     */
    String getAttribute(StateNode node, String attribute);

    /**
     * Checks if the given attribute has been set.
     *
     * @param node
     *            the node containing the data
     * @param attribute
     *            the attribute name
     * @return true if the attribute has been set, false otherwise
     */
    boolean hasAttribute(StateNode node, String attribute);

    /**
     * Removes the given attribute if it has been set.
     *
     * @param node
     *            the node containing the data
     * @param attribute
     *            the attribute name
     */
    void removeAttribute(StateNode node, String attribute);

    /**
     * Gets the defined attributes names.
     *
     * @param node
     *            the node containing the data
     * @return the defined attribute names
     */
    Set<String> getAttributeNames(StateNode node);

}

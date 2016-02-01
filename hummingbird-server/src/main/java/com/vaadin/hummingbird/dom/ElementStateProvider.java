package com.vaadin.hummingbird.dom;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

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
     * Gets the tag name for the given node
     *
     * @param node
     *            the node containing the data
     * @return the tag name
     */
    String getTag(StateNode node);

}

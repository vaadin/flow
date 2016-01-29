package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for basic element information.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementDataNamespace extends MapNamespace {

    private static final String TAG = "tag";

    /**
     * Creates a new element data namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     *
     */
    public ElementDataNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        put(TAG, tag);
    }

    /**
     * Gets the tag name of the element
     * 
     * @return the tag name
     */
    public String getTag() {
        return (String) get(TAG);
    }
}

package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for nodes describing the child elements of an element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementChildrenNamespace extends ListNamespace {

    /**
     * Creates a new element children namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ElementChildrenNamespace(StateNode node) {
        super(node);
    }

}

package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for nodes describing the child elements of an element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementChildrenNamespace extends ListNamespace<StateNode> {
    /**
     * Creates a new element children namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ElementChildrenNamespace(StateNode node) {
        super(node, true);
    }

    @Override
    public void add(int index, StateNode node) {
        super.add(index, node);
    }

    @Override
    public StateNode get(int index) {
        return super.get(index);
    }
}

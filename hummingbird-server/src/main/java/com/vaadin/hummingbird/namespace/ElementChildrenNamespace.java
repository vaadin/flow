package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

public class ElementChildrenNamespace extends ListNamespace<StateNode> {

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

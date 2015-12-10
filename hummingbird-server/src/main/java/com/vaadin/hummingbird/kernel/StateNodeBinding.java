package com.vaadin.hummingbird.kernel;

public class StateNodeBinding implements Binding {
    private String propertyName;

    public StateNodeBinding(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Object getValue(StateNode node) {
        return node.get(propertyName);
    }
}
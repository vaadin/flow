package com.vaadin.hummingbird.kernel;

@FunctionalInterface
public interface Binding {
    public Object getValue(StateNode node);
}

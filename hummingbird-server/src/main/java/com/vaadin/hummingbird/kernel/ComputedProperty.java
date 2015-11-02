package com.vaadin.hummingbird.kernel;

public abstract class ComputedProperty {

    private final String name;

    public ComputedProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Object compute(StateNode context);

}

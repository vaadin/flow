package com.vaadin.hummingbird.kernel;

public abstract class ComputedProperty {

    private final String name;
    private final String clientCode;

    public ComputedProperty(String name, String clientCode) {
        this.name = name;
        this.clientCode = clientCode;
    }

    public String getName() {
        return name;
    }

    public String getClientCode() {
        return clientCode;
    }

    public boolean hasClientCode() {
        return clientCode != null;
    }

    public abstract Object compute(StateNode context);

}

package com.vaadin.hummingbird.kernel;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ComputedProperty that = (ComputedProperty) obj;

        return Objects.equals(name, that.name)
                && Objects.equals(clientCode, that.clientCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, clientCode);
    }

}

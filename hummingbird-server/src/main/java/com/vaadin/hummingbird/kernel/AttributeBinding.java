package com.vaadin.hummingbird.kernel;

public abstract class AttributeBinding {
    private String attributeName;

    public AttributeBinding(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public abstract Object getValue(StateNode node);
}

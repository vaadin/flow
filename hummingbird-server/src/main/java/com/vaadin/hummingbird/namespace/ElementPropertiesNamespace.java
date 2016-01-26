package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

public class ElementPropertiesNamespace extends MapNamespace {

    public ElementPropertiesNamespace(StateNode node) {
        super(node);
    }

    public void setProperty(String name, String value) {
        put(name, value);
    }

    public boolean hasProperty(String name) {
        return contains(name);
    }

    @Override
    public void remove(String name) {
        super.remove(name);
    }

    public String getProperty(String name) {
        return (String) get(name);
    }
}

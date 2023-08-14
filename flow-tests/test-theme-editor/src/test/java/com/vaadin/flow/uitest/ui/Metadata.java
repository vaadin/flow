package com.vaadin.flow.uitest.ui;

import java.util.List;

public class Metadata {

    private String selector;
    private String displayName;
    private List<Property> properties;

    public String getSelector() {
        return selector;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Property> getProperties() {
        return properties;
    }
}

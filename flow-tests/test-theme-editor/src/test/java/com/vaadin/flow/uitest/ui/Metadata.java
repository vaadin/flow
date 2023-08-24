package com.vaadin.flow.uitest.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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

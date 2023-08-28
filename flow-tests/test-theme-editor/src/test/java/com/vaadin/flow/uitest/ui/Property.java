package com.vaadin.flow.uitest.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Property {

    private String propertyName;
    private String displayName;
    private String editorType;

    public String getPropertyName() {
        return propertyName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEditorType() {
        return editorType;
    }
}

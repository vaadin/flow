package com.vaadin.base.devserver.themeeditor.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CssRule {

    private String selector;

    private Map<String, String> properties;

    public CssRule() {
        properties = new HashMap<>();
    }

    public CssRule(String selector, Map<String, String> properties) {
        this.selector = selector;
        this.properties = properties;
    }

    public CssRule(String selector, String property, String value) {
        this.selector = selector;
        this.properties = Collections.singletonMap(property, value);
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

}

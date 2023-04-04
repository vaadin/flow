package com.vaadin.base.devserver.themeeditor.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CssRule implements Cloneable {

    private String selector;

    private Map<String, String> properties;

    public CssRule() {
        properties = new HashMap<>();
    }

    public CssRule(String selector, Map<String, String> properties) {
        this.selector = selector;
        this.properties = Collections.unmodifiableMap(properties);
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

    @Override
    public CssRule clone() {
        return new CssRule(selector, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CssRule cssRule = (CssRule) o;
        return Objects.equals(selector, cssRule.selector)
                && Objects.equals(properties, cssRule.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CssRule[");
        sb.append(selector + "{");
        properties.forEach(
                (k, v) -> sb.append(k).append(":").append(v).append(";"));
        return sb.append("}]").toString();
    }
}

package com.vaadin.base.devserver.themeeditor.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CssRule implements Cloneable {

    private String tagName;

    private String partName;

    private String className;

    private Map<String, String> properties;

    public CssRule() {
        properties = new HashMap<>();
    }

    public CssRule(String tagName, String partName, String className,
            Map<String, String> properties) {
        this.tagName = tagName;
        this.partName = partName;
        this.className = className;
        this.properties = Collections.unmodifiableMap(properties);
    }

    public CssRule(String tagName, String property, String value) {
        this(tagName, null, null, property, value);
    }

    public CssRule(String tagName, String partName, String property,
            String value) {
        this(tagName, partName, null, property, value);
    }

    public CssRule(String tagName, String partName, String className,
            String property, String value) {
        this.tagName = tagName;
        this.partName = partName;
        this.className = className;
        this.properties = Collections.singletonMap(property, value);
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    @JsonIgnore
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @JsonIgnore
    public String getSelector() {
        StringBuilder sb = new StringBuilder(tagName);
        if (className != null) {
            sb.append(".").append(className);
        }
        if (partName != null) {
            sb.append("::part(").append(partName).append(")");
        }
        return sb.toString();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public CssRule clone() {
        return new CssRule(tagName, partName, className, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CssRule cssRule = (CssRule) o;
        return Objects.equals(tagName, cssRule.tagName)
                && Objects.equals(partName, cssRule.partName)
                && Objects.equals(className, cssRule.className)
                && Objects.equals(properties, cssRule.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagName, partName, className, properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CssRule[");
        sb.append(getSelector() + "{");
        properties.forEach(
                (k, v) -> sb.append(k).append(":").append(v).append(";"));
        return sb.append("}]").toString();
    }
}

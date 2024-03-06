/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom.impl;

import java.util.stream.Stream;

import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.StyleUtil;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;

/**
 * Implementation of {@link Style} for {@link BasicElementStateProvider}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BasicElementStyle implements Style {

    private ElementStylePropertyMap propertyMap;

    /**
     * Creates an instance connected to the given map.
     *
     * @param propertyMap
     *            the feature where the data is stored
     */
    public BasicElementStyle(ElementStylePropertyMap propertyMap) {
        this.propertyMap = propertyMap;
    }

    @Override
    public Style set(String name, String value) {
        ElementUtil.validateStylePropertyName(name);
        if (value == null) {
            return this.remove(name);
        }
        String trimmedValue = value.trim();
        ElementUtil.validateStylePropertyValue(trimmedValue);

        propertyMap.setProperty(StyleUtil.stylePropertyToAttribute(name),
                trimmedValue, true);
        return this;
    }

    @Override
    public Style remove(String name) {
        ElementUtil.validateStylePropertyName(name);

        propertyMap.removeProperty(StyleUtil.stylePropertyToAttribute(name));
        return this;
    }

    @Override
    public Style clear() {
        propertyMap.removeAllProperties();
        return this;
    }

    @Override
    public String get(String name) {
        ElementUtil.validateStylePropertyName(name);

        return (String) propertyMap
                .getProperty(StyleUtil.stylePropertyToAttribute(name));
    }

    @Override
    public Stream<String> getNames() {
        return propertyMap.getPropertyNames();
    }

    @Override
    public boolean has(String name) {
        return propertyMap
                .hasProperty(StyleUtil.stylePropertyToAttribute(name));
    }
}

/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.dom.impl;

import java.util.stream.Stream;

import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.StyleUtil;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;

/**
 * Implementation of {@link Style} for {@link BasicElementStateProvider}.
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

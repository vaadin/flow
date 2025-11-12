/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.StyleUtil;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Implementation of {@link Style} for {@link BasicElementStateProvider}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class BasicElementStyle implements Style {

    private final ElementStylePropertyMap propertyMap;

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
        String attr = StyleUtil.stylePropertyToAttribute(name);
        if (propertyMap.hasSignal(attr)) {
            throw new BindingActiveException("Style '" + name
                    + "' is bound and cannot be modified manually");
        }
        if (value == null) {
            return this.remove(name);
        }
        String trimmedValue = value.trim();
        ElementUtil.validateStylePropertyValue(trimmedValue);

        propertyMap.setProperty(attr, trimmedValue, true);
        return this;
    }

    @Override
    public Style remove(String name) {
        ElementUtil.validateStylePropertyName(name);
        String attr = StyleUtil.stylePropertyToAttribute(name);
        if (propertyMap.hasSignal(attr)) {
            throw new BindingActiveException("Style '" + name
                    + "' is bound and cannot be modified manually");
        }
        propertyMap.removeProperty(attr);
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
    public Style bind(String name, Signal<String> value) {
        ElementUtil.validateStylePropertyName(name);
        String attr = StyleUtil.stylePropertyToAttribute(name);
        Element owner = Element.get(propertyMap.getNode());
        propertyMap.bindSignal(owner, attr, value);
        return this;
    }

    @Override
    public boolean has(String name) {
        return propertyMap
                .hasProperty(StyleUtil.stylePropertyToAttribute(name));
    }
}

/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom.impl;

import java.util.stream.Stream;

import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.namespace.ElementStylePropertyNamespace;

/**
 * Implementation of {@link Style} for {@link BasicElementStateProvider}.
 *
 * @author Vaadin Ltd
 * @since
 */
public class BasicElementStyle implements Style {

    private ElementStylePropertyNamespace namespace;

    /**
     * Creates an instance connected to the given namespace.
     *
     * @param namespace
     *            the namespace where the data is stored
     */
    public BasicElementStyle(ElementStylePropertyNamespace namespace) {
        this.namespace = namespace;
    }

    @Override
    public Style set(String name, String value) {
        ElementUtil.validateStylePropertyName(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    ElementUtil.A_STYLE_VALUE_CANNOT_BE_NULL);
        }
        String trimmedValue = value.trim();
        ElementUtil.validateStylePropertyValue(trimmedValue);

        namespace.setProperty(name, trimmedValue, true);
        return this;
    }

    @Override
    public Style remove(String name) {
        ElementUtil.validateStylePropertyName(name);

        namespace.removeProperty(name);
        return this;
    }

    @Override
    public Style clear() {
        namespace.removeAllProperties();
        return this;
    }

    @Override
    public String get(String name) {
        ElementUtil.validateStylePropertyName(name);

        return (String) namespace.getProperty(name);
    }

    @Override
    public Stream<String> getNames() {
        return namespace.getPropertyNames();
    }

    @Override
    public boolean has(String name) {
        return namespace.hasProperty(name);
    }
}

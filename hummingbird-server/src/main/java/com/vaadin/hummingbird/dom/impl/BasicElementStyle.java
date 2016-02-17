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

import java.util.Collections;
import java.util.Set;

import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.namespace.ElementStylePropertyNamespace;

/**
 * Implementation of {@link Style} for {@link BasicElementStateProvider}.
 *
 * @author Vaadin
 * @since
 */
public class BasicElementStyle implements Style {

    private static final String STYLE_VALUE_CANNOT_END_IN_A_SEMICOLON = "A style value cannot end in a semicolon";
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
    public void set(String name, String value) {
        ElementUtil.validateStylePropertyName(name);
        ElementUtil.validateStylePropertyValue(value);
        String trimmedValue = value.trim();
        if (trimmedValue.endsWith(";")) {
            throw new IllegalArgumentException(
                    STYLE_VALUE_CANNOT_END_IN_A_SEMICOLON);
        }

        namespace.setProperty(name, trimmedValue);
    }

    @Override
    public void remove(String name) {
        ElementUtil.validateStylePropertyName(name);

        namespace.removeProperty(name);
    }

    @Override
    public void clear() {
        namespace.removeAllProperties();
    }

    @Override
    public String get(String name) {
        ElementUtil.validateStylePropertyName(name);

        return (String) namespace.getProperty(name);
    }

    @Override
    public Set<String> getNames() {
        // Intentionally not making a copy for performance reasons
        return Collections.unmodifiableSet(namespace.getPropertyNames());
    }

    @Override
    public boolean has(String name) {
        return namespace.hasProperty(name);
    }
}

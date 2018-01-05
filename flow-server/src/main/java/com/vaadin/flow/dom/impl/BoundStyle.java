/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.StyleUtil;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.template.angular.ElementTemplateNode;

/**
 * Handles inline styles for a template element.
 *
 * @author Vaadin Ltd
 */
public class BoundStyle implements Style {

    private final LinkedHashMap<String, String> staticStyles;

    /**
     * Creates a new style holder for the given template node using data from
     * the given state node.
     *
     * @param templateNode
     *            the template node
     * @param node
     *            the state node
     */
    public BoundStyle(ElementTemplateNode templateNode, StateNode node) {
        String styleAttribute = templateNode.getAttributeBinding("style")
                .map(binding -> binding.getValue(node, "")).orElse("");
        if (!styleAttribute.isEmpty()) {
            staticStyles = StyleAttributeHandler.parseStyles(styleAttribute);
        } else {
            staticStyles = new LinkedHashMap<>();
        }
    }

    @Override
    public String get(String styleProperty) {
        return staticStyles
                .get(StyleUtil.styleAttributeToProperty(styleProperty));
    }

    @Override
    public Style set(String styleProperty, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Style remove(String styleProperty) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean has(String styleProperty) {
        return staticStyles
                .containsKey(StyleUtil.styleAttributeToProperty(styleProperty));
    }

    @Override
    public Stream<String> getNames() {
        return staticStyles.keySet().stream();
    }

    @Override
    public Style clear() {
        throw new UnsupportedOperationException();
    }
}

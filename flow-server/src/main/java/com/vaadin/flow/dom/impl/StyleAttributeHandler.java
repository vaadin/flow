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

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.StyleUtil;
import com.vaadin.flow.internal.nodefeature.ElementStylePropertyMap;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Emulates the <code>style</code> attribute by delegating to
 * {@link Element#getStyle()}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class StyleAttributeHandler extends CustomAttribute {

    private static final String ERROR_PARSING_STYLE = "Error parsing style '%s': %s";

    @Override
    public boolean hasAttribute(Element element) {
        return element.getStyle().getNames().findAny().isPresent();
    }

    @Override
    public String getAttribute(Element element) {
        if (!hasAttribute(element)) {
            return null;
        }
        if (element.getStyle() instanceof BasicElementStyle style) {
            return style.getNames().map(styleName -> {
                return StyleUtil.stylePropertyToAttribute(styleName) + ":"
                        + style.get(styleName);
            }).collect(Collectors.joining(";"));
        }
        return null;
    }

    @Override
    public void setAttribute(Element element, String attributeValue) {
        if (element.getNode().getFeature(ElementStylePropertyMap.class)
                .getSignal() != null) {
            throw new BindingActiveException(
                    "setAttribute is not allowed while binding is active.");
        }

        if (element.getStyle() instanceof BasicElementStyle style) {
            ElementStylePropertyMap map = element.getNode()
                    .getFeature(ElementStylePropertyMap.class);
            if (map.getSignal() != null) {
                // remove any existing binding
                map.bindSignal(element, null);
            }

            style.clear(false);
            parseStyles(attributeValue)
                    .forEach((name, value) -> style.set(name, value, false));
        }
    }

    @Override
    public void bindSignal(Element element, Signal<String> signal) {
        element.getNode().getFeature(ElementStylePropertyMap.class)
                .bindSignal(element, signal);
    }

    private static final char COLON = ':';
    private static final char SEMICOLON = ';';
    private static final char PARENTHESIS_OPEN = '(';
    private static final char PARENTHESIS_CLOSED = ')';

    /**
     * Parses the given style string and populates the given style object with
     * the found styles.
     *
     * @param styleString
     *            the string to parse
     * @return a map containing the found style rules
     */
    public static LinkedHashMap<String, String> parseStyles(
            String styleString) {
        try {
            LinkedHashMap<String, String> parsedStyles = new LinkedHashMap<>();
            StringBuilder nameBuffer = new StringBuilder();
            StringBuilder valueBuffer = new StringBuilder();
            boolean nameRead = false;
            int parenthesisOpen = 0;
            for (int i = 0; i < styleString.length(); i++) {
                char c = styleString.charAt(i);
                if (nameRead) {
                    boolean valueTerminated = false;
                    if (c == PARENTHESIS_OPEN) {
                        parenthesisOpen++;
                    } else if (c == PARENTHESIS_CLOSED) {
                        parenthesisOpen--;
                    } else if (parenthesisOpen == 0 && c == SEMICOLON) {
                        valueTerminated = true;
                    }
                    if (valueTerminated) {
                        addRule(nameBuffer, valueBuffer, parsedStyles);
                        nameBuffer = new StringBuilder();
                        valueBuffer = new StringBuilder();
                        nameRead = false;
                    } else {
                        valueBuffer.append(c);
                    }
                } else {
                    if (c == COLON) {
                        nameRead = true;
                    } else {
                        nameBuffer.append(c);
                    }
                }
            }
            if (nameRead) {
                addRule(nameBuffer, valueBuffer, parsedStyles);
            } else if (!nameBuffer.isEmpty()) {
                throw new IllegalArgumentException(
                        "Value for CSS rule was not found.");
            }
            return parsedStyles;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    protected static void addRule(StringBuilder nameBuffer,
            StringBuilder valueBuffer,
            LinkedHashMap<String, String> parsedStyles)
            throws IllegalArgumentException {
        var name = nameBuffer.toString().trim();
        var value = valueBuffer.toString().trim();
        if (name.isEmpty() || value.isEmpty()) {
            throw new IllegalArgumentException(
                    "Style rule must contain name and value");
        }
        parsedStyles.put(name, value);
    }

    @Override
    public void removeAttribute(Element element) {
        if (element.getNode().getFeature(ElementStylePropertyMap.class)
                .getSignal() != null) {
            throw new BindingActiveException(
                    "removeAttribute is not allowed while binding is active.");
        }
        if (element.getStyle() instanceof BasicElementStyle style) {
            style.clear();
        }
    }
}

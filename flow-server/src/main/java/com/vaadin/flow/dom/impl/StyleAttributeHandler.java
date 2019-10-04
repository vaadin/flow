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

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.CSSDeclarationList;
import com.helger.css.reader.CSSReaderDeclarationList;
import com.helger.css.reader.errorhandler.CollectingCSSParseErrorHandler;
import com.helger.css.writer.CSSWriterSettings;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.dom.StyleUtil;

/**
 * Emulates the <code>style</code> attribute by delegating to
 * {@link Element#getStyle()}.
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
        Style style = element.getStyle();

        return style.getNames().map(styleName -> {
            return StyleUtil.stylePropertyToAttribute(styleName) + ":"
                    + style.get(styleName);
        }).collect(Collectors.joining(";"));
    }

    @Override
    public void setAttribute(Element element, String attributeValue) {
        Style style = element.getStyle();
        style.clear();
        parseStyles(attributeValue).forEach(style::set);
    }

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
        CollectingCSSParseErrorHandler errorCollector = new CollectingCSSParseErrorHandler();
        CSSDeclarationList parsed = CSSReaderDeclarationList.readFromString(
                styleString, ECSSVersion.LATEST, errorCollector);
        if (errorCollector.hasParseErrors()) {
            throw new IllegalArgumentException(String
                    .format(ERROR_PARSING_STYLE, styleString, errorCollector
                            .getAllParseErrors().get(0).getErrorMessage()));
        }
        if (parsed == null) {
            // Did not find any styles
            throw new IllegalArgumentException(String.format(
                    ERROR_PARSING_STYLE, styleString, "No styles found"));
        }

        LinkedHashMap<String, String> parsedStyles = new LinkedHashMap<>();
        for (CSSDeclaration declaration : parsed.getAllDeclarations()) {
            String key = declaration.getProperty();
            String value = declaration.getExpression()
                    .getAsCSSString(new CSSWriterSettings(ECSSVersion.LATEST)
                            .setOptimizedOutput(true), 0);
            parsedStyles.put(StyleUtil.styleAttributeToProperty(key), value);
        }

        return parsedStyles;
    }

    @Override
    public void removeAttribute(Element element) {
        element.getStyle().clear();
    }
}

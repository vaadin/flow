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
package com.vaadin.flow.dom;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;

/**
 * Provides utility methods for {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementUtil {

    /**
     * Pattern for matching valid tag names, according to
     * https://www.w3.org/TR/html-markup/syntax.html#tag-name "HTML elements all
     * have names that only use characters in the range 0–9, a–z, and A–Z."
     */
    private static Pattern tagNamePattern = Pattern.compile("^[a-zA-Z0-9-]+$");

    private ElementUtil() {
        // Util methods only
    }

    /**
     * Checks if the given tag name is valid.
     *
     * @param tag
     *            the tag name
     * @return true if the string is valid as a tag name, false otherwise
     */
    public static boolean isValidTagName(String tag) {
        return tag != null && tagNamePattern.matcher(tag).matches();
    }

    /**
     * Checks if the given attribute name is valid.
     *
     * @param attribute
     *            the name of the attribute in lower case
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidAttributeName(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return false;
        }
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        // https://html.spec.whatwg.org/multipage/syntax.html#attributes-2
        // Attribute names must consist of one or more characters other than the
        // space characters, U+0000 NULL, U+0022 QUOTATION MARK ("), U+0027
        // APOSTROPHE ('), U+003E GREATER-THAN SIGN (>), U+002F SOLIDUS (/), and
        // U+003D EQUALS SIGN (=) characters, the control characters, and any
        // characters that are not defined by Unicode.
        char[] illegalCharacters = new char[] { 0, ' ', '"', '\'', '>', '/',
                '=' };
        for (char c : illegalCharacters) {
            if (attribute.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the given style property name and throws an exception if the
     * name is invalid.
     *
     * @param name
     *            the style property name to validate
     */
    public static void validateStylePropertyName(String name) {
        String reason = getInvalidStylePropertyNameError(name);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
    }

    private static String getInvalidStylePropertyNameError(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "A style property name cannot be null or empty";
        }

        if (name.startsWith(" ") || name.endsWith(" ")) {
            return String.format(
                    "Invalid style property name '%s': a style property name cannot start or end in whitespace",
                    name);
        }

        if (name.contains(":")) {
            return String.format(
                    "Invalid style property name '%s': a style property name cannot contain colons",
                    name);
        }

        return null;
    }

    /**
     * Checks if the given style property name is valid.
     *
     * @param name
     *            the name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidStylePropertyName(String name) {
        return getInvalidStylePropertyNameError(name) == null;
    }

    /**
     * Checks if the given style property value is valid.
     *
     * @param value
     *            the value to validate
     * @return true if the value is valid, false otherwise
     */
    public static boolean isValidStylePropertyValue(String value) {
        return getInvalidStylePropertyValueError(value) == null;
    }

    /**
     * Checks if the given style property value is valid.
     * <p>
     * Throws an exception if it's certain the value is invalid
     *
     * @param value
     *            the value to validate
     */
    public static void validateStylePropertyValue(String value) {
        String reason = getInvalidStylePropertyValueError(value);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
    }

    private static String getInvalidStylePropertyValueError(String value) {
        if (value.endsWith(";")) {
            return "A style value cannot end in semicolon";
        }
        return null;
    }

    /**
     * Defines a mapping between this element and the given {@link Component}.
     * <p>
     * An element can only be mapped to one component and the mapping cannot be
     * changed. The only exception is {@link Composite} which can overwrite the
     * mapping for its content.
     *
     * @param element
     *            the element to map to the component
     * @param component
     *            the component this element is attached to
     */
    public static void setComponent(Element element, Component component) {
        if (element == null) {
            throw new IllegalArgumentException("Element must not be null");
        }
        if (component == null) {
            throw new IllegalArgumentException("Component must not be null");
        }

        Optional<Component> currentComponent = element.getComponent();
        if (currentComponent.isPresent()) {
            // Composite can replace its content
            boolean isCompositeReplacingItsContent = component instanceof Composite
                    && component.getChildren().findFirst()
                            .get() == currentComponent.get();
            if (!isCompositeReplacingItsContent) {
                throw new IllegalStateException("A component of type "
                        + currentComponent.get().getClass().getName()
                        + " is already attached to this element");
            }
        }
        element.getStateProvider().setComponent(element.getNode(), component);
    }

    /**
     * Converts the given element and its children to a JSoup node with
     * children.
     *
     * @param document
     *            A JSoup document
     * @param element
     *            The element to convert
     * @return A JSoup node containing the converted element
     */
    public static Node toJsoup(Document document, Element element) {
        if (element.isTextNode()) {
            return new TextNode(element.getText(), document.baseUri());
        }

        org.jsoup.nodes.Element target = document
                .createElement(element.getTag());
        if (element.hasProperty("innerHTML")) {
            target.html((String) element.getPropertyRaw("innerHTML"));
        }

        element.getAttributeNames().forEach(name -> {
            String attributeValue = element.getAttribute(name);
            if ("".equals(attributeValue)) {
                target.attr(name, true);
            } else {
                target.attr(name, attributeValue);
            }
        });

        element.getChildren()
                .forEach(child -> target.appendChild(toJsoup(document, child)));

        return target;
    }

    /**
     * Checks whether the given element is a custom element or not.
     * <p>
     * Custom elements (Web Components) are recognized by having at least one
     * dash in the tag name.
     *
     * @param element
     *            the element to check
     * @return <code>true</code> if a custom element, <code>false</code> if not
     */
    public static boolean isCustomElement(Element element) {
        return !element.isTextNode() && element.getTag().contains("-");
    }

    /**
     * Checks whether the given element is a <code>script</code> or not.
     *
     * @param element
     *            the element to check
     * @return <code>true</code> if a script, <code>false</code> if not
     */
    public static boolean isScript(Element element) {
        return !element.isTextNode()
                && "script".equalsIgnoreCase(element.getTag());
    }

}

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
package com.vaadin.hummingbird.dom;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Provides utility methods for {@link Element}.
 *
 * @author Vaadin
 * @since
 */
public class ElementUtil {

    /**
     * Pattern for maching valid tag names, according to
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
            return "A style property name cannot start or end in whitespace";
        }

        if (name.contains(":")) {
            return "A style property name cannot contain colons";
        }

        if (name.contains("-")) {
            return "A style property name cannot contain dashes. Use the camelCase style property name.";
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
     * Checks if the given property value is valid.
     *
     * @param value
     *            the value to validate
     * @return true if the value is valid, false otherwise
     */
    public static boolean isValidPropertyValue(Serializable value) {
        if (value == null) {
            return true;
        }

        if (value instanceof String) {
            return true;
        } else if (value instanceof Boolean) {
            return true;
        } else if (value instanceof Double) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the given style property value is valid.
     *
     * @param value
     *            the value to validate
     * @return true if the value is valid, false otherwise
     */
    public static boolean isValidStylePropertyValue(String value) {
        if (value == null) {
            return false;
        }

        return !value.endsWith(";");
    }

    /**
     * Checks if the given style property value is valid.
     * <p>
     * Throws an exception if it's certain the value is invalid
     *
     * @param value
     *            the value
     */
    public static void validateStylePropertyValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("A style value cannot be null");
        }
    }

}

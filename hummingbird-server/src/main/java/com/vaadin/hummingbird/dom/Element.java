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
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;

/**
 * A class representing an element in the DOM.
 * <p>
 * Contains methods for updating and querying various parts of the element, such
 * as attributes.
 *
 * @author Vaadin
 * @since
 */
public class Element implements Serializable {

    private static final String ATTRIBUTE_NAME_CANNOT_BE_NULL = "The attribute name cannot be null";

    private ElementStateProvider stateProvider;
    private StateNode node;
    private static HashMap<String, String> unsettableAttributes = new HashMap<>();

    /**
     * Pattern for maching valid tag names, according to
     * https://www.w3.org/TR/html-markup/syntax.html#tag-name "HTML elements all
     * have names that only use characters in the range 0–9, a–z, and A–Z."
     */
    private static Pattern tagNamePattern = Pattern.compile("^[a-zA-Z0-9-]+$");

    static {
        unsettableAttributes.put("is",
                "it must be set when constructing the element");
    }

    /**
     * Creates an element using the given tag name.
     *
     * @param tag
     *            the tag name of the element. Must be a non-empty string and
     *            can contain letters, numbers and dashes ({@literal -})
     */
    public Element(String tag) {
        this(tag, null);
    }

    /**
     * Creates an element using the given tag name and {@code is} attribute.
     *
     * @param tag
     *            the tag name of the element. Must be a non-empty string and
     *            can contain letters, numbers and dashes ({@literal -})
     * @param is
     *            the {@code is} attribute, describing the type of a custom
     *            element when using a standard tag name, e.g.
     *            {@literal my-element}
     */
    public Element(String tag, String is) {
        init(tag, is);
    }

    /**
     * Initializes the element using the given tag and {@code is} attribute.
     *
     * @param tag
     *            the tag name of the element.
     * @param is
     *            the {@code is} attribute or null
     */
    private void init(String tag, String is) {
        if (!isValidTagName(tag)) {
            throw new IllegalArgumentException(
                    "Tag " + tag + " is not a valid tag name");
        }
        if (is != null && is.isEmpty()) {
            throw new IllegalArgumentException(
                    "The is attribute cannot be empty");
        }
        stateProvider = BasicElementStateProvider.get();
        if (is == null) {
            node = BasicElementStateProvider.createStateNode(tag);
        } else {
            node = BasicElementStateProvider.createStateNode(tag, is);
        }

        assert stateProvider.supports(node);
    }

    /**
     * Gets the node this element is connected to.
     *
     * @return the node for this element
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Gets the tag name for the element.
     *
     * @param node
     *            the node containing the data
     * @return the tag name
     */
    public String getTag() {
        return stateProvider.getTag(node);
    }

    /**
     * Sets the given attribute to the given value.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * An attribute always has a String key and a String value.
     * <p>
     * Note: An empty attribute value ({@literal ""}) will be rendered as
     * {@literal <div something>} and not {@literal <div something="">}.
     *
     * @param attribute
     *            the name of the attribute
     * @param value
     *            the value of the attribute, not null
     * @return this element
     */
    public Element setAttribute(String attribute, String value) {
        if (attribute == null) {
            throw new IllegalArgumentException(ATTRIBUTE_NAME_CANNOT_BE_NULL);
        }

        String lowerCaseAttribute = attribute.toLowerCase(Locale.ENGLISH);
        if (!isValidAttributeName(lowerCaseAttribute)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute \"%s\" is not a valid attribute name",
                    lowerCaseAttribute));
        }

        if (unsettableAttributes.containsKey(lowerCaseAttribute)) {
            throw new IllegalArgumentException("You cannot set the attribute \""
                    + attribute + "\" for an element using setAttribute: "
                    + unsettableAttributes.get(attribute));
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        stateProvider.setAttribute(node, lowerCaseAttribute, value);
        return this;
    }

    /**
     * Gets the value of the given attribute.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * An attribute always has a String key and a String value.
     *
     * @param attribute
     *            the name of the attribute
     * @return the value of the attribute or null if the attribute has not been
     *         set
     */
    public String getAttribute(String attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException(ATTRIBUTE_NAME_CANNOT_BE_NULL);
        }
        return stateProvider.getAttribute(node,
                attribute.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Checks if the given attribute has been set.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     *
     * @param attribute
     *            the name of the attribute
     * @return true if the attribute has been set, false otherwise
     */
    public boolean hasAttribute(String attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException(ATTRIBUTE_NAME_CANNOT_BE_NULL);
        }
        return stateProvider.hasAttribute(node,
                attribute.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Gets the defined attribute names.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * The returned collection might be connected so that adding/removing
     * attributes through {@link #setAttribute(String, String)} or
     * {@link #removeAttribute(String)} affects the collection. If you want to
     * store the attribute names for later usage, you should make a copy.
     * <p>
     * You cannot modify attributes through the returned set.
     *
     * @return the defined attribute names
     */
    public Set<String> getAttributeNames() {
        // Intentionally not making a copy for performance reasons
        return Collections
                .unmodifiableSet(stateProvider.getAttributeNames(node));
    }

    /**
     * Removes the given attribute.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * If the attribute has not been set, does nothing.
     *
     * @param attribute
     *            the name of the attribute
     * @return this element
     */
    public Element removeAttribute(String attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException(ATTRIBUTE_NAME_CANNOT_BE_NULL);
        }
        stateProvider.removeAttribute(node,
                attribute.toLowerCase(Locale.ENGLISH));
        return this;
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
        Character[] illegalCharacters = new Character[] { 0, ' ', '"', '\'',
                '>', '/', '=' };
        for (Character c : illegalCharacters) {
            if (attribute.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }

}
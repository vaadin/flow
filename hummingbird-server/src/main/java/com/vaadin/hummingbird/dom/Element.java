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
import java.util.Locale;
import java.util.Objects;
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

    private static final String THE_CHILDREN_ARRAY_CANNOT_BE_NULL = "The children array cannot be null";

    private static final String ATTRIBUTE_NAME_CANNOT_BE_NULL = "The attribute name cannot be null";

    private static final String CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN = "Cannot %s element with index %d when there are %d children";

    private ElementStateProvider stateProvider;
    private StateNode node;

    /**
     * Pattern for maching valid tag names, according to
     * https://www.w3.org/TR/html-markup/syntax.html#tag-name "HTML elements all
     * have names that only use characters in the range 0–9, a–z, and A–Z."
     */
    private static Pattern tagNamePattern = Pattern.compile("^[a-zA-Z0-9-]+$");

    /**
     * Private constructor for initializing with an existing node
     */
    private Element(StateNode node) {
        assert node != null;

        // TODO This needs to be fixed once there are several
        // ElementStateProvider implementations
        stateProvider = BasicElementStateProvider.get();
        if (!stateProvider.supports(node)) {
            throw new IllegalArgumentException(
                    "BasicElementStateProvider does not support the given state node");
        }

        this.node = node;
    }

    /**
     * Creates an element using the given tag name.
     *
     * @param tag
     *            the tag name of the element. Must be a non-empty string and
     *            can contain letters, numbers and dashes ({@literal -})
     */
    public Element(String tag) {
        this(createStateNode(tag));
        assert node != null;
        assert stateProvider != null;
    }

    /**
     * Gets the element mapped to the given state node.
     *
     * @param node
     *            the state node
     * @return the element for the node
     */
    public static Element get(StateNode node) {
        return new Element(node);
    }

    /**
     * Creates a state node for an element using the given tag.
     *
     * @param tag
     *            the tag name of the element.
     */
    private static StateNode createStateNode(String tag) {
        if (!isValidTagName(tag)) {
            throw new IllegalArgumentException(
                    "Tag " + tag + " is not a valid tag name");
        }
        return BasicElementStateProvider.createStateNode(tag);
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
     * Removes this element from its parent.
     * <p>
     * Has no effect if the element does not have a parent
     *
     * @return this element
     */
    public Element removeFromParent() {
        Element parent = getParent();
        if (parent != null) {
            parent.removeChild(this);
        }
        return this;
    }

    /**
     * Gets the parent element.
     *
     * @return the parent element or null if this element does not have a parent
     */
    public Element getParent() {
        return stateProvider.getParent(node);
    }

    /**
     * Gets the number of child elements.
     *
     * @return the number of child elements
     */
    public int getChildCount() {
        return stateProvider.getChildCount(node);
    }

    /**
     * Returns the child element at the given position.
     *
     * @param index
     *            the index of the child element to return
     * @return the child element
     */
    public Element getChild(int index) {
        if (index < 0 || index >= getChildCount()) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "get",
                    index, getChildCount()));
        }

        return stateProvider.getChild(node, index);
    }

    /**
     * Adds the given children as the last children of this element.
     *
     * @param children
     *            the element(s) to add
     * @return this element
     */
    public Element appendChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        insertChild(getChildCount(), children);

        return this;
    }

    /**
     * Inserts the given child element(s) at the given position.
     *
     * @param index
     *            the position at which to insert the new child
     * @param children
     *            the child element(s) to insert
     * @return this element
     */
    public Element insertChild(int index, Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }
        if (index > getChildCount()) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "insert",
                    index, getChildCount()));
        }

        for (int i = 0; i < children.length; i++) {
            stateProvider.insertChild(node, index + i, children[i]);
            assert Objects.equals(this, children[i]
                    .getParent()) : "Child should have this element as parent after being inserted";
        }

        return this;
    }

    /**
     * Replaces the child at the given position with the given child element.
     *
     * @param index
     *            the position of the child element to replace
     * @param child
     *            the child element to insert
     * @return this element
     */
    public Element setChild(int index, Element child) {
        if (child == null) {
            throw new IllegalArgumentException("The child cannot be null");
        }
        int childCount = getChildCount();
        if (index < 0) {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "set",
                    index, getChildCount()));
        } else if (index < childCount) {
            removeChild(index);
            insertChild(index, child);
        } else {
            throw new IllegalArgumentException(String.format(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN, "set",
                    index, getChildCount()));
        }
        return this;
    }

    /**
     * Removes the given child element(s).
     *
     * @param children
     *            the child element(s) to remove
     * @return this element
     */
    public Element removeChild(Element... children) {
        if (children == null) {
            throw new IllegalArgumentException(
                    THE_CHILDREN_ARRAY_CANNOT_BE_NULL);
        }

        for (int i = 0; i < children.length; i++) {
            if (!Objects.equals(children[i].getParent(), this)) {
                throw new IllegalArgumentException(
                        "The given element is not a child of this element");
            }
            stateProvider.removeChild(node, children[i]);
        }
        return this;
    }

    /**
     * Removes the child at the given index.
     *
     * @param index
     *            the index of the child to remove
     * @return this element
     */
    public Element removeChild(int index) {
        if (index < 0 || index >= getChildCount()) {
            throw new IllegalArgumentException(
                    CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN);

        }

        stateProvider.removeChild(node, index);
        return this;
    }

    /**
     * Removes all child elements.
     *
     * @return this element
     */
    public Element removeAllChildren() {
        stateProvider.removeAllChildren(node);

        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, stateProvider);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Element other = (Element) obj;

        // Constructors guarantee that neither node nor stateProvider is null
        return other.node.equals(node)
                && other.stateProvider.equals(stateProvider);
    }

}

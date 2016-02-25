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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;
import com.vaadin.hummingbird.dom.impl.TextElementStateProvider;
import com.vaadin.hummingbird.dom.impl.TextNodeNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;

import elemental.json.Json;
import elemental.json.JsonValue;

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
    /**
     * Callbacks for handling attributes with special semantics. This is used
     * for e.g. <code>class</code> which is assembled from a separate list of
     * tokens instead of being stored as a regular attribute string.
     */
    private interface CustomAttribute extends Serializable {
        /**
         * Checks what {@link Element#hasAttribute(String)} should return for
         * this attribute.
         *
         * @param element
         *            the element to check
         * @return <code>true</code> if the element has a value for this
         *         attribute, otherwise <code>false</code>
         */
        boolean hasAttribute(Element element);

        /**
         * Gets the value that should be returned by
         * {@link Element#getAttribute(String)} for this attribute.
         *
         * @param element
         *            the element to check.
         * @return the attribute value
         */
        String getAttribute(Element element);

        /**
         * Sets the value when {@link Element#setAttribute(String, String)} is
         * called for this attribute.
         *
         * @param element
         *            the element to check.
         * @param value
         *            the new attribute value
         */
        void setAttribute(Element element, String value);

        /**
         * Removes the attribute when {@link Element#removeAttribute(String)} is
         * called for this attribute.
         *
         * @param element
         *            the element to check.
         */
        void removeAttribute(Element element);
    }

    /**
     * Emulates the <code>class</code> attribute by delegating to
     * {@link Element#getClassList()}.
     */
    private static class ClassAttributeHandler implements CustomAttribute {
        @Override
        public boolean hasAttribute(Element element) {
            return !element.getClassList().isEmpty();
        }

        @Override
        public String getAttribute(Element element) {
            Set<String> classList = element.getClassList();
            if (classList.isEmpty()) {
                return null;
            } else {
                return classList.stream().collect(Collectors.joining(" "));
            }
        }

        @Override
        public void setAttribute(Element element, String value) {
            Set<String> classList = element.getClassList();
            classList.clear();

            String[] parts = value.split("\\s+");
            classList.addAll(Arrays.asList(parts));
        }

        @Override
        public void removeAttribute(Element element) {
            element.getClassList().clear();
        }
    }

    /**
     * Emulates the <code>style</code> attribute by delegating to
     * {@link Element#getStyle()}.
     */
    private static class StyleAttributeHandler implements CustomAttribute {
        @Override
        public boolean hasAttribute(Element element) {
            return !element.getStyle().getNames().isEmpty();
        }

        @Override
        public String getAttribute(Element element) {
            Style style = element.getStyle();
            Set<String> styleNames = style.getNames();
            if (styleNames.isEmpty()) {
                return null;
            } else {
                return styleNames.stream().map(styleName -> {
                    return styleName + ":" + style.get(styleName);
                }).collect(Collectors.joining(";"));
            }
        }

        @Override
        public void setAttribute(Element element, String attributeValue) {
            throw new UnsupportedOperationException(
                    "Styles must be set using Element.getStyles()");
        }

        @Override
        public void removeAttribute(Element element) {
            element.getStyle().clear();
        }
    }

    static final String THE_CHILDREN_ARRAY_CANNOT_BE_NULL = "The children array cannot be null";

    static final String ATTRIBUTE_NAME_CANNOT_BE_NULL = "The attribute name cannot be null";

    static final String CANNOT_X_WITH_INDEX_Y_WHEN_THERE_ARE_Z_CHILDREN = "Cannot %s element with index %d when there are %d children";

    private static final Map<String, CustomAttribute> customAttributes = new HashMap<>();

    static {
        customAttributes.put("class", new ClassAttributeHandler());
        customAttributes.put("style", new StyleAttributeHandler());
    }

    // Can't set $name as a property, use $replacement instead.
    private static final Map<String, String> illegalPropertyReplacements = new HashMap<>();

    static {
        illegalPropertyReplacements.put("textContent",
                "setTextContent(String)");
        illegalPropertyReplacements.put("classList", "getClassList()");
        illegalPropertyReplacements.put("className", "getClassList()");
    }

    private ElementStateProvider stateProvider;
    private StateNode node;

    /**
     * Private constructor for initializing with an existing node and state
     * provider.
     *
     * @param node
     *            the state node, not null
     * @param stateProvider
     *            the state provider, not null
     */
    private Element(StateNode node, ElementStateProvider stateProvider) {
        assert node != null;
        assert stateProvider != null;

        if (!stateProvider.supports(node)) {
            throw new IllegalArgumentException(
                    "BasicElementStateProvider does not support the given state node");
        }

        this.stateProvider = stateProvider;
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
        this(createStateNode(tag), BasicElementStateProvider.get());
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
        if (node.hasNamespace(TextNodeNamespace.class)) {
            return new Element(node, TextElementStateProvider.get());
        } else if (node.hasNamespace(ElementDataNamespace.class)) {
            return new Element(node, BasicElementStateProvider.get());
        } else {
            throw new IllegalArgumentException(
                    "Node is not valid as an element");
        }
    }

    /**
     * Creates a text node with the given text.
     *
     * @param text
     *            the text in the node
     * @return an element representing the text node
     */
    public static Element createText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }

        return new Element(TextElementStateProvider.createStateNode(text),
                TextElementStateProvider.get());
    }

    /**
     * Creates a state node for an element using the given tag.
     *
     * @param tag
     *            the tag name of the element.
     */
    private static StateNode createStateNode(String tag) {
        if (!ElementUtil.isValidTagName(tag)) {
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
        if (!ElementUtil.isValidAttributeName(lowerCaseAttribute)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute \"%s\" is not a valid attribute name",
                    lowerCaseAttribute));
        }

        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        CustomAttribute customAttribute = customAttributes
                .get(lowerCaseAttribute);
        if (customAttribute != null) {
            customAttribute.setAttribute(this, value);
        } else {
            stateProvider.setAttribute(node, lowerCaseAttribute, value);
        }
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

        String lowerCaseAttribute = attribute.toLowerCase(Locale.ENGLISH);
        CustomAttribute customAttribute = customAttributes
                .get(lowerCaseAttribute);
        if (customAttribute != null) {
            return customAttribute.getAttribute(this);
        } else {
            return stateProvider.getAttribute(node, lowerCaseAttribute);
        }
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
        String lowerCaseAttribute = attribute.toLowerCase(Locale.ENGLISH);
        CustomAttribute customAttribute = customAttributes
                .get(lowerCaseAttribute);
        if (customAttribute != null) {
            return customAttribute.hasAttribute(this);
        } else {
            return stateProvider.hasAttribute(node, lowerCaseAttribute);
        }

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
        Set<String> regularNames = stateProvider.getAttributeNames(node);

        Set<String> attributeNames = null;

        for (Entry<String, CustomAttribute> entry : customAttributes
                .entrySet()) {
            if (entry.getValue().hasAttribute(this)) {
                if (attributeNames == null) {
                    attributeNames = new HashSet<>(regularNames);
                }
                attributeNames.add(entry.getKey());
            }
        }

        if (attributeNames != null) {
            return attributeNames;
        } else {
            // Intentionally not making a copy for performance reasons
            return Collections.unmodifiableSet(regularNames);
        }
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
        String lowerCaseAttribute = attribute.toLowerCase(Locale.ENGLISH);
        CustomAttribute customAttribute = customAttributes
                .get(lowerCaseAttribute);
        if (customAttribute != null) {
            customAttribute.removeAttribute(this);
        } else {
            stateProvider.removeAttribute(node, lowerCaseAttribute);
        }
        return this;
    }

    /**
     * Adds an event listener for the given event type.
     * <p>
     * When an event is fired in the browser, custom JavaScript expressions
     * defined in the <code>evendDataDefinitions</code> parameter are evaluated
     * to extract data that is sent back to the server. The expression is
     * evaluated in a context where <code>element</code> refers to this element
     * and <code>event</code> refers to the fired event. The result of the
     * evaluation is available in {@link DomEvent#getEventData()} with the
     * expression as the key in the JSON object. An expression might be e.g.
     *
     * <ul>
     * <li><code>element.value</code> the get the value of an input element for
     * a change event.
     * <li><code>event.button === 0</code> to get true for click events
     * triggered by the primary mouse button.
     * </ul>
     * <p>
     * Event listeners are triggered in the order they are registered.
     *
     * @param eventType
     *            the type of event to listen to, not <code>null</code>
     * @param listener
     *            the listener to add, not <code>null</code>
     * @param eventDataExpressions
     *            definitions for data that should be passed back to the server
     *            together with the event
     * @return a handle that can be used for removing the listener
     */
    public EventRegistrationHandle addEventListener(String eventType,
            DomEventListener listener, String... eventDataExpressions) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null");
        }
        if (eventDataExpressions == null) {
            throw new IllegalArgumentException(
                    "The event data expressions array must not be null");
        }

        return stateProvider.addEventListener(node, eventType, listener,
                eventDataExpressions);
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

    /**
     * Sets the given property to the given string value.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value
     * @return this element
     */
    public Element setProperty(String name, String value) {
        return setRawProperty(name, value);
    }

    /**
     * Sets the given property to the given boolean value.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value
     * @return this element
     */
    public Element setProperty(String name, boolean value) {
        return setRawProperty(name, Boolean.valueOf(value));
    }

    /**
     * Sets the given property to the given numeric value.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value
     * @return this element
     */
    public Element setProperty(String name, double value) {
        return setRawProperty(name, Double.valueOf(value));
    }

    /**
     * Sets the given property to the given JSON value. Please note that this
     * method does not accept <code>null</code> as a value, since
     * {@link Json#createNull()} should be used instead for JSON values.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    // Distinct name so setProperty("foo", null) is not ambiguous
    public Element setPropertyJson(String name, JsonValue value) {
        verifySetPropertyName(name);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Json.createNull() should be used instead of null for JSON values");
        }

        stateProvider.setJsonProperty(node, name, value);

        return this;
    }

    private Element setRawProperty(String name, Serializable value) {
        verifySetPropertyName(name);

        stateProvider.setProperty(node, name, value);

        return this;
    }

    private static void verifySetPropertyName(String name) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "A property name cannot be null");
        }

        String replacement = illegalPropertyReplacements.get(name);
        if (replacement != null) {
            throw new IllegalArgumentException("Can't set " + name
                    + " as a property, use " + replacement + " instead.");
        }
    }

    /**
     * Gets the value of the given property as a string. The returned value is
     * converted to a string if it has been set as some other type.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param defaultValue
     *            the value to return if the property is not set, or if the
     *            value is <code>null</code>
     * @return the property value
     */
    public String getProperty(String name, String defaultValue) {
        Object value = getPropertyRaw(name);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof JsonValue) {
            return ((JsonValue) value).toJson();
        } else if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();
            int intValue = (int) doubleValue;

            // Special comparison to keep sonarqube happy
            if (Double.doubleToRawLongBits(doubleValue - intValue) == 0) {
                // Java adds ".0" at the end of integer-ish doubles
                return Integer.toString(intValue);
            } else {
                return Double.toString(doubleValue);
            }
        } else {
            return value.toString();
        }
    }

    /**
     * Gets the value of the given property as a string.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @return the property value, or <code>null</code> if no value is set
     */
    public String getProperty(String name) {
        return getProperty(name, null);
    }

    /**
     * Gets the value of the given property as a boolean, or the given default
     * value if the underlying value is <code>null</code>.
     * <p>
     * A value defined as some other type than boolean is converted according to
     * JavaScript semantics:
     * <ul>
     * <li>String values are <code>true</code>, except for the empty string.
     * <li>Numerical values are <code>true</code>, except for 0 and
     * <code>NaN</code>.
     * <li>JSON object and JSON array values are always <code>true</code>.
     * </ul>
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param defaultValue
     *            the value to return if the property is not set, or if the
     *            value is <code>null</code>
     * @return the property value
     */
    public boolean getProperty(String name, boolean defaultValue) {
        Object value = getPropertyRaw(name);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof JsonValue) {
            return ((JsonValue) value).asBoolean();
        } else if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            // Special comparison to keep sonarqube happy
            return !Double.isNaN(number)
                    && Double.doubleToLongBits(number) != 0;
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        } else {
            throw new IllegalStateException(
                    "Unsupported property type: " + value.getClass());
        }
    }

    /**
     * Gets the value of the given property as a double, or the given default
     * value if the underlying value is <code>null</code>
     * <p>
     * A value defined as some other type than double is converted according to
     * JavaScript semantics:
     * <ul>
     * <li>String values are parsed to a number. <code>NaN</code> is returned if
     * the string can't be parsed.
     * <li>boolean <code>true</code> is 1, boolean <code>false</code> is 0.
     * <li>JSON object and JSON array values are always <code>NaN</code>.
     * </ul>
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param defaultValue
     *            the value to return if the property is not set, or if the
     *            value is <code>null</code>
     * @return the property value
     */
    public double getProperty(String name, double defaultValue) {
        Object value = getPropertyRaw(name);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number) {
            Number number = (Number) value;
            return number.doubleValue();
        } else if (value instanceof JsonValue) {
            return ((JsonValue) value).asNumber();
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
        } else if (value instanceof String) {
            String string = (String) value;
            if (string.isEmpty()) {
                return 0;
            } else {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException ignore) {
                    return Double.NaN;
                }
            }
        } else {
            throw new IllegalStateException(
                    "Unsupported property type: " + value.getClass());
        }
    }

    /**
     * Gets the value of the given property as an integer, or the given default
     * value if the underlying value is <code>null</code>
     * <p>
     * The value is converted in the same way as for
     * {@link #getProperty(String, double)}, and then truncated to
     * <code>int</code>.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param defaultValue
     *            the value to return if the property is not set, or if the
     *            value is <code>null</code>
     * @return the property value
     */
    public int getProperty(String name, int defaultValue) {
        return (int) getProperty(name, (double) defaultValue);
    }

    /**
     * Gets the raw property value without any value conversion. The type of the
     * value is {@link String}, {@link Double}, {@link Boolean} or
     * {@link JsonValue}. <code>null</code> is returned if there is no property
     * with the given name or if the value is set to <code>null</code>.
     *
     * @param name
     *            the property name, not null
     * @return the raw property value, or <code>null</code>
     */
    public Object getPropertyRaw(String name) {
        return stateProvider.getProperty(node, name);
    }

    /**
     * Removes the given property.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @return this element
     */
    public Element removeProperty(String name) {
        stateProvider.removeProperty(node, name);
        return this;
    }

    /**
     * Checks whether this element has a property with the given name.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @return <code>true</code> if the property is present; otherwise
     *         <code>false</code>
     */
    public boolean hasProperty(String name) {
        return stateProvider.hasProperty(node, name);
    }

    /**
     * Gets the defined property names.
     * <p>
     * The returned collection might be connected so that adding/removing
     * properties through e.g. {@link #setProperty(String, String)} or
     * {@link #removeProperty(String)} affects the collection. If you want to
     * store the property names for later usage, you should make a copy.
     * <p>
     * You cannot modify properties through the returned set.
     *
     * @return the defined property names
     */
    public Set<String> getPropertyNames() {
        // Intentionally not making a copy for performance reasons
        return Collections
                .unmodifiableSet(stateProvider.getPropertyNames(node));
    }

    /**
     * Checks whether this element represents a text node.
     *
     * @return <code>true</code> if this element is a text node; otherwise
     *         <code>false</code>
     */
    public boolean isTextNode() {
        return stateProvider.isTextNode(node);
    }

    /**
     * Sets the text content of this element, replacing any existing children.
     *
     * @param textContent
     *            the text content to set, <code>null</code> is interpreted as
     *            an empty string
     * @return this element
     */
    public Element setTextContent(String textContent) {
        if (textContent == null) {
            // Browsers work this way
            textContent = "";
        }

        if (isTextNode()) {
            stateProvider.setTextContent(node, textContent);
        } else {
            boolean hasText = !textContent.isEmpty();
            if (getChildCount() == 1 && getChild(0).isTextNode() && hasText) {
                getChild(0).setTextContent(textContent);
            } else {
                removeAllChildren();
                if (hasText) {
                    appendChild(createText(textContent));
                }
            }
        }

        return this;
    }

    /**
     * Gets the text content of this element. The text content recursively
     * includes the text content of all child nodes.
     *
     * @return the text content
     */
    public String getTextContent() {
        if (isTextNode()) {
            return stateProvider.getTextContent(node);
        } else {
            StringBuilder builder = new StringBuilder();
            appendTextContent(builder);
            return builder.toString();
        }
    }

    private void appendTextContent(StringBuilder builder) {
        if (isTextNode()) {
            builder.append(getTextContent());
        } else {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                getChild(i).appendTextContent(builder);
            }
        }
    }

    /**
     * Gets the set of CSS class names used for this element. The returned set
     * can be modified to add or remove class names. The contents of the set is
     * also reflected in the value of the <code>class</code> attribute.
     * <p>
     * Despite the name implying a list being returned, the return type is
     * actually a {@link Set} since the the in-browser return value behaves like
     * a <code>Set</code> in Java.
     *
     * @return a list of class names
     */
    public Set<String> getClassList() {
        return stateProvider.getClassList(node);
    }

    /**
     * Gets the style instance for managing element inline styles.
     *
     * @return the style object for the element
     */
    public Style getStyle() {
        return stateProvider.getStyle(node);
    }

}

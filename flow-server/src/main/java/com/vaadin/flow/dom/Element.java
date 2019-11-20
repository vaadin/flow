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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.dom.impl.BasicTextElementStateProvider;
import com.vaadin.flow.dom.impl.CustomAttribute;
import com.vaadin.flow.dom.impl.ThemeListImpl;
import com.vaadin.flow.internal.JavaScriptSemantics;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.TextNodeMap;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * Represents an element in the DOM.
 * <p>
 * Contains methods for updating and querying various parts of the element, such
 * as attributes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Element extends Node<Element> {
    private static final String EVENT_TYPE_MUST_NOT_BE_NULL = "Event type must not be null";

    static final String ATTRIBUTE_NAME_CANNOT_BE_NULL = "The attribute name cannot be null";

    // Can't set $name as a property, use $replacement instead.
    private static final Map<String, String> illegalPropertyReplacements = new HashMap<>();

    static {
        illegalPropertyReplacements.put("textContent",
                "setTextContent(String)");
        illegalPropertyReplacements.put("classList", "getClassList()");
        illegalPropertyReplacements.put("className", "getClassList()");
        illegalPropertyReplacements.put("outerHTML",
                "getParent().setProperty('innerHTML',value)");
    }

    /**
     * No-op DOM listener implementation used by e.g.
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}.
     */
    private static final DomEventListener NO_OP_DOM_LISTENER = event -> {
        // No op
    };

    /**
     * Private constructor for initializing with an existing node and state
     * provider.
     *
     * @param node
     *            the state node, not null
     * @param stateProvider
     *            the state provider, not null
     */
    protected Element(StateNode node, ElementStateProvider stateProvider) {
        super(node, stateProvider);
    }

    /**
     * Creates an element using the given tag name.
     *
     * @param tag
     *            the tag name of the element. Must be a non-empty string and
     *            can contain letters, numbers and dashes ({@literal -})
     */
    public Element(String tag) {
        super(createStateNode(tag), BasicElementStateProvider.get());

        assert getNode() != null;
        assert getStateProvider() != null;
    }

    /**
     * Creates an element using the given tag name.
     *
     * @param tag
     *            tag name of the element. Must be a non-empty string and can
     *            contain letters, numbers and dashes ({@literal -})
     * @param autocreate
     *            parameter is ignored, but retained in the API for backwards
     *            compatibility
     * @deprecated The {@code autoCreate} parameter no longer has any effect.
     *             Use {@link #Element(String)} instead.
     */
    @Deprecated
    public Element(String tag, boolean autocreate) {
        this(tag);
    }

    /**
     * Gets the element mapped to the given state node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @return the element for the node, not <code>null</code>
     */
    public static Element get(StateNode node) {
        assert node != null;

        if (node.hasFeature(TextNodeMap.class)) {
            return get(node, BasicTextElementStateProvider.get());
        } else if (node.hasFeature(ElementData.class)) {
            return get(node, BasicElementStateProvider.get());
        } else {
            throw new IllegalArgumentException(
                    "Node is not valid as an element");
        }
    }

    /**
     * Gets the element mapped to the given state node and element state
     * provider.
     *
     * @param node
     *            the state node
     * @param stateProvider
     *            the element state provider
     * @return an element for the node and state provider, not <code>null</code>
     */
    // Static builder instead of regular constructor to keep it slightly out of
    // view
    public static Element get(StateNode node,
            ElementStateProvider stateProvider) {
        return new Element(node, stateProvider);
    }

    /**
     * Gets the number of child elements.
     * <p>
     * If the property "innerHTML" has been set explicitly then its value (the
     * new element structure) won't be populated on the server side and this
     * method will return <code>0</code>.
     *
     * @see #setProperty(String, String)
     * @return the number of child elements
     */
    @Override
    public int getChildCount() {
        return super.getChildCount();
    }

    /**
     * Returns the child element at the given position.
     * <p>
     * If property "innerHTML" has been set explicitly then its value (the new
     * element structure) won't be populated on the server side and this method
     * will not work.
     *
     * @see #setProperty(String, String)
     * @param index
     *            the index of the child element to return
     * @return the child element
     */
    @Override
    public Element getChild(int index) {
        return super.getChild(index);
    }

    /**
     * Gets all the children of this element.
     * <p>
     * If property "innerHTML" has been set explicitly then its value (the new
     * element structure) won't be populated on the server side and this method
     * returns an empty stream.
     *
     * @see #setProperty(String, String)
     *
     * @return a stream of children
     */
    @Override
    public Stream<Element> getChildren() {
        return super.getChildren();
    }

    /**
     * Creates a text node with the given text.
     *
     * @param text
     *            the text in the node, not <code>null</code>
     * @return an element representing the text node
     */
    public static Element createText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }

        return new Element(BasicTextElementStateProvider.createStateNode(text),
                BasicTextElementStateProvider.get());
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
     * Gets the tag name for the element.
     *
     * @return the tag name
     */
    public String getTag() {
        return getStateProvider().getTag(getNode());
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
     * <p>
     * Note that setting the attribute <code>class</code> will override anything
     * that has been set previously via {@link #getClassList()}.
     * <p>
     * Note that you cannot set the attribute <code>style</code> using this
     * method. Instead you should use {@link #getStyle()} object.
     * <p>
     * Note that attribute changes made on the server are sent to the client but
     * attribute changes made on the client side are not reflected back to the
     * server.
     *
     * @param attribute
     *            the name of the attribute
     * @param value
     *            the value of the attribute, not null
     * @return this element
     */
    public Element setAttribute(String attribute, String value) {
        String lowerCaseAttribute = validateAttribute(attribute, value);

        Optional<CustomAttribute> customAttribute = CustomAttribute
                .get(lowerCaseAttribute);
        if (customAttribute.isPresent()) {
            customAttribute.get().setAttribute(this, value);
        } else {
            getStateProvider().setAttribute(getNode(), lowerCaseAttribute,
                    value);
        }
        return this;
    }

    /**
     * Sets the given attribute to the given boolean value. Setting the value to
     * <code>true</code> will internally set the value to <code>""</code>, which
     * will be rendered as {@literal <div name>}, i.e. without any explicit
     * value. Setting the value to <code>false</code> is a shorthand for
     * removing the attribute.
     * <p>
     * Use {@link #hasAttribute(String)} to check whether a boolean attribute
     * has been set.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     *
     * @see #setAttribute(String, String)
     *
     * @param attribute
     *            the name of the attribute
     * @param value
     *            the value of the attribute
     * @return this element
     */
    public Element setAttribute(String attribute, boolean value) {
        if (value) {
            return setAttribute(attribute, "");
        } else {
            return removeAttribute(attribute);
        }
    }

    /**
     * Sets the given attribute to the given {@link StreamResource} value.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * This is a convenience method to register a {@link StreamResource}
     * instance into the session and use the registered resource URI as an
     * element attribute.
     * <p>
     *
     * @see #setAttribute(String, String)
     *
     * @param attribute
     *            the name of the attribute
     * @param resource
     *            the resource value, not null
     * @return this element
     */
    public Element setAttribute(String attribute,
            AbstractStreamResource resource) {
        String lowerCaseAttribute = validateAttribute(attribute, resource);

        Optional<CustomAttribute> customAttribute = CustomAttribute
                .get(lowerCaseAttribute);
        if (!customAttribute.isPresent()) {
            getStateProvider().setAttribute(getNode(), attribute, resource);
        } else {
            throw new IllegalArgumentException("Can't set " + attribute
                    + " to StreamResource value. This attribute has special semantic");
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
     * <p>
     * Note that for attribute <code>class</code> the contents of the
     * {@link #getClassList()} collection are returned as a single concatenated
     * string.
     * <p>
     * Note that for attribute <code>style</code> the contents of the
     * {@link #getStyle()} object are returned as a single concatenated string.
     * <p>
     * Note that attribute changes made on the server are sent to the client but
     * attribute changes made on the client side are not reflected back to the
     * server.
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

        return CustomAttribute.get(lowerCaseAttribute)
                .map(attr -> attr.getAttribute(this))
                .orElseGet(() -> getStateProvider().getAttribute(getNode(),
                        lowerCaseAttribute));
    }

    /**
     * Checks if the given attribute has been set.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * Note that attribute changes made on the server are sent to the client but
     * attribute changes made on the client side are not reflected back to the
     * server.
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

        Optional<CustomAttribute> customAttribute = CustomAttribute
                .get(lowerCaseAttribute);
        if (customAttribute.isPresent()) {
            return customAttribute.get().hasAttribute(this);
        } else {
            return getStateProvider().hasAttribute(getNode(),
                    lowerCaseAttribute);
        }

    }

    /**
     * Gets the defined attribute names.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * Note that attribute changes made on the server are sent to the client but
     * attribute changes made on the client side are not reflected back to the
     * server.
     *
     * @return a stream of defined attribute names
     */
    public Stream<String> getAttributeNames() {
        assert getStateProvider().getAttributeNames(getNode())
                .map(CustomAttribute::get).filter(Optional::isPresent)
                .filter(attr -> attr.get().hasAttribute(this))
                .count() == 0 : "Overlap between stored attributes and existing custom attributes";

        Stream<String> regularNames = getStateProvider()
                .getAttributeNames(getNode());

        Stream<String> customNames = CustomAttribute.getNames().stream().filter(
                name -> CustomAttribute.get(name).get().hasAttribute(this));

        return Stream.concat(regularNames, customNames);
    }

    /**
     * Removes the given attribute.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * If the attribute has not been set, does nothing.
     * <p>
     * Note that attribute changes made on the server are sent to the client but
     * attribute changes made on the client side are not reflected back to the
     * server.
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
        if (hasAttribute(lowerCaseAttribute)) {
            Optional<CustomAttribute> customAttribute = CustomAttribute
                    .get(lowerCaseAttribute);
            if (customAttribute.isPresent()) {
                customAttribute.get().removeAttribute(this);
            } else {
                getStateProvider().removeAttribute(getNode(),
                        lowerCaseAttribute);
            }
        }
        return this;
    }

    /**
     * Adds an event listener for the given event type.
     * <p>
     * Event listeners are triggered in the order they are registered.
     *
     * @see DomListenerRegistration
     *
     * @param eventType
     *            the type of event to listen to, not <code>null</code>
     * @param listener
     *            the listener to add, not <code>null</code>
     * @return a handle that can be used for configuring or removing the
     *         listener
     */
    public DomListenerRegistration addEventListener(String eventType,
            DomEventListener listener) {
        if (eventType == null) {
            throw new IllegalArgumentException(EVENT_TYPE_MUST_NOT_BE_NULL);
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null");
        }
        return getStateProvider().addEventListener(getNode(), eventType,
                listener);
    }

    /**
     * Adds an event listener and event data expressions for the given event
     * type.
     * <p>
     * When an event is fired in the browser, custom JavaScript expressions
     * defined in the <code>eventDataExpressions</code> parameter are evaluated
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
     * @return a handle that can be used for configuring or removing the
     *         listener
     *
     * @deprecated Instead, use the returned registration instance for adding
     *             event data expressions
     */
    @Deprecated
    public DomListenerRegistration addEventListener(String eventType,
            DomEventListener listener, String... eventDataExpressions) {
        if (eventDataExpressions == null) {
            throw new IllegalArgumentException(
                    "The event data expressions array must not be null");
        }

        DomListenerRegistration registration = addEventListener(eventType,
                listener);
        Stream.of(eventDataExpressions).forEach(registration::addEventData);

        return registration;
    }

    /**
     * Removes this element from its parent.
     * <p>
     * Has no effect if the element does not have a parent
     *
     * @return this element
     */
    public Element removeFromParent() {
        Node<?> parent = getParentNode();
        if (parent != null) {
            parent.removeChild(this);
        }
        return this;
    }

    /**
     * Removes this element from its parent and state tree.
     *
     * @return this element
     */
    public Element removeFromTree() {
        Node<?> parent = getParentNode();
        if (parent != null
                && parent.getChildren().anyMatch(Predicate.isEqual(this))) {
            parent.removeChild(this);
        }
        getNode().removeFromTree();
        return this;
    }

    /**
     * Gets the parent element.
     * <p>
     * The method may return {@code null} if the parent is not an element but a
     * {@link Node}.
     *
     * @see #getParentNode()
     *
     * @return the parent element or null if this element does not have a parent
     *         or the parent is not an element
     */
    public Element getParent() {
        Node<?> parent = getParentNode();
        if (parent instanceof Element) {
            return (Element) parent;
        }
        return null;
    }

    /**
     * Sets the given property to the given string value.
     * <p>
     * Note in order to update the following properties, you need to use the
     * specific API for that:
     * <table>
     * <caption>Properties with different API</caption>
     * <tr>
     * <th>Property</th>
     * <th>Method</th>
     * </tr>
     * <tr>
     * <td>classList / className</td>
     * <td>{@link Element#getClassList()}</td>
     * </tr>
     * <tr>
     * <td>style</td>
     * <td>{@link Element#getStyle()}</td>
     * </tr>
     * <tr>
     * <td>textContent</td>
     * <td>{@link Element#getText()} and {@link Element#getTextRecursively()}
     * </td>
     * </tr>
     * </table>
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     * <p>
     * The "innerHTML" property has an impact on the descendants structure of
     * the element. So setting the "innerHTML" property removes all the
     * children.
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
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
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
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
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
     * Sets the given property to the given JSON value.
     * <p>
     * Please note that this method does not accept <code>null</code> as a
     * value, since {@link Json#createNull()} should be used instead for JSON
     * values.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    // Distinct name so setProperty("foo", null) is not ambiguous
    public Element setPropertyJson(String name, JsonValue value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Json.createNull() must be used instead of null for JSON values");
        }

        setRawProperty(name, value);
        return this;
    }

    /**
     * Adds a property change listener.
     * <p>
     * Use either two way Polymer binding or synchronize property explicitly to
     * be able to get property change events from the client.
     *
     * @see #synchronizeProperty(String, String)
     * @param name
     *            the property name to add the listener for, not
     *            <code>null</code>
     * @param listener
     *            listener to get notifications about property value changes,
     *            not <code>null</code>
     * @return an event registration handle for removing the listener
     */
    public Registration addPropertyChangeListener(String name,
            PropertyChangeListener listener) {
        return getStateProvider().addPropertyChangeListener(getNode(), name,
                listener);
    }

    /**
     * Adds a property change listener and configures the property to be
     * synchronized to the server when a given DOM event is fired.
     *
     * #see {@link #addPropertyChangeListener(String, PropertyChangeListener)}
     *
     * @param propertyName
     *            the name of the element property to listen to, not
     *            <code>null</code>
     * @param domEventName
     *            the name of the DOM event for which the property should be
     *            synchronized to the server, not <code>null</code>
     * @param listener
     *            the property change listener not add, not <code>null</code>
     * @return a handle that can be used for configuring or removing the
     *         listener
     *
     * @since 1.3
     */
    public DomListenerRegistration addPropertyChangeListener(
            String propertyName, String domEventName,
            PropertyChangeListener listener) {
        Registration propertyListenerRegistration = addPropertyChangeListener(
                propertyName, listener);

        // No-op DOM listener since we're also listening to property changes
        return addEventListener(domEventName, NO_OP_DOM_LISTENER)
                .synchronizeProperty(propertyName)
                .onUnregister(propertyListenerRegistration::remove);
    }

    private Element setRawProperty(String name, Serializable value) {
        verifySetPropertyName(name);

        if ("innerHTML".equals(name)) {
            removeAllChildren();
        }
        getStateProvider().setProperty(getNode(), name, value, true);

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
        } else {
            return JavaScriptSemantics.isTrueish(value);
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
    public Serializable getPropertyRaw(String name) {
        return getStateProvider().getProperty(getNode(), name);
    }

    /**
     * Removes the given property.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @return this element
     */
    public Element removeProperty(String name) {
        getStateProvider().removeProperty(getNode(), name);
        return this;
    }

    /**
     * Checks whether this element has a property with the given name.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @return <code>true</code> if the property is present; otherwise
     *         <code>false</code>
     */
    public boolean hasProperty(String name) {
        return getStateProvider().hasProperty(getNode(), name);
    }

    /**
     * Gets the defined property names.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     *
     * @return a stream of defined property names
     */
    public Stream<String> getPropertyNames() {
        return getStateProvider().getPropertyNames(getNode());
    }

    /**
     * Checks whether this element represents a text node.
     *
     * @return <code>true</code> if this element is a text node; otherwise
     *         <code>false</code>
     */
    public boolean isTextNode() {
        return getStateProvider().isTextNode(getNode());
    }

    /**
     * Sets the text content of this element, replacing any existing children.
     *
     * @param textContent
     *            the text content to set, <code>null</code> is interpreted as
     *            an empty string
     * @return this element
     */
    public Element setText(String textContent) {
        if (textContent == null) {
            // Browsers work this way
            textContent = "";
        }

        if (isTextNode()) {
            getStateProvider().setTextContent(getNode(), textContent);
        } else {
            if (textContent.isEmpty()) {
                removeAllChildren();
            } else {
                setTextContent(textContent);
            }
        }

        return this;
    }

    private void setTextContent(String textContent) {
        Element child;
        if (getChildCount() == 1 && getChild(0).isTextNode()) {
            child = getChild(0).setText(textContent);
        } else {
            child = createText(textContent);
        }
        removeAllChildren();
        appendChild(child);
    }

    /**
     * Gets the text content of this element. This includes only the text from
     * any immediate child text nodes, but ignores text inside child elements.
     * Use {@link #getTextRecursively()} to get the full text that recursively
     * includes the text content of the entire element tree.
     *
     * @see #getTextRecursively()
     * @see #setText(String)
     *
     * @return the text content of this element
     */
    public String getText() {
        return getTextContent(Element::isTextNode);
    }

    /**
     * Gets the text content of this element tree. This includes the text
     * content of all child nodes recursively. Use {@link #getText()} to only
     * get the text from text nodes that are immediate children of this element.
     *
     * @see #getText()
     *
     * @return the text content of this element and all child elements
     */
    public String getTextRecursively() {
        return getTextContent(e -> true);
    }

    /**
     * Returns the text content for this element by including children matching
     * the given filter.
     *
     * @param childFilter
     *            the filter used to decide whether to include a child or not
     * @return the text content for this element and any matching child nodes
     *         recursively, never {@code null}
     */
    private String getTextContent(Predicate<? super Element> childFilter) {
        if (isTextNode()) {
            return getStateProvider().getTextContent(getNode());
        } else {
            StringBuilder builder = new StringBuilder();
            appendTextContent(builder, childFilter);
            return builder.toString();
        }
    }

    private void appendTextContent(StringBuilder builder,
            Predicate<? super Element> childFilter) {
        if (isTextNode()) {
            builder.append(getText());
        } else {
            getChildren().filter(childFilter)
                    .forEach(e -> e.appendTextContent(builder, childFilter));
        }
    }

    /**
     * Gets the set of CSS class names used for this element. The returned set
     * can be modified to add or remove class names. The contents of the set is
     * also reflected in the value of the <code>class</code> attribute.
     * <p>
     * Despite the name implying a list being returned, the return type is
     * actually a {@link Set} since the in-browser return value behaves like a
     * <code>Set</code> in Java.
     *
     * @return a list of class names, never <code>null</code>
     */
    public ClassList getClassList() {
        return getStateProvider().getClassList(getNode());
    }

    /**
     * Gets the set of the theme names applied to the corresponding element in
     * {@code theme} attribute. The set returned can be modified to add or
     * remove the theme names, changes to the set will be reflected in the
     * attribute value.
     * <p>
     * Despite the name implying a list being returned, the return type is
     * actually a {@link Set} since the in-browser return value behaves like a
     * {@link Set} in Java.
     *
     * @return a list of theme names, never {@code null}
     */
    public ThemeList getThemeList() {
        return new ThemeListImpl(this);
    }

    /**
     * Gets the style instance for managing element inline styles.
     *
     * @return the style object for the element
     */
    public Style getStyle() {
        return getStateProvider().getStyle(getNode());
    }

    /**
     * Synchronize the given {@code property}'s value when the given
     * {@code eventType} occurs on this element on the client side. As a result
     * the {@code property}'s value is automatically updated to this
     * {@link Element}.
     * <p>
     * Only properties which can be set using setProperty can be synchronized,
     * e.g. classList cannot be synchronized.
     * <p>
     * This is convenience method for batching
     * {@link #addSynchronizedProperty(String)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     * <p>
     * The method is shorthand for
     * {@link #synchronizeProperty(String, String, DisabledUpdateMode)} with
     * {@literal DisabledUpdateMode.ONLY_WHEN_ENABLED} parameter value.
     *
     * @param property
     *            the property name to synchronize
     * @param eventType
     *            the client side event which trigger synchronization of the
     *            property values to the server
     *
     * @see #synchronizeProperty(String, String, DisabledUpdateMode)
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element synchronizeProperty(String property, String eventType) {
        return synchronizeProperty(property, eventType,
                DisabledUpdateMode.ONLY_WHEN_ENABLED);
    }

    /**
     * Synchronize the given {@code property}'s value when the given
     * {@code eventType} occurs on this element on the client side. As a result
     * the {@code property}'s value is automatically updated to this
     * {@link Element}.
     * <p>
     * Only properties which can be set using setProperty can be synchronized,
     * e.g. classList cannot be synchronized.
     * <p>
     * When multiple update mode settings are defined for the same property, the
     * most permissive mode is used. This means that there might be unexpected
     * updates for a disabled component if multiple parties independently
     * configure different aspects for the same component. This is based on the
     * assumption that if a property is explicitly safe to update for disabled
     * components in one context, then the nature of that property is probably
     * such that it's also safe to update in other contexts.
     * <p>
     * This is convenience method for batching
     * {@link #addSynchronizedProperty(String, DisabledUpdateMode)} and
     * {@link #addSynchronizedPropertyEvent(String)}.
     *
     * @param property
     *            the property name to synchronize
     * @param eventType
     *            the client side event which trigger synchronization of the
     *            property values to the server
     * @param mode
     *            controls property update from the client side to the server
     *            side when the element is disabled, not {@code null}
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element synchronizeProperty(String property, String eventType,
            DisabledUpdateMode mode) {
        addSynchronizedProperty(property, mode);
        addSynchronizedPropertyEvent(eventType);
        return this;
    }

    /**
     * Adds the property whose value should automatically be synchronized from
     * the client side and updated in this {@link Element}.
     * <p>
     * Synchronization takes place whenever one of the events defined using
     * {@link #addSynchronizedPropertyEvent(String)} is fired for the element.
     * <p>
     * Only properties which can be set using setProperty can be synchronized,
     * e.g. classList cannot be synchronized.
     * <p>
     * The method is shorthand for
     * {@link #addSynchronizedProperty(String, DisabledUpdateMode)} with
     * {@literal DisabledUpdateMode.ONLY_WHEN_ENABLED} parameter value.
     * {@link #addSynchronizedProperty(String, DisabledUpdateMode)}
     *
     * @param property
     *            the property name to synchronize
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element addSynchronizedProperty(String property) {
        return addSynchronizedProperty(property,
                DisabledUpdateMode.ONLY_WHEN_ENABLED);
    }

    /**
     * Adds the property whose value should automatically be synchronized from
     * the client side and updated in this {@link Element}.
     * <p>
     * Synchronization takes place whenever one of the events defined using
     * {@link #addSynchronizedPropertyEvent(String)} is fired for the element.
     * <p>
     * Only properties which can be set using setProperty can be synchronized,
     * e.g. classList cannot be synchronized.
     * <p>
     * When multiple update mode settings are defined for the same property, the
     * most permissive mode is used. This means that there might be unexpected
     * updates for a disabled component if multiple parties independently
     * configure different aspects for the same component. This is based on the
     * assumption that if a property is explicitly safe to update for disabled
     * components in one context, then the nature of that property is probably
     * such that it's also safe to update in other contexts.
     *
     * @param property
     *            the property name to synchronize
     * @param mode
     *            controls property update from the client side to the server
     *            side when the element is disabled, not {@code null}
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element addSynchronizedProperty(String property,
            DisabledUpdateMode mode) {
        verifySetPropertyName(property);
        if (mode == null) {
            throw new IllegalArgumentException(
                    "Property update control mode for disabled alement must not be null");
        }
        getStateProvider().addSynchronizedProperty(getNode(), property, mode);
        return this;
    }

    /**
     * Adds the event to use for property synchronization from the client side.
     * <p>
     * Synchronization takes place whenever one of the given events is fired for
     * the element (on the client side).
     * <p>
     * Use {@link #addSynchronizedProperty(String)} to define which properties
     * to synchronize.
     *
     * @param eventType
     *            the client side event which trigger synchronization of the
     *            property values to the server
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element addSynchronizedPropertyEvent(String eventType) {
        verifyEventType(eventType);
        getStateProvider().getSynchronizedPropertyEvents(getNode())
                .add(eventType);
        return this;
    }

    /**
     * Removes the property from the synchronized properties set (
     * {@link #getSynchronizedProperties()}).
     *
     * @see #addSynchronizedProperty(String)
     *
     * @param property
     *            the property name to remove
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element removeSynchronizedProperty(String property) {
        verifySetPropertyName(property);
        getStateProvider().getSynchronizedProperties(getNode())
                .remove(property);
        return this;
    }

    /**
     * Removes the event from the event set that is used for property
     * synchronization ({@link #getSynchronizedPropertyEvents()}).
     *
     * @see #addSynchronizedPropertyEvent(String)
     *
     * @param eventType
     *            the client side event which trigger synchronization of the
     *            property values to the server
     * @return this element
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Element removeSynchronizedPropertyEvent(String eventType) {
        verifyEventType(eventType);
        getStateProvider().getSynchronizedPropertyEvents(getNode())
                .remove(eventType);
        return this;
    }

    /**
     * Gets the properties whose values should automatically be synchronized
     * from the client side and updated in this {@link Element}.
     *
     * @see #addSynchronizedProperty(String)
     * @see #addSynchronizedPropertyEvent(String)
     *
     * @return the property names which are synchronized
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Stream<String> getSynchronizedProperties() {
        return getStateProvider().getSynchronizedProperties(getNode()).stream();
    }

    /**
     * Gets the events to use for property synchronization from the client side.
     *
     * @see #addSynchronizedProperty(String)
     * @see #addSynchronizedPropertyEvent(String)
     *
     * @return the client side events which trigger synchronization of the
     *         property values to the server
     * @deprecated Use
     *             {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     *             or
     *             {@link DomListenerRegistration#synchronizeProperty(String)}
     *             instead.
     */
    @Deprecated
    public Stream<String> getSynchronizedPropertyEvents() {
        return getStateProvider().getSynchronizedPropertyEvents(getNode())
                .stream();
    }

    /**
     * Gets the component this element has been mapped to, if any.
     *
     * @return an optional component, or an empty optional if no component has
     *         been mapped to this element
     */
    public Optional<Component> getComponent() {
        return getStateProvider().getComponent(getNode());
    }

    private String validateAttribute(String attribute, Object value) {
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
        return lowerCaseAttribute;
    }

    private static void verifyEventType(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type must not be null");
        }
    }

    /**
     * Adds an attach listener for this element. It is invoked when the element
     * is attached to a UI.
     * <p>
     * When a hierarchy of elements is being attached, the event is fired
     * child-first.
     *
     * @param attachListener
     *            the attach listener to add
     * @return an event registration handle for removing the listener
     */
    public Registration addAttachListener(
            ElementAttachListener attachListener) {
        if (attachListener == null) {
            throw new IllegalArgumentException(
                    "ElementAttachListener cannot be null");
        }

        return getNode().addAttachListener(
                // This explicit class instantiation is the workaround
                // which fixes a JVM optimization+serialization bug.
                // Do not convert to lambda
                // Detected under Win7_64 /JDK 1.8.0_152, 1.8.0_172
                // see ElementAttributeMap#deferRegistration
                new Command() {
                    @Override
                    public void execute() {
                        attachListener
                                .onAttach(new ElementAttachEvent(Element.this));
                    }
                });
    }

    /**
     * Adds a detach listener for this element. It is invoked when the element
     * is detached from a UI.
     * <p>
     * When a hierarchy of elements is being detached, the event is fired
     * child-first.
     *
     * @param detachListener
     *            the detach listener to add
     * @return an event registration handle for removing the listener
     */
    public Registration addDetachListener(
            ElementDetachListener detachListener) {
        if (detachListener == null) {
            throw new IllegalArgumentException(
                    "ElementDetachListener cannot be null");
        }

        return getNode().addDetachListener(
                // This explicit class instantiation is the workaround
                // which fixes a JVM optimization+serialization bug.
                // Do not convert to lambda
                // Detected under Win7_64 /JDK 1.8.0_152, 1.8.0_172
                // see ElementAttributeMap#deferRegistration
                new Command() {
                    @Override
                    public void execute() {
                        detachListener
                                .onDetach(new ElementDetachEvent(Element.this));
                    }
                });
    }

    @Override
    public String toString() {
        return getOuterHTML();
    }

    /**
     * Gets the outer HTML for the element.
     * <p>
     * This operation recursively iterates the element and all children and
     * should not be called unnecessarily.
     *
     * @return the outer HTML for the element
     */
    public String getOuterHTML() {
        return ElementUtil.toJsoup(new Document(""), this).outerHtml();
    }

    /**
     * Creates a new component instance using this element.
     * <p>
     * You can use this method when you have an element instance and want to use
     * it through the API of a {@link Component} class.
     * <p>
     * This method makes the component instance use the underlying element but
     * does not attach the new component instance to the element so that
     * {@link Element#getComponent()} would return the component instance. This
     * means that {@link #getParent()}, {@link #getChildren()} and possibly
     * other methods which rely on {@link Element} -&gt; {@link Component}
     * mappings will not work.
     * <p>
     * To also map the element to the {@link Component} instance, use
     * {@link Component#from(Element, Class)}
     *
     * @see Component#from(Element, Class)
     *
     * @param <T>
     *            the component type
     * @param componentType
     *            the component type
     * @return the component instance connected to the given element
     */
    public <T extends Component> T as(Class<T> componentType) {
        return ComponentUtil.componentFromElement(this, componentType, false);
    }

    /**
     * Calls the given function on the element with the given arguments.
     * <p>
     * The function will be called after all pending DOM updates have completed,
     * at the same time that {@link Page#executeJs(String, Serializable...)}
     * calls are invoked.
     * <p>
     * If the element is not attached, the function call will be deferred until
     * the element is attached.
     *
     * @see JsonCodec JsonCodec for supported argument types
     *
     * @param functionName
     *            the name of the function to call, may contain dots to indicate
     *            a function on a property.
     * @param arguments
     *            the arguments to pass to the function. Must be of a type
     *            supported by the communication mechanism, as defined by
     *            {@link JsonCodec}
     *
     * @deprecated Use {@link #callJsFunction(String,Serializable...)} instead
     *             since it also allows getting return value back.
     */
    @Deprecated
    public void callFunction(String functionName, Serializable... arguments) {
        // Ignore return value
        callJsFunction(functionName, arguments);
    }

    /**
     * Calls the given function on the element with the given arguments.
     * <p>
     * It is possible to get access to the return value of the execution by
     * registering a handler with the returned pending result. If no handler is
     * registered, the return value will be ignored.
     * <p>
     * The function will be called after all pending DOM updates have completed,
     * at the same time that {@link Page#executeJs(String, Serializable...)}
     * calls are invoked.
     * <p>
     * If the element is not attached, the function call will be deferred until
     * the element is attached.
     *
     * @see JsonCodec JsonCodec for supported argument types
     *
     * @param functionName
     *            the name of the function to call, may contain dots to indicate
     *            a function on a property.
     * @param arguments
     *            the arguments to pass to the function. Must be of a type
     *            supported by the communication mechanism, as defined by
     *            {@link JsonCodec}
     * @return a pending result that can be used to get a return value from the
     *         execution
     */
    public PendingJavaScriptResult callJsFunction(String functionName,
            Serializable... arguments) {
        assert functionName != null;
        assert !functionName
                .startsWith(".") : "Function name should not start with a dot";

        // "$1,$2,$3,..."
        String paramPlaceholderString = IntStream.range(1, arguments.length + 1)
                .mapToObj(i -> "$" + i).collect(Collectors.joining(","));
        // Inject the element as $0
        Stream<Serializable> jsParameters = Stream.concat(Stream.of(this),
                Stream.of(arguments));

        return scheduleJavaScriptInvocation("return $0." + functionName + "("
                + paramPlaceholderString + ")", jsParameters);
    }

    // When updating JavaDocs here, keep in sync with Page.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser in the
     * context of this element. This element will be available to the expression
     * as <code>this</code>. The given parameters will be available as variables
     * named <code>$0</code>, <code>$1</code>, and so on. Supported parameter
     * types are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link JsonValue}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
     * </ul>
     * Note that the parameter variables can only be used in contexts where a
     * JavaScript variable can be used. You should for instance do
     * <code>'prefix' + $0</code> instead of <code>'prefix$0'</code> and
     * <code>value[$0]</code> instead of <code>value.$0</code> since JavaScript
     * variables aren't evaluated inside strings or property names.
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @deprecated Use {@link #executeJs(String,Serializable...)} instead since
     *             it also allows getting return value back.
     */
    @Deprecated
    public void executeJavaScript(String expression,
            Serializable... parameters) {
        // Ignore return value
        executeJs(expression, parameters);
    }

    // When updating JavaDocs here, keep in sync with Page.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser in the
     * context of this element. The returned
     * <code>PendingJavaScriptResult</code> can be used to retrieve any
     * <code>return</code> value from the JavaScript expression. If no return
     * value handler is registered, the return value will be ignored.
     * <p>
     * This element will be available to the expression as <code>this</code>.
     * The given parameters will be available as variables named
     * <code>$0</code>, <code>$1</code>, and so on. Supported parameter types
     * are:
     * <ul>
     * <li>{@link String}
     * <li>{@link Integer}
     * <li>{@link Double}
     * <li>{@link Boolean}
     * <li>{@link JsonValue}
     * <li>{@link Element} (will be sent as <code>null</code> if the server-side
     * element instance is not attached when the invocation is sent to the
     * client)
     * </ul>
     * Note that the parameter variables can only be used in contexts where a
     * JavaScript variable can be used. You should for instance do
     * <code>'prefix' + $0</code> instead of <code>'prefix$0'</code> and
     * <code>value[$0]</code> instead of <code>value.$0</code> since JavaScript
     * variables aren't evaluated inside strings or property names.
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @return a pending result that can be used to get a value returned from
     *         the expression
     */
    public PendingJavaScriptResult executeJs(String expression,
            Serializable... parameters) {

        // Add "this" as the last parameter
        Stream<Serializable> wrappedParameters = Stream
                .concat(Stream.of(parameters), Stream.of(this));

        // Wrap in a function that is applied with last parameter as "this"
        String wrappedExpression = "return (function() { " + expression
                + "}).apply($" + parameters.length + ")";

        return scheduleJavaScriptInvocation(wrappedExpression,
                wrappedParameters);
    }

    private PendingJavaScriptResult scheduleJavaScriptInvocation(
            String expression, Stream<Serializable> parameters) {
        StateNode node = getNode();

        JavaScriptInvocation invocation = new JavaScriptInvocation(expression,
                parameters.toArray(Serializable[]::new));

        PendingJavaScriptInvocation pending = new PendingJavaScriptInvocation(
                node, invocation);

        node.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(node, context -> {
                    if (!pending.isCanceled()) {
                        context.getUI().getInternals()
                                .addJavaScriptInvocation(pending);
                    }
                }));

        return pending;
    }

    /**
     * Attaches shadow root node.
     *
     * @return the attached shadow root
     */
    public ShadowRoot attachShadow() {
        if (getShadowRoot().isPresent()) {
            throw new IllegalStateException(
                    "The element already has shadow root");
        }
        return ShadowRoot.get(getStateProvider().attachShadow(getNode()));
    }

    /**
     * Gets the shadow root of the element, if any.
     *
     * @return an optional shadow root node, or an empty optional if no shadow
     *         root has been attached
     */
    public Optional<ShadowRoot> getShadowRoot() {
        StateNode shadowRoot = getStateProvider().getShadowRoot(getNode());
        if (shadowRoot == null) {
            return Optional.empty();
        }
        return Optional.of(ShadowRoot.get(shadowRoot));
    }

    /**
     * Sets the element visibility value.
     *
     * @param visible
     *            the element visibility value
     * @return this element
     */
    public Element setVisible(boolean visible) {
        getStateProvider().setVisible(getNode(), visible);
        return getSelf();
    }

    /**
     * Gets the element visibility value.
     *
     * @return {@code true} if the element is visible, {@code false} otherwise
     */
    public boolean isVisible() {
        return getStateProvider().isVisible(getNode());
    }

    /**
     * Sets the enabled state of the element.
     *
     * @param enabled
     *            the enabled state
     * @return the element
     */
    public Element setEnabled(final boolean enabled) {
        getNode().setEnabled(enabled);

        Optional<Component> componentOptional = getComponent();
        if (componentOptional.isPresent()) {
            Component component = componentOptional.get();
            component.onEnabledStateChanged(enabled);
            informChildrenOfStateChange(enabled, component);
        }
        return getSelf();
    }

    private void informChildrenOfStateChange(boolean enabled,
            Component component) {
        component.getChildren().forEach(child -> {
            child.onEnabledStateChanged(
                    enabled ? child.getElement().isEnabled() : false);
            informChildrenOfStateChange(enabled, child);
        });
        if (component.getElement().getNode()
                .hasFeature(VirtualChildrenList.class)) {
            component.getElement().getNode()
                    .getFeatureIfInitialized(VirtualChildrenList.class)
                    .ifPresent(list -> {
                        final Consumer<Component> stateChangeInformer = virtual -> {
                            virtual.onEnabledStateChanged(
                                    enabled ? virtual.getElement().isEnabled()
                                            : false);

                            informChildrenOfStateChange(enabled, virtual);
                        };
                        final Consumer<StateNode> childNodeConsumer = childNode -> Element
                                .get(childNode).getComponent()
                                .ifPresent(stateChangeInformer);

                        list.forEachChild(childNodeConsumer);
                    });
        }
    }

    /**
     * Get the enabled state of the element.
     * <p>
     * Object may be enabled by itself by but if its ascendant is disabled then
     * it's considered as (implicitly) disabled.
     *
     *
     * @return {@code true} if node is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return getNode().isEnabled();
    }

    @Override
    protected Element getSelf() {
        return this;
    }

}

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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.dom.impl.BasicTextElementStateProvider;
import com.vaadin.flow.dom.impl.CustomAttribute;
import com.vaadin.flow.dom.impl.ThemeListImpl;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JavaScriptSemantics;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.TextBindingFeature;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.streams.ElementRequestHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

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

    private static final String USE_SET_PROPERTY_WITH_JSON_NULL = "setProperty(name, Json.createNull()) must be used to set a property to null";
    private static final String USE_SET_PROPERTY_WITH_JACKSON_NULL = "setProperty(name, JacksonUtils.nullNode()) must be used to set a property to null";

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
     * Gets the element mapped to the given state node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @return the element for the node, not <code>null</code>
     */
    public static Element get(StateNode node) {
        assert node != null;

        return ElementUtil.from(node)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Node is not valid as an element"));
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
     *            the text in the node, <code>null</code> is interpreted as an
     *            empty string
     * @return an element representing the text node, never <code>null</code>
     */
    public static Element createText(String text) {
        if (text == null) {
            text = "";
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
     * Binds a {@link Signal}'s value to a given attribute and creates a Signal
     * effect function executing the setter whenever the signal value changes.
     * <code>null</code> signal unbinds existing binding.
     * <p>
     * Same rules applies for the attribute name and value from the bound Signal
     * as in {@link #setAttribute(String, String)}.
     * <p>
     * Attribute value is synchronized with the signal value when element is
     * attached. When the element is detached, signal value changes have no
     * effect.
     * <p>
     * While a Signal is bound to an attribute, any attempt to set or remove
     * attribute value manually throws
     * {@link com.vaadin.signals.BindingActiveException}. Same happens when
     * trying to bind a new Signal while one is already bound.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ValueSignal&lt;String&gt; signal = new ValueSignal&lt;&gt;("");
     * Element element = new Element("span");
     * getElement().appendChild(element);
     * element.bindAttribute("mol", signal);
     * signal.value("42"); // The element now has attribute mol="42"
     * </pre>
     *
     * @param attribute
     *            the name of the attribute
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setAttribute(String, String)
     */
    public void bindAttribute(String attribute, Signal<String> signal) {
        String validAttribute = validateAttribute(attribute);
        if (!ElementUtil.isValidAttributeName(validAttribute)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute \"%s\" is not a valid attribute name",
                    validAttribute));
        }

        SerializableSupplier<Registration> bindAction = signal == null ? null
                : () -> ElementEffect.bind(this, signal, (component,
                        value) -> setAttributeValue(attribute, value, true));

        Optional<CustomAttribute> customAttribute = CustomAttribute
                .get(validAttribute);
        if (customAttribute.isPresent()) {
            throw new UnsupportedOperationException(
                    "Binding style or class attribute to a Signal is not supported.");
        } else {
            getStateProvider().bindAttributeSignal(getNode(), validAttribute,
                    signal, bindAction);
        }
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
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        return setAttributeValue(attribute, value, false);
    }

    private Element setAttributeValue(String attribute, String value,
            boolean ignoreSignal) {
        String lowerCaseAttribute = validateAttribute(attribute);

        Optional<CustomAttribute> customAttribute = CustomAttribute
                .get(lowerCaseAttribute);
        if (customAttribute.isPresent()) {
            if (value == null) {
                throw new IllegalArgumentException("Value cannot be null");
            }
            customAttribute.get().setAttribute(this, value);
        } else {
            // null value should be allowed only from signal value changes to
            // support removal of attribute
            getStateProvider().setAttribute(getNode(), lowerCaseAttribute,
                    value, ignoreSignal);
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
        String lowerCaseAttribute = validateAttribute(attribute);
        if (resource == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

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
     * Sets the given attribute to the given {@link ElementRequestHandler}
     * value.
     * <p>
     * Attribute names are considered case insensitive and all names will be
     * converted to lower case automatically.
     * <p>
     * This is a convenience method to register a {@link ElementRequestHandler}
     * instance into the session and use the registered resource URI as an
     * element attribute.
     *
     * @see #setAttribute(String, String)
     *
     * @param attribute
     *            the name of the attribute
     * @param requestHandler
     *            the resource value, not null
     * @return this element
     */
    public Element setAttribute(String attribute,
            ElementRequestHandler requestHandler) {
        AbstractStreamResource resource = new StreamResourceRegistry.ElementStreamResource(
                requestHandler, this);

        return setAttribute(attribute, resource);
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
                .filter(attr -> attr.get().hasAttribute(this)).count() == 0
                : "Overlap between stored attributes and existing custom attributes";

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
     * <p>
     * This will send a detach event for the node, fully releasing it on the
     * client.
     *
     * @return this element
     */
    public Element removeFromTree() {
        return removeFromTree(true);
    }

    /**
     * Removes this element from its parent and state tree.
     * <p>
     * sendDetach can be given as false to fully reset the node and not send a
     * detach event. This for instance when having preserveOnRefresh that only
     * moves the node.
     * <p>
     * This method is meant for internal use only.
     *
     * @param sendDetach
     *            if the detach event should be sent to the client
     * @return this element
     */
    public Element removeFromTree(boolean sendDetach) {
        Node<?> parent = getParentNode();
        if (parent != null) {
            if (parent.getChildren().anyMatch(Predicate.isEqual(this))) {
                parent.removeChild(this);
            }
            if (isVirtualChild()) {
                parent.removeVirtualChild(this);
            }
        }
        getNode().removeFromTree(sendDetach);
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
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * value, since {@link com.vaadin.flow.internal.JacksonUtils#nullNode()}
     * should be used instead for JSON values.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    // Distinct name so setProperty("foo", null) is not ambiguous
    public Element setPropertyJson(String name, BaseJsonNode value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    USE_SET_PROPERTY_WITH_JACKSON_NULL);
        }

        setRawProperty(name, value);
        return this;
    }

    /**
     * Sets the given property to the given bean, converted to a JSON object.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    // Distinct name so setProperty("foo", null) is not ambiguous
    public Element setPropertyBean(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(USE_SET_PROPERTY_WITH_JSON_NULL);
        }
        return setPropertyJson(name, JacksonUtils.beanToJson(value));
    }

    /**
     * Sets the given property to the given list of beans or primitive values,
     * converted to a JSON array.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param <T>
     *            the type of items in the list
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    public <T> Element setPropertyList(String name, List<T> value) {
        if (value == null) {
            throw new IllegalArgumentException(USE_SET_PROPERTY_WITH_JSON_NULL);
        }

        return setPropertyJson(name, JacksonUtils.listToJson(value));
    }

    /**
     * Sets the given property to the given map of beans or primitive values,
     * converted to a JSON object.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param name
     *            the property name, not <code>null</code>
     * @param value
     *            the property value, not <code>null</code>
     * @return this element
     */
    public Element setPropertyMap(String name, Map<String, ?> value) {
        if (value == null) {
            throw new IllegalArgumentException(USE_SET_PROPERTY_WITH_JSON_NULL);
        }

        return setPropertyJson(name, JacksonUtils.mapToJson(value));
    }

    /**
     * Adds a property change listener which is triggered when the property's
     * value is updated on the server side.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @see #addPropertyChangeListener(String, String, PropertyChangeListener)
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
        verifySetPropertyName(name);

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
            throw new IllegalArgumentException(
                    "Can't set or synchronize " + name + " as a property, use "
                            + replacement + " instead.");
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
        if (value == null || value instanceof NullNode) {
            return defaultValue;
        } else if (value instanceof JsonNode) {
            return ((JsonNode) value).toString();
        } else if (value instanceof NullNode) {
            return defaultValue;
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
        } else if (value instanceof BooleanNode) {
            return ((BooleanNode) value).booleanValue() ? 1 : 0;
        } else if (value instanceof JsonNode) {
            return ((JsonNode) value).asDouble(Double.NaN);
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
     * {@link BaseJsonNode}. <code>null</code> is returned if there is no
     * property with the given name or if the value is set to <code>null</code>.
     *
     * @param name
     *            the property name, not null
     * @return the raw property value, or <code>null</code>
     */
    public Serializable getPropertyRaw(String name) {
        return getStateProvider().getProperty(getNode(), name);
    }

    /**
     * Gets the value of the given property, deserialized as the given type.
     * This method supports arbitrary bean types through Jackson
     * deserialization.
     * <p>
     * Example usage:
     *
     * <pre>
     * MyDto dto = element.getPropertyBean("userData", MyDto.class);
     * </pre>
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param <T>
     *            the type to deserialize to
     * @param name
     *            the property name, not <code>null</code>
     * @param type
     *            the class to deserialize the property value to, not
     *            <code>null</code>
     * @return the property value deserialized as the given type, or
     *         <code>null</code> if not set
     */
    public <T> T getPropertyBean(String name, Class<T> type) {
        Serializable raw = getPropertyRaw(name);
        if (raw == null || raw instanceof NullNode) {
            return null;
        }
        return com.vaadin.flow.internal.JacksonCodec.decodeAs((JsonNode) raw,
                type);
    }

    /**
     * Gets the value of the given property, deserialized as the type specified
     * by the {@link TypeReference}. This method supports generic types such as
     * {@code List<MyBean>} and {@code Map<String, MyBean>} through Jackson's
     * TypeReference mechanism.
     * <p>
     * Example usage:
     *
     * <pre>
     * TypeReference&lt;List&lt;MyDto&gt;&gt; typeRef = new TypeReference&lt;List&lt;MyDto&gt;&gt;() {
     * };
     * List&lt;MyDto&gt; dtos = element.getPropertyBean("userList", typeRef);
     * </pre>
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
     *
     * @param <T>
     *            the type to deserialize to
     * @param name
     *            the property name, not <code>null</code>
     * @param typeReference
     *            the type reference describing the target type, not
     *            <code>null</code>
     * @return the property value deserialized as the given type, or
     *         <code>null</code> if not set
     */
    public <T> T getPropertyBean(String name, TypeReference<T> typeReference) {
        Serializable raw = getPropertyRaw(name);
        if (raw == null || raw instanceof NullNode) {
            return null;
        }
        return com.vaadin.flow.internal.JacksonCodec.decodeAs((JsonNode) raw,
                typeReference);
    }

    /**
     * Removes the given property.
     * <p>
     * Note that properties changed on the server are updated on the client but
     * changes made on the client side are not reflected back to the server
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * and {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * unless configured using
     * {@link #addPropertyChangeListener(String, String, PropertyChangeListener)}
     * or {@link DomListenerRegistration#synchronizeProperty(String)}.
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
     * @throws BindingActiveException
     *             if a binding has been set on the text content of this element
     */
    public Element setText(String textContent) {
        TextBindingFeature feature = getNode()
                .getFeature(TextBindingFeature.class);
        if (feature.hasBinding()) {
            throw new BindingActiveException(
                    "setText is not allowed while a binding for text exists.");
        }

        if (textContent == null) {
            // Browsers work this way
            textContent = "";
        }
        setTextContent(textContent);

        return this;
    }

    private void setTextContent(String textContent) {
        if (isTextNode()) {
            getStateProvider().setTextContent(getNode(), textContent);
        } else {
            if (textContent.isEmpty()) {
                removeAllChildren();
            } else {
                Element child;
                if (getChildCount() == 1 && getChild(0).isTextNode()) {
                    child = getChild(0).setText(textContent);
                } else {
                    child = createText(textContent);
                }
                removeAllChildren();
                appendChild(child);
            }
        }
    }

    /**
     * Binds a {@link Signal}'s value to the text content of this element and
     * creates a Signal effect function executing the setter whenever the signal
     * value changes. <code>null</code> signal unbinds the existing binding.
     * <p>
     * Text content is synchronized with the signal value whenever the element
     * is attached. When the element is detached, signal value changes have no
     * effect.
     * <p>
     * While a Signal is bound to an attribute, any attempt to set the text
     * content manually throws
     * {@link com.vaadin.signals.BindingActiveException}. Same happens when
     * trying to bind a new Signal while one is already bound.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ValueSignal&lt;String&gt; signal = new ValueSignal&lt;&gt;("");
     * Element element = new Element("span");
     * getElement().appendChild(element);
     * element.bindText(signal);
     * signal.value("text"); // The element text content is set to "text"
     * </pre>
     *
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setText(String)
     */
    public void bindText(Signal<String> signal) {
        TextBindingFeature feature = getNode()
                .getFeature(TextBindingFeature.class);

        if (signal == null) {
            feature.removeBinding();
        } else {
            if (feature.hasBinding() && getNode().isAttached()) {
                throw new BindingActiveException();
            }

            Registration registration = ElementEffect.bind(this, signal,
                    (element, value) -> setTextContent(value));
            feature.setBinding(registration, signal);
        }
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
     * Gets the component this element has been mapped to, if any.
     *
     * @return an optional component, or an empty optional if no component has
     *         been mapped to this element
     */
    public Optional<Component> getComponent() {
        return getStateProvider().getComponent(getNode());
    }

    private String validateAttribute(String attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException(ATTRIBUTE_NAME_CANNOT_BE_NULL);
        }

        String lowerCaseAttribute = attribute.toLowerCase(Locale.ENGLISH);
        if (!ElementUtil.isValidAttributeName(lowerCaseAttribute)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute \"%s\" is not a valid attribute name",
                    lowerCaseAttribute));
        }
        return lowerCaseAttribute;
    }

    static void verifyEventType(String eventType) {
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
     * It is possible to get access to the return value of the execution by
     * registering a handler with the returned pending result. If no handler is
     * registered, the return value will be ignored.
     * <p>
     * The function will be called after all pending DOM updates have completed,
     * at the same time that {@link Page#executeJs(String, Object...)} calls are
     * invoked.
     * <p>
     * If the element is not attached or not visible, the function call will be
     * deferred until the element is attached and visible.
     *
     * @param functionName
     *            the name of the function to call, may contain dots to indicate
     *            a function on a property.
     * @param arguments
     *            the arguments to pass to the function. All types supported by
     *            Jackson for JSON serialization are supported. Special cases:
     *            {@link Element} instances (will be sent as DOM element
     *            references to the browser if attached when invoked, or as
     *            <code>null</code> if not attached).
     * @return a pending result that can be used to get a return value from the
     *         execution
     */
    public PendingJavaScriptResult callJsFunction(String functionName,
            Object... arguments) {
        assert functionName != null;
        assert !functionName.startsWith(".")
                : "Function name should not start with a dot";

        // "$1,$2,$3,..."
        String paramPlaceholderString = IntStream.range(1, arguments.length + 1)
                .mapToObj(i -> "$" + i).collect(Collectors.joining(","));
        // Inject the element as $0
        Serializable[] jsParameters;
        if (arguments.length == 0) {
            jsParameters = new Serializable[] { this };
        } else {
            jsParameters = new Serializable[arguments.length + 1];
            jsParameters[0] = this;
            System.arraycopy(arguments, 0, jsParameters, 1, arguments.length);
        }

        return scheduleJavaScriptInvocation("return $0." + functionName + "("
                + paramPlaceholderString + ")", jsParameters);
    }

    /**
     * Calls the given JavaScript function with this element as
     * <code>this</code> and the given arguments.
     *
     * @deprecated Use {@link #callJsFunction(String, Object...)} instead. This
     *             method exists only for binary compatibility.
     * @param functionName
     *            the name of the function to call
     * @param arguments
     *            the arguments to pass to the function
     * @return a pending result that can be used to get a return value from the
     *         execution
     */
    @Deprecated
    public PendingJavaScriptResult callJsFunction(String functionName,
            Serializable[] arguments) {
        return callJsFunction(functionName, (Object[]) arguments);
    }

    // When updating JavaDocs here, keep in sync with Page.executeJavaScript
    /**
     * Asynchronously runs the given JavaScript expression in the browser in the
     * context of this element. The expression is executed in an
     * <code>async</code> JavaScript method, so you can utilize
     * <code>await</code> syntax when consuming JavaScript API returning a
     * <code>Promise</code>. The returned <code>PendingJavaScriptResult</code>
     * can be used to retrieve the <code>return</code> value from the JavaScript
     * expression. If a <code>Promise</code> is returned in the JavaScript
     * expression, <code>PendingJavaScriptResult</code> will report the resolved
     * value once it becomes available. If no return value handler is
     * registered, the return value will be ignored.
     * <p>
     * Return values from JavaScript can be automatically deserialized into Java
     * objects. All types supported by Jackson for JSON deserialization are
     * supported as return values, including custom bean classes.
     * <p>
     * This element will be available to the expression as <code>this</code>.
     * The given parameters will be available as variables named
     * <code>$0</code>, <code>$1</code>, and so on. All types supported by
     * Jackson for JSON serialization are supported as parameters. Special
     * cases:
     * <ul>
     * <li>{@link Element} (will be sent as a DOM element reference to the
     * browser if the server-side element instance is attached when the
     * invocation is sent to the client, or as <code>null</code> if not
     * attached)
     * <li>{@link BaseJsonNode} (sent as-is without additional wrapping)
     * </ul>
     * Note that the parameter variables can only be used in contexts where a
     * JavaScript variable can be used. You should for instance do
     * <code>'prefix' + $0</code> instead of <code>'prefix$0'</code> and
     * <code>value[$0]</code> instead of <code>value.$0</code> since JavaScript
     * variables aren't evaluated inside strings or property names.
     * <p>
     * If the element is not attached or not visible, the function call will be
     * deferred until the element is attached and visible.
     *
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @return a pending result that can be used to get a value returned from
     *         the expression
     */
    public PendingJavaScriptResult executeJs(String expression,
            Object... parameters) {

        // Add "this" as the last parameter
        Object[] wrappedParameters;
        if (parameters.length == 0) {
            wrappedParameters = new Object[] { this };
        } else {
            wrappedParameters = Arrays.copyOf(parameters,
                    parameters.length + 1);
            wrappedParameters[parameters.length] = this;
        }

        // Wrap in a function that is applied with last parameter as "this"
        String wrappedExpression = "return (async function() { " + expression
                + "}).apply($" + parameters.length + ")";

        return scheduleJavaScriptInvocation(wrappedExpression,
                wrappedParameters);
    }

    /**
     * Asynchronously runs the given JavaScript expression in the browser in the
     * context of this element.
     *
     * @deprecated Use {@link #executeJs(String, Object...)} instead. This
     *             method exists only for binary compatibility.
     * @param expression
     *            the JavaScript expression to invoke
     * @param parameters
     *            parameters to pass to the expression
     * @return a pending result that can be used to get a value returned from
     *         the expression
     */
    @Deprecated
    public PendingJavaScriptResult executeJs(String expression,
            Serializable[] parameters) {
        return executeJs(expression, (Object[]) parameters);
    }

    private PendingJavaScriptResult scheduleJavaScriptInvocation(
            String expression, Object[] parameters) {
        StateNode node = getNode();

        JavaScriptInvocation invocation = new JavaScriptInvocation(expression,
                parameters);

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
     * Also executes pending javascript invocations, if their execution was
     * requested while the element was not visible, and the element is now set
     * as visible.
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

        informEnabledStateChange(enabled, this);
        return getSelf();
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

    private void informEnabledStateChange(boolean enabled, Element element) {
        Optional<Component> componentOptional = element.getComponent();
        if (componentOptional.isPresent()) {
            Component component = componentOptional.get();
            component.onEnabledStateChanged(
                    enabled ? element.isEnabled() : false);
        }
        informChildrenOfStateChange(enabled, element);
    }

    private void informChildrenOfStateChange(boolean enabled, Element element) {
        element.getChildren().forEach(child -> {
            informEnabledStateChange(enabled, child);
        });
        if (element.getNode().hasFeature(VirtualChildrenList.class)) {
            element.getNode().getFeatureIfInitialized(VirtualChildrenList.class)
                    .ifPresent(list -> list.forEachChild(
                            node -> informEnabledStateChange(enabled,
                                    Element.get(node))));
        }
    }

    /**
     * Executes the similarly named DOM method on the client side.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView">Mozilla
     *      docs</a>
     * @return the element
     */
    public Element scrollIntoView() {
        return scrollIntoView(null);
    }

    /**
     * Executes the similarly named DOM method on the client side.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView">Mozilla
     *      docs</a>
     * @param scrollOptions
     *            the scroll options to pass to the method
     * @return the element
     */
    public Element scrollIntoView(ScrollOptions scrollOptions) {
        // for an unknown reason, needs to be called deferred to work on a newly
        // created element
        String options = scrollOptions == null ? "" : scrollOptions.toJson();

        executeJs("var el = this; setTimeout(function() {el.scrollIntoView("
                + options + ");}, 0);");
        return getSelf();
    }

}

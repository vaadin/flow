package com.vaadin.hummingbird.dom;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;

/**
 * A class representing an element in the application server side DOM.
 * <p>
 * Contains methods for updating and querying various parts of the element, such
 * as attributes.
 *
 * @author Vaadin
 * @since
 */
public class Element implements Serializable {

    private ElementStateProvider stateProvider;
    private StateNode node;
    private static HashMap<String, String> unsettableAttributes = new HashMap<>();

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
        this(BasicElementStateProvider.get(),
                BasicElementStateProvider.createStateNode(tag));
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
        this(BasicElementStateProvider.get(),
                BasicElementStateProvider.createStateNode(tag, is));
    }

    /**
     * Creates an element using the given state provider and element node.
     *
     * @param stateProvider
     *            the state provider to use
     * @param elementNode
     *            the state node to use with the state provider
     */
    private Element(ElementStateProvider stateProvider, StateNode node) {
        assert stateProvider.supports(node) : "ElementStateProvider "
                + stateProvider + " does not support node " + node;

        this.stateProvider = stateProvider;
        this.node = node;

    }

    /**
     * Gets the node this element is connected to
     *
     * @return the node for this element
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Gets the tag name for the element
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
        assert isValidAttributeName(attribute) : "Attribute " + attribute
                + " is not a valid attribute name";
        if (unsettableAttributes.containsKey(attribute)) {
            throw new IllegalArgumentException("You cannot set the attribute \""
                    + attribute + "\" for an element using setAttribute: "
                    + unsettableAttributes.get(attribute));
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        stateProvider.setAttribute(node, attribute, value);
        return this;
    }

    /**
     * Gets the value of the given attribute.
     * <p>
     * An attribute always has a String key and a String value.
     *
     * @param attribute
     *            the name of the attribute
     * @return the value of the attribute or null if the attribute has not been
     *         set
     */
    public String getAttribute(String attribute) {
        assert isValidAttributeName(attribute) : "Attribute " + attribute
                + " is not a valid attribute name";
        return stateProvider.getAttribute(node, attribute);
    }

    /**
     * Checks if the given attribute has been set.
     * <p>
     * Note that this will return false for properties set with
     * {@link PropertyMode#PROPERTY_OR_ATTRIBUTE}.
     *
     * @param attribute
     *            the name of the attribute
     * @return true if the attribute has been set, false otherwise
     */
    public boolean hasAttribute(String attribute) {
        assert isValidAttributeName(attribute) : "Attribute " + attribute
                + " is not a valid attribute name";

        return stateProvider.hasAttribute(node, attribute);
    }

    /**
     * Gets the defined attributes names.
     *
     * @return the defined attribute names
     */
    public Set<String> getAttributeNames() {
        return stateProvider.getAttributeNames(node);
    }

    /**
     * Removes the given attribute.
     * <p>
     * If the attribute has not been set, does nothing.
     *
     * @param attribute
     *            the name of the attribute
     * @return this element
     */
    public Element removeAttribute(String attribute) {
        assert isValidAttributeName("attribute");
        stateProvider.removeAttribute(node, attribute);
        return this;
    }

    /**
     * Checks if the given attribute name is valid.
     *
     * @param attribute
     *            the name of the attribute
     * @return true if the name is valid, false otherwise
     */
    private boolean isValidAttributeName(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return false;
        }
        // https://html.spec.whatwg.org/multipage/syntax.html#attributes-2
        // Attribute names must consist of one or more characters other than the
        // space characters, U+0000 NULL, U+0022 QUOTATION MARK ("), U+0027
        // APOSTROPHE ('), U+003E GREATER-THAN SIGN (>), U+002F SOLIDUS (/), and
        // U+003D EQUALS SIGN (=) characters, the control characters, and any
        // characters that are not defined by Unicode.
        if (attribute.indexOf(0) != -1 || attribute.indexOf(' ') != -1
                || attribute.indexOf('"') != -1 || attribute.indexOf('\'') != -1
                || attribute.indexOf('>') != -1 || attribute.indexOf('/') != -1
                || attribute.indexOf('=') != -1) {
            return false;
        }
        return true;
    }

}
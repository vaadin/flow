package com.vaadin.hummingbird.dom.impl;

import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ElementStateProvider;
import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;

/**
 * Implementation which stores data for basic elements, i.e. elements which are
 * not bound to any template and have no special functionality.
 * <p>
 * This should be considered a low level class focusing on performance and
 * leaving most sanity checks to the caller.
 * <p>
 * The data is stored directly in the state node but this should be considered
 * an implementation detail which can change.
 *
 * @author Vaadin
 * @since
 */
public class BasicElementStateProvider implements ElementStateProvider {

    private static BasicElementStateProvider instance = new BasicElementStateProvider();

    private BasicElementStateProvider() {
        // Not meant to be sub classed and only once instance should ever exist
    }

    /**
     * Gets the one and only instance
     *
     * @return the instance to use for all basic elements
     */
    public static BasicElementStateProvider get() {
        return instance;
    }

    /**
     * Creates a compatible element state node using the given {@code tag}.
     *
     * @param tag
     *            the tag to use for the element
     * @return a initialized and compatible state node
     */
    public static StateNode createStateNode(String tag) {
        assert isValidTagName(tag) : "Invalid tag name " + tag;
        StateNode node = new StateNode(ElementDataNamespace.class,
                ElementAttributeNamespace.class);

        node.getNamespace(ElementDataNamespace.class).setTag(tag);

        return node;
    }

    /**
     * Creates a compatible element state node using the given {@code tag} and
     * {@code is} attribute.
     *
     * @param tag
     *            the tag to use for the element
     * @param is
     *            the is attribute to use for the element
     * @return a initialized and compatible state node
     */
    public static StateNode createStateNode(String tag, String is) {
        assert is != null && !is.isEmpty();
        StateNode node = createStateNode(tag);
        node.getNamespace(ElementDataNamespace.class).setIs(is);
        return node;
    }

    /**
     * Checks if the given string is valid as a tag name
     *
     * @param tag
     *            the string to check
     * @return true if the string is valid as a tag name, false otherwise
     */
    private static boolean isValidTagName(String tag) {
        // https://www.w3.org/TR/html-markup/syntax.html#tag-name
        // "HTML elements all have names that only use characters in the range
        // 0–9, a–z, and A–Z."
        return tag != null && tag.matches("^[a-zA-Z0-9-]+$");
    }

    @Override
    public boolean supports(StateNode node) {
        ElementDataNamespace ns = node.getNamespace(ElementDataNamespace.class);
        return ns != null && ns.getTag() != null;
    }

    @Override
    public String getTag(StateNode node) {
        return getDataNamespace(node).getTag();
    }

    /**
     * Gets the element data namespace for the given node and asserts it is
     * non-null
     *
     * @param node
     *            the node
     * @return the data name space
     */
    private ElementDataNamespace getDataNamespace(StateNode node) {
        ElementDataNamespace ns = node.getNamespace(ElementDataNamespace.class);
        assert ns != null;
        return ns;
    }

    /**
     * Gets the element attribute namespace for the given node and asserts it is
     * non-null
     *
     * @param node
     *            the node
     * @return the data name space
     */
    private ElementAttributeNamespace getAttributeNamespace(StateNode node) {
        ElementAttributeNamespace ns = node
                .getNamespace(ElementAttributeNamespace.class);
        assert ns != null;
        return ns;
    }

    @Override
    public void setAttribute(StateNode node, String attribute, String value) {
        getAttributeNamespace(node).set(attribute, value);

    }

    @Override
    public String getAttribute(StateNode node, String attribute) {
        if ("is".equals(attribute)) {
            return getDataNamespace(node).getIs();
        }
        return getAttributeNamespace(node).get(attribute);
    }

    @Override
    public boolean hasAttribute(StateNode node, String attribute) {
        return getAttributeNamespace(node).has(attribute);
    }

    @Override
    public void removeAttribute(StateNode node, String attribute) {
        getAttributeNamespace(node).remove(attribute);
    }

    @Override
    public Set<String> getAttributeNames(StateNode node) {
        return getAttributeNamespace(node).attributes();
    }

}

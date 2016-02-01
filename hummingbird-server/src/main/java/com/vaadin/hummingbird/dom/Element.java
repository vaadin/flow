package com.vaadin.hummingbird.dom;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.BasicElementStateProvider;

/**
 * A class representing an element in the application server side DOM.
 *
 * @author Vaadin
 * @since
 */
public class Element implements Serializable {

    private ElementStateProvider stateProvider;
    private StateNode node;

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
     * Gets the tag name for the element
     *
     * @param node
     *            the node containing the data
     * @return the tag name
     */
    public String getTag() {
        return stateProvider.getTag(node);
    }

}
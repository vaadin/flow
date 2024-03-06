/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

import elemental.dom.Element;

/**
 * Mapping between a server side node identifier which has been requested to
 * attach existing client side element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class ExistingElementMap {

    private final JsMap<Element, Integer> elementToId = JsCollections.map();
    // JsArray is used as a Map<Integer,Element> here. So this is a map between
    // an id and an Element.
    private final JsArray<Element> idToElement = JsCollections.array();

    /**
     * Gets the element stored via the {@link #add(int, Element)} method by the
     * given {@code id}.
     *
     * @param id
     *            identifier associated with an element
     * @return the element associated with the {@code id} or null if it doesn't
     *         exist
     */
    public Element getElement(int id) {
        return idToElement.get(id);
    }

    /**
     * Gets the id stored via the {@link #add(int, Element)} method by the given
     * {@code element}.
     *
     * @param element
     *            element associated with an identifier
     * @return the identifier associated with the {@code element} or null if it
     *         doesn't exist
     */
    public Integer getId(Element element) {
        return elementToId.get(element);
    }

    /**
     * Remove the identifier and the associated element from the mapping.
     *
     * @param id
     *            identifier to remove
     */
    public void remove(int id) {
        Element element = idToElement.get(id);
        if (element != null) {
            idToElement.set(id, null);
            elementToId.delete(element);
        }
    }

    /**
     * Adds the {@code id} and the {@code element} to the mapping.
     *
     * @param id
     *            identifier of the server side node
     * @param element
     *            element associated with the identifier
     */
    public void add(int id, Element element) {
        idToElement.set(id, element);
        elementToId.set(element, id);
    }

}

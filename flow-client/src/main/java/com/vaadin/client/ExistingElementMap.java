/*
 * Copyright 2000-2019 Vaadin Ltd.
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

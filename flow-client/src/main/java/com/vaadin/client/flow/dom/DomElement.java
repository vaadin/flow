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
package com.vaadin.client.flow.dom;

import com.vaadin.client.flow.collection.JsArray;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.HTMLCollection;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Element that has all methods from
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Element">Element
 * API</a> that have been overridden in
 * <a href="https://www.polymer-project.org/1.0/docs/devguide/local-dom">Polymer
 * DOM module</a>.
 * <p>
 * No instances of this class should be created directly, but instead
 * {@link DomApi#wrap(elemental.dom.Node)} should be used
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true)
public interface DomElement extends DomNode {

    /**
     * A
     * <a href="https://developer.mozilla.org/en-US/docs/Web/API/DOMTokenList">
     * DOMTokenList</a> java representation.
     */
    @JsType(isNative = true)
    interface DomTokenList {
        /**
         * Returns the <code>length</code> property.
         *
         * @return the token list length
         */
        @JsProperty
        int getLength();

        /**
         * Returns an item in the list by its index.
         *
         * @param index
         *            the index to look for the item
         * @return the token at the given index
         */
        String item(int index);

        /**
         * Returns whether the underlying string contains <code>token</code>.
         *
         * @param token
         *            the token to check for
         * @return <code>true</code> if token was found, <code>false</code> if
         *         not
         */
        boolean contains(String token);

        /**
         * Adds <code>token</code> to the underlying string.
         *
         * @param token
         *            the token to add
         */
        void add(String token);

        /**
         * Removes <code>token</code> from the underlying string.
         *
         * @param token
         *            the token to remove
         */
        void remove(String token);

        /**
         * Removes <code>token</code> from string and returns <code>false</code>
         * . If <code>token</code> doesn't exist it's added and the function
         * returns <code>true</code>.
         *
         * @param token
         *            the token to toggle
         * @return <code>true</code> if token did not exist and was added,
         *         <code>false</code> if token existed and was removed
         */
        boolean toggle(String token);
    }

    /**
     * Returns the <code>classList</code> property.
     *
     * @return the class list
     */
    @JsProperty
    DomTokenList getClassList();

    /**
     * Returns the <code>firstElementChild</code> property.
     *
     * @return the first element child
     */
    @JsProperty
    Element getFirstElementChild();

    /**
     * Returns the <code>lastElementChild</code> property.
     *
     * @return the last last element child
     */
    @JsProperty
    Element getLastElementChild();

    /**
     * Returns the <code>innerHTML</code> property.
     *
     * @return the inner html
     */
    @JsProperty
    String getInnerHTML();

    /**
     * Sets the <code>innerHTML</code> property to the given string.
     *
     * @param innerHTML
     *            the inner html to set
     */
    @JsProperty
    void setInnerHTML(String innerHTML);

    /**
     * Returns the <code>children</code> property containing all child elements
     * of the element, as a live collection.
     *
     * @return a collection of all child elements
     */
    @JsProperty
    HTMLCollection getChildren();

    /**
     * Returns the first <code>Node</code> which matches the specified selector
     * string relative to the element.
     *
     * @param selectors
     *            a group of selectors to match on
     * @return the first node that matched the given selectors
     */
    Element querySelector(String selectors);

    /**
     * Returns a non-live <code>NodeList</code> of all elements descended from
     * this element and match the given group of CSS selectors.
     * <p>
     * NOTE: returns an array since that is what the Polymer.dom API does, and
     * luckily native NodeList items can be accessed array like with
     * <code>list[index].</code>
     * <p>
     * This means that only {@link JsArray#get(int)} and
     * {@link JsArray#length()} methods can be used from the returned "array".
     *
     * @param selectors
     *            a group of selectors to match on
     * @return a non-live node list of elements that matched the given selectors
     */
    JsArray<Node> querySelectorAll(String selectors);

    /**
     * Sets an attribute value for this node.
     *
     * @param name
     *            the attribute name
     * @param value
     *            the attribute value
     */
    void setAttribute(String name, String value);

    /**
     * Removes an attribute from this node.
     *
     * @param name
     *            the attribute name
     */
    void removeAttribute(String name);
}

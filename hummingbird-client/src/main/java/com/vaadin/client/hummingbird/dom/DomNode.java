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
package com.vaadin.client.hummingbird.dom;

import elemental.dom.Node;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Node that has all methods from
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Node">Node API</a>
 * that have been overridden in
 * <a href="https://www.polymer-project.org/1.0/docs/devguide/local-dom">Polymer
 * DOM module</a>.
 * <p>
 * No instances of this class should be created directly, but instead
 * {@link DomApi#wrap(elemental.dom.Node)} should be used
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true)
public interface DomNode {

    /**
     * A <a href="https://developer.mozilla.org/en-US/docs/Web/API/NodeList">
     * NodeList</a> java representation.
     */
    @JsType(isNative = true)
    interface DomNodeList {

        /**
         * Returns the <code>length</code> property.
         *
         * @return the node list length
         */
        @JsProperty
        int getLength();

        /**
         * Returns an item in the list by its index.
         *
         * @param index
         *            the index to look for the item
         * @return the node at the given index
         */
        Node item(int index);
    }

    /**
     * Returns the <code>childNodes</code> property.
     *
     * @return the child nodes
     */
    @JsProperty
    DomNodeList getChildNodes();

    /**
     * Returns the <code>firstChild</code> property.
     *
     * @return the first child
     */
    @JsProperty
    Node getFirstChild();

    /**
     * Returns the <code>lastChild</code> property.
     *
     * @return the last child
     */
    @JsProperty
    Node getLastChild();

    /**
     * Returns the <code>nextSibling</code> property.
     *
     * @return the next sibling
     */
    @JsProperty
    Node getNextSibling();

    /**
     * Returns the <code>previousSibling</code> property.
     *
     * @return the previous sibling
     */
    @JsProperty
    Node getPreviousSibling();

    /**
     * Returns the <code>textContent</code> property.
     *
     * @return the text content
     */
    @JsProperty
    String getTextContent();

    /**
     * A setter for the <code>childNodes</code> property.
     *
     * @param textContent
     *            the text content to set
     */
    @JsProperty
    void setTextContent(String textContent);

    /**
     * Insert a node as the last child node of this element.
     *
     * @param node
     *            the node to append
     */
    void appendChild(Node node);

    /**
     * Inserts the first Node given in a parameter immediately before the
     * second, child of this element, Node.
     *
     * @param newChild
     *            the node to be inserted
     * @param refChild
     *            the node before which newChild is inserted
     */
    void insertBefore(Node newChild, Node refChild);

    /**
     * Removes a child node from the current node, which much be a child of the
     * current node.
     *
     * @param childNode
     *            the child node to remove
     */
    void removeChild(Node childNode);

    /**
     * Replaces one child Node of the current one with the second one given in
     * parameter.
     *
     * @param newChild
     *            the new node to replace the oldChild. If it already exists in
     *            the DOM, it is first removed.
     * @param oldChild
     *            is the existing child to be replaced.
     */
    void replaceChild(Node newChild, Node oldChild);

    /**
     * Clone a Node, and optionally, all of its contents. By default, it clones
     * the content of the node.
     *
     * @param deep
     *            <code>true</code> if the children of the node should also be
     *            cloned, or <code>false</code> to clone only the specified
     *            node.
     * @return a clone of this node
     */
    Node cloneNode(boolean deep);

}

/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utils class, intended to ease working with DOM elements on client side.
 *
 * @author Vaadin Ltd
 */
public class ElementUtil {

    private ElementUtil() {
        // Only static helpers
    }

    /**
     * Checks whether the {@code node} has required {@code tag}.
     *
     * @param node
     *            the node to check
     * @param tag
     *            the required tag name
     * @return {@code true} if the node has required tag name
     */
    public static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }

    /**
     * Searches the shadow root of the given context element for the given id or
     * searches the light DOM if the element has no shadow root.
     *
     * @param context
     *            the container element to search through
     * @param id
     *            the identifier of the element to search for
     * @return the element with the given {@code id} if found, otherwise
     *         <code>null</code>
     */
    public static native Element getElementById(Node context, String id)
    /*-{
       if (context.shadowRoot) {
         return context.shadowRoot.getElementById(id);
       } else if (context.querySelector) {
         return context.getElementById(id);
       } else {
         return null;
       }
    }-*/;

}

/*
 * Copyright 2000-2026 Vaadin Ltd.
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
 * <p>
 * The implementation has been migrated to TypeScript (see
 * {@code ElementUtil.ts} registered on
 * {@code window.Vaadin.Flow.internal.ElementUtil}); these methods delegate to
 * it.
 *
 * @author Vaadin Ltd
 */
public final class ElementUtil {

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
    public static native boolean hasTag(Node node, String tag)
    /*-{
        return $wnd.Vaadin.Flow.internal.ElementUtil.hasTag(node, tag);
    }-*/;

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
        return $wnd.Vaadin.Flow.internal.ElementUtil.getElementById(context, id);
    }-*/;

    /**
     * Searches the context for an element with the given {@code name}
     * attribute.
     *
     * @param context
     *            the container element to search through
     * @param name
     *            the name attribute value to search for
     * @return the element if found, otherwise <code>null</code>
     */
    public static native Element getElementByName(Node context, String name)
    /*-{
        return $wnd.Vaadin.Flow.internal.ElementUtil.getElementByName(context, name);
    }-*/;
}

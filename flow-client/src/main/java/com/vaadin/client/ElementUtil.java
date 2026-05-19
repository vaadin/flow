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
 * {@link #getElementById(Node, String)} and
 * {@link #getElementByName(Node, String)} delegate to the TypeScript
 * implementation at {@code src/main/frontend/internal/client/ElementUtil.ts}.
 * {@link #hasTag} is a pure-Java {@code instanceof} check with no browser API
 * to delegate to.
 *
 * @author Vaadin Ltd
 */
public class ElementUtil {

    private ElementUtil() {
        // Only static helpers
    }

    /**
     * Checks whether the {@code node} has required {@code tag}.
     */
    public static boolean hasTag(Node node, String tag) {
        return node instanceof Element
                && tag.equalsIgnoreCase(((Element) node).getTagName());
    }

    /**
     * Searches the shadow root of the given context element for the given id or
     * searches the light DOM if the element has no shadow root.
     */
    public static Element getElementById(Node context, String id) {
        return NativeElementUtil.getElementById(context, id);
    }

    /**
     * Searches the element by the given {@code name} attribute.
     */
    public static Element getElementByName(Node context, String name) {
        return NativeElementUtil.getElementByName(context, name);
    }
}

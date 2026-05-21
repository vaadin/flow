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

import jsinterop.annotations.JsType;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utils class, intended to ease working with DOM elements on client side. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/ElementUtil.ts}.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ElementUtil")
public final class ElementUtil {

    private ElementUtil() {
        // Native, not instantiated from Java
    }

    /**
     * Searches the shadow root of the given context element for the given id or
     * searches the light DOM if the element has no shadow root.
     */
    public static native Element getElementById(Node context, String id);

    /**
     * Searches the element by the given {@code name} attribute.
     */
    public static native Element getElementByName(Node context, String name);

    /** Checks whether the {@code node} has the required {@code tag}. */
    public static native boolean hasTag(Node node, String tag);
}

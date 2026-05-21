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

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Utility class which handles javascript execution context. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/ExecuteJavaScriptElementUtils.ts}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ExecuteJavaScriptElementUtils")
public final class ExecuteJavaScriptElementUtils {

    private ExecuteJavaScriptElementUtils() {
    }

    /**
     * Checks whether the given property is declared on the custom element's
     * {@code constructor.properties} bag.
     */
    public static native boolean isPropertyDefined(Node node, String property);

    /**
     * Calculate the data required for the server-side callback to attach an
     * existing element, and send it to the server.
     */
    public static native void attachExistingElement(StateNode parent,
            Element previousSibling, String tagName, int id);

    /**
     * Populate model {@code properties}: add them into
     * {@code NodeFeatures.ELEMENT_PROPERTIES} if they are not defined by the
     * client-side element, or send their client-side value to the server
     * otherwise.
     */
    public static native void populateModelProperties(StateNode node,
            JsArray<String> properties);

    /** Register the updatable model properties of the {@code node}. */
    public static native void registerUpdatableModelProperties(StateNode node,
            JsArray<String> properties);
}

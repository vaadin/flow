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
import elemental.dom.ShadowRoot;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * JsInterop binding for the TypeScript {@code PolymerUtils} implementation
 * published at {@code window.Vaadin.Flow.internal.client.PolymerUtils}. Source
 * lives in {@code src/main/frontend/internal/client/PolymerUtils.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "PolymerUtils")
final class NativePolymerUtils {

    private NativePolymerUtils() {
        // Native, not instantiated from Java
    }

    static native void setListValueByIndex(Element htmlNode, String path,
            int listIndex, JsonValue newValue);

    static native void splice(Element htmlNode, String path, int startIndex,
            int deleteCount, JsonArray itemsToAdd);

    static native void storeNodeId(Node domNode, int id, String path);

    static native boolean isPolymerElement(Element htmlNode);

    static native boolean mayBePolymerElement(Element htmlNode);

    static native Node searchForElementInShadowRoot(ShadowRoot shadowRoot,
            String cssQuery);

    static native Node getElementInShadowRootById(ShadowRoot shadowRoot,
            String id);

    static native Element getDomElementById(Node shadowRootParent, String id);

    static native boolean isReady(Node shadowRootParent);

    static native Element getDomRoot(Node templateElement);

    static native void invokeWhenDefined(String tagName, JsRunnable runnable);

    static native void setProperty(Element element, String path, Object value);

    static native boolean isInShadowRoot(Element element);
}

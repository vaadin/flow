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
 * JsInterop binding for the TypeScript {@code ElementUtil} implementation
 * published at {@code window.Vaadin.Flow.internal.client.ElementUtil}. Source
 * lives in {@code src/main/frontend/internal/client/ElementUtil.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ElementUtil")
final class NativeElementUtil {

    private NativeElementUtil() {
        // Native, not instantiated from Java
    }

    static native Element getElementById(Node context, String id);

    static native Element getElementByName(Node context, String name);
}

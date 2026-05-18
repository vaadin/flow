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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * JsInterop binding for the TypeScript {@code LitUtils} implementation
 * published at {@code window.Vaadin.Flow.internal.client.LitUtils}. Source
 * lives in {@code src/main/frontend/internal/client/LitUtils.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "LitUtils")
final class NativeLitUtils {

    /**
     * Callback shape used to pass Java lambdas into TS bridge methods that
     * expect a {@code () => void} function.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsRunnable {
        void run();
    }

    private NativeLitUtils() {
        // Native, not instantiated from Java
    }

    static native boolean isLitElement(Node element);

    static native void whenRendered(Element element, JsRunnable runnable);
}

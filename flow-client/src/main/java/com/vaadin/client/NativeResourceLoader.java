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

import com.vaadin.client.NativeLitUtils.JsRunnable;

import elemental.dom.Element;

/**
 * JsInterop binding for the TypeScript {@code ResourceLoader} implementation
 * published at {@code window.Vaadin.Flow.internal.client.ResourceLoader}.
 * Source lives in {@code src/main/frontend/internal/client/ResourceLoader.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ResourceLoader")
final class NativeResourceLoader {

    /**
     * Callback shape used to pass Java {@link java.util.function.Supplier}s
     * into TS bridge methods that expect a {@code () => unknown} function.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsSupplier {
        Object get();
    }

    private NativeResourceLoader() {
        // Native, not instantiated from Java
    }

    static native boolean supportsHtmlWhenReady();

    static native void addHtmlImportsReadyHandler(JsRunnable handler);

    static native void addOnloadHandler(Element element, JsRunnable onLoad,
            JsRunnable onError);

    static native int getStyleSheetLength(String url);

    static native void runPromiseExpression(String expression,
            JsSupplier promiseSupplier, JsRunnable onSuccess,
            JsRunnable onError);
}

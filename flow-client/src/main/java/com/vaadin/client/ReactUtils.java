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

/**
 * Utils class, intended to ease working with React component related code on
 * the client side.
 *
 * <p>
 * Pure {@code @JsType(isNative = true)} binding to the TypeScript
 * implementation at {@code src/main/frontend/internal/client/ReactUtils.ts}.
 * The class has no Java body. Acceptable here because ReactUtils is only
 * consumed by {@code SimpleElementBindingStrategy} (no JUnit test).
 *
 * @author Vaadin Ltd
 * @since 24.5.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ReactUtils")
public final class ReactUtils {

    private ReactUtils() {
    }

    public static native void addReadyCallback(Element element, String name,
            JsRunnable runnable);

    public static native boolean isInitialized(JsObjectSupplier elementLookup);
}

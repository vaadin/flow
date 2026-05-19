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
 * Utils class, intended to ease working with LitElement related code on client
 * side.
 *
 * <p>
 * Pure {@code @JsType(isNative = true)} binding to the TypeScript
 * implementation at {@code src/main/frontend/internal/client/LitUtils.ts}. The
 * class has no Java body — calling these methods from the JVM throws.
 * Acceptable here because LitUtils is only consumed by
 * {@code SimpleElementBindingStrategy}, which has no JUnit test (only a Gwt
 * test), so no JVM transitively reaches LitUtils.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "LitUtils")
public final class LitUtils {

    private LitUtils() {
    }

    public static native boolean isLitElement(Node element);

    public static native void whenRendered(Element element,
            JsRunnable runnable);
}

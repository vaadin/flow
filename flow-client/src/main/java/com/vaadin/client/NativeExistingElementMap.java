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
 * JsInterop binding for the TypeScript {@code ExistingElementMap}
 * implementation published at
 * {@code window.Vaadin.Flow.internal.client.ExistingElementMap}. Source lives
 * in {@code src/main/frontend/internal/client/ExistingElementMap.ts}.
 *
 * Stateful: each {@code new NativeExistingElementMap()} on the GWT side yields
 * its own JS-side instance. The public {@link ExistingElementMap} Java class
 * wraps one of these and keeps a JVM-side {@link java.util.Map} fallback so
 * JUnit tests run unchanged.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ExistingElementMap")
final class NativeExistingElementMap {

    NativeExistingElementMap() {
        // Defined by the TS class constructor.
    }

    native Element getElement(int id);

    native Integer getId(Element element);

    native void remove(int id);

    native void add(int id, Element element);
}

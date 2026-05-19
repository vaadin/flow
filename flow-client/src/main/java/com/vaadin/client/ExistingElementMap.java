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
 * Mapping between a server-side node identifier which has been requested to
 * attach an existing client-side element.
 *
 * <p>
 * Pure {@code @JsType(isNative = true)} binding to the TypeScript
 * implementation at
 * {@code src/main/frontend/internal/client/ExistingElementMap.ts}. Stateful:
 * each {@code new ExistingElementMap()} yields its own JS-side instance.
 * Acceptable here because the class is only consumed at runtime from
 * {@code ExecuteJavaScriptElementUtils} and {@code ApplicationConnection}; the
 * JUnit coverage moved to {@code src/test/frontend/ExistingElementMapTests.ts}.
 *
 * @author Vaadin Ltd
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ExistingElementMap")
public class ExistingElementMap {

    public ExistingElementMap() {
        // Defined by the TS class constructor.
    }

    public native Element getElement(int id);

    public native Integer getId(Element element);

    public native void remove(int id);

    public native void add(int id, Element element);
}

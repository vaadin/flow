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

import com.google.gwt.core.client.JavaScriptObject;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * JsInterop binding for the TypeScript {@code WidgetUtil} implementation
 * published at {@code window.Vaadin.Flow.internal.client.WidgetUtil}. Source
 * lives in {@code src/main/frontend/internal/client/WidgetUtil.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "WidgetUtil")
final class NativeWidgetUtil {

    private NativeWidgetUtil() {
        // Native, not instantiated from Java
    }

    static native void redirect(String url);

    static native boolean isAbsoluteUrl(String url);

    static native <T> T crazyJsCast(Object value);

    static native <T extends JavaScriptObject> T crazyJsoCast(Object value);

    static native String toPrettyJsonJsni(JsonValue value);

    static native void setJsProperty(Object object, String name, Object value);

    static native Object getJsProperty(Object object, String name);

    static native boolean hasOwnJsProperty(Object object, String name);

    static native boolean hasJsProperty(Object object, String name);

    static native boolean isUndefined(Object property);

    static native void deleteJsProperty(Object object, String name);

    static native JsonObject createJsonObjectWithoutPrototype();

    static native JsonObject createJsonObject();

    static native boolean isTrueish(Object value);

    static native String[] getKeys(Object value);

    static native String stringify(JsonObject payload);

    static native boolean equalsInJS(Object obj1, Object obj2);
}

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

import elemental.dom.Element;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Utility methods which are related to client side code only. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/WidgetUtil.ts}.
 *
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "WidgetUtil")
public class WidgetUtil {

    /** Refreshes the browser. */
    public static native void refresh();

    /**
     * Redirects the browser to the given url or refreshes the page if url is
     * null.
     */
    public static native void redirect(String url);

    /**
     * Resolve a relative URL to an absolute URL based on the current document's
     * location.
     */
    public static native String getAbsoluteUrl(String url);

    /** Detects if a URL is absolute. */
    public static native boolean isAbsoluteUrl(String url);

    /** Identity cast — JavaScript is dynamically typed. */
    public static native <T> T crazyJsCast(Object value);

    /** Identity cast targeting a {@link JavaScriptObject} subtype. */
    public static native <T extends JavaScriptObject> T crazyJsoCast(
            Object value);

    /** Converts a JSON value to a formatted string. */
    public static native String toPrettyJson(JsonValue json);

    /**
     * Updates the {@code attribute} value for the {@code element} to the given
     * {@code value}. If {@code value} is {@code null} the attribute is removed.
     */
    public static native void updateAttribute(Element element, String attribute,
            String value);

    /** Assigns a value as JavaScript property of an object. */
    public static native void setJsProperty(Object object, String name,
            Object value);

    /** Retrieves the value of a JavaScript property. */
    public static native Object getJsProperty(Object object, String name);

    /**
     * Checks whether the object itself has a JS property with the given name.
     */
    public static native boolean hasOwnJsProperty(Object object, String name);

    /**
     * Checks whether the object has or inherits a JS property with the name.
     */
    public static native boolean hasJsProperty(Object object, String name);

    /** Checks if the value is explicitly undefined. */
    public static native boolean isUndefined(Object property);

    /** Removes a JavaScript property from an object. */
    public static native void deleteJsProperty(Object object, String name);

    /** Creates a new {@link JsonObject} without any JS prototype at all. */
    public static native JsonObject createJsonObjectWithoutPrototype();

    /** Creates a new {@link JsonObject} with the JS prototype. */
    public static native JsonObject createJsonObject();

    /** Truthiness check using JavaScript semantics. */
    public static native boolean isTrueish(Object value);

    /** Returns all JavaScript property names of the given object. */
    public static native String[] getKeys(Object value);

    /**
     * Serializes a JsonObject, refusing to include any DOM nodes (which would
     * cause cyclic dependencies if sent to the server).
     */
    public static native String stringify(JsonObject payload);

    /**
     * Checks whether the objects are equal either as Java objects or as JS
     * values.
     */
    public static native boolean equals(Object obj1, Object obj2);

    /** Checks whether the objects are equal as JS values. */
    public static native boolean equalsInJS(Object obj1, Object obj2);
}

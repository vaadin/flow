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

import java.util.Objects;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.flow.dom.DomApi;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.AnchorElement;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Utility methods which are related to client side code only.
 * <p>
 * Under GWT the JSNI helpers delegate to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/WidgetUtil.ts} via
 * {@link NativeWidgetUtil}. Non-JSNI helpers ({@link #refresh},
 * {@link #updateAttribute}, {@link #toPrettyJson}, {@link #equals}) and the JVM
 * fallbacks stay in this class.
 *
 * @since 1.0
 */
public class WidgetUtil {

    /**
     * Refreshes the browser.
     */
    public static void refresh() {
        redirect(null);
    }

    /**
     * Redirects the browser to the given url or refreshes the page if url is
     * null.
     */
    public static void redirect(String url) {
        if (GWT.isScript()) {
            NativeWidgetUtil.redirect(url);
        }
    }

    /**
     * Resolve a relative URL to an absolute URL based on the current document's
     * location.
     */
    public static String getAbsoluteUrl(String url) {
        AnchorElement a = (AnchorElement) Browser.getDocument()
                .createElement("a");
        a.setHref(url);
        return a.getHref();
    }

    /**
     * Detects if an URL is absolute.
     */
    public static boolean isAbsoluteUrl(String url) {
        return url.matches("^(?:[a-zA-Z]+:)?//.*");
    }

    /**
     * Anything in, anything out. It's JavaScript after all. This method just
     * makes the Java compiler accept the fact. The unbounded type parameter
     * erases to {@code Object} at compile time, so the same unchecked cast
     * works in both compiled JavaScript and pure JVM runs.
     */
    public static <T> T crazyJsCast(Object value) {
        @SuppressWarnings("unchecked")
        T cast = (T) value;
        return cast;
    }

    /**
     * Anything in, JSO out. The regular crazy cast doesn't work for JSOs since
     * the generics still makes the compiler insert a JSO check.
     */
    public static <T extends JavaScriptObject> T crazyJsoCast(Object value) {
        if (GWT.isScript()) {
            return NativeWidgetUtil.crazyJsoCast(value);
        }
        @SuppressWarnings("unchecked")
        T cast = (T) value;
        return cast;
    }

    /**
     * Converts a JSON value to a formatted string.
     */
    public static String toPrettyJson(JsonValue json) {
        if (GWT.isScript()) {
            return NativeWidgetUtil.toPrettyJsonJsni(json);
        }
        // Don't use JsonUtil.stringify here or SDM will break
        return json.toJson();
    }

    /**
     * Updates the {@code attribute} value for the {@code element} to the given
     * {@code value}. If {@code value} is {@code null} the attribute is removed.
     */
    public static void updateAttribute(Element element, String attribute,
            String value) {
        if (value == null) {
            DomApi.wrap(element).removeAttribute(attribute);
        } else {
            DomApi.wrap(element).setAttribute(attribute, value);
        }
    }

    /**
     * Assigns a value as JavaScript property of an object.
     */
    public static void setJsProperty(Object object, String name, Object value) {
        if (GWT.isScript()) {
            NativeWidgetUtil.setJsProperty(object, name, value);
        }
    }

    /**
     * Retrieves the value of a JavaScript property.
     */
    public static Object getJsProperty(Object object, String name) {
        return GWT.isScript() ? NativeWidgetUtil.getJsProperty(object, name)
                : null;
    }

    /**
     * Checks whether the provided object itself has a JavaScript property with
     * the given name. Inherited properties are not taken into account.
     */
    public static boolean hasOwnJsProperty(Object object, String name) {
        return GWT.isScript()
                && NativeWidgetUtil.hasOwnJsProperty(object, name);
    }

    /**
     * Checks whether the provided object has or inherits a JavaScript property
     * with the given name.
     */
    public static boolean hasJsProperty(Object object, String name) {
        return GWT.isScript() && NativeWidgetUtil.hasJsProperty(object, name);
    }

    /**
     * Checks if the given value is explicitly undefined.
     */
    public static boolean isUndefined(Object property) {
        return GWT.isScript() && NativeWidgetUtil.isUndefined(property);
    }

    /**
     * Removes a JavaScript property from an object.
     */
    public static void deleteJsProperty(Object object, String name) {
        if (GWT.isScript()) {
            NativeWidgetUtil.deleteJsProperty(object, name);
        }
    }

    /**
     * Creates a new {@link JsonObject} without any JavaScript prototype at all.
     */
    public static JsonObject createJsonObjectWithoutPrototype() {
        return GWT.isScript()
                ? NativeWidgetUtil.createJsonObjectWithoutPrototype()
                : null;
    }

    /**
     * Creates a new {@link JsonObject} with the JavaScript prototype.
     */
    public static JsonObject createJsonObject() {
        return GWT.isScript() ? NativeWidgetUtil.createJsonObject() : null;
    }

    /**
     * Gets the boolean value of the provided value based on JavaScript
     * semantics.
     */
    public static boolean isTrueish(Object value) {
        return GWT.isScript() ? NativeWidgetUtil.isTrueish(value)
                : value != null;
    }

    /**
     * Gets all JavaScript property names of the given object.
     */
    public static String[] getKeys(Object value) {
        return GWT.isScript() ? NativeWidgetUtil.getKeys(value) : new String[0];
    }

    /**
     * Serializes a JsonObject, refusing to include any DOM nodes (which would
     * cause cyclic dependencies if sent to the server).
     */
    public static String stringify(JsonObject payload) {
        if (GWT.isScript()) {
            return NativeWidgetUtil.stringify(payload);
        }
        return payload.toJson();
    }

    /**
     * Checks whether the objects are equal either as Java objects or as JS
     * values.
     */
    public static boolean equals(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2) || equalsInJS(obj1, obj2);
    }

    /**
     * Checks whether the objects are equal as JS values.
     */
    public static boolean equalsInJS(Object obj1, Object obj2) {
        return GWT.isScript() && NativeWidgetUtil.equalsInJS(obj1, obj2);
    }
}

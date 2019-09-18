/*
 * Copyright 2000-2018 Vaadin Ltd.
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
     * null
     *
     * @param url
     *            The url to redirect to or null to refresh
     */
    public static native void redirect(String url)
    /*-{
        if (url) {
                $wnd.location = url;
        } else {
                $wnd.location.reload(false);
        }
    }-*/;

    /**
     * Resolve a relative URL to an absolute URL based on the current document's
     * location.
     *
     * @param url
     *            a string with the relative URL to resolve
     * @return the corresponding absolute URL as a string
     */
    public static String getAbsoluteUrl(String url) {
        AnchorElement a = (AnchorElement) Browser.getDocument()
                .createElement("a");
        a.setHref(url);
        return a.getHref();
    }

    /**
     * Anything in, anything out. It's JavaScript after all. This method just
     * makes the Java compiler accept the fact.
     *
     * @param value
     *            anything
     * @param <T>
     *            the object type
     * @return the same stuff
     */
    public static native <T> T crazyJsCast(Object value)
    /*-{
        return value;
    }-*/;

    /**
     * Anything in, JSO out. It's JavaScript after all. This method just makes
     * the Java compiler accept the fact. The regular crazy cast doesn't work
     * for JSOs since the generics still makes the compiler insert a JSO check.
     *
     * @param value
     *            anything
     * @param <T>
     *            the object type
     * @return the same stuff
     */
    public static native <T extends JavaScriptObject> T crazyJsoCast(
            Object value)
    /*-{
        return value;
    }-*/;

    /**
     * Converts a JSON value to a formatted string.
     *
     *
     * @param json
     *            the JSON value to stringify
     * @return the JSON string
     */
    public static String toPrettyJson(JsonValue json) {
        if (GWT.isScript()) {
            return toPrettyJsonJsni(json);
        } else {
            // Don't use JsonUtil.stringify here or SDM will break
            return json.toJson();
        }
    }

    /**
     * Updates the {@code attribute} value for the {@code element} to the given
     * {@code value}.
     * <p>
     * If {@code value} is {@code null} then {@code attribute} is removed,
     * otherwise {@code value.toString()} is set as its value.
     *
     * @param element
     *            the DOM element owning attribute
     * @param attribute
     *            the attribute to update
     * @param value
     *            the value to update
     */
    public static void updateAttribute(Element element, String attribute,
            Object value) {
        if (value == null) {
            DomApi.wrap(element).removeAttribute(attribute);
        } else {
            DomApi.wrap(element).setAttribute(attribute, value.toString());
        }
    }

    // JsJsonValue.toJson with indentation set to 4
    private static native String toPrettyJsonJsni(JsonValue value)
    /*-{
      // skip hashCode field
      return $wnd.JSON.stringify(value, function(keyName, value) {
        if (keyName == "$H") {
          return undefined; // skip hashCode property
        }
        return value;
      }, 4);
    }-*/;

    /**
     * Assigns a value as JavaScript property of an object.
     *
     *
     * @param object
     *            the target object
     * @param name
     *            the property name
     * @param value
     *            the property value
     */
    public static native void setJsProperty(Object object, String name,
            Object value)
    /*-{
        object[name] = value;
    }-*/;

    /**
     * Retrieves the value of a JavaScript property.
     *
     * @param object
     *            the target object
     * @param name
     *            the property name
     * @return the value
     */
    public static native Object getJsProperty(Object object, String name)
    /*-{
        return object[name];
    }-*/;

    /**
     * Checks whether the provided object itself has a JavaScript property with
     * the given name. Inherited properties are not taken into account.
     *
     * @see #hasJsProperty(Object, String)
     *
     * @param object
     *            the target object
     * @param name
     *            the name of the property
     * @return <code>true</code> if the object itself has the named property;
     *         <code>false</code> if it doesn't have the property of if the
     *         property is inherited
     */
    public static native boolean hasOwnJsProperty(Object object, String name)
    /*-{
      return Object.prototype.hasOwnProperty.call(object, name);
    }-*/;

    /**
     * Checks whether the provided object has or inherits a JavaScript property
     * with the given name.
     *
     * @see #hasOwnJsProperty(Object, String)
     *
     * @param object
     *            the target object
     * @param name
     *            the name of the property
     * @return <code>true</code> if the object itself has or inherits the named
     *         property; <code>false</code> otherwise
     */
    public static native boolean hasJsProperty(Object object, String name)
    /*-{
      return name in object;
    }-*/;

    /**
     * Checks if the given value is explicitly undefined. <code>null</code>
     * values returns <code>false</code>.
     * 
     * @param property
     *            the value to be verified
     * @return <code>true</code> is the value is explicitly undefined,
     *         <code>false</code> otherwise
     */
    public static native boolean isUndefined(Object property)
    /*-{
      return property === undefined;
    }-*/;

    /**
     * Removes a JavaScript property from an object.
     *
     * @param object
     *            the object from which to remove the property
     * @param name
     *            the name of the property to remove
     */
    public static native void deleteJsProperty(Object object, String name)
    /*-{
      delete object[name];
    }-*/;

    /**
     * Creates a new {@link JsonObject} without any JavaScript prototype at all.
     * Not having any prototype is only relevant for objects that are displayed
     * through the browser console.
     *
     *
     * @return a new json object
     */
    public static native JsonObject createJsonObjectWithoutPrototype()
    /*-{
      return $wnd.Object.create(null);
    }-*/;

    /**
     * Creates a new {@link JsonObject} with the JavaScript prototype.
     *
     * @return a new json object
     */
    public static native JsonObject createJsonObject()
    /*-{
      return {};
    }-*/;

    /**
     * Gets the boolean value of the provided value based on JavaScript
     * semantics.
     *
     * @param value
     *            the value to check for truthness
     * @return <code>true</code> if the provided value is trueish according to
     *         JavaScript semantics, otherwise <code>false</code>
     */
    public static native boolean isTrueish(Object value)
    /*-{
        return !!value;
    }-*/;

    /**
     * Gets all JavaScript property names of the given object. This directly
     * calls <code>Object.keys</code>.
     *
     * @param value
     *            the value to get keys for
     * @return an array of key names
     */
    public static native String[] getKeys(Object value)
    /*-{
      return Object.keys(value);
    }-*/;

    /**
     * When serializing the JsonObject we check the values for dom nodes and
     * throw and exception if one is found as they should not be synced and may
     * create cyclic dependencies.
     *
     * @param payload
     *            JsonObject to stringify
     * @return json string of given object
     */
    public static native String stringify(JsonObject payload) /*-{
                                                              return JSON.stringify(payload, function(key, value) {
                                                              if(value instanceof Node){
                                                              throw "Message JsonObject contained a dom node reference which " +
                                                              "should not be sent to the server and can cause a cyclic dependecy.";
                                                              }
                                                              return value;
                                                              });
                                                              }-*/;
}

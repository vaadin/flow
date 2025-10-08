/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.flow.collection.JsArray;

/**
 * Old abstraction for a UIDL JSON message.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class ValueMap extends JavaScriptObject {
    /**
     * JSO constructor.
     */
    protected ValueMap() {
    }

    /**
     * Gets the value with the given key as an integer.
     *
     * @param name
     *            the map key
     * @return the value as an integer
     */
    public native int getInt(final String name)
    /*-{
        return this[name];
    }-*/;

    /**
     * Gets the value with the given key as a string.
     *
     * @param name
     *            the map key
     * @return the value as a string
     */
    public native String getString(String name)
    /*-{
        return this[name];
    }-*/;

    /**
     * Gets the value with the given key as an string array.
     *
     * @param name
     *            the map key
     * @return the value as a string array
     */
    public native JsArray<String> getJSStringArray(String name)
    /*-{
        return this[name];
    }-*/;

    /**
     * Checks if the map contains the given key.
     *
     * @param name
     *            the map key
     * @return true if the map contains the key, false otherwise
     */
    public native boolean containsKey(final String name)
    /*-{
         return name in this;
    }-*/;

    /**
     * Gets the value with the given key as a map.
     *
     * @param name
     *            the map key
     * @return the value as a map
     */
    public native ValueMap getValueMap(String name)
    /*-{
        return this[name];
    }-*/;

}

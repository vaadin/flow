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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsArray;

/**
 * Old abstraction for a UIDL JSON message. Backed directly by the underlying JS
 * instance — keyed reads compile to property reads via
 * {@link WidgetUtil#getJsProperty(Object, String)}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public final class ValueMap {

    /**
     * Gets the value with the given key as an integer.
     *
     * @param name
     *            the map key
     * @return the value as an integer
     */
    @JsOverlay
    public final int getInt(String name) {
        return ((Number) WidgetUtil.getJsProperty(this, name)).intValue();
    }

    /**
     * Gets the value with the given key as a string.
     *
     * @param name
     *            the map key
     * @return the value as a string
     */
    @JsOverlay
    public final String getString(String name) {
        return (String) WidgetUtil.getJsProperty(this, name);
    }

    /**
     * Gets the value with the given key as an string array.
     *
     * @param name
     *            the map key
     * @return the value as a string array
     */
    @SuppressWarnings("unchecked")
    @JsOverlay
    public final JsArray<String> getJSStringArray(String name) {
        return (JsArray<String>) WidgetUtil.getJsProperty(this, name);
    }

    /**
     * Checks if the map contains the given key.
     *
     * @param name
     *            the map key
     * @return true if the map contains the key, false otherwise
     */
    @JsOverlay
    public final boolean containsKey(String name) {
        return WidgetUtil.hasJsProperty(this, name);
    }

    /**
     * Gets the value with the given key as a map.
     *
     * @param name
     *            the map key
     * @return the value as a map
     */
    @JsOverlay
    public final ValueMap getValueMap(String name) {
        return (ValueMap) WidgetUtil.getJsProperty(this, name);
    }
}

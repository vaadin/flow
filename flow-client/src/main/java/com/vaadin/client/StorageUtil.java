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

/**
 * Includes utility methods to interact with HTML storage API.
 */
public class StorageUtil {

    private StorageUtil() {
    }

    /**
     * Gets an item value from the local storage.
     *
     * @param key
     *            the item key
     * @return the value of the item
     */
    public static native String getLocalItem(String key)
    /*-{
        return window.localStorage.getItem(key);
    }-*/;

    /**
     * Sets an item value in the local storage.
     *
     * @param key
     *            the item key
     * @param value
     *            the item value
     */
    public static native void setLocalItem(String key, String value)
    /*-{
        window.localStorage.setItem(key, value);
    }-*/;

    /**
     * Gets an item value from the session storage.
     *
     * @param key
     *            the item key
     * @return the value of the item
     */
    public static native String getSessionItem(String key)
    /*-{
        return window.sessionStorage.getItem(key);
    }-*/;

    /**
     * Sets an item value in the session storage.
     *
     * @param key
     *            the item key
     * @param value
     *            the item value
     */
    public static native void setSessionItem(String key, String value)
    /*-{
        window.sessionStorage.setItem(key, value);
    }-*/;
}

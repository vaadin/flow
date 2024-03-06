/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

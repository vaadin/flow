/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

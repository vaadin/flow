/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.client.hummingbird.collection;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.hummingbird.collection.jre.JreJsArray;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Native JS Array interface with an alternative implementation for JRE usage.
 * Use {@link JsCollections#array()} to create an appropriate instance.
 *
 * @since
 * @author Vaadin Ltd
 * @param <T>
 *            the type of the array items
 */
@JsType(isNative = true)
public class JsArray<T> {
    /*
     * Don't look at this class as an example of how to integrate a JS API with
     * a JRE-fallback. The use of @JsOverlay means that it must be made in
     * needlessly complex way. JsMap is a better example.
     */

    /**
     * Should not be directly created.
     */
    protected JsArray() {
        // prevent direct instantiation
    }

    /**
     * Gets the item at the given index. This is corresponding to
     * <code>return array[index]</code> in JavaScript.
     *
     * @param index
     *            the index to get
     * @return the item at the given index
     */
    @JsOverlay
    public final T get(int index) {
        if (GWT.isScript()) {
            return JsniHelper.getValueNative(this, index);
        } else {
            return ((JreJsArray<T>) this).doGet(index);
        }
    }

    /**
     * Sets the item at the given index. This is corresponding to
     * <code>array[index] = value</code> in JavaScript.
     *
     * @param index
     *            the index to set
     * @param value
     *            the value to set
     */
    @JsOverlay
    public final void set(int index, T value) {
        if (GWT.isScript()) {
            JsniHelper.setValueNative(this, index, value);
        } else {
            ((JreJsArray<T>) this).doSet(index, value);
        }
    }

    /**
     * Adds an item to the end of this array.
     *
     * @param value
     *            the new value to add
     * @return the new length of the array
     */
    public native int push(T value);

    /**
     * Gets the current length of this array.
     *
     * @return the array length
     */
    @JsProperty(name = "length")
    public native int length();
}

// Helper for stuff not allowed in a @JsType class
class JsniHelper {
    private JsniHelper() {
        // Only static stuff here, should never be instantiated
    }

    @JsIgnore
    static native <T> T getValueNative(JsArray<T> array, int index)
    /*-{
        return array[index];
    }-*/;

    @JsIgnore
    static native <T> void setValueNative(JsArray<T> array, int i, T value)
    /*-{
        array[i] = value;
    }-*/;
}

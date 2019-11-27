/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.client.flow.collection;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;
import com.vaadin.client.flow.collection.jre.JreJsArray;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Native JS Array interface with an alternative implementation for JRE usage.
 * Use {@link JsCollections#array()} to create an appropriate instance.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the array items
 */
@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
@SuppressWarnings("deprecation")
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
     * Adds items to the end of this array.
     *
     * @param values
     *            the new values to add
     * @return the new length of the array
     */
    public native int push(@SuppressWarnings("unchecked") T... values);

    /**
     * Adds items to the end of this array.
     *
     * @param values
     *            the new values to add
     * @return the new length of the array
     */
    @JsOverlay
    public final int pushArray(JsArray<? extends T> values) {
        if (GWT.isScript()) {
            return JsniHelper.pushArray(this, values);
        } else {
            spliceArray(length(), 0, values);
            return length();
        }
    }

    /**
     * Gets the current length of this array.
     *
     * @return the array length
     */
    @JsProperty(name = "length")
    public native int length();

    /**
     * Removes and adds a number of items at the given index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            new items to add
     * @return an array of removed items
     */
    @JsOverlay
    public final JsArray<T> spliceArray(int index, int remove,
            JsArray<? extends T> add) {
        if (GWT.isScript()) {
            return JsniHelper.spliceArray(this, index, remove, add);
        } else {
            return ((JreJsArray<T>) this).doSpliceArray(index, remove, add);
        }
    }

    /**
     * Removes and adds a number of items at the given index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            new items to add
     * @return an array of removed items
     */
    public native JsArray<T> splice(int index, int remove,
            @SuppressWarnings("unchecked") T... add);

    /**
     * Removes the item at the given index.
     *
     * @param index
     *            the index to remove
     * @return the remove item
     */
    @JsOverlay
    @SuppressWarnings("unchecked")
    public final T remove(int index) {
        return splice(index, 1).get(0);
    }

    /**
     * Clears the array.
     *
     * @return the cleared array
     */
    @JsOverlay
    public final JsArray<T> clear() {
        if (GWT.isScript()) {
            JsniHelper.clear(this);
        } else {
            ((JreJsArray<T>) this).doClear();
        }
        return this;
    }

    /**
     * Checks if the array is empty (length == 0).
     *
     * @return true if the array is empty, false otherwise
     */
    @JsOverlay
    public final boolean isEmpty() {
        return length() == 0;
    }

    /**
     * Removes the given item from the array.
     *
     * @param toRemove
     *            the item to remove
     * @return <code>true</code> if the item was found and removed from the
     *         array, <code>false</code> otherwise
     */
    @JsOverlay
    public final boolean remove(T toRemove) {
        for (int i = 0; i < length(); i++) {
            if (get(i) == toRemove) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes and returns the first value from the array.
     *
     * @return the first value of the array or null if the array is empty
     */
    public native T shift();

    /**
     * Invokes the provided callback for each value in this array.
     *
     * @param callback
     *            the callback to invoke for each value
     */
    public native void forEach(ForEachCallback<T> callback);

}

// Helper for stuff not allowed in a @JsType class
class JsniHelper {
    private JsniHelper() {
        // Only static stuff here, should never be instantiated
    }

    static native <T> int pushArray(JsArray<T> array,
            JsArray<? extends T> values)
            /*-{
                return array.push.apply(array, values);
            }-*/;

    static native <T> JsArray<T> spliceArray(JsArray<T> array, int index,
            int remove, JsArray<? extends T> add)
            /*-{
                return array.splice.apply(array, [index, remove].concat(add));
            }-*/;

    static native void clear(JsArray<?> array)
    /*-{
        array.length = 0;
    }-*/;

    static native <T> T getValueNative(JsArray<T> array, int index)
    /*-{
        return array[index];
    }-*/;

    static native <T> void setValueNative(JsArray<T> array, int i, T value)
    /*-{
        array[i] = value;
    }-*/;

}

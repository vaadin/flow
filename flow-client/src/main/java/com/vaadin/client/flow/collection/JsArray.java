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
package com.vaadin.client.flow.collection;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;

/**
 * Native JS Array binding. Use {@link JsCollections#array()} to create an
 * instance.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the array items
 */
@JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
public class JsArray<T> {

    /**
     * Should not be directly created.
     */
    protected JsArray() {
        // prevent direct instantiation
    }

    /**
     * Gets the item at the given index. This is corresponding to
     * <code>return array[index]</code> in JavaScript.
     */
    @JsOverlay
    public final T get(int index) {
        return JsniHelper.getValueNative(this, index);
    }

    /**
     * Sets the item at the given index. This is corresponding to
     * <code>array[index] = value</code> in JavaScript.
     */
    @JsOverlay
    public final void set(int index, T value) {
        JsniHelper.setValueNative(this, index, value);
    }

    /**
     * Adds items to the end of this array.
     *
     * @return the new length of the array
     */
    public native int push(@SuppressWarnings("unchecked") T... values);

    /**
     * Adds items to the end of this array.
     *
     * @return the new length of the array
     */
    @JsOverlay
    public final int pushArray(JsArray<? extends T> values) {
        return JsniHelper.pushArray(this, values);
    }

    /**
     * Gets the current length of this array.
     */
    @JsProperty(name = "length")
    public native int length();

    /**
     * Removes and adds a number of items at the given index.
     */
    @JsOverlay
    public final JsArray<T> spliceArray(int index, int remove,
            JsArray<? extends T> add) {
        return JsniHelper.spliceArray(this, index, remove, add);
    }

    /**
     * Removes and adds a number of items at the given index.
     */
    public native JsArray<T> splice(int index, int remove,
            @SuppressWarnings("unchecked") T... add);

    /**
     * Removes the item at the given index.
     */
    @JsOverlay
    @SuppressWarnings("unchecked")
    public final T remove(int index) {
        return splice(index, 1).get(0);
    }

    /**
     * Clears the array.
     */
    @JsOverlay
    public final JsArray<T> clear() {
        JsniHelper.clear(this);
        return this;
    }

    /**
     * Checks if the array is empty (length == 0).
     */
    @JsOverlay
    public final boolean isEmpty() {
        return length() == 0;
    }

    /**
     * Removes the given item from the array.
     *
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
     */
    public native T shift();

    /**
     * Invokes the provided callback for each value in this array.
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

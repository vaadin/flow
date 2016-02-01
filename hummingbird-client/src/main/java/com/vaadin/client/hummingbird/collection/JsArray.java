/*
 * Copyright 2000-2016 Vaadin Ltd.
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

    /**
     * Removes and adds a number of items at some index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @param add
     *            new items to add
     * @return an array of removed items
     */
    @SafeVarargs
    @JsOverlay
    public final JsArray<T> splice(int index, int remove, T... add) {
        if (GWT.isScript()) {
            return JsniHelper.splice(this, index, remove, add);
        } else {
            return ((JreJsArray<T>) this).doSplice(index, remove, add);
        }
    }

    /**
     * Removes a number of items at some index.
     *
     * @param index
     *            the index at which do do the operation
     * @param remove
     *            the number of items to remove
     * @return an array of removed items
     */
    public native JsArray<T> splice(int index, int remove);

    /**
     * Removes the item at the given index
     *
     * @param index
     *            the index to remove
     * @return the remove item
     */
    @JsOverlay
    public final T remove(int index) {
        return splice(index, 1).get(0);
    }

    /**
     * Clears the array
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
     * Checks if the array is empty (length == 0)
     *
     * @return true if the array is empty, false otherwise
     */
    @JsOverlay
    public final boolean isEmpty() {
        return length() == 0;
    }

    /**
     * Add all items in the source array to the end of this array
     *
     * @param source
     *            the source array to read from
     */
    @JsOverlay
    public final void addAll(JsArray<T> source) {
        if (this == source) {
            throw new IllegalArgumentException(
                    "Target and source cannot be the same array");
        }

        for (int i = 0; i < source.length(); i++) {
            push(source.get(i));
        }
    }

    /**
     * Removes the given item from the array
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

}

// Helper for stuff not allowed in a @JsType class
class JsniHelper {
    private JsniHelper() {
        // Only static stuff here, should never be instantiated
    }

    static native <T> JsArray<T> splice(JsArray<T> array, int index, int remove,
            T[] add)
            /*-{
                var args = [index, remove];
                args.push.apply(args, add);
            
                return array.splice.apply(array, args);
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

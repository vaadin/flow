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

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;

/**
 * Native JS Set interface with an alternative implementation for JRE usage. Use
 * {@link JsCollections#set()} to create an appropriate instance.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <V>
 *            the value type
 */
@JsType(isNative = true, name = "Set", namespace = JsPackage.GLOBAL)
public class JsSet<V> {

    /**
     * Creates a new instance.
     */
    public JsSet() {
        // handled by GWT
    }

    /**
     * Adds a value to this set, overwriting any previous value if present.
     *
     * @param value
     *            the value to add
     * @return this set, for chaining.
     */
    public native JsSet<V> add(V value);

    /**
     * Checks whether this set contains the given value.
     *
     * @param value
     *            the value to check for
     * @return {@code true} if the value is in the set; {@code false} otherwise
     */
    public native boolean has(V value);

    /**
     * Removes the given value from the set.
     *
     * @param value
     *            the value to remove
     * @return {@code true} if the map contained the value prior to calling this
     *         method; {@code false} otherwise
     */
    public native boolean delete(V value);

    /**
     * Removes all values from this set.
     */
    public native void clear();

    /**
     * Invokes the provided callback for each value in this set.
     *
     * @param callback
     *            the callback to invoke for each value
     */
    public native void forEach(ForEachCallback<V> callback);

    /**
     * Gets the number of values in this set.
     *
     * @return the value count
     */
    @JsProperty(name = "size")
    public native int size();

    /**
     * Checks if the set is empty (size == 0).
     *
     * @return {@code true} if the set is empty, {@code false} otherwise
     */
    @JsOverlay
    public final boolean isEmpty() {
        return size() == 0;
    }

}

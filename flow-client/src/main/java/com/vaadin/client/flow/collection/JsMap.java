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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Native JS Map interface with an alternative implementation for JRE usage. Use
 * {@link JsCollections#map()} to create an appropriate instance.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 */
@JsType(isNative = true, name = "Map", namespace = JsPackage.GLOBAL)
public class JsMap<K, V> {

    /**
     * Functional interface for iterating all the entries in a {@link JsMap}.
     *
     * @param <K>
     *            the key type
     * @param <V>
     *            the value type
     */
    @FunctionalInterface
    @JsFunction
    public interface ForEachCallback<K, V> {
        /**
         * Receives one key-value pair.
         *
         * @param value
         *            the value
         * @param key
         *            the key
         */
        void accept(V value, K key);
    }

    /**
     * Creates a new instance.
     */
    public JsMap() {
        // handled by GWT
    }

    /**
     * Sets a value in this map, overwriting any previous mapping if present.
     *
     * @param key
     *            the key to set
     * @param value
     *            the value to set
     * @return this map, for chaining.
     */
    public native JsMap<K, V> set(K key, V value);

    /**
     * Gets the value mapped for the given key. Returns <code>null</code> if
     * there is no mapping or if the key is explicitly mapped to
     * <code>null</code>.
     *
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the given key; or <code>null</code>
     *         there is no mapping.
     */
    public native V get(K key);

    /**
     * Checks whether this map contains a mapping for the given key.
     *
     * @param key
     *            the key to check
     * @return {@code true} if there is a mapping for the key; {@code false} if
     *         there is no mapping
     */
    public native boolean has(K key);

    /**
     * Removes the mapping for a given key.
     *
     * @param key
     *            the key for which to remove the mapping
     * @return {@code true} if the map contained a mapping for the given key
     *         prior to calling this method; {@code false} if no mapping was
     *         present
     */
    public native boolean delete(K key);

    /**
     * Removes all mappings from this map.
     */
    public native void clear();

    /**
     * Invokes the provided callback for each mapping in this map.
     * <p>
     * Note that this is the only way of iteration supported in IE11.
     *
     * @param callback
     *            the callback to invoke for each mapping
     */
    public native void forEach(ForEachCallback<K, V> callback);

    /**
     * Gets the number of entries in this map.
     *
     * @return the entry count
     */
    @JsProperty(name = "size")
    public native int size();

    /**
     * Checks if the map is empty (size == 0).
     *
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    @JsOverlay
    public final boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns an array of the values in this {@link JsMap}.
     *
     * @return an array of the values in the map
     */
    @JsOverlay
    @SuppressWarnings("unchecked")
    public final JsArray<V> mapValues() {
        JsArray<V> result = JsCollections.array();
        forEach((value, key) -> result.push(value));

        return result;
    }

}

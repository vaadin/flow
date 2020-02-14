/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import jsinterop.annotations.JsType;

/**
 * Native JS WeakMap interface with an alternative implementation for JRE usage.
 * Use {@link JsCollections#weakMap()} to create an appropriate instance.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 */
@JsType(isNative = true)
public interface JsWeakMap<K, V> {
    /**
     * Sets a value in this map, overwriting any previous mapping if present.
     *
     * @param key
     *            the key to set
     * @param value
     *            the value to set
     * @return this map, for chaining.
     */
    JsWeakMap<K, V> set(K key, V value);

    /**
     * Gets the value mapped for the given key. Returns <code>null</code> if
     * there is no mapping, if the key is explicitly mapped to <code>null</code>
     * or if the key has been garbage collected.
     *
     * @param key
     *            the key to get a value for
     * @return the value corresponding to the given key; or <code>null</code>
     *         there is no mapping.
     */
    V get(K key);

    /**
     * Checks whether this map contains a mapping for the given key.
     *
     * @param key
     *            the key to check
     * @return <code>true</code> if there is a mapping for the key;
     *         <code>false</code> if there is no mapping
     */
    boolean has(K key);

    /**
     * Removes the mapping for a given key.
     *
     * @param key
     *            the key for which to remove the mapping
     * @return <code>true</code> if the map contained a mapping for the given
     *         key prior to calling this method; <code>false</code> if no
     *         mapping was present
     */
    boolean delete(K key);
}

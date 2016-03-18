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

import com.vaadin.client.hummingbird.collection.JsCollections.ForEachCallback;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Native JS Set interface with an alternative implementation for JRE usage. Use
 * {@link JsCollections#set()} to create an appropriate instance.
 *
 * @since
 * @author Vaadin Ltd
 * @param <V>
 *            the value type
 */
@JsType(isNative = true)
public interface JsSet<V> {

    /**
     * Adds a value to this set, overwriting any previous value if present.
     *
     * @param value
     *            the value to add
     * @return this set, for chaining.
     */
    JsSet<V> add(V value);

    /**
     * Checks whether this set contains the given value.
     *
     * @param value
     *            the value to check for
     * @return <code>true</code> if the value is in the set; <code>false</code>
     *         otherwise
     */
    boolean has(V value);

    /**
     * Removes the given value from the set.
     *
     * @param value
     *            the value to remove
     * @return <code>true</code> if the map contained the value prior to calling
     *         this method; <code>false</code> otherwise
     */
    boolean delete(V value);

    /**
     * Removes all vaues from this set.
     */
    void clear();

    /**
     * Invokes the provided callback for each value in this set.
     *
     * @param callback
     *            the callback to invoke for each value
     */
    void forEach(ForEachCallback<V> callback);

    /**
     * Gets the number of values in this set.
     *
     * @return the value count
     */
    @JsProperty(name = "size")
    int size();

}

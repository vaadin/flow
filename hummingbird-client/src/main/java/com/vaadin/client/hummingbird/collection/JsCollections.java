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
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.collection.jre.JreJsArray;
import com.vaadin.client.hummingbird.collection.jre.JreJsMap;
import com.vaadin.client.hummingbird.collection.jre.JreJsSet;

/**
 * Factory for JavaScript collection implementations with support for
 * alternative JRE implementations.
 *
 * @since
 * @author Vaadin Ltd
 */
public class JsCollections {
    private JsCollections() {
        // Only static stuff here, should never be instantiated
    }

    /**
     * Creates a new JavaScript Array.
     *
     * @return a new JS array instance
     */
    public static <T> JsArray<T> array() {
        if (GWT.isScript()) {
            return createNativeArray();
        } else {
            return new JreJsArray<>();
        }
    }

    /**
     * Creates a new JavaScript Map.
     *
     * @return a new JS map instance
     */
    public static <K, V> JsMap<K, V> map() {
        if (GWT.isScript()) {
            return createNativeMap();
        } else {
            return new JreJsMap<>();
        }
    }

    /**
     * Creates a new JavaScript Set.
     *
     * @return a new JS Set instance
     */
    public static <V> JsSet<V> set() {
        if (GWT.isScript()) {
            return createNativeSet();
        } else {
            return new JreJsSet<>();
        }
    }

    // TODO Make non static and move to JsMap so it is easier to use
    /**
     * Returns an array of the values in a {@link JsMap}.
     *
     * @param map
     *            the source map
     * @return an array of the values in the map
     */
    public static <K, V> JsArray<V> mapValues(JsMap<K, V> map) {
        JsArray<V> result = JsCollections.array();

        map.forEach(new ForEachCallback<K, V>() {
            @Override
            public void accept(V value, K key) {
                result.push(value);
            }
        });

        return result;
    }

    private static native <T> JsArray<T> createNativeArray()
    /*-{
        return [];
    }-*/;

    private static native <K, V> JsMap<K, V> createNativeMap()
    /*-{
        return new $wnd.Map();
    }-*/;

    private static native <V> JsSet<V> createNativeSet()
    /*-{
        return new $wnd.Set();
    }-*/;

    /**
     * Checks if the given map is empty, i.e. has no mappings.
     *
     * @param map
     *            the map to check
     * @return <code>true</code> if the map is empty, false otherwise
     */
    public static <K, V> boolean isEmpty(JsMap<K, V> map) {
        return map.size() == 0;
    }

    /**
     * Checks if the given set is empty.
     *
     * @param set
     *            the set to check
     * @return <code>true</code> if the set is empty, false otherwise
     */
    public static <V> boolean isEmpty(JsSet<V> set) {
        return set.size() == 0;
    }

}

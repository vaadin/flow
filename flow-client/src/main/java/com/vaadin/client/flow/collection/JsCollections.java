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

import jsinterop.annotations.JsFunction;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Factory for JavaScript collection implementations.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JsCollections {

    /**
     * Functional interface for iterating all the entries in a {@link JsSet} or
     * {@link JsArray}.
     */
    @FunctionalInterface
    @JsFunction
    public interface ForEachCallback<V> {
        void accept(V value);
    }

    private JsCollections() {
        // Only static stuff here, should never be instantiated
    }

    /**
     * Creates a new JavaScript Array.
     */
    public static <T> JsArray<T> array() {
        return asArray(JavaScriptObject.createArray());
    }

    /**
     * Creates a new JavaScript Array with the given contents.
     */
    @SafeVarargs
    public static <T> JsArray<T> array(T... values) {
        return asArray(values);
    }

    /**
     * Creates a new JavaScript Map.
     */
    public static <K, V> JsMap<K, V> map() {
        return new JsMap<>();
    }

    /**
     * Creates a new JavaScript WeakMap.
     */
    public static <K, V> JsWeakMap<K, V> weakMap() {
        return createNativeWeakMap();
    }

    /**
     * Creates a new empty JavaScript Set.
     */
    public static <V> JsSet<V> set() {
        return new JsSet<>();
    }

    /**
     * Creates a new JavaScript Set with the same contents as another set.
     */
    public static <T> JsSet<T> set(JsSet<T> values) {
        JsSet<T> newSet = new JsSet<>();
        values.forEach(newSet::add);
        return newSet;
    }

    private static native <T> JsArray<T> asArray(Object values)
    /*-{
        return values;
    }-*/;

    private static native <K, V> JsWeakMap<K, V> createNativeWeakMap()
    /*-{
        return new $wnd.WeakMap();
    }-*/;
}

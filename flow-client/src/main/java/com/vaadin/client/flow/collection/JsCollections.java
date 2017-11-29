/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import com.vaadin.client.flow.collection.jre.JreJsArray;
import com.vaadin.client.flow.collection.jre.JreJsMap;
import com.vaadin.client.flow.collection.jre.JreJsSet;
import com.vaadin.client.flow.collection.jre.JreJsWeakMap;

/**
 * Factory for JavaScript collection implementations with support for
 * alternative JRE implementations.
 *
 * @author Vaadin Ltd
 */
@SuppressWarnings("deprecation")
public class JsCollections {

    /**
     * Functional interface for iterating all the entries in a {@link JsSet} or
     * {@link JsArray}.
     *
     * @param <V>
     *            the value type
     */
    @FunctionalInterface
    @JsFunction
    public interface ForEachCallback<V> {
        /**
         * Receives one value.
         *
         * @param value
         *            the value
         */
        void accept(V value);
    }

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
            return asArray(JavaScriptObject.createArray());
        } else {
            return new JreJsArray<>();
        }
    }

    /**
     * Creates a new JavaScript Array with the given contents.
     *
     * @param values
     *            the values of the new array
     *
     * @return a new JS array instance
     */
    @SafeVarargs
    public static <T> JsArray<T> array(T... values) {
        if (GWT.isScript()) {
            return asArray(values);
        } else {
            return new JreJsArray<>(values);
        }
    }

    /**
     * Creates a new JavaScript Map.
     *
     * @return a new JS map instance
     */
    public static <K, V> JsMap<K, V> map() {
        if (GWT.isScript()) {
            checkJunitPolyfillStatus();
            return new JsMap<>();
        } else {
            return new JreJsMap<>();
        }
    }

    /**
     * Creates a new JavaScript WeakMap.
     *
     * @return a new JS weak map instance
     */
    public static <K, V> JsWeakMap<K, V> weakMap() {
        if (GWT.isScript()) {
            checkJunitPolyfillStatus();
            return createNativeWeakMap();
        } else {
            return new JreJsWeakMap<>();
        }
    }

    /**
     * Creates a new empty JavaScript Set.
     *
     * @return a new empty JS Set instance
     */
    public static <V> JsSet<V> set() {
        if (GWT.isScript()) {
            checkJunitPolyfillStatus();
            return new JsSet<>();
        } else {
            return new JreJsSet<>();
        }
    }

    /**
     * Creates a new JavaScript Set with the same contents as another set.
     *
     * @param values
     *            a set of values to add to the new set
     * @return a new JS Set with the provided contents
     */
    public static <T> JsSet<T> set(JsSet<T> values) {
        if (GWT.isScript()) {
            checkJunitPolyfillStatus();
            return new JsSet<>(values);
        }
        return new JreJsSet<>((JreJsSet<T>) values);
    }

    /**
     * Returns an array of the values in a {@link JsMap}.
     *
     * @param map
     *            the source map
     * @return an array of the values in the map
     * 
     * @deprecated please use {@link JsMap#mapValues()} instead
     */
    @Deprecated
    public static <K, V> JsArray<V> mapValues(JsMap<K, V> map) {
        return map.mapValues();
    }

    private static native <T> JsArray<T> asArray(Object values)
    /*-{
        return values;
    }-*/;

    private static native <K, V> JsWeakMap<K, V> createNativeWeakMap()
    /*-{
        return new $wnd.WeakMap();
    }-*/;

    /**
     * Checks if the given map is empty, i.e. has no mappings.
     *
     * @param map
     *            the map to check
     * @return {@code true} if the map is empty, {@code false} otherwise
     * 
     * @deprecated please use {@link JsMap#isEmpty()} instead
     */
    @Deprecated
    public static <K, V> boolean isEmpty(JsMap<K, V> map) {
        return map.size() == 0;
    }

    /**
     * Checks if the given set is empty.
     *
     * @param set
     *            the set to check
     * @return {@code true} if the set is empty, {@code false} otherwise
     *
     * @deprecated please use {@link JsSet#isEmpty()} instead
     */
    @Deprecated
    public static <V> boolean isEmpty(JsSet<V> set) {
        return set.size() == 0;
    }

    private static void checkJunitPolyfillStatus() {
        // "emulated" for JUnit compiles, "native" for normal compiles
        // Inlined by the compiler -> method is a no-op for normal compiles
        if (!"emulated".equals(System.getProperty("compiler.stackMode"))) {
            return;
        }

        // Discover if emulated stack mode is used outside JUnit compilations
        if (!GWT.getModuleName().endsWith(".JUnit")) {
            throw new IllegalStateException(
                    "This is not a JUnit compilation even though compiler.stackMode check has the expected value");
        }

        assert !isNativeMapConstructor() : "ES6 collection cannot be created before gwtSetUp has set up the polyfill";
    }

    private static native boolean isNativeMapConstructor()
    /*-{
      // Assuming it's a native implementation if string representation contains "[native"
      return ('' + $wnd.Map).indexOf('[native') != -1;
    }-*/;
}

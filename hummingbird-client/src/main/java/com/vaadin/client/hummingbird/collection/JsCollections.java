/*
 * Copyright 2000-2014 Vaadin Ltd.
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
import com.vaadin.client.hummingbird.collection.jre.JreJsMap;

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

    private static native <T> JsArray<T> createNativeArray()
    /*-{
        return [];
    }-*/;

    private static native <K, V> JsMap<K, V> createNativeMap()
    /*-{
        return new $wnd.Map();
    }-*/;
}

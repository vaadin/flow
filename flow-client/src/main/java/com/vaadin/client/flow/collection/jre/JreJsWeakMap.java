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

package com.vaadin.client.flow.collection.jre;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsWeakMap;

/**
 * JRE implementation of {@link JsMap}, should only be used for testing.
 *
 *
 * @deprecated Should only be used for testing.
 * @author Vaadin Ltd
 * @since 1.0
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 */
@Deprecated
public class JreJsWeakMap<K, V> implements JsWeakMap<K, V> {
    /*
     * Using an ordinary HashMap here since WeakHashMap would make the GWT
     * compiler upset. The difference can't be observed since there are no way
     * of iterating the contents of a WeakMap or checking its size. Leaking
     * memory is no issue since the JRE version is only used for testing.
     */
    private Map<K, V> values = new HashMap<>();

    @Override
    public JsWeakMap<K, V> set(K key, V value) {
        /*
         * The native JavaScript implementation requires the keys to be
         * "typeof object".
         *
         * We test for the most common offending types here to help the
         * developer notice problems as early as possible.
         */
        if (key instanceof String || key instanceof Boolean
                || key instanceof Double) {
            throw new IllegalArgumentException("Key must be a JS object type");
        }

        values.put(key, value);
        return this;
    }

    @Override
    public V get(K key) {
        return values.get(key);
    }

    @Override
    public boolean has(K key) {
        return values.containsKey(key);
    }

    @Override
    public boolean delete(K key) {
        boolean contained = values.containsKey(key);
        values.remove(key);
        return contained;
    }
}

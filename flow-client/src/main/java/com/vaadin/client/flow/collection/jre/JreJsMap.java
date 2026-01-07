/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.client.flow.collection.JsMap;

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
public class JreJsMap<K, V> extends JsMap<K, V> {
    private Map<K, V> values = new LinkedHashMap<>();

    @Override
    public JsMap<K, V> set(K key, V value) {
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

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public void forEach(JsMap.ForEachCallback<K, V> callback) {
        // Can't use values.forEach because of GWT
        for (Entry<K, V> entry : values.entrySet()) {
            callback.accept(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public int size() {
        return values.size();
    }
}

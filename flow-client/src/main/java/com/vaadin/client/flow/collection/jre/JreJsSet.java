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

package com.vaadin.client.flow.collection.jre;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.client.flow.collection.JsCollections.ForEachCallback;
import com.vaadin.client.flow.collection.JsSet;

/**
 * JRE implementation of {@link JsSet}, should only be used for testing.
 *
 *
 * @deprecated Should only be used for testing.
 * @author Vaadin Ltd
 * @since 1.0
 * @param <V>
 *            the value type
 */
@Deprecated
public class JreJsSet<V> extends JsSet<V> {
    private final Set<V> values = new HashSet<>();

    /**
     * Creates a new empty JRE Set.
     */
    public JreJsSet() {
        // Nothing to do
    }

    @Override
    public JsSet<V> add(V value) {
        values.add(value);
        return this;
    }

    @Override
    public boolean has(V value) {
        return values.contains(value);
    }

    @Override
    public boolean delete(V value) {
        boolean contained = values.contains(value);
        values.remove(value);
        return contained;
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public void forEach(ForEachCallback<V> callback) {
        // Can't use values.forEach because of GWT
        for (V value : values) {
            callback.accept(value);
        }
    }

    @Override
    public int size() {
        return values.size();
    }
}

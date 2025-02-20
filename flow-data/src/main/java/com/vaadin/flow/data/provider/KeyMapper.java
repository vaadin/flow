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

package com.vaadin.flow.data.provider;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.function.ValueProvider;

/**
 * <code>KeyMapper</code> is the simple two-way map for generating textual keys
 * for objects and retrieving the objects later with the key.
 *
 * @param <V>
 *            the type of mapped objects
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class KeyMapper<V> implements DataKeyMapper<V> {

    private int lastKey = 0;

    private final HashMap<Object, String> objectIdKeyMap = new HashMap<>();

    private final HashMap<String, V> keyObjectMap = new HashMap<>();

    private ValueProvider<V, Object> identifierGetter;

    /**
     * Constructs a new mapper.
     *
     * @param identifierGetter
     *            has to return a unique key for every bean, and the returned
     *            key has to follow general {@code hashCode()} and
     *            {@code equals()} contract, see {@link Object#hashCode()} for
     *            details.
     */
    public KeyMapper(ValueProvider<V, Object> identifierGetter) {
        this.identifierGetter = identifierGetter;
    }

    /**
     * Constructs a new mapper with trivial {@code identifierGetter}
     */
    public KeyMapper() {
        this(v -> v);
    }

    /**
     * Gets key for an object.
     *
     * @param o
     *            the object.
     */
    @Override
    public String key(V o) {

        if (o == null) {
            return "null";
        }

        // If the object is already mapped, use existing key
        Object id = identifierGetter.apply(o);
        String key = objectIdKeyMap.get(id);
        if (key != null) {
            return key;
        }

        // If the object is not yet mapped, map it
        key = createKey();
        objectIdKeyMap.put(id, key);
        keyObjectMap.put(key, o);

        return key;
    }

    /**
     * Creates a key for a new item.
     *
     * This method can be overridden to customize the keys used.
     *
     * @return new key
     */
    protected String createKey() {
        return String.valueOf(++lastKey);
    }

    @Override
    public boolean has(V o) {
        return objectIdKeyMap.containsKey(identifierGetter.apply(o));
    }

    /**
     * Retrieves object with the key.
     *
     * @param key
     *            the name with the desired value.
     * @return the object with the key.
     */
    @Override
    public V get(String key) {
        return keyObjectMap.get(key);
    }

    /**
     * Removes object from the mapper.
     *
     * @param removeobj
     *            the object to be removed.
     */
    @Override
    public void remove(V removeobj) {
        final String key = objectIdKeyMap
                .remove(identifierGetter.apply(removeobj));
        if (key != null) {
            keyObjectMap.remove(key);
        }
    }

    /**
     * Removes all objects from the mapper.
     */
    @Override
    public void removeAll() {
        objectIdKeyMap.clear();
        keyObjectMap.clear();
    }

    /**
     * Checks if the given key is mapped to an object.
     *
     * @param key
     *            the key to check
     * @return <code>true</code> if the key is currently mapped,
     *         <code>false</code> otherwise
     */
    public boolean containsKey(String key) {
        return keyObjectMap.containsKey(key);
    }

    @Override
    public void refresh(V dataObject) {
        Object id = identifierGetter.apply(dataObject);
        String key = objectIdKeyMap.get(id);
        if (key != null) {
            keyObjectMap.put(key, dataObject);
        }
    }

    @Override
    public void setIdentifierGetter(ValueProvider<V, Object> identifierGetter) {
        if (this.identifierGetter != identifierGetter) {
            this.identifierGetter = identifierGetter;
            objectIdKeyMap.clear();
            for (Map.Entry<String, V> entry : keyObjectMap.entrySet()) {
                objectIdKeyMap.put(identifierGetter.apply(entry.getValue()),
                        entry.getKey());
            }
        }
    }
}

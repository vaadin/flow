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
package com.vaadin.flow.internal;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A thread-safe cache for the result of doing some reflection lookup based on a
 * class. Cached values never expire since it's assumed that the there is a
 * finite number of classes for which reflection results are used.
 *
 * @author Vaadin Ltd
 * @param <C>
 *            the class types that are used as the cache keys
 * @param <T>
 *            the cached value type
 */
public class ReflectionCache<C, T> {
    private static final Set<ReflectionCache<?, ?>> caches = Collections
            .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final ConcurrentHashMap<Class<? extends C>, T> values = new ConcurrentHashMap<>();

    private final Function<? extends Class<C>, T> valueProvider;

    /**
     * Creates a new reflection cache with the given value provider. The value
     * provider will be used to produce a new cached value whenever there is a
     * cache miss.
     *
     * @param valueProvider
     *            a function that computes the cached value for a class, not
     *            <code>null</code>
     */
    public ReflectionCache(Function<? extends Class<C>, T> valueProvider) {
        if (valueProvider == null) {
            throw new IllegalArgumentException("value provider cannot be null");
        }
        this.valueProvider = valueProvider;

        caches.add(this);
    }

    /**
     * Gets a cached value. If this cache does not contain a value for the key,
     * the value is computed using the configured value provider and the cache
     * is populated with the new value.
     *
     * @param type
     *            the type for which to get reflection results
     * @return the reflection results
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public T get(Class<? extends C> type) {
        /*
         * Raw cast since the mapping function is declared to accept <? super
         * K>, which in this case becomes <? super Class<?>>, which isn't
         * compatible with Class<C>.
         */
        Object value = values.computeIfAbsent(type, (Function) valueProvider);
        // Explicit cast since javac doesn't agree with ecj
        return (T) value;
    }

    /**
     * Checks whether this cache contains an entry for the given type.
     *
     * @param type
     *            the type to check for
     * @return <code>true</code> if there is a mapping for the type,
     *         <code>false</code> if there is no mapping
     */
    public boolean contains(Class<? extends C> type) {
        return values.containsKey(type);
    }

    /**
     * Removes all mappings from this cache.
     */
    public void clear() {
        values.clear();
    }

    /**
     * Clears all mappings from all reflection caches.
     */
    public static void clearAll() {
        caches.forEach(ReflectionCache::clear);
    }
}

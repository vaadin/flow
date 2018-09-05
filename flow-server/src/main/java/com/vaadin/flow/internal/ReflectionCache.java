/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;

/**
 * A thread-safe cache for the result of doing some reflection lookup based on a
 * class. Cached values never expire since it's assumed that the there is a
 * finite number of classes for which reflection results are used.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <C>
 *            the class types that are used as the cache keys
 * @param <T>
 *            the cached value type
 */
public class ReflectionCache<C, T> {
    private static final Set<Runnable> clearAllActions = Collections
            .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final ConcurrentHashMap<Class<? extends C>, T> values = new ConcurrentHashMap<>();

    private final SerializableFunction<Class<? extends C>, T> valueProvider;

    /**
     * Creates a new reflection cache with the given value provider. The value
     * provider will be used to produce a new cached value whenever there is a
     * cache miss. It will be run in a context where no {@link CurrentInstance}
     * is available to prevent accidentally caching values that are computed
     * differently depending on external circumstances.
     *
     * @param valueProvider
     *            a function that computes the cached value for a class, not
     *            <code>null</code>
     */
    public ReflectionCache(SerializableFunction<Class<C>, T> valueProvider) {
        if (valueProvider == null) {
            throw new IllegalArgumentException("value provider cannot be null");
        }
        this.valueProvider = wrapValueProvider(valueProvider);

        addClearAllAction(this::clear);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <C, T> SerializableFunction<Class<? extends C>, T> wrapValueProvider(
            SerializableFunction<Class<C>, T> valueProvider) {
        return type -> {
            Map<Class<?>, CurrentInstance> instances = CurrentInstance
                    .getInstances();
            try {
                CurrentInstance.clearAll();

                /*
                 * Raw cast to deal with weird generics of valueProvider which
                 * in turn is there to deal with the fact that javac in some
                 * cases cannot infer type parameters for Foo::new as a
                 * Function<Class<? extends C>, T> even when Foo has a
                 * constructor that takes Class<? extends Something>.
                 */
                return (T) ((Function) valueProvider).apply(type);
            } finally {
                CurrentInstance.restoreInstances(instances);
            }
        };
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
    public T get(Class<? extends C> type) {
        return values.computeIfAbsent(type, valueProvider);
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
     * Adds an action that will be run when all reflection caches are cleared.
     * <p>
     * The actions are held with a weak reference, which typically means that
     * the action will be ignored if the returned registration is garbage
     * collected.
     *
     * @see #clearAll()
     *
     * @param action
     *            the action to run
     * @return a registration for removing the action
     */
    public static Registration addClearAllAction(Runnable action) {
        clearAllActions.add(action);
        return () -> clearAllActions.remove(action);
    }

    /**
     * Clears all mappings from all reflection caches and related resources.
     */
    public static void clearAll() {
        clearAllActions.forEach(Runnable::run);
    }
}

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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.vaadin.flow.component.ComponentEvent;

/**
 * Cache for tracking global information related to {@link ComponentEvent}
 * types.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class EventDataCache implements Serializable {

    private ConcurrentHashMap<Class<? extends ComponentEvent<?>>, LinkedHashMap<String, Class<?>>> dataExpressions = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class<? extends ComponentEvent<?>>, Constructor<?>> eventConstructors = new ConcurrentHashMap<>();

    /**
     * Gets the cached data expressions for the given event type.
     *
     * @param eventType
     *            the component event type
     * @return an optional containing the data expressions or an empty optional
     *         if no data expressions have been cached for the given event type
     */
    public Optional<LinkedHashMap<String, Class<?>>> getDataExpressions(
            Class<? extends ComponentEvent<?>> eventType) {
        return Optional.ofNullable(dataExpressions.get(eventType));
    }

    /**
     * Stores the given data expressions for the given event type in the cache.
     *
     * @param eventType
     *            the component event type
     * @param expressions
     *            the data expressions to store
     * @return the stored data expressions
     */
    public LinkedHashMap<String, Class<?>> setDataExpressions(
            Class<? extends ComponentEvent<?>> eventType,
            LinkedHashMap<String, Class<?>> expressions) {
        dataExpressions.put(eventType, expressions);
        return expressions;
    }

    /**
     * Gets the cached DOM event constructor for the given event type.
     *
     * @param <T>
     *            the component event type
     * @param eventType
     *            the component event type
     * @return an optional containing the DOM event constructor or an empty
     *         optional if no DOM event constructor has been cached for the
     *         given event type
     */
    @SuppressWarnings("unchecked")
    public <T extends ComponentEvent<?>> Optional<Constructor<T>> getEventConstructor(
            Class<T> eventType) {
        return Optional
                .ofNullable((Constructor<T>) eventConstructors.get(eventType));
    }

    /**
     * Stores the given DOM event constructor for the given event type in the
     * cache.
     *
     * @param eventType
     *            the component event type
     * @param constructor
     *            the DOM event constructor to store
     * @param <T>
     *            the event type
     * @return the stored DOM event constructor
     */
    public <T extends ComponentEvent<?>> Constructor<T> setEventConstructor(
            Class<T> eventType, Constructor<T> constructor) {
        eventConstructors.put(eventType, constructor);
        return constructor;
    }

    /**
     * Removes any cached values.
     * <p>
     * Internal method for testing only.
     */
    void clear() {
        dataExpressions.clear();
        eventConstructors.clear();
    }
}

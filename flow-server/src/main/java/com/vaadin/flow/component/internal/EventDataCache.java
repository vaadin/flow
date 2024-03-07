/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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

/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import com.vaadin.flow.function.ValueProvider;

/**
 * A callback interface that is used to provide the identifier of an item.
 *
 * @param <T>
 *            the type of the item
 * @since
 */
@FunctionalInterface
public interface IdentifierProvider<T> extends ValueProvider<T, Object> {
    /**
     * Returns an identifier provider that always returns its input argument.
     *
     * @param <T>
     *            the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> IdentifierProvider<T> identity() {
        return t -> t;
    }
}

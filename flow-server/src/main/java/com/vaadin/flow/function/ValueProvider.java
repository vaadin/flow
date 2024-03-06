/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.function;

/**
 * A callback interface for providing values from a given source.
 * <p>
 * For example this interface can be implemented to simply extract a value with
 * a getter, or to create a composite value based on the fields of the source
 * object.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the object used to provide the value
 * @param <TARGET>
 *            the type of the provided value
 */
@FunctionalInterface
public interface ValueProvider<SOURCE, TARGET>
        extends SerializableFunction<SOURCE, TARGET> {

    /**
     * Returns a value provider that always returns its input argument.
     *
     * @param <T>
     *            the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> ValueProvider<T, T> identity() {
        return t -> t;
    }

    /**
     * Provides a value from the given source object.
     *
     * @param source
     *            the source to retrieve the value from
     * @return the value provided by the source
     */
    @Override
    TARGET apply(SOURCE source);
}

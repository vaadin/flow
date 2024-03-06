/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.function;

import java.io.Serializable;

/**
 * Like {@link SerializableBiConsumer}, but with three arguments.
 *
 * @param <T>
 *            the type of the first argument to the operation
 * @param <U>
 *            the type of the second argument to the operation
 * @param <V>
 *            the type of the third argument to the operation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface SerializableTriConsumer<T, U, V> extends Serializable {
    /**
     * Performs the action.
     *
     * @param t
     *            the first argument
     * @param u
     *            the second argument
     * @param v
     *            the third argument
     */
    void accept(T t, U u, V v);
}

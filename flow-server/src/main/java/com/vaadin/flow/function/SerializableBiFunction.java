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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link BiFunction} that is also {@link Serializable}.
 *
 * @see Function
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the first function parameter
 * @param <U>
 *            the type of the second function parameter
 * @param <R>
 *            the type of the result of the function
 */
@FunctionalInterface
public interface SerializableBiFunction<T, U, R>
        extends BiFunction<T, U, R>, Serializable {
    // Only method inherited from BiFunction
}

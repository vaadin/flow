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
import java.util.function.Supplier;

/**
 * A {@link Supplier} that is also {@link Serializable}.
 *
 * @see Supplier
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the input to the function
 */
@FunctionalInterface
public interface SerializableSupplier<T> extends Supplier<T>, Serializable {
    // Only method inherited from Supplier
}

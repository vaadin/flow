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
import java.util.function.BiPredicate;

/**
 * A {@link BiPredicate} that is also {@link Serializable}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the first input to the predicate
 * @param <U>
 *            the type of the second input to the predicate
 */
public interface SerializableBiPredicate<T, U>
        extends BiPredicate<T, U>, Serializable {
    // Only method inherited from BiPredicate
}

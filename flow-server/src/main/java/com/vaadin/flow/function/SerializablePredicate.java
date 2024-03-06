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
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A {@link Predicate} that is also {@link Serializable}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the type of the input to the predicate
 *
 */
public interface SerializablePredicate<T> extends Predicate<T>, Serializable {

    @Override
    default SerializablePredicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return t -> test(t) && other.test(t);
    }

    @Override
    default SerializablePredicate<T> negate() {
        return t -> !test(t);
    }

    @Override
    default SerializablePredicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return t -> test(t) || other.test(t);
    }
}

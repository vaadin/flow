/*
 * Copyright 2000-2025 Vaadin Ltd.
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

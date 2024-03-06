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
import java.util.function.Consumer;

/**
 * A {@link Consumer} that is also {@link Serializable}.
 *
 * @see Consumer
 * @param <T>
 *            the type of the first argument to the operation
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@FunctionalInterface
public interface SerializableConsumer<T> extends Consumer<T>, Serializable {
    // Only method inherited from Consumer
}

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
import java.util.function.BiConsumer;

/**
 * A {@link BiConsumer} that is also {@link Serializable}.
 *
 *
 * @param <T>
 *            the type of the first argument to the operation
 * @param <U>
 *            the type of the second argument to the operation
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @see BiConsumer
 */
public interface SerializableBiConsumer<T, U>
        extends BiConsumer<T, U>, Serializable {

}

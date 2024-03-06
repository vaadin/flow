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
import java.util.Comparator;

/**
 * A {@link Comparator} that is also {@link Serializable}.
 * <p>
 * You can create a serializable comparator from a regular comparator through a
 * method reference by appending <code>::compare</code>. For example
 * <code>SerializableComparator&lt;Employee&gt;
 * comparator = Comparator.comparing(Employee::getFirstName)::compare</code>.
 * The resulting comparator will in most cases cause exceptions if it is
 * actually being serialized, but this construct will enable using the
 * shorthands in {@link Comparator} in applications where session will not be
 * serialized.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of objects that may be compared by this comparator
 */
@FunctionalInterface
public interface SerializableComparator<T> extends Comparator<T>, Serializable {
    // Relevant methods inherited from Comparator
}

/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;

/**
 * Sorting information for one field.
 *
 * @param <T>
 *            the type of the sorting information, usually a String (field id)
 *            or a {@link java.util.Comparator}.
 * @since 1.0
 */
public class SortOrder<T> implements Serializable {

    private final T sorted;
    private final SortDirection direction;

    /**
     * Constructs a field sorting information.
     *
     * @param sorted
     *            sorting information, usually field id or
     *            {@link java.util.Comparator}
     * @param direction
     *            sorting direction
     */
    public SortOrder(T sorted, SortDirection direction) {
        this.sorted = sorted;
        this.direction = direction;
    }

    /**
     * Sorting information.
     *
     * @return sorting entity, usually field id or {@link java.util.Comparator}
     */
    public T getSorted() {
        return sorted;
    }

    /**
     * Sorting direction.
     *
     * @return sorting direction
     */
    public SortDirection getDirection() {
        return direction;
    }
}

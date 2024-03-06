/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

/**
 * Sorting information for {@link Query}.
 *
 * @see Query
 * @since 1.0
 */
public class QuerySortOrder extends SortOrder<String> {

    /**
     * Constructs sorting information for usage in a {@link Query}.
     *
     * @param sorted
     *            sorting information, usually field id
     * @param direction
     *            sorting direction
     */
    public QuerySortOrder(String sorted, SortDirection direction) {
        super(sorted, direction);
    }

    /**
     * Creates a new query sort builder with given sorting using ascending sort
     * direction.
     *
     * @param by
     *            the string to sort by
     *
     * @return the query sort builder
     */
    public static QuerySortOrderBuilder asc(String by) {
        return new QuerySortOrderBuilder().thenAsc(by);
    }

    /**
     * Creates a new query sort builder with given sorting using descending sort
     * direction.
     *
     * @param by
     *            the string to sort by
     *
     * @return the query sort builder
     */
    public static QuerySortOrderBuilder desc(String by) {
        return new QuerySortOrderBuilder().thenDesc(by);
    }
}

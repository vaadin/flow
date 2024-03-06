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
 * Helper classes with fluent API for constructing {@link QuerySortOrder} lists.
 * When the sort order is ready to be passed on, calling {@link #build()} will
 * create the list of sort orders.
 *
 * @see QuerySortOrder
 * @see #thenDesc(String)
 * @see #thenDesc(String)
 * @see #build()
 * @since 1.0
 */
public class QuerySortOrderBuilder
        extends SortOrderBuilder<QuerySortOrder, String> {

    @Override
    public QuerySortOrderBuilder thenAsc(String by) {
        return (QuerySortOrderBuilder) super.thenAsc(by);
    }

    @Override
    public QuerySortOrderBuilder thenDesc(String by) {
        return (QuerySortOrderBuilder) super.thenDesc(by);
    }

    @Override
    protected QuerySortOrder createSortOrder(String by,
            SortDirection direction) {
        return new QuerySortOrder(by, direction);
    }
}
